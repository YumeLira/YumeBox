package config

import (
	"errors"
	"fmt"
	"strings"

	"github.com/dlclark/regexp2"

	"cfa/native/common"

	"github.com/metacubex/mihomo/common/utils"
	"github.com/metacubex/mihomo/config"
	C "github.com/metacubex/mihomo/constant"
)

// processors 处理链
var processors = []processor{
	patchExternalController,
	patchGeneral,
	patchProfile,
	patchDns,
	patchTun,
	patchListeners,
	patchProviders,
	validConfig,
}

type processor func(cfg *config.RawConfig, profileDir string) error

func patchExternalController(cfg *config.RawConfig, _ string) error {
	// Preserve profile-defined external controller values.
	return nil
}

func patchGeneral(cfg *config.RawConfig, profileDir string) error {
	cfg.Interface = ""
	cfg.RoutingMark = 0
	if cfg.ExternalController != "" || cfg.ExternalControllerTLS != "" {
		cfg.ExternalUI = profileDir + "/ui"
	}

	return nil
}

func patchProfile(cfg *config.RawConfig, _ string) error {
	cfg.Profile.StoreSelected = true
	cfg.Profile.StoreFakeIP = true

	return nil
}

func patchDns(cfg *config.RawConfig, _ string) error {
	if !cfg.DNS.Enable {
		cfg.DNS = config.RawDNS{
			Enable:            true,
			UseHosts:          true,
			DefaultNameserver: defaultNameServers,
			NameServer:        defaultNameServers,
			EnhancedMode:      C.DNSFakeIP,
			FakeIPRange:       defaultFakeIPRange,
			FakeIPFilter:      defaultFakeIPFilter,
		}

		cfg.ClashForAndroid.AppendSystemDNS = true
	}

	if cfg.ClashForAndroid.AppendSystemDNS {
		cfg.DNS.NameServer = append(cfg.DNS.NameServer, "system://")
	}

	return nil
}

func patchTun(cfg *config.RawConfig, _ string) error {
	cfg.Tun.Enable = false
	cfg.Tun.AutoRoute = false
	cfg.Tun.AutoDetectInterface = false
	return nil
}

func patchListeners(cfg *config.RawConfig, _ string) error {
	newListeners := make([]map[string]any, 0, len(cfg.Listeners))
	for _, mapping := range cfg.Listeners {
		if proxyType, existType := mapping["type"].(string); existType {
			switch proxyType {
			case "tproxy", "redir", "tun":
				continue // remove those listeners which is not supported
			}
		}
		newListeners = append(newListeners, mapping)
	}
	cfg.Listeners = newListeners
	return nil
}

func patchProviders(cfg *config.RawConfig, profileDir string) error {
	forEachProviders(cfg, func(index int, total int, key string, provider map[string]any, prefix string) {
		path, _ := provider["path"].(string)
		extension := providerExtension(provider, prefix)
		if strings.TrimSpace(path) != "" {
			path = normalizeProviderPath(path, profileDir, prefix, extension)
		} else if url, ok := provider["url"].(string); ok {
			path = profileProviderPath(profileDir, prefix, ensureProviderExtension(utils.MakeHash([]byte(url)).String(), extension))
		} else {
			return // both path and url is empty, WTF???
		}
		provider["path"] = path
	})

	return nil
}

func providerExtension(provider map[string]any, prefix string) string {
	if prefix == RULES {
		if format, ok := provider["format"].(string); ok && strings.EqualFold(format, "mrs") {
			return "mrs"
		}
	}
	return "yaml"
}

func normalizeProviderPath(path string, profileDir string, prefix string, extension string) string {
	normalized := strings.ReplaceAll(strings.TrimSpace(path), "\\", "/")
	base := profileProviderBase(profileDir, prefix)
	if strings.HasPrefix(normalized, base+"/") {
		return normalized
	}

	if strings.HasPrefix(normalized, "/") {
		normalized = baseName(normalized)
	} else {
		normalized = trimProviderPrefix(common.ResolveAsRoot(normalized))
	}

	return profileProviderPath(profileDir, prefix, ensureProviderExtension(normalized, extension))
}

func profileProviderBase(profileDir string, prefix string) string {
	return profileDir + "/providers/" + prefix
}

func profileProviderPath(profileDir string, prefix string, relative string) string {
	tail := strings.TrimPrefix(strings.ReplaceAll(relative, "\\", "/"), "/")
	if tail == "" {
		tail = "provider.yaml"
	}
	return profileProviderBase(profileDir, prefix) + "/" + tail
}

func trimProviderPrefix(path string) string {
	current := path
	for {
		parts := strings.SplitN(current, "/", 2)
		head := parts[0]
		if head != "providers" && head != "provider" && head != "clash" &&
			head != "ruleset" && head != "rules" && head != "proxies" {
			break
		}
		if len(parts) == 1 {
			current = ""
			break
		}
		current = parts[1]
	}
	return current
}

func ensureProviderExtension(path string, extension string) string {
	trimmed := strings.TrimSpace(path)
	if trimmed == "" {
		return "provider." + extension
	}
	last := baseName(trimmed)
	if strings.Contains(last, ".") {
		return trimmed
	}
	return trimmed + "." + extension
}

func baseName(path string) string {
	trimmed := strings.Trim(strings.ReplaceAll(path, "\\", "/"), "/")
	if trimmed == "" {
		return ""
	}
	parts := strings.Split(trimmed, "/")
	return parts[len(parts)-1]
}

func validConfig(cfg *config.RawConfig, _ string) error {
	if len(cfg.Proxy) == 0 && len(cfg.ProxyProvider) == 0 {
		return errors.New("profile does not contain `proxies` or `proxy-providers`")
	}

	if _, err := regexp2.Compile(cfg.ClashForAndroid.UiSubtitlePattern, 0); err != nil {
		return fmt.Errorf("compile ui-subtitle-pattern: %s", err.Error())
	}

	return nil
}

func process(cfg *config.RawConfig, profileDir string) error {
	for _, p := range processors {
		if err := p(cfg, profileDir); err != nil {
			return err
		}
	}

	return nil
}
