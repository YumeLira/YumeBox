package main

//#include "bridge.h"
import "C"

import (
	"encoding/json"
	"runtime"
	"strings"
	"unsafe"

	"cfa/native/app"
	"cfa/native/config"

	"github.com/metacubex/mihomo/hub"
)

type ageKeyPair struct {
	SecretKey string `json:"secretKey"`
	PublicKey string `json:"publicKey"`
}

type compileRawResult struct {
	Success     bool     `json:"success"`
	Fingerprint string   `json:"fingerprint"`
	ConfigRaw   string   `json:"configRaw"`
	Warnings    []string `json:"warnings"`
	Error       string   `json:"error"`
}

type inspectResult struct {
	Success bool   `json:"success"`
	Payload string `json:"payload"`
	Error   string `json:"error"`
}

type compileRawSummary struct {
	Success     bool     `json:"success"`
	Fingerprint string   `json:"fingerprint"`
	Warnings    []string `json:"warnings"`
	Error       string   `json:"error"`
}

type remoteValidCallback struct {
	callback unsafe.Pointer
}

func (r *remoteValidCallback) reportStatus(json string) {
	C.fetch_report(r.callback, marshalString(json))
}

//export fetchAndValid
func fetchAndValid(callback unsafe.Pointer, path, url C.c_string, force C.int) {
	go func(path, url string, callback unsafe.Pointer) {
		cb := &remoteValidCallback{callback: callback}

		err := config.FetchAndValid(path, url, force != 0, cb.reportStatus)

		C.fetch_complete(callback, marshalString(err))

		C.release_object(callback)

		runtime.GC()
	}(C.GoString(path), C.GoString(url), callback)
}

//export load
func load(completable unsafe.Pointer, path C.c_string) {
	go func(path string) {
		C.complete(completable, marshalString(config.Load(path)))

		C.release_object(completable)

		runtime.GC()
	}(C.GoString(path))
}

//export loadCompiledConfig
func loadCompiledConfig(completable unsafe.Pointer, path C.c_string) {
	go func(path string) {
		C.complete(completable, marshalString(config.LoadCompiled(path)))

		C.release_object(completable)

		runtime.GC()
	}(C.GoString(path))
}

//export loadCompiledRaw
func loadCompiledRaw(completable unsafe.Pointer, configRawJson *C.char) {
	rawCopy := C.GoString(configRawJson)
	C.free(unsafe.Pointer(configRawJson))
	go func(raw string) {
		defer C.release_object(completable)
		defer runtime.GC()

		rawCfg, cfg, err := config.ParseCompiledRaw(raw)
		if err != nil {
			C.complete(completable, marshalString(err.Error()))
			return
		}
		hub.ApplyConfig(cfg)
		app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)
		C.complete(completable, nil)
	}(rawCopy)
}

//export compiledRawResultError
func compiledRawResultError(resultJson C.c_string) *C.char {
	result, err := decodeCompileRawResult(C.GoString(resultJson))
	if err != nil {
		return marshalString(err)
	}
	if result.Success {
		return nil
	}
	message := strings.TrimSpace(result.Error)
	if message == "" {
		message = "compile raw config failed"
	}
	return marshalString(message)
}

//export compiledRawResultConfigRaw
func compiledRawResultConfigRaw(resultJson C.c_string) *C.char {
	result, err := decodeCompileRawResult(C.GoString(resultJson))
	if err != nil || !result.Success || strings.TrimSpace(result.ConfigRaw) == "" {
		return nil
	}
	return marshalString(result.ConfigRaw)
}

//export compiledRawResultSummary
func compiledRawResultSummary(resultJson C.c_string) *C.char {
	result, err := decodeCompileRawResult(C.GoString(resultJson))
	if err != nil {
		return marshalJson(compileRawSummary{Success: false, Error: err.Error()})
	}
	return marshalJson(compileRawSummary{
		Success:     result.Success,
		Fingerprint: result.Fingerprint,
		Warnings:    result.Warnings,
		Error:       result.Error,
	})
}

//export compiledRawFallbackSummary
func compiledRawFallbackSummary(errorMessage C.c_string) *C.char {
	message := strings.TrimSpace(C.GoString(errorMessage))
	if message == "" {
		message = "compile raw config failed"
	}
	return marshalJson(compileRawSummary{Success: false, Error: message})
}

//export inspectErrorResult
func inspectErrorResult(errorMessage C.c_string) *C.char {
	message := strings.TrimSpace(C.GoString(errorMessage))
	if message == "" {
		message = "native inspect failed"
	}
	return marshalJson(inspectResult{Success: false, Error: message})
}

func decodeCompileRawResult(resultJson string) (*compileRawResult, error) {
	var result compileRawResult
	if err := json.Unmarshal([]byte(resultJson), &result); err != nil {
		return nil, err
	}
	return &result, nil
}

//export inspectCompiledGroups
func inspectCompiledGroups(configRawJson C.c_string, profileDir C.c_string, excludeNotSelectable C.int) *C.char {
	groups, err := config.QueryProxyGroupsFromCompiledRaw(
		C.GoString(configRawJson),
		C.GoString(profileDir),
		excludeNotSelectable != 0,
	)
	if err != nil {
		return nil
	}
	return marshalYaml(groups)
}

//export inspectCompiledGroupsResult
func inspectCompiledGroupsResult(configRawJson C.c_string, profileDir C.c_string, excludeNotSelectable C.int) *C.char {
	groups, err := config.QueryProxyGroupsFromCompiledRaw(
		C.GoString(configRawJson),
		C.GoString(profileDir),
		excludeNotSelectable != 0,
	)
	if err != nil {
		return marshalJson(inspectResult{Success: false, Error: err.Error()})
	}
	payload, err := yamlString(groups)
	if err != nil {
		return marshalJson(inspectResult{Success: false, Error: err.Error()})
	}
	return marshalJson(inspectResult{Success: true, Payload: payload})
}

//export inspectCompiledTunRouteExcludeAddress
func inspectCompiledTunRouteExcludeAddress(configRawJson C.c_string) *C.char {
	addresses, err := config.QueryTunRouteExcludeAddressFromCompiledRaw(C.GoString(configRawJson))
	if err != nil {
		return nil
	}
	return marshalJson(addresses)
}

//export inspectCompiledTunRouteExcludeAddressResult
func inspectCompiledTunRouteExcludeAddressResult(configRawJson C.c_string) *C.char {
	addresses, err := config.QueryTunRouteExcludeAddressFromCompiledRaw(C.GoString(configRawJson))
	if err != nil {
		return marshalJson(inspectResult{Success: false, Error: err.Error()})
	}
	payload, err := jsonString(addresses)
	if err != nil {
		return marshalJson(inspectResult{Success: false, Error: err.Error()})
	}
	return marshalJson(inspectResult{Success: true, Payload: payload})
}

//export setAgeSecretKey
func setAgeSecretKey(key C.c_string) {
	if key == nil {
		config.SetGlobalSecretKeys()
		return
	}

	config.SetGlobalSecretKeys(C.GoString(key))
}

//export genX25519KeyPair
func genX25519KeyPair() *C.char {
	secretKey, publicKey, err := config.GenX25519KeyPair()
	if err != nil {
		return nil
	}

	return marshalJson(ageKeyPair{SecretKey: secretKey, PublicKey: publicKey})
}

//export verifySecretKeys
func verifySecretKeys(secretKeys C.c_string) C.int {
	if config.VerifySecretKeys(C.GoString(secretKeys)) != nil {
		return 0
	}

	return 1
}

//export toPublicKeys
func toPublicKeys(secretKeys C.c_string) *C.char {
	publicKeys, err := config.ToPublicKeys(C.GoString(secretKeys))
	if err != nil {
		return nil
	}

	return marshalJson(publicKeys)
}

//export verifyPublicKeys
func verifyPublicKeys(publicKeys C.c_string) C.int {
	if config.VerifyPublicKeys(C.GoString(publicKeys)) != nil {
		return 0
	}

	return 1
}
