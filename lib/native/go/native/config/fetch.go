package config

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	U "net/url"
	"os"
	P "path"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"

	"cfa/native/app"

	clashHttp "github.com/metacubex/mihomo/component/http"
)

type Status struct {
	Action            string   `json:"action"`
	Args              []string `json:"args"`
	Progress          int      `json:"progress"`
	MaxProgress       int      `json:"max"`
	SubUpload         *int64   `json:"subUpload,omitempty"`
	SubDownload       *int64   `json:"subDownload,omitempty"`
	SubTotal          *int64   `json:"subTotal,omitempty"`
	SubExpire         *int64   `json:"subExpire,omitempty"`
	SubUpdateInterval *int64   `json:"subUpdateInterval,omitempty"`
	SubTitle          string   `json:"subTitle,omitempty"`
	SubFilename       string   `json:"subFilename,omitempty"`
}

type fetchHeader struct {
	SubscriptionUserInfo  string
	ProfileUpdateInterval string
	ProfileTitle          string
	SubscriptionTitle     string
	ContentDisposition    string
}

var (
	customUserAgent string
	userAgentMutex  sync.RWMutex

	filenameStarPattern = regexp.MustCompile(`(?i)filename\*=([^']*)'([^']*)'([^;]+)`)
	filenamePattern     = regexp.MustCompile(`(?i)filename=([^;]+)`)
)

func SetCustomUserAgent(ua string) {
	userAgentMutex.Lock()
	defer userAgentMutex.Unlock()
	customUserAgent = ua
}

func GetCustomUserAgent() string {
	userAgentMutex.RLock()
	defer userAgentMutex.RUnlock()
	if customUserAgent != "" {
		return customUserAgent
	}
	return "ClashMetaForAndroid/" + app.VersionName()
}

func openUrl(ctx context.Context, url string) (io.ReadCloser, fetchHeader, error) {
	response, err := clashHttp.HttpRequest(ctx, url, http.MethodGet, http.Header{"User-Agent": {GetCustomUserAgent()}}, nil)

	if err != nil {
		return nil, fetchHeader{}, err
	}

	return response.Body, fetchHeader{
		SubscriptionUserInfo:  response.Header.Get("subscription-userinfo"),
		ProfileUpdateInterval: response.Header.Get("profile-update-interval"),
		ProfileTitle:          response.Header.Get("profile-title"),
		SubscriptionTitle:     response.Header.Get("subscription-title"),
		ContentDisposition:    response.Header.Get("content-disposition"),
	}, nil
}

func openContent(url string) (io.ReadCloser, error) {
	return app.OpenContent(url)
}

func fetch(url *U.URL, file string) (fetchHeader, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	var reader io.ReadCloser
	var header fetchHeader
	var err error

	switch url.Scheme {
	case "http", "https":
		reader, header, err = openUrl(ctx, url.String())
	case "content":
		reader, err = openContent(url.String())
	default:
		err = fmt.Errorf("unsupported scheme %s of %s", url.Scheme, url)
	}

	if err != nil {
		return fetchHeader{}, err
	}

	defer reader.Close()

	_ = os.MkdirAll(P.Dir(file), 0700)

	f, err := os.OpenFile(file, os.O_WRONLY|os.O_TRUNC|os.O_CREATE, 0600)
	if err != nil {
		return fetchHeader{}, err
	}

	defer f.Close()

	_, err = io.Copy(f, reader)
	if err != nil {
		_ = os.Remove(file)
	}

	return header, err
}

func parseSubscriptionInteger(value string) int64 {
	value = strings.TrimSpace(value)
	if value == "" {
		return 0
	}
	digits := strings.Builder{}
	for _, r := range value {
		if r < '0' || r > '9' {
			break
		}
		digits.WriteRune(r)
	}
	if digits.Len() == 0 {
		value = strings.SplitN(value, ".", 2)[0]
	} else {
		value = digits.String()
	}
	parsed, err := strconv.ParseInt(value, 10, 64)
	if err != nil {
		return 0
	}
	return parsed
}

func parseProfileUpdateInterval(value string) (int64, bool) {
	hours, err := strconv.ParseInt(strings.TrimSpace(value), 10, 64)
	if err != nil {
		return 0, false
	}
	if hours <= 0 {
		return 0, true
	}
	interval := time.Duration(hours) * time.Hour
	if interval < 15*time.Minute {
		interval = 15 * time.Minute
	}
	return int64(interval / time.Millisecond), true
}

func decodeSubscriptionTitle(value string) string {
	value = strings.Trim(strings.TrimSpace(value), `"'`)
	if value == "" {
		return ""
	}

	decodeBase64 := func(encoded string) string {
		encoded = strings.Trim(strings.TrimSpace(encoded), `"'`)
		if encoded == "" {
			return ""
		}
		bytes, err := base64.StdEncoding.DecodeString(encoded)
		if err != nil {
			return ""
		}
		return strings.TrimSpace(string(bytes))
	}

	if strings.HasPrefix(strings.ToLower(value), "base64:") {
		if decoded := decodeBase64(value[len("base64:"):]); decoded != "" {
			return decoded
		}
		return value
	}

	if parts := strings.SplitN(value, "'", 3); len(parts) == 3 {
		if decoded, err := U.QueryUnescape(parts[2]); err == nil && strings.TrimSpace(decoded) != "" {
			return strings.TrimSpace(decoded)
		}
	}

	if decoded, err := U.QueryUnescape(value); err == nil && strings.TrimSpace(decoded) != "" {
		return strings.TrimSpace(decoded)
	}
	if decoded := decodeBase64(value); decoded != "" {
		return decoded
	}
	return value
}

func parseFilenameFromContentDisposition(value string) string {
	value = strings.TrimSpace(value)
	if value == "" {
		return ""
	}
	if match := filenameStarPattern.FindStringSubmatch(value); len(match) == 4 {
		filename := strings.Trim(match[3], ` "'`)
		if decoded, err := U.QueryUnescape(filename); err == nil && strings.TrimSpace(decoded) != "" {
			return strings.TrimSpace(decoded)
		}
		return filename
	}
	if match := filenamePattern.FindStringSubmatch(value); len(match) == 2 {
		return strings.Trim(match[1], ` "'`)
	}
	return ""
}

func reportSubscriptionInfo(header fetchHeader, reportStatus func(string)) {
	if header.SubscriptionUserInfo == "" && header.ProfileUpdateInterval == "" && header.ProfileTitle == "" && header.SubscriptionTitle == "" && header.ContentDisposition == "" {
		return
	}

	status := Status{
		Action:      "SubscriptionInfo",
		Args:        []string{},
		Progress:    -1,
		MaxProgress: -1,
	}

	if header.SubscriptionUserInfo != "" {
		for _, flag := range strings.Split(header.SubscriptionUserInfo, ";") {
			parts := strings.SplitN(flag, "=", 2)
			if len(parts) != 2 {
				continue
			}
			key := strings.ToLower(strings.TrimSpace(parts[0]))
			value := strings.TrimSpace(parts[1])
			switch {
			case strings.Contains(key, "upload"):
				v := parseSubscriptionInteger(value)
				status.SubUpload = &v
			case strings.Contains(key, "download"):
				v := parseSubscriptionInteger(value)
				status.SubDownload = &v
			case strings.Contains(key, "total"):
				v := parseSubscriptionInteger(value)
				status.SubTotal = &v
			case strings.Contains(key, "expire"):
				v := parseSubscriptionInteger(value) * 1000
				status.SubExpire = &v
			}
		}
	}

	if interval, ok := parseProfileUpdateInterval(header.ProfileUpdateInterval); ok {
		status.SubUpdateInterval = &interval
	}
	status.SubTitle = decodeSubscriptionTitle(firstNonEmpty(header.ProfileTitle, header.SubscriptionTitle))
	status.SubFilename = parseFilenameFromContentDisposition(header.ContentDisposition)

	bytes, _ := json.Marshal(&status)
	reportStatus(string(bytes))
}

func firstNonEmpty(values ...string) string {
	for _, value := range values {
		if strings.TrimSpace(value) != "" {
			return value
		}
	}
	return ""
}

func FetchAndValid(
	path string,
	url string,
	force bool,
	reportStatus func(string),
) error {
	configPath := P.Join(path, "config.yaml")

	if _, err := os.Stat(configPath); os.IsNotExist(err) || force {
		url, err := U.Parse(url)
		if err != nil {
			return err
		}

		bytes, _ := json.Marshal(&Status{
			Action:      "FetchConfiguration",
			Args:        []string{url.Host},
			Progress:    -1,
			MaxProgress: -1,
		})

		reportStatus(string(bytes))

		header, err := fetch(url, configPath)
		if err != nil {
			return err
		}
		reportSubscriptionInfo(header, reportStatus)
	}

	defer runtime.GC()

	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		return err
	}

	forEachProviders(rawCfg, func(index int, total int, name string, provider map[string]any, prefix string) {
		bytes, _ := json.Marshal(&Status{
			Action:      "FetchProviders",
			Args:        []string{name},
			Progress:    index,
			MaxProgress: total,
		})

		reportStatus(string(bytes))

		u, uok := provider["url"]
		p, pok := provider["path"]

		if !uok || !pok {
			return
		}

		us, uok := u.(string)
		ps, pok := p.(string)

		if !uok || !pok {
			return
		}

		if _, err := os.Stat(ps); err == nil {
			return
		}

		url, err := U.Parse(us)
		if err != nil {
			return
		}

		_, _ = fetch(url, ps)
	})

	bytes, _ := json.Marshal(&Status{
		Action:      "Verifying",
		Args:        []string{},
		Progress:    0xffff,
		MaxProgress: 0xffff,
	})

	reportStatus(string(bytes))

	return nil
}
