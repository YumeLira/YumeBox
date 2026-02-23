# ========================================
# Native / Android Core
# ========================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# JNI bridge entry points
-keep class com.github.yumelira.yumebox.core.bridge.** { *; }
-keep class com.github.yumelira.yumebox.core.Global { *; }

# Parcelable CREATOR
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ========================================
# Kotlin / Serialization (targeted)
# ========================================
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
-keepattributes LineNumberTable, SourceFile

# kotlinx.serialization generated serializers / companions
-dontnote kotlinx.serialization.AnnotationsKt
-dontwarn kotlinx.serialization.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$$serializer {
    static ** INSTANCE;
}

# Enum serializers often rely on these members
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# JNI bridge (core/src/cpp/main.c) reflects these exact Kotlin/coroutines types by name.
# Do not obfuscate/remove them.
-keep class kotlin.Unit {
    public static final kotlin.Unit INSTANCE;
}
-keep interface kotlinx.coroutines.CompletableDeferred { *; }

# Optional micro-optimization: strip Kotlin runtime null-check helpers
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
}

# Coroutines debug flags (safe shrinking)
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    boolean getASSERTIONS_ENABLED() return false;
    boolean getDEBUG() return false;
    boolean getRECOVER_STACK_TRACES() return false;
}

# ========================================
# Javet / Native JS
# ========================================
-keep class com.caoccao.javet.** { *; }
-keep interface com.caoccao.javet.** { *; }

# JMX classes not available on Android
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn javax.management.NotificationListener

# Compression / parsing optional classes
-dontwarn com.github.luben.zstd.**
-dontwarn org.tukaani.xz.**
-dontwarn org.objectweb.asm.**
-dontwarn org.brotli.dec.**

# Misc missing classes on Android / desugared env
-dontwarn java.lang.invoke.MethodHandleProxies
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.lang.model.element.Modifier

# ========================================
# EMAS / Taobao Update SDK (reflection heavy)
# ========================================
# Scope reflection-based constructor/event keeps to update SDK packages only
-keepclassmembers class com.taobao.update.** {
    @com.google.inject.Inject <init>(...);
    void *(**On*Event);
    public <init>();
    public <init>(android.content.Context);
}
-keepclassmembers class com.alibaba.sdk.android.update.** {
    @com.google.inject.Inject <init>(...);
    void *(**On*Event);
    public <init>();
    public <init>(android.content.Context);
}
-keepclassmembers class mtopsdk.** {
    public <init>();
    public <init>(android.content.Context);
}
-keepclassmembers class com.taobao.accs.** {
    public <init>();
    public <init>(android.content.Context);
}

-keepclassmembernames class **.R$* { *; }
-keepclassmembernames class **.R { *; }
-keepclassmembers class ** {
    public static final <fields>;
}

-keep class com.taobao.update.** { *; }
-keep class com.alibaba.sdk.android.update.** { *; }
-keep class mtopsdk.** { *; }
-keep class com.taobao.accs.** { *; }

-keep class com.taobao.update.apk.MainUpdateData { *; }
-keep class com.taobao.update.apk.ApkUpdater { *; }
-keep class com.taobao.update.common.framework.** { *; }
-keep class com.taobao.update.common.utils.** { *; }
-keep class com.taobao.update.common.dialog.** { *; }
-keep class com.taobao.update.common.Config { *; }
-keep class com.taobao.update.common.dialog.CustomUpdateInfo {
    public <methods>;
}
-keep interface com.taobao.update.common.dialog.UpdateNotifyListener { *; }

-dontwarn mtopsdk.mtop.intf.Mtop
-dontwarn com.taobao.update.**
