package config

import "testing"

func TestVerifySecretKeysRejectsHybridKeys(t *testing.T) {
	err := VerifySecretKeys("AGE-SECRET-KEY-PQ-1EXAMPLE")
	if err == nil {
		t.Fatal("expected hybrid age secret key to be rejected")
	}
	if err.Error() != "hybrid age secret keys are not supported by the Rust override decryptor yet" {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestToPublicKeysRejectsHybridKeys(t *testing.T) {
	_, err := ToPublicKeys("AGE-SECRET-KEY-PQ-1EXAMPLE")
	if err == nil {
		t.Fatal("expected hybrid age secret key to be rejected")
	}
	if err.Error() != "hybrid age secret keys are not supported by the Rust override decryptor yet" {
		t.Fatalf("unexpected error: %v", err)
	}
}
