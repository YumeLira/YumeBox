#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include <string.h>

#include "bridge_helper.h"
#include "jni_helper.h"
#include "trace.h"

#include "version.h"

#include <dlfcn.h>
#include <mutex>
#include "libclash.h"

extern "C" {

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeInit(JNIEnv *env, jobject thiz,
                                                          jstring home,
                                                          jstring version_name, jint sdk_version) {
    TRACE_METHOD();

    scoped_string _home = get_string(home);
    scoped_string _version_name = get_string(version_name);
    const char* _git_version = make_String(GIT_VERSION);

    coreInit(_home, _version_name, _git_version, sdk_version);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeReset(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    reset();
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeForceGc(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    forceGc();
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeSuspend(JNIEnv *env, jobject thiz,
                                                             jboolean suspended) {
    TRACE_METHOD();

    suspend((int) suspended);
}


JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryTunnelState(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    scoped_string response = queryTunnelState();

    return new_string(response);
}

JNIEXPORT jlong JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryTrafficNow(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    uint64_t upload = 0l, download = 0l;

    queryNow(&upload, &download);

    return (jlong) (down_scale_traffic(upload) << 32u | down_scale_traffic(download));
}

JNIEXPORT jlong JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryTrafficTotal(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    uint64_t upload = 0l, download = 0l;

    queryTotal(&upload, &download);

    return (jlong) (down_scale_traffic(upload) << 32u | down_scale_traffic(download));
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryConnections(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    scoped_string response = queryConnections();

    return new_string(response);
}

JNIEXPORT jboolean JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCloseConnection(JNIEnv *env, jobject thiz,
                                                                     jstring id) {
    TRACE_METHOD();

    scoped_string _id = get_string(id);

    return (jboolean) closeConnection(_id);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCloseAllConnections(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    closeAllConnections();
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeNotifyDnsChanged(JNIEnv *env, jobject thiz,
                                                                      jstring dns_list) {
    TRACE_METHOD();

    scoped_string _dns_list = get_string(dns_list);

    notifyDnsChanged(_dns_list);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeNotifyTimeZoneChanged(JNIEnv *env, jobject thiz,
                                                                           jstring name, jint offset) {
    TRACE_METHOD();

    scoped_string _name = get_string(name);

    notifyTimeZoneChanged(_name, offset);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeStartTun(JNIEnv *env, jobject thiz,
                                                              jint fd,
                                                              jstring stack,
                                                              jstring gateway,
                                                              jstring portal,
                                                              jstring dns,
                                                              jobject cb) {
    TRACE_METHOD();

    scoped_string _stack = get_string(stack);
    scoped_string _gateway = get_string(gateway);
    scoped_string _portal = get_string(portal);
    scoped_string _dns = get_string(dns);
    jobject _interface = new_global(cb);

    startTun(fd, _stack, _gateway, _portal, _dns, _interface);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeStopTun(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    stopTun();
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeStartRootTun(JNIEnv *env, jobject thiz,
                                                                  jstring config_yaml) {
    TRACE_METHOD();

    scoped_string _config_yaml = get_string(config_yaml);
    scoped_string error = startRootTun(_config_yaml);

    if (error == NULL)
        return NULL;

    return new_string(error);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeStopRootTun(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    stopRootTun();
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeStartHttp(JNIEnv *env, jobject thiz,
                                                               jstring listen_at) {
    TRACE_METHOD();

    scoped_string _listen_at = get_string(listen_at);

    scoped_string listened = startHttp(_listen_at);

    if (listened == NULL)
        return NULL;

    return new_string(listened);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeStopHttp(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    stopHttp();
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryGroupNames(JNIEnv *env, jobject thiz,
                                                                      jboolean exclude_not_selectable) {
    TRACE_METHOD();

    scoped_string response = queryGroupNames((int) exclude_not_selectable);

    return new_string(response);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryGroup(JNIEnv *env, jobject thiz,
                                                                 jstring name, jstring mode) {
    TRACE_METHOD();

    scoped_string _name = get_string(name);
    scoped_string _mode = get_string(mode);

    scoped_string response = queryGroup(_name, _mode);

    if (response == NULL)
        return NULL;

    return new_string(response);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeHealthCheck(JNIEnv *env, jobject thiz,
                                                                 jobject completable,
                                                                 jstring name) {
    TRACE_METHOD();

    jobject _completable = new_global(completable);
    scoped_string _name = get_string(name);

    healthCheck(_completable, _name);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeHealthCheckAll(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    healthCheckAll();
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeHealthCheckProxy(JNIEnv *env, jobject thiz,
jobject completable,
jstring proxy_name) {
TRACE_METHOD();

jobject _completable = new_global(completable);
scoped_string _proxy_name = get_string(proxy_name);

healthCheckProxy(_completable, _proxy_name);
}

JNIEXPORT jboolean JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativePatchSelector(JNIEnv *env, jobject thiz,
                                                                   jstring selector, jstring name) {
    TRACE_METHOD();

    scoped_string _selector = get_string(selector);
    scoped_string _name = get_string(name);

    return (jboolean) patchSelector(_selector, _name);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeLoad(JNIEnv *env, jobject thiz,
                                                          jobject completable, jstring path) {
    TRACE_METHOD();

    jobject _completable = new_global(completable);
    scoped_string _path = get_string(path);

    load(_completable, _path);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeLoadCompiledConfig(JNIEnv *env, jobject thiz,
                                                                         jobject completable,
                                                                         jstring path) {
    TRACE_METHOD();

    jobject _completable = new_global(completable);
    scoped_string _path = get_string(path);

    loadCompiledConfig(_completable, _path);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeFetchAndValid(JNIEnv *env, jobject thiz,
                                                                   jobject callback,
                                                                   jstring path,
                                                                   jstring url, jboolean force) {
    TRACE_METHOD();

    jobject _completable = new_global(callback);
    scoped_string _path = get_string(path);
    scoped_string _url = get_string(url);

    fetchAndValid(_completable, _path, _url, force);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeSetAgeSecretKey(JNIEnv *env, jobject thiz,
                                                                      jstring key) {
    TRACE_METHOD();

    if (key == NULL) {
        setAgeSecretKey(NULL);
        return;
    }

    scoped_string _key = get_string(key);
    setAgeSecretKey(_key);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeGenX25519KeyPair(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    scoped_string response = genX25519KeyPair();
    if (response == NULL)
        return NULL;

    return new_string(response);
}

JNIEXPORT jboolean JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeVerifySecretKeys(JNIEnv *env, jobject thiz,
                                                                       jstring secret_keys) {
    TRACE_METHOD();

    if (secret_keys == NULL)
        return JNI_FALSE;

    scoped_string _secret_keys = get_string(secret_keys);
    return (jboolean) verifySecretKeys(_secret_keys);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeToPublicKeys(JNIEnv *env, jobject thiz,
                                                                   jstring secret_keys) {
    TRACE_METHOD();

    if (secret_keys == NULL)
        return NULL;

    scoped_string _secret_keys = get_string(secret_keys);
    scoped_string response = toPublicKeys(_secret_keys);
    if (response == NULL)
        return NULL;

    return new_string(response);
}

JNIEXPORT jboolean JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeVerifyPublicKeys(JNIEnv *env, jobject thiz,
                                                                       jstring public_keys) {
    TRACE_METHOD();

    if (public_keys == NULL)
        return JNI_FALSE;

    scoped_string _public_keys = get_string(public_keys);
    return (jboolean) verifyPublicKeys(_public_keys);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryProviders(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    scoped_string response = queryProviders();

    return new_string(response);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeUpdateProvider(JNIEnv *env, jobject thiz,
                                                                    jobject completable,
                                                                    jstring type,
                                                                    jstring name) {
    TRACE_METHOD();

    jobject _completable = new_global(completable);
    scoped_string _type = get_string(type);
    scoped_string _name = get_string(name);

    updateProvider(_completable, _type, _name);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeQueryConfiguration(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    scoped_string response = queryConfiguration();

    return new_string(response);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeInspectCompiledGroups(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jstring yaml_text,
                                                                            jstring profile_dir,
                                                                            jboolean exclude_not_selectable) {
    TRACE_METHOD();

    scoped_string _yaml_text = get_string(yaml_text);
    scoped_string _profile_dir = get_string(profile_dir);
    scoped_string response = inspectCompiledGroups(_yaml_text, _profile_dir, (int) exclude_not_selectable);

    if (response == NULL)
        return NULL;

    return new_string(response);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeSubscribeLogcat(JNIEnv *env, jobject thiz,
                                                                     jobject callback) {
    TRACE_METHOD();

    jobject _callback = new_global(callback);

    subscribeLogcat(_callback);
}


static jmethodID m_tun_interface_mark_socket;
static jmethodID m_tun_interface_query_socket_owner;
static jmethodID m_completable_complete;
static jmethodID m_completable_complete_exceptionally;
static jmethodID m_logcat_interface_received;
static jmethodID m_clash_exception;
static jmethodID m_fetch_callback_report;
static jmethodID m_fetch_callback_complete;
static jmethodID m_open;
static jmethodID m_get_message;
static jclass c_clash_exception;
static jclass c_content;
static jobject o_unit;

static void call_tun_interface_mark_socket_impl(void *tun_interface, int fd) {
    TRACE_METHOD();

    ATTACH_JNI();

    env->CallVoidMethod((jobject) tun_interface,
                        (jmethodID) m_tun_interface_mark_socket,
                        (jint) fd);
}

static char *call_tun_interface_query_socket_owner_impl(void *tun_interface, int protocol,
                                                        const char *source, const char *target) {
    TRACE_METHOD();

    ATTACH_JNI();

    jstring source_string = new_string(source);
    jstring target_string = new_string(target);
    jstring result = (jstring) env->CallObjectMethod(
            (jobject) tun_interface,
            (jmethodID) m_tun_interface_query_socket_owner,
            (jint) protocol,
            source_string,
            target_string);

    if (source_string != NULL) {
        env->DeleteLocalRef(source_string);
    }
    if (target_string != NULL) {
        env->DeleteLocalRef(target_string);
    }

    if (jni_catch_exception(env) || result == NULL) {
        return NULL;
    }

    scoped_string value = get_string(result);
    env->DeleteLocalRef(result);
    return value == NULL ? NULL : strdup(value);
}

static void call_completable_complete_impl(void *completable, const char *exception) {
    TRACE_METHOD();

    ATTACH_JNI();

    if (exception == NULL) {
        env->CallBooleanMethod(
                (jobject) completable,
                (jmethodID) m_completable_complete,
                (jobject) o_unit);
    } else {
        jstring exception_string = new_string(exception);
        jthrowable _exception = (jthrowable)
                env->NewObject(
                        (jclass) c_clash_exception,
                        (jmethodID) m_clash_exception,
                        exception_string
                );

        env->CallBooleanMethod(
                (jobject) completable,
                (jmethodID) m_completable_complete_exceptionally,
                (jobject) _exception);

        if (exception_string != NULL) {
            env->DeleteLocalRef(exception_string);
        }
        if (_exception != NULL) {
            env->DeleteLocalRef(_exception);
        }
    }
}

static void call_completable_complete_with_string_impl(void *completable, const char *result) {
    TRACE_METHOD();

    ATTACH_JNI();

    jstring result_string = new_string(result);
    env->CallBooleanMethod(
            (jobject) completable,
            (jmethodID) m_completable_complete,
            result_string);

    if (result_string != NULL) {
        env->DeleteLocalRef(result_string);
    }
}

static void call_fetch_callback_report_impl(void *fetch_callback, const char *status_json) {
    TRACE_METHOD();

    ATTACH_JNI();

    jstring _status_json = new_string(status_json);

    env->CallVoidMethod(
            (jobject) fetch_callback,
            (jmethodID) m_fetch_callback_report,
            (jstring) _status_json);

    if (_status_json != NULL) {
        env->DeleteLocalRef(_status_json);
    }
}

static void call_fetch_callback_complete_impl(void *fetch_callback, const char *error) {
    TRACE_METHOD();

    ATTACH_JNI();

    jstring _error = NULL;

    if (error != NULL)
        _error = new_string(error);

    env->CallVoidMethod(
            (jobject) fetch_callback,
            (jmethodID) m_fetch_callback_complete,
            (jstring) _error);

    if (_error != NULL) {
        env->DeleteLocalRef(_error);
    }
}

static int call_logcat_interface_received_impl(void *callback, const char *payload) {
    TRACE_METHOD();

    ATTACH_JNI();

    jstring payload_string = new_string(payload);
    env->CallVoidMethod(
            (jobject) callback,
            (jmethodID) m_logcat_interface_received,
            payload_string);

    if (payload_string != NULL) {
        env->DeleteLocalRef(payload_string);
    }

    if (jni_catch_exception(env)) {
        return 1;
    }

    return 0;
}

static int open_content_impl(const char *url, char *error, int error_length) {
    TRACE_METHOD();

    ATTACH_JNI();

    jstring url_string = new_string(url);
    int fd = env->CallStaticIntMethod(c_content, m_open, url_string);

    if (url_string != NULL) {
        env->DeleteLocalRef(url_string);
    }

    if (env->ExceptionCheck()) {
        jthrowable exception = env->ExceptionOccurred();

        env->ExceptionClear();

        jstring message = (jstring) env->CallObjectMethod(
                (jthrowable) exception,
                (jmethodID) m_get_message
        );

        if (message == NULL) {
            strncpy(error, "unknown", error_length - 1);
        } else {
            scoped_string _message = get_string(message);

            strncpy(error, _message, error_length - 1);
            env->DeleteLocalRef(message);
        }

        env->DeleteLocalRef(exception);

        return -1;
    }

    return fd;
}

static void release_jni_object_impl(void *obj) {
    TRACE_METHOD();

    ATTACH_JNI();

    del_global((jobject) obj);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    TRACE_METHOD();

    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK)
        return JNI_ERR;

    initialize_jni(vm, env);

    jclass c_tun_interface = find_class("com/github/yumelira/yumebox/core/bridge/TunInterface");
    jclass c_completable = find_class("kotlinx/coroutines/CompletableDeferred");
    jclass c_fetch_callback = find_class("com/github/yumelira/yumebox/core/bridge/FetchCallback");
    jclass c_logcat_interface = find_class("com/github/yumelira/yumebox/core/bridge/LogcatInterface");
    jclass _c_clash_exception = find_class("com/github/yumelira/yumebox/core/bridge/ClashException");
    jclass _c_content = find_class("com/github/yumelira/yumebox/core/bridge/Content");
    jclass c_throwable = find_class("java/lang/Throwable");
    jclass c_unit = find_class("kotlin/Unit");

    m_tun_interface_mark_socket = find_method(c_tun_interface, "markSocket",
                                              "(I)V");
    m_tun_interface_query_socket_owner = find_method(c_tun_interface, "querySocketOwner",
                                                     "(ILjava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    m_completable_complete = find_method(c_completable, "complete",
                                         "(Ljava/lang/Object;)Z");
    m_fetch_callback_report = find_method(c_fetch_callback, "report",
                                          "(Ljava/lang/String;)V");
    m_fetch_callback_complete = find_method(c_fetch_callback, "complete",
                                            "(Ljava/lang/String;)V");
    m_completable_complete_exceptionally = find_method(c_completable, "completeExceptionally",
                                                       "(Ljava/lang/Throwable;)Z");
    m_logcat_interface_received = find_method(c_logcat_interface, "received",
                                              "(Ljava/lang/String;)V");
    m_clash_exception = find_method(_c_clash_exception, "<init>",
                                    "(Ljava/lang/String;)V");
    m_get_message = find_method(c_throwable, "getMessage",
                                "()Ljava/lang/String;");
    m_open = env->GetStaticMethodID(_c_content, "open",
                                    "(Ljava/lang/String;)I");

    o_unit = env->GetStaticObjectField(c_unit,
                                       env->GetStaticFieldID(c_unit, "INSTANCE",
                                                             "Lkotlin/Unit;"));

    c_clash_exception = (jclass) new_global(_c_clash_exception);
    c_content = (jclass) new_global(_c_content);
    o_unit = new_global(o_unit);

    mark_socket_func = &call_tun_interface_mark_socket_impl;
    query_socket_owner_func = &call_tun_interface_query_socket_owner_impl;
    complete_func = &call_completable_complete_impl;
    complete_with_string_func = &call_completable_complete_with_string_impl;
    fetch_report_func = &call_fetch_callback_report_impl;
    fetch_complete_func = &call_fetch_callback_complete_impl;
    logcat_received_func = &call_logcat_interface_received_impl;
    open_content_func = &open_content_impl;
    release_object_func = &release_jni_object_impl;

    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCoreVersion(JNIEnv *env, jobject thiz) {
    TRACE_METHOD();

    const char* Version = make_String(GIT_VERSION);

    return new_string(Version);
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeSetCustomUserAgent(JNIEnv *env, jobject thiz, jstring user_agent) {
    TRACE_METHOD();

    scoped_string ua = get_string(user_agent);

    setCustomUserAgent(ua);
}


typedef char* (*override_compile_raw_fn)(const char*);
typedef void (*override_free_string_fn)(char*);

extern char* compiledRawResultSummary(const char*);
extern char* compiledRawFallbackSummary(const char*);
extern char* inspectErrorResult(const char*);
extern char* inspectCompiledGroupsResult(const char*, const char*, int);
extern char* inspectCompiledTunRouteExcludeAddressResult(const char*);

struct override_symbols {
    override_compile_raw_fn compile_raw;
    override_free_string_fn free_string;
};

static override_symbols resolve_override_symbols() {
    static override_symbols symbols = {nullptr, nullptr};
    static std::once_flag resolve_once;

    std::call_once(resolve_once, []() {
        // liboverride.so is loaded via System.loadLibrary with LOCAL symbol visibility, so its
        // exports are NOT in libbridge's RTLD_DEFAULT scope (it is not a DT_NEEDED dependency).
        // Resolve against an explicit handle to the already-loaded library; dlsym(handle) is
        // unaffected by RTLD_LOCAL. Fall back to RTLD_DEFAULT if the handle cannot be obtained.
        void* handle = dlopen("liboverride.so", RTLD_NOW | RTLD_NOLOAD);
        if (handle == nullptr) {
            handle = dlopen("liboverride.so", RTLD_NOW);
        }
        void* scope = handle != nullptr ? handle : RTLD_DEFAULT;
        symbols.compile_raw = (override_compile_raw_fn)dlsym(scope, "override_compile_raw");
        symbols.free_string = (override_free_string_fn)dlsym(scope, "override_free_string");
    });

    return symbols;
}

static char* compile_override_raw_result(const char* request_json, override_symbols symbols) {
    if (!symbols.compile_raw || !symbols.free_string) {
        return NULL;
    }
    return symbols.compile_raw(request_json);
}

struct raw_compile_payload {
    char* config_raw;
    char* summary_json;
    char* error;
};

static void free_raw_compile_payload(raw_compile_payload* payload) {
    if (payload == NULL) {
        return;
    }
    free(payload->config_raw);
    free(payload->summary_json);
    free(payload->error);
    payload->config_raw = NULL;
    payload->summary_json = NULL;
    payload->error = NULL;
}

static const char* raw_compile_error_or_default(raw_compile_payload* payload, const char* fallback) {
    if (payload != NULL && payload->error != NULL) {
        return payload->error;
    }
    return fallback;
}

static char* raw_compile_summary_or_fallback(raw_compile_payload* payload, const char* fallback_error) {
    if (payload != NULL && payload->summary_json != NULL) {
        return strdup(payload->summary_json);
    }
    return compiledRawFallbackSummary(fallback_error);
}

static raw_compile_payload compile_override_raw_payload(const char* request_json, override_symbols symbols) {
    raw_compile_payload payload = {NULL, NULL, NULL};
    char* result_json = compile_override_raw_result(request_json, symbols);
    if (result_json == NULL) {
        payload.error = strdup("compile raw config failed");
        return payload;
    }

    payload.summary_json = compiledRawResultSummary(result_json);
    char* result_error = compiledRawResultError(result_json);
    if (result_error != NULL) {
        payload.error = strdup(result_error);
        free(result_error);
        symbols.free_string(result_json);
        return payload;
    }

    payload.config_raw = compiledRawResultConfigRaw(result_json);
    symbols.free_string(result_json);
    if (payload.config_raw == NULL) {
        payload.error = strdup("compile raw config returned empty configRaw");
    }
    return payload;
}

static char* compile_override_raw_config(const char* request_json, override_symbols symbols, char** error) {
    if (error != NULL) {
        *error = NULL;
    }
    raw_compile_payload payload = compile_override_raw_payload(request_json, symbols);
    char* config_raw = payload.config_raw;
    payload.config_raw = NULL;
    if (config_raw == NULL && error != NULL && payload.error != NULL) {
        *error = strdup(payload.error);
    }
    free_raw_compile_payload(&payload);
    return config_raw;
}

JNIEXPORT void JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCompileAndLoadConfig(JNIEnv *env, jobject thiz,
                                                                         jobject completable,
                                                                         jstring request_json) {
    TRACE_METHOD();

    override_symbols symbols = resolve_override_symbols();

    if (!symbols.compile_raw || !symbols.free_string) {
        jobject _completable = new_global(completable);
        call_completable_complete_impl(_completable, "override library symbols not found");
        release_jni_object_impl(_completable);
        return;
    }

    jobject _completable = new_global(completable);
    scoped_string _request_json = get_string(request_json);

    // Call Rust to compile the source to RawConfig JSON in native memory.
    scoped_string compile_error = NULL;
    char* config_raw = compile_override_raw_config(_request_json, symbols, &compile_error);

    if (config_raw == NULL) {
        call_completable_complete_impl(_completable, compile_error != NULL ? compile_error : "compile raw config failed");
        release_jni_object_impl(_completable);
        return;
    }

    // Call Go to load the raw config (async, spawns goroutine, calls complete)
    loadCompiledRaw(_completable, config_raw);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCompileAndLoadConfigSummary(JNIEnv *env,
                                                                                      jobject thiz,
                                                                                      jobject completable,
                                                                                      jstring request_json) {
    TRACE_METHOD();

    override_symbols symbols = resolve_override_symbols();

    if (!symbols.compile_raw || !symbols.free_string) {
        jobject _completable = new_global(completable);
        scoped_string summary_json = compiledRawFallbackSummary("override library symbols not found");
        call_completable_complete_impl(_completable, "override library symbols not found");
        release_jni_object_impl(_completable);
        return new_string(summary_json);
    }

    jobject _completable = new_global(completable);
    scoped_string _request_json = get_string(request_json);
    raw_compile_payload payload = compile_override_raw_payload(_request_json, symbols);
    if (payload.config_raw == NULL) {
        const char* compile_error = raw_compile_error_or_default(&payload, "compile raw config failed");
        scoped_string summary_json = raw_compile_summary_or_fallback(&payload, compile_error);
        call_completable_complete_impl(_completable, compile_error);
        free_raw_compile_payload(&payload);
        release_jni_object_impl(_completable);
        return new_string(summary_json);
    }

    if (payload.summary_json == NULL) {
        scoped_string summary_json = compiledRawFallbackSummary("compile raw summary failed");
        free_raw_compile_payload(&payload);
        call_completable_complete_impl(_completable, "compile raw summary failed");
        release_jni_object_impl(_completable);
        return new_string(summary_json);
    }

    char* config_raw = payload.config_raw;
    payload.config_raw = NULL;
    jstring summary = new_string(payload.summary_json);
    free_raw_compile_payload(&payload);
    loadCompiledRaw(_completable, config_raw);
    return summary;
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCompileAndInspectGroups(JNIEnv *env, jobject thiz,
                                                                              jstring request_json,
                                                                              jstring profile_dir,
                                                                              jboolean exclude_not_selectable) {
    TRACE_METHOD();

    override_symbols symbols = resolve_override_symbols();

    if (!symbols.compile_raw || !symbols.free_string) {
        scoped_string error_result = inspectErrorResult("override library symbols not found");
        return new_string(error_result);
    }

    scoped_string _request_json = get_string(request_json);
    scoped_string _profile_dir = get_string(profile_dir);

    // Compile encrypted source to RawConfig JSON in native memory.
    scoped_string compile_error = NULL;
    char* config_raw = compile_override_raw_config(_request_json, symbols, &compile_error);
    if (config_raw == NULL) {
        scoped_string error_result = inspectErrorResult(compile_error != NULL ? compile_error : "compile raw config failed");
        return new_string(error_result);
    }

    // Inspect groups via Go (returns YAML of group list)
    scoped_string groups_yaml = inspectCompiledGroupsResult(config_raw, _profile_dir, (int) exclude_not_selectable);

    free(config_raw);

    if (groups_yaml == NULL) {
        scoped_string error_result = inspectErrorResult("inspect compiled groups failed");
        return new_string(error_result);
    }

    return new_string(groups_yaml);
}

JNIEXPORT jstring JNICALL
Java_com_github_yumelira_yumebox_core_bridge_Bridge_nativeCompileAndInspectTunRouteExcludeAddress(JNIEnv *env,
                                                                                                  jobject thiz,
                                                                                                  jstring request_json) {
    TRACE_METHOD();

    override_symbols symbols = resolve_override_symbols();

    if (!symbols.compile_raw || !symbols.free_string) {
        scoped_string error_result = inspectErrorResult("override library symbols not found");
        return new_string(error_result);
    }

    scoped_string _request_json = get_string(request_json);

    scoped_string compile_error = NULL;
    char* config_raw = compile_override_raw_config(_request_json, symbols, &compile_error);
    if (config_raw == NULL) {
        scoped_string error_result = inspectErrorResult(compile_error != NULL ? compile_error : "compile raw config failed");
        return new_string(error_result);
    }

    scoped_string route_exclude_address_json = inspectCompiledTunRouteExcludeAddressResult(config_raw);

    free(config_raw);

    if (route_exclude_address_json == NULL) {
        scoped_string error_result = inspectErrorResult("inspect compiled tun route-exclude-address failed");
        return new_string(error_result);
    }

    return new_string(route_exclude_address_json);
}

} // extern "C"
