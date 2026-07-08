# ========================================
# R8 Configuration: Shrink, Optimize, Obfuscate
# ========================================
-optimizationpasses 5
-allowaccessmodification
-repackageclasses
-adaptclassstrings

# ========================================
# Native / Android Core
# ========================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# JNI bridge entry points
-keep class com.github.yumelira.yumebox.core.bridge.** { *; }
-keep class com.github.yumelira.yumebox.core.Global {
    public static ** INSTANCE;
    public final android.content.Context getApplication();
    public final void init(android.content.Context);
    public final void destroy();
}

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
-keepclassmembers class * {
    @com.caoccao.javet.annotations.V8Function <methods>;
    @com.caoccao.javet.annotations.V8Property <methods>;
}

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

-keepclassmembernames class **.R$* { *; }
-keepclassmembernames class **.R { *; }

# ========================================
# ML Kit (Google) - Component registration only
# ========================================
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep @com.google.firebase.components.ComponentRegistrar class * { *; }
-keep class com.google.mlkit.vision.barcode.BarcodeScannerOptions { *; }
-keep class com.google.mlkit.vision.barcode.common.Barcode { *; }
-keep class com.google.android.libraries.barhopper.** { *; }

# ========================================
# Koin Dependency Injection
# ========================================
-keepnames class * extends org.koin.core.component.KoinComponent
-keepclassmembers class * {
    @org.koin.core.inject *** inject(...);
}
