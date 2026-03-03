package config

import (
	"os"
	"strings"

	"cfa/native/tunnel"
	"gopkg.in/yaml.v3"
)

// QueryProxyGroupNamesFromPath parses profile config and returns proxy-group names
// without applying runtime load (hub.ApplyConfig).
func QueryProxyGroupNamesFromPath(path string, excludeNotSelectable bool) ([]string, error) {
	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		return nil, err
	}

	if len(rawCfg.ProxyGroup) == 0 {
		return []string{}, nil
	}

	result := make([]string, 0, len(rawCfg.ProxyGroup))
	seen := make(map[string]struct{}, len(rawCfg.ProxyGroup))

	for _, group := range rawCfg.ProxyGroup {
		name, _ := group["name"].(string)
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}

		if excludeNotSelectable {
			groupType, _ := group["type"].(string)
			if !isSelectableProxyGroupType(groupType) {
				continue
			}
		}

		if _, ok := seen[name]; ok {
			continue
		}
		seen[name] = struct{}{}
		result = append(result, name)
	}

	return result, nil
}

// QueryProxyGroupsFromPath parses profile config and returns proxy groups with
// proxy entries without applying runtime load (hub.ApplyConfig).
func QueryProxyGroupsFromPath(path string, excludeNotSelectable bool) ([]*tunnel.ProxyGroup, error) {
	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		return nil, err
	}

	if len(rawCfg.ProxyGroup) == 0 {
		return []*tunnel.ProxyGroup{}, nil
	}

	proxyTypeByName := make(map[string]string, len(rawCfg.Proxy))
	for _, proxy := range rawCfg.Proxy {
		name, _ := proxy["name"].(string)
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}
		rawType, _ := proxy["type"].(string)
		proxyTypeByName[name] = normalizeProxyTypeName(rawType)
	}

	groupTypeByName := make(map[string]string, len(rawCfg.ProxyGroup))
	for _, group := range rawCfg.ProxyGroup {
		name, _ := group["name"].(string)
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}
		rawType, _ := group["type"].(string)
		groupTypeByName[name] = normalizeProxyTypeName(rawType)
	}

	proxyProviderNodes := make(map[string][]string, len(rawCfg.ProxyProvider))
	for providerName, provider := range rawCfg.ProxyProvider {
		path, _ := provider["path"].(string)
		path = strings.TrimSpace(path)
		if path == "" {
			continue
		}
		proxyProviderNodes[providerName] = parseProxyProviderNodeNames(path)
	}

	result := make([]*tunnel.ProxyGroup, 0, len(rawCfg.ProxyGroup))
	seenGroups := make(map[string]struct{}, len(rawCfg.ProxyGroup))
	for _, group := range rawCfg.ProxyGroup {
		name, _ := group["name"].(string)
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}
		if _, ok := seenGroups[name]; ok {
			continue
		}
		seenGroups[name] = struct{}{}

		rawType, _ := group["type"].(string)
		if excludeNotSelectable && !isSelectableProxyGroupType(rawType) {
			continue
		}
		groupType := normalizeProxyTypeName(rawType)

		memberNames := collectGroupMemberNames(group, proxyProviderNodes)
		proxies := make([]*tunnel.Proxy, 0, len(memberNames))
		seenMembers := make(map[string]struct{}, len(memberNames))
		for _, member := range memberNames {
			member = strings.TrimSpace(member)
			if member == "" {
				continue
			}
			if _, ok := seenMembers[member]; ok {
				continue
			}
			seenMembers[member] = struct{}{}

			memberType := proxyTypeByName[member]
			if memberType == "" {
				memberType = groupTypeByName[member]
			}
			if memberType == "" {
				memberType = "Unknown"
			}

			proxies = append(proxies, &tunnel.Proxy{
				Name:     member,
				Title:    member,
				Subtitle: memberType,
				Type:     memberType,
				Delay:    0,
			})
		}

		now, _ := group["now"].(string)
		now = strings.TrimSpace(now)
		if now == "" && len(proxies) > 0 {
			now = proxies[0].Name
		}

		icon, _ := group["icon"].(string)
		icon = strings.TrimSpace(icon)

		result = append(result, &tunnel.ProxyGroup{
			Name:    name,
			Type:    groupType,
			Now:     now,
			Icon:    icon,
			Proxies: proxies,
		})
	}

	return result, nil
}

func collectGroupMemberNames(group map[string]any, proxyProviderNodes map[string][]string) []string {
	names := collectStringSlice(group["proxies"])
	if len(names) == 0 {
		uses := collectStringSlice(group["use"])
		if len(uses) == 0 {
			return nil
		}
		out := make([]string, 0, len(uses))
		for _, providerName := range uses {
			nodes := proxyProviderNodes[providerName]
			// Skip unresolved provider names to avoid showing fake proxy nodes.
			if len(nodes) == 0 {
				continue
			}
			out = append(out, nodes...)
		}
		names = out
	}
	return names
}

func parseProxyProviderNodeNames(path string) []string {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil
	}

	var root map[string]any
	if err := yaml.Unmarshal(data, &root); err != nil {
		return nil
	}

	rawProxies, ok := root["proxies"]
	if !ok {
		return nil
	}
	items, ok := rawProxies.([]any)
	if !ok {
		return nil
	}
	names := make([]string, 0, len(items))
	for _, item := range items {
		m, ok := item.(map[string]any)
		if !ok {
			continue
		}
		name, _ := m["name"].(string)
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}
		names = append(names, name)
	}
	return names
}

func collectStringSlice(raw any) []string {
	switch v := raw.(type) {
	case []any:
		out := make([]string, 0, len(v))
		for _, item := range v {
			s, ok := item.(string)
			if !ok {
				continue
			}
			s = strings.TrimSpace(s)
			if s == "" {
				continue
			}
			out = append(out, s)
		}
		return out
	case []string:
		out := make([]string, 0, len(v))
		for _, s := range v {
			s = strings.TrimSpace(s)
			if s == "" {
				continue
			}
			out = append(out, s)
		}
		return out
	default:
		return nil
	}
}

func normalizeProxyTypeName(raw string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "direct":
		return "Direct"
	case "reject":
		return "Reject"
	case "reject-drop":
		return "RejectDrop"
	case "compatible":
		return "Compatible"
	case "pass":
		return "Pass"
	case "ss", "shadowsocks":
		return "Shadowsocks"
	case "ssr", "shadowsocksr":
		return "ShadowsocksR"
	case "snell":
		return "Snell"
	case "socks5", "socks":
		return "Socks5"
	case "http":
		return "Http"
	case "vmess":
		return "Vmess"
	case "vless":
		return "Vless"
	case "trojan":
		return "Trojan"
	case "hysteria":
		return "Hysteria"
	case "hysteria2":
		return "Hysteria2"
	case "tuic":
		return "Tuic"
	case "wireguard", "wire-guard":
		return "WireGuard"
	case "dns":
		return "Dns"
	case "ssh":
		return "Ssh"
	case "mieru":
		return "Mieru"
	case "anytls", "any-tls":
		return "AnyTLS"
	case "sudoku":
		return "Sudoku"
	case "masque":
		return "Masque"
	case "trust-tunnel", "trusttunnel":
		return "TrustTunnel"
	case "relay":
		return "Relay"
	case "select", "selector":
		return "Selector"
	case "fallback":
		return "Fallback"
	case "url-test", "urltest":
		return "URLTest"
	case "load-balance", "loadbalance":
		return "LoadBalance"
	case "smart":
		return "Smart"
	default:
		return "Unknown"
	}
}

func isSelectableProxyGroupType(groupType string) bool {
	switch strings.ToLower(strings.TrimSpace(groupType)) {
	case "select", "selector":
		return true
	default:
		return false
	}
}
