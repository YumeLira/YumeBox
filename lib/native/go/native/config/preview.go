package config

import (
	"strings"

	"github.com/dlclark/regexp2"

	"cfa/native/app"

	"github.com/metacubex/mihomo/adapter/outboundgroup"
	mihomoConfig "github.com/metacubex/mihomo/config"
	C "github.com/metacubex/mihomo/constant"
)

// Proxy 代理结构体（本地定义，避免依赖 tunnel 包）
type Proxy struct {
	Name     string `json:"name"`
	Title    string `json:"title"`
	Subtitle string `json:"subtitle"`
	Type     string `json:"type"`
	Delay    int    `json:"delay"`
	IsGroup  bool   `json:"isGroup"`
}

// ProxyGroup 代理组结构体（本地定义，避免依赖 tunnel 包）
type ProxyGroup struct {
	Name    string   `json:"name"`
	Type    string   `json:"type"`
	Now     string   `json:"now"`
	Icon    string   `json:"icon"`
	Hidden  bool     `json:"hidden"`
	Proxies []*Proxy `json:"proxies"`
}

func buildProxyGroupsFromParsed(
	cfg *mihomoConfig.Config,
	orderedNames []string,
	excludeNotSelectable bool,
) []*ProxyGroup {
	if cfg == nil || len(orderedNames) == 0 {
		return []*ProxyGroup{}
	}

	result := make([]*ProxyGroup, 0, len(orderedNames))
	pattern := app.SubtitlePattern()

	for _, name := range orderedNames {
		proxy := cfg.Proxies[name]
		if proxy == nil {
			continue
		}
		if excludeNotSelectable && proxy.Type() != C.Selector {
			continue
		}

		group, ok := proxy.Adapter().(outboundgroup.ProxyGroup)
		if !ok {
			continue
		}

		result = append(result, &ProxyGroup{
			Name:    name,
			Type:    normalizeProxyType(proxy.Type().String()),
			Now:     group.Now(),
			Icon:    group.Icon(),
			Hidden:  group.Hidden(),
			Proxies: convertPreviewProxies(group.Proxies(), pattern),
		})
	}

	return result
}

func convertPreviewProxies(
	proxies []C.Proxy,
	uiSubtitlePattern *regexp2.Regexp,
) []*Proxy {
	result := make([]*Proxy, 0, len(proxies))
	for _, proxy := range proxies {
		result = append(result, buildPreviewProxy(proxy, uiSubtitlePattern))
	}
	return result
}

func buildPreviewProxy(
	proxy C.Proxy,
	uiSubtitlePattern *regexp2.Regexp,
) *Proxy {
	name := proxy.Name()
	title := name
	subtitle := proxy.Type().String()
	_, isGroup := proxy.Adapter().(outboundgroup.ProxyGroup)

	if uiSubtitlePattern != nil {
		if !isGroup {
			runes := []rune(name)
			match, err := uiSubtitlePattern.FindRunesMatch(runes)
			if err == nil && match != nil {
				title = string(runes[:match.Index]) + string(runes[match.Index+match.Length:])
				subtitle = string(runes[match.Index : match.Index+match.Length])
			}
		}
	}

	return &Proxy{
		Name:     name,
		Title:    strings.TrimSpace(title),
		Subtitle: strings.TrimSpace(subtitle),
		Type:     normalizeProxyType(proxy.Type().String()),
		Delay:    int(proxy.LastDelayForTestUrl(getPreviewTestURL(proxy))),
		IsGroup:  isGroup,
	}
}

func getPreviewTestURL(proxy C.Proxy) string {
	for key := range proxy.ExtraDelayHistories() {
		if len(key) > 0 {
			return key
		}
	}
	return "https://www.gstatic.com/generate_204"
}

func normalizeProxyType(proxyType string) string {
	switch proxyType {
	case "Direct",
		"Reject",
		"RejectDrop",
		"Compatible",
		"Pass",
		"PassRule",
		"Shadowsocks",
		"ShadowsocksR",
		"Snell",
		"Socks5",
		"Http",
		"Vmess",
		"Vless",
		"Trojan",
		"Hysteria",
		"Hysteria2",
		"Tuic",
		"WireGuard",
		"Dns",
		"Ssh",
		"Mieru",
		"AnyTLS",
		"Sudoku",
		"Masque",
		"TrustTunnel",
		"OpenVPN",
		"Tailscale",
		"GostRelay",
		"Relay",
		"Selector",
		"Fallback",
		"URLTest",
		"LoadBalance",
		"Smart",
		"Unknown":
		return proxyType
	default:
		return "Unknown"
	}
}
