package config

import (
	"os"
	"runtime"
	"strings"

	"cfa/native/app"

	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/log"
	"gopkg.in/yaml.v3"
)

func LoadCompiled(path string) error {
	configData, err := os.ReadFile(path)
	if err != nil {
		log.Errorln("Load compiled %s: %s", path, err.Error())
		return err
	}

	rawCfg, err := config.UnmarshalRawConfig(configData)
	if err != nil {
		log.Errorln("Load compiled %s: %s", path, err.Error())
		return err
	}

	cfg, err := config.Parse(configData)
	if err != nil {
		log.Errorln("Load compiled %s: %s", path, err.Error())
		return err
	}

	hub.ApplyConfig(cfg)
	app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)
	runtime.GC()
	return nil
}

func QueryProxyGroupsFromCompiledRaw(configRaw string, profileDir string, excludeNotSelectable bool) ([]*ProxyGroup, error) {
	_ = profileDir
	rawCfg, cfg, err := ParseCompiledRaw(configRaw)
	if err != nil {
		return nil, err
	}

	app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)

	groupNames := make([]string, 0, len(rawCfg.ProxyGroup))
	seen := make(map[string]struct{}, len(rawCfg.ProxyGroup))
	for _, mapping := range rawCfg.ProxyGroup {
		name, _ := mapping["name"].(string)
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}
		if _, ok := seen[name]; ok {
			continue
		}
		seen[name] = struct{}{}
		groupNames = append(groupNames, name)
	}

	return buildProxyGroupsFromParsed(cfg, groupNames, excludeNotSelectable), nil
}

func QueryTunRouteExcludeAddressFromCompiledRaw(configRaw string) ([]string, error) {
	rawCfg, err := UnmarshalCompiledRaw(configRaw)
	if err != nil {
		return nil, err
	}

	addresses := make([]string, 0, len(rawCfg.Tun.RouteExcludeAddress))
	for _, prefix := range rawCfg.Tun.RouteExcludeAddress {
		addresses = append(addresses, prefix.String())
	}
	return addresses, nil
}

func UnmarshalCompiledRaw(configRaw string) (*config.RawConfig, error) {
	return config.UnmarshalRawConfig([]byte(configRaw))
}

func ParseCompiledRaw(configRaw string) (*config.RawConfig, *config.Config, error) {
	rawCfg, err := UnmarshalCompiledRaw(configRaw)
	if err != nil {
		return nil, nil, err
	}
	cfg, err := config.ParseRawConfig(rawCfg)
	if err != nil {
		return nil, nil, err
	}
	return rawCfg, cfg, nil
}

func QueryConfigFromCompiledYaml(yamlText string) (map[string]any, error) {
	var root map[string]any
	if err := yaml.Unmarshal([]byte(yamlText), &root); err != nil {
		return nil, err
	}
	return root, nil
}
