package config

import (
	"fmt"
	"strings"

	"github.com/metacubex/mihomo/component/age"
)

const unsupportedHybridAgeSecretKeyPrefix = "AGE-SECRET-KEY-PQ-1"

func SetGlobalSecretKeys(secretKeys ...string) {
	identities := make([]age.Identity, 0, len(secretKeys))
	for _, secretKey := range secretKeys {
		key := strings.TrimSpace(secretKey)
		if key == "" {
			continue
		}

		parsed, err := age.ParseIdentities(key)
		if err != nil {
			continue
		}
		identities = append(identities, parsed...)
	}

	age.SetGlobalIdentities(identities)
}

func GenX25519KeyPair() (secretKey string, publicKey string, err error) {
	return age.GenX25519KeyPair()
}

func ToPublicKeys(secretKeys ...string) (publicKeys []string, err error) {
	for _, secretKey := range secretKeys {
		key := strings.TrimSpace(secretKey)
		if err := rejectUnsupportedAgeSecretKeys(key); err != nil {
			return nil, err
		}
		identities, err := age.ParseIdentities(key)
		if err != nil {
			return nil, err
		}

		for _, identity := range identities {
			recipient, err := age.ConvertToRecipient(identity)
			if err != nil {
				return nil, err
			}
			publicKey, ok := recipient.(fmt.Stringer)
			if !ok {
				return nil, fmt.Errorf("unexpected recipient type: %T", recipient)
			}
			publicKeys = append(publicKeys, publicKey.String())
		}
	}

	return publicKeys, nil
}

func VerifySecretKeys(secretKeys ...string) error {
	for _, secretKey := range secretKeys {
		key := strings.TrimSpace(secretKey)
		if err := rejectUnsupportedAgeSecretKeys(key); err != nil {
			return err
		}
		if _, err := age.ParseIdentities(key); err != nil {
			return err
		}
	}

	return nil
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

func VerifyPublicKeys(publicKeys ...string) error {
	for _, publicKey := range publicKeys {
		if _, err := age.ParseRecipients(strings.TrimSpace(publicKey)); err != nil {
			return err
		}
	}

	return nil
}
