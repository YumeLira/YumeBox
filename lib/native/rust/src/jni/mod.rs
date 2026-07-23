use age::secrecy::ExposeSecret;
use jni::objects::{JObject, JString};
use jni::sys::jstring;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::{Env, EnvUnowned};

use crate::compiler::compile_request;
use crate::model::{CompileRequest, CompileResult};

// Age x25519 keygen, moved off the (deleted) Go libclash. Bound to the Kotlin `Compiler` object.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_github_yumelira_yumebox_core_bridge_Compiler_nativeGenAgeKey<'local>(
    mut env: EnvUnowned<'local>,
    _compiler: JObject<'local>,
) -> jstring {
    env.with_env(|env| {
        let identity = age::x25519::Identity::generate();
        let secret = identity.to_string().expose_secret().to_string();
        let public = identity.to_public().to_string();
        let json = serde_json::json!({ "secretKey": secret, "publicKey": public }).to_string();
        Ok::<_, jni::errors::Error>(new_java_string(env, json))
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

/// Derives the age public key for a secret key, or "" when it does not parse.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_github_yumelira_yumebox_core_bridge_Compiler_nativeAgePublicKey<'local>(
    mut env: EnvUnowned<'local>,
    _compiler: JObject<'local>,
    secret: JString<'local>,
) -> jstring {
    env.with_env(|env| {
        let secret_str = match secret.try_to_string(env) {
            Ok(value) => value,
            Err(_) => {
                let _ = env.exception_clear();
                return Ok::<_, jni::errors::Error>(new_java_string(env, String::new()));
            }
        };
        let public = secret_str
            .trim()
            .parse::<age::x25519::Identity>()
            .map(|identity| identity.to_public().to_string())
            .unwrap_or_default();
        Ok::<_, jni::errors::Error>(new_java_string(env, public))
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

// Full compile (write_output = false): returns CompileResult{finalYaml,...}. This is the compiler
// seam for the out-of-process core — the app compiles here and streams finalYaml to the core; there
// is no in-process load path. Bound to the Kotlin `Compiler` object.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_github_yumelira_yumebox_core_bridge_Compiler_nativeCompile<'local>(
    mut env: EnvUnowned<'local>,
    _compiler: JObject<'local>,
    request_json: JString<'local>,
) -> jstring {
    env.with_env(|env| Ok::<_, jni::errors::Error>(handle_compile_request(env, request_json)))
        .resolve::<ThrowRuntimeExAndDefault>()
}

fn handle_compile_request(env: &mut Env, request_json: JString) -> jstring {
    let payload = match request_json.try_to_string(env) {
        Ok(value) => value,
        Err(err) => {
            let _ = env.exception_clear();
            return new_java_string(env, error_result(format!("read JNI request: {err}")));
        }
    };

    let result = match serde_json::from_str::<CompileRequest>(&payload) {
        Ok(request) => compile_request(request, false),
        Err(err) => Err(format!("decode override request: {err}")),
    };

    let response_json = match result {
        Ok(result) => encode_result(result),
        Err(err) => error_result(err),
    };
    new_java_string(env, response_json)
}

fn encode_result(result: CompileResult) -> String {
    serde_json::to_string(&result).unwrap_or_else(|_| {
        "{\"success\":false,\"fingerprint\":\"\",\"finalYaml\":\"\",\"warnings\":[],\"error\":\"override result encode failed\"}".to_string()
    })
}

fn error_result(message: impl Into<String>) -> String {
    serde_json::to_string(&CompileResult {
        success: false,
        fingerprint: String::new(),
        final_yaml: String::new(),
        warnings: Vec::new(),
        error: Some(message.into()),
    })
    .unwrap_or_else(|_| {
        "{\"success\":false,\"fingerprint\":\"\",\"finalYaml\":\"\",\"warnings\":[],\"error\":\"override processor failed\"}".to_string()
    })
}

fn new_java_string(env: &mut Env, content: String) -> jstring {
    env.new_string(content)
        .expect("create JNI response string")
        .into_raw()
}
