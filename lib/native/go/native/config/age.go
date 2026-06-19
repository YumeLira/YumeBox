package config

import (
	"fmt"
	"strings"

	"github.com/metacubex/mihomo/component/age"
)

const unsupportedHybridAgeSecretKeyPrefix = "AGE-SECRET-KEY-PQ-1"

func SetGlobalSecretKeys(secretKeys ...string) {
	trimmed := make([]string, 0, len(secretKeys))
	for _, secretKey := range secretKeys {
		key := strings.TrimSpace(secretKey)
		if key == "" {
			continue
		}
		trimmed = append(trimmed, key)
	}
	age.SetGlobalSecretKeys(trimmed...)
}

func GenX25519KeyPair() (secretKey string, publicKey string, err error) {
	return age.GenX25519KeyPair()
}

func ToPublicKeys(secretKeys ...string) (publicKeys []string, err error) {
	trimmed := make([]string, 0, len(secretKeys))
	for _, secretKey := range secretKeys {
		key := strings.TrimSpace(secretKey)
		if err := rejectUnsupportedAgeSecretKeys(key); err != nil {
			return nil, err
		}
		trimmed = append(trimmed, key)
	}
	return age.ToPublicKeys(trimmed...)
}

func VerifySecretKeys(secretKeys ...string) error {
	for _, secretKey := range secretKeys {
		key := strings.TrimSpace(secretKey)
		if err := rejectUnsupportedAgeSecretKeys(key); err != nil {
			return err
		}
	}
	trimmed := make([]string, 0, len(secretKeys))
	for _, secretKey := range secretKeys {
		trimmed = append(trimmed, strings.TrimSpace(secretKey))
	}
	return age.VeritySecretKeys(trimmed...)
}

func VerifyPublicKeys(publicKeys ...string) error {
	trimmed := make([]string, 0, len(publicKeys))
	for _, publicKey := range publicKeys {
		trimmed = append(trimmed, strings.TrimSpace(publicKey))
	}
	return age.VerityPublicKeys(trimmed...)
}

func rejectUnsupportedAgeSecretKeys(secretKeys string) error {
	for _, rawLine := range strings.Split(secretKeys, "\n") {
		line := strings.TrimSpace(rawLine)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		if strings.HasPrefix(line, unsupportedHybridAgeSecretKeyPrefix) {
			return fmt.Errorf("hybrid age secret keys are not supported by the Rust override decryptor yet")
		}
	}
	return nil
}
