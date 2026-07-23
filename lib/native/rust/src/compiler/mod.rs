pub mod normalize;
pub mod patch;
pub mod schema;

use serde_json::{Map as JsonMap, Value as JsonValue};
use serde_yaml::Value as YamlValue;
use sha2::{Digest, Sha256};
use std::fs;
use std::path::Path;

use crate::engine;
use crate::io::{load_overrides, write_atomic};
use crate::model::{CompileRawResult, CompileRequest, CompileResult, REQUEST_SCHEMA_VERSION};

struct CompiledRoot {
    root: JsonValue,
    warnings: Vec<String>,
}

pub fn compile_request(
    request: CompileRequest,
    write_output: bool,
) -> Result<CompileResult, String> {
    validate_request_schema(&request)?;
    let source_bytes =
        fs::read(&request.profile_path).map_err(|err| format!("read profile yaml: {err}"))?;
    if is_age_encrypted(&source_bytes) {
        return Err(
            "encrypted profiles must use native compile raw output; YAML output is disabled"
                .to_string(),
        );
    }
    let source_yaml = String::from_utf8(source_bytes)
        .map_err(|err| format!("source yaml is not utf-8: {err}"))?;
    let compiled = compile_root_from_source(&request, source_yaml, false)?;

    let final_yaml = serde_yaml::to_string(&normalize::normalize_root(&compiled.root))
        .map_err(|err| format!("encode final yaml: {err}"))?;
    let fingerprint = {
        let mut hasher = Sha256::new();
        hasher.update(request.profile_uuid.as_bytes());
        hasher.update(final_yaml.as_bytes());
        hasher.finalize().iter().map(|byte| format!("{byte:02x}")).collect()
    };

    if write_output {
        let output_path = request.output_path.trim();
        if output_path.is_empty() {
            return Err("compile mode requires outputPath".to_string());
        }
        write_atomic(Path::new(&output_path), final_yaml.as_bytes())
            .map_err(|err| format!("write runtime yaml: {err}"))?;
    }

    Ok(CompileResult {
        success: true,
        fingerprint,
        final_yaml,
        warnings: compiled.warnings,
        error: None,
    })
}

pub fn compile_raw_request(request: CompileRequest) -> Result<CompileRawResult, String> {
    let compiled = compile_root(&request)?;
    let config_raw = serde_json::to_string(&compiled.root)
        .map_err(|err| format!("encode raw config json: {err}"))?;
    let fingerprint = fingerprint_for(request.profile_uuid.as_bytes(), config_raw.as_bytes());
    Ok(CompileRawResult {
        success: true,
        fingerprint,
        config_raw,
        warnings: compiled.warnings,
        error: None,
    })
}

fn compile_root(request: &CompileRequest) -> Result<CompiledRoot, String> {
    validate_request_schema(request)?;
    let (source_yaml, encrypted) = load_source_yaml(request)?;
    compile_root_from_source(request, source_yaml, encrypted)
}

fn compile_root_from_source(
    request: &CompileRequest,
    source_yaml: String,
    encrypted: bool,
) -> Result<CompiledRoot, String> {
    let mut source_value: YamlValue =
        serde_yaml::from_str(&source_yaml).map_err(|err| format!("parse source yaml: {err}"))?;
    // serde_yaml resolves `&`/`*` aliases but not YAML merge keys (`<<`). Applying merges walks
    // the full document, which is costly for subscriptions with thousands of nodes or rules, so
    // avoid it entirely unless the source can actually contain a merge key.
    if source_yaml.contains("<<:") {
        source_value
            .apply_merge()
            .map_err(|err| format!("apply yaml merge keys: {err}"))?;
    }
    let mut root: JsonValue = serde_json::to_value(source_value)
        .map_err(|err| format!("convert source yaml to json: {err}"))?;

    let loaded_overrides = load_overrides(&request.overrides, encrypted)?;
    let mut warnings = loaded_overrides.warnings;
    let apply_result = engine::apply_overrides(root, &loaded_overrides.items, encrypted)?;
    root = apply_result.root;
    warnings.extend(apply_result.warnings);

    let profile_dir = Path::new(&request.profile_dir);
    if !root.is_object() {
        return Err("compiled root config must be an object".to_string());
    }
    if !request.skip_runtime_patches {
        patch::patch_static_runtime(&mut root, profile_dir, request.run_mode);
    }

    let object = root
        .as_object_mut()
        .ok_or_else(|| "compiled root config must be an object".to_string())?;
    validate_root_config(object)?;
    if !request.skip_runtime_patches {
        patch::validate_provider_paths(object, profile_dir)?;
    }

    Ok(CompiledRoot { root, warnings })
}

fn validate_request_schema(request: &CompileRequest) -> Result<(), String> {
    if request.schema_version != REQUEST_SCHEMA_VERSION {
        return Err(format!(
            "unsupported schema version: {}",
            request.schema_version
        ));
    }
    Ok(())
}

fn load_source_yaml(request: &CompileRequest) -> Result<(String, bool), String> {
    let source_bytes =
        fs::read(&request.profile_path).map_err(|err| format!("read profile yaml: {err}"))?;
    let encrypted = is_age_encrypted(&source_bytes);
    let plaintext = if encrypted {
        decrypt_age_source(&source_bytes, request.age_secret_key.as_deref())?
    } else {
        source_bytes
    };
    let source_yaml =
        String::from_utf8(plaintext).map_err(|err| format!("source yaml is not utf-8: {err}"))?;
    Ok((source_yaml, encrypted))
}

fn is_age_encrypted(bytes: &[u8]) -> bool {
    bytes.starts_with(b"age-encryption.org/v1")
        || bytes.starts_with(b"-----BEGIN AGE ENCRYPTED FILE-----")
}

fn decrypt_age_source(ciphertext: &[u8], secret_key: Option<&str>) -> Result<Vec<u8>, String> {
    let secret_key = secret_key
        .map(str::trim)
        .filter(|value| !value.is_empty())
        .ok_or_else(|| "age encrypted profile requires ageSecretKey".to_string())?;
    let identities = parse_age_identities(secret_key)?;
    let mut last_error = String::new();
    for identity in identities {
        match age::decrypt(&identity, ciphertext) {
            Ok(plaintext) => return Ok(plaintext),
            Err(err) => last_error = err.to_string(),
        }
    }
    if last_error.is_empty() {
        last_error = "no matching age identity".to_string();
    }
    Err(format!("decrypt age profile: {last_error}"))
}

fn parse_age_identities(secret_keys: &str) -> Result<Vec<age::x25519::Identity>, String> {
    let mut identities = Vec::new();
    for (index, raw_line) in secret_keys.lines().enumerate() {
        let line = raw_line.trim();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        if line.starts_with("AGE-SECRET-KEY-PQ-1") {
            return Err(
                "hybrid age secret keys are not supported by the Rust override decryptor yet"
                    .to_string(),
            );
        }
        let identity = line
            .parse::<age::x25519::Identity>()
            .map_err(|err| format!("parse age secret key at line {}: {err}", index + 1))?;
        identities.push(identity);
    }
    if identities.is_empty() {
        return Err("no supported age secret keys found".to_string());
    }
    Ok(identities)
}

fn fingerprint_for(profile_uuid: &[u8], payload: &[u8]) -> String {
    let mut hasher = Sha256::new();
    hasher.update(profile_uuid);
    hasher.update(payload);
    hasher.finalize().iter().map(|byte| format!("{byte:02x}")).collect()
}

fn validate_root_config(object: &JsonMap<String, JsonValue>) -> Result<(), String> {
    validate_geosite_matcher(object)?;
    Ok(())
}

fn validate_geosite_matcher(object: &JsonMap<String, JsonValue>) -> Result<(), String> {
    let Some(value) = object.get("geosite-matcher") else {
        return Ok(());
    };
    let Some(value) = value.as_str() else {
        return Err(
            "geosite-matcher must be a string (supported values: mph, succinct)".to_string(),
        );
    };
    if matches!(value, "mph" | "succinct") {
        return Ok(());
    }
    Err(format!(
        "geosite-matcher must be one of: mph, succinct (got {value})"
    ))
}

// C ABI exports for the CLI/tests; the app calls liboverride through its own JNI (see src/jni)
use std::ffi::{c_char, CStr, CString};

/// # Safety
/// Caller must pass a valid null-terminated UTF-8 JSON string.
/// Returns a CompileRawResult JSON string as a Rust-allocated CString that must
/// be freed with override_free_string.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn override_compile_raw(request_json: *const c_char) -> *mut c_char {
    if request_json.is_null() {
        return compile_raw_error_result("read raw compile request: null pointer").into_raw();
    }
    let json_str = match unsafe { CStr::from_ptr(request_json) }.to_str() {
        Ok(s) => s,
        Err(err) => {
            return compile_raw_error_result(format!("read raw compile request: {err}")).into_raw()
        }
    };
    let request: CompileRequest = match serde_json::from_str(json_str) {
        Ok(r) => r,
        Err(err) => {
            return compile_raw_error_result(format!("decode raw compile request: {err}"))
                .into_raw()
        }
    };
    let response = match compile_raw_request(request) {
        Ok(result) => serde_json::to_string(&result)
            .unwrap_or_else(|_| raw_error_json("raw compile result encode failed".to_string())),
        Err(err) => raw_error_json(err),
    };
    CString::new(response).unwrap_or_default().into_raw()
}

fn compile_raw_error_result(message: impl Into<String>) -> CString {
    CString::new(raw_error_json(message.into())).unwrap_or_default()
}

fn raw_error_json(message: String) -> String {
    serde_json::to_string(&CompileRawResult {
        success: false,
        fingerprint: String::new(),
        config_raw: String::new(),
        warnings: Vec::new(),
        error: Some(message),
    })
    .unwrap_or_else(|_| {
        "{\"success\":false,\"fingerprint\":\"\",\"configRaw\":\"\",\"warnings\":[],\"error\":\"raw compile failed\"}".to_string()
    })
}

/// # Safety
/// Caller must pass a pointer previously returned by override_compile_raw.
/// Passing any other pointer or a null pointer is undefined behavior.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn override_free_string(s: *mut c_char) {
    if !s.is_null() {
        drop(unsafe { CString::from_raw(s) });
    }
}
