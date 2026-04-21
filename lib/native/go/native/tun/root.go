package tun

import (
	"fmt"
	"io"
	"net/netip"
	"strings"

	C "github.com/metacubex/mihomo/constant"
	LC "github.com/metacubex/mihomo/listener/config"
	"github.com/metacubex/mihomo/listener/sing_tun"
	"github.com/metacubex/mihomo/log"
	"github.com/metacubex/mihomo/tunnel"
	"gopkg.in/yaml.v3"
)

type RootTunConfig struct {
	IfName             string   `json:"ifName" yaml:"ifName"`
	MTU                int      `json:"mtu" yaml:"mtu"`
	Stack              string   `json:"stack" yaml:"stack"`
	Inet4Address       []string `json:"inet4Address" yaml:"inet4Address"`
	Inet6Address       []string `json:"inet6Address" yaml:"inet6Address"`
	DNSHijack          []string `json:"dnsHijack" yaml:"dnsHijack"`
	AutoRoute          bool     `json:"autoRoute" yaml:"autoRoute"`
	StrictRoute        bool     `json:"strictRoute" yaml:"strictRoute"`
	AutoRedirect       bool     `json:"autoRedirect" yaml:"autoRedirect"`
	IncludeUID         []uint32 `json:"includeUid" yaml:"includeUid"`
	ExcludeUID         []uint32 `json:"excludeUid" yaml:"excludeUid"`
	IncludeAndroidUser []int    `json:"includeAndroidUser" yaml:"includeAndroidUser"`
	RouteAddress       []string `json:"routeAddress" yaml:"routeAddress"`
	RouteExclude       []string `json:"routeExcludeAddress" yaml:"routeExcludeAddress"`
	DNSMode            string   `json:"dnsMode" yaml:"dnsMode"`
	FakeIPRange        string   `json:"fakeIpRange" yaml:"fakeIpRange"`
	FakeIPRange6       string   `json:"fakeIpRange6" yaml:"fakeIpRange6"`
	AllowIPv6          bool     `json:"allowIpv6" yaml:"allowIpv6"`
}

func StartRoot(configYaml string) (io.Closer, error) {
	var cfg RootTunConfig
	if err := yaml.Unmarshal([]byte(configYaml), &cfg); err != nil {
		return nil, fmt.Errorf("decode root tun config: %w", err)
	}

	log.Debugln(
		"ROOT_TUN: native includeUid=%d excludeUid=%v dnsHijack=%v routeAddress=%d",
		len(cfg.IncludeUID),
		cfg.ExcludeUID,
		cfg.DNSHijack,
		len(cfg.RouteAddress),
	)

	options, err := cfg.toListenerOptions()
	if err != nil {
		return nil, err
	}

	payload, _ := yaml.Marshal(cfg)
	log.Debugln("ROOT_TUN: config=\n%s", strings.TrimSpace(string(payload)))

	listener, err := sing_tun.New(options, tunnel.Tunnel)
	if err != nil {
		log.Errorln("ROOT_TUN: %v", err)
		return nil, err
	}

	return listener, nil
}

func (c RootTunConfig) toListenerOptions() (LC.Tun, error) {
	stack, ok := C.StackTypeMapping[strings.ToLower(c.Stack)]
	if !ok {
		stack = C.TunSystem
	}

	inet4Address, err := parsePrefixes(c.Inet4Address)
	if err != nil {
		return LC.Tun{}, fmt.Errorf("parse inet4Address: %w", err)
	}

	inet6Address, err := parsePrefixes(c.Inet6Address)
	if err != nil {
		return LC.Tun{}, fmt.Errorf("parse inet6Address: %w", err)
	}

	if !c.AllowIPv6 {
		inet6Address = nil
	}

	routeAddress, err := parsePrefixes(c.RouteAddress)
	if err != nil {
		return LC.Tun{}, fmt.Errorf("parse routeAddress: %w", err)
	}

	routeExcludeAddress, err := parsePrefixes(c.RouteExclude)
	if err != nil {
		return LC.Tun{}, fmt.Errorf("parse routeExcludeAddress: %w", err)
	}

	if err := validateDNSMode(c.DNSMode); err != nil {
		return LC.Tun{}, err
	}

	if err := validateFakeIP(c, inet4Address, inet6Address); err != nil {
		return LC.Tun{}, err
	}

	return LC.Tun{
		Enable:              true,
		Device:              c.IfName,
		Stack:               stack,
		DNSHijack:           append([]string(nil), c.DNSHijack...),
		AutoRoute:           c.AutoRoute,
		StrictRoute:         c.StrictRoute,
		AutoRedirect:        c.AutoRedirect,
		AutoDetectInterface: false,
		MTU:                 uint32(c.MTU),
		Inet4Address:        inet4Address,
		Inet6Address:        inet6Address,
		IncludeUID:          append([]uint32(nil), c.IncludeUID...),
		ExcludeUID:          append([]uint32(nil), c.ExcludeUID...),
		IncludeAndroidUser:  append([]int(nil), c.IncludeAndroidUser...),
		RouteAddress:        routeAddress,
		RouteExcludeAddress: routeExcludeAddress,
		FileDescriptor:      0,
	}, nil
}

func parsePrefixes(values []string) ([]netip.Prefix, error) {
	prefixes := make([]netip.Prefix, 0, len(values))
	for _, value := range values {
		value = strings.TrimSpace(value)
		if value == "" {
			continue
		}

		prefix, err := netip.ParsePrefix(value)
		if err != nil {
			return nil, err
		}

		prefixes = append(prefixes, prefix)
	}

	return prefixes, nil
}

func validateDNSMode(mode string) error {
	switch mode {
	case "", "redir-host", "fake-ip":
		return nil
	default:
		return fmt.Errorf("unsupported dnsMode: %s", mode)
	}
}

func validateFakeIP(cfg RootTunConfig, inet4Address []netip.Prefix, inet6Address []netip.Prefix) error {
	if cfg.DNSMode != "fake-ip" {
		return nil
	}

	if cfg.FakeIPRange == "" && (!cfg.AllowIPv6 || cfg.FakeIPRange6 == "") {
		return fmt.Errorf("fake-ip requires at least one fake ip range")
	}

	if cfg.FakeIPRange != "" {
		fake4, err := netip.ParsePrefix(strings.TrimSpace(cfg.FakeIPRange))
		if err != nil {
			return fmt.Errorf("parse fakeIpRange: %w", err)
		}

		for _, prefix := range inet4Address {
			if prefixOverlaps(fake4, prefix) {
				return fmt.Errorf("fakeIpRange overlaps tun subnet: %s", prefix.String())
			}
		}
	}

	if cfg.AllowIPv6 && cfg.FakeIPRange6 != "" {
		fake6, err := netip.ParsePrefix(strings.TrimSpace(cfg.FakeIPRange6))
		if err != nil {
			return fmt.Errorf("parse fakeIpRange6: %w", err)
		}

		for _, prefix := range inet6Address {
			if prefixOverlaps(fake6, prefix) {
				return fmt.Errorf("fakeIpRange6 overlaps tun subnet: %s", prefix.String())
			}
		}
	}

	return nil
}

func prefixOverlaps(left netip.Prefix, right netip.Prefix) bool {
	return left.Contains(right.Addr()) || right.Contains(left.Addr())
}
