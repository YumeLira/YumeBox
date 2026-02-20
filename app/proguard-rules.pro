# ========================================
# YumeBox KMP ProGuard Configuration
# ========================================
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ========================================
# Native Methods & Android Core
# ========================================
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Bridge class and all native methods
-keep class com.github.yumelira.yumebox.core.bridge.** { *; }

# Keep Global class
-keep class com.github.yumelira.yumebox.core.Global { *; }

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ========================================
# Kotlin Multiplatform (KMP) Preservation
# ========================================

# --------------------------------
# Kotlin Core & Metadata
# --------------------------------
# Preserve Kotlin metadata - Critical for KMP
-keep class kotlin.Metadata { *; }

# Preserve all Kotlin internal classes used by KMP
-keep class kotlin.** { *; }
-keep class kotlin.internal.** { *; }

# Preserve KMP platform-specific implementations
-keep class kotlin.native.** { *; }
-keep class kotlin.js.** { *; }

# Preserve Kotlin collections and utility classes
-keep class kotlin.collections.** { *; }
-keep class kotlin.ranges.** { *; }
-keep class kotlin.text.** { *; }
-keep class kotlin.experimental.** { *; }

# --------------------------------
# Kotlin Coroutines (KMP)
# --------------------------------
# Complete coroutines preservation for KMP
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepclassmembers interface kotlinx.coroutines.** { *; }

# Coroutine Builders
-keep class kotlinx.coroutines.flow.** { *; }
-keep interface kotlinx.coroutines.flow.** { *; }

# Channel & ReceiveChannel
-keep class kotlinx.coroutines.channels.** { *; }
-keep interface kotlinx.coroutines.channels.** { *; }

# Async/Deferred patterns
-keepnames class kotlinx.coroutines.CompletableDeferred {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Main dispatcher optimizations
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatcherLoader {
    boolean FAST_SERVICE_LOADER_ENABLED return false;
}
-assumenosideeffects class kotlinx.coroutines.internal.FastServiceLoaderKt {
    boolean ANDROID_DETECTED return true;
}
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatchersKt {
    boolean SUPPORT_MISSING return false;
}
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    boolean getASSERTIONS_ENABLED() return false;
    boolean getDEBUG() return false;
    boolean getRECOVER_STACK_TRACES() return false;
}

# Preserve coroutine scope implementations
-keep class kotlinx.coroutines.** implements kotlinx.coroutines.CoroutineScope {
    public <methods>;
}

# Android-specific coroutine support
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# --------------------------------
# Kotlinx Serialization (KMP)
# --------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt

# Complete serialization support
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve serialization polymorphic serialization
-keepclassmembers class ** {
    kotlinx.serialization.PolymorphicSerializer serializer;
}
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
-dontwarn kotlinx.serialization.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# --------------------------------
# Kotlinx DateTime (KMP)
# --------------------------------
# Preserve DateTime library
-keep class kotlinx.datetime.** { *; }
-keep class org.jetbrains.kotlinx.datetime.** { *; }

# --------------------------------
# Ktor (KMP HTTP Client)
# --------------------------------
# Preserve Ktor client/server
-keep class io.ktor.** { *; }
-keep class io.ktor.client.** { *; }
-keep class io.ktor.server.** { *; }
-keep class io.ktor.util.** { *; }
-keep class io.ktor.http.** { *; }
-keep class io.ktor.content.** { *; }
-keep class io.ktor.routing.** { *; }
-keep class io.ktor.features.** { *; }
-keep interface io.ktor.** { *; }
-keep interface io.ktor.client.** { *; }
-keep interface io.ktor.server.** { *; }

# --------------------------------
# Logging (KMP)
# --------------------------------
# Preserve SLF4J and common logging
-keep class org.slf4j.** { *; }
-keep class kotlin.logging.** { *; }

# --------------------------------
# AtomicFu (KMP)
# --------------------------------
# Preserve Atomic operations
-keep class kotlinx.atomicfu.** { *; }
-keep class org.jetbrains.kotlinx.atomicfu.** { *; }

# --------------------------------
# Coroutines Utils (KMP)
# --------------------------------
# Preserve coroutines utilities
-keep class kotlinx.coroutines.rx2.** { *; }
-keep class kotlinx.coroutines.rx3.** { *; }

# ========================================
# JetBrains Libraries
# ========================================
-keep class org.jetbrains.** { *; }
-keep interface org.jetbrains.** { *; }
-keepclassmembers class org.jetbrains.** { *; }

# ========================================
# Android Specific Libraries
# ========================================

# --------------------------------
# Compose (KMP)
# --------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Preserve Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep interface androidx.compose.runtime.** { *; }

# Preserve Compose UI
-keep class androidx.compose.ui.** { *; }
-keep interface androidx.compose.ui.** { *; }

# Preserve Compose foundation
-keep class androidx.compose.foundation.** { *; }
-keep interface androidx.compose.foundation.** { *; }

# Preserve Compose material
-keep class androidx.compose.material.** { *; }
-keep interface androidx.compose.material.** { *; }

# ========================================
# Kotlin Null Safety Optimization
# ========================================
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkNotNullExpressionValue(...);
}

# ========================================
# Project-Specific KMP Classes
# ========================================
# Preserve KMP platform identifier
-keep class com.github.yumelira.yumebox.core.KMPIdentifier { *; }

# Preserve actual implementation
-keep class com.github.yumelira.yumebox.core.platform.** { *; }

# Preserve KMP shared module
-keep class com.github.yumelira.yumebox.shared.** { *; }
-keep interface com.github.yumelira.yumebox.shared.** { *; }

# ========================================
# JavaScript Interop (KMP)
# ========================================
# Preserve JS interoperability if used
-keep class kotlin.js.** { *; }
-keep class kotlinx.browser.** { *; }
-keep class kotlinx.wasm.** { *; }

# ========================================
# Reflection & Serialization
# ========================================
# Preserve reflection for KMP
-keep class kotlin.reflect.** { *; }
-keepclassmembers class ** {
    ** Companion;
    ** INSTANCE;
}

# Preserve enums for serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========================================
# Optimization Configuration
# ========================================
# Aggressive optimization for better compression
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-dontpreverify
-repackageclasses ''

# Preserve metadata attributes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
-keepattributes LineNumberTable, SourceFile

# ========================================
# Third-Party Libraries
# ========================================

# --------------------------------
# Javet (JavaScript Engine for KMP)
# --------------------------------
-keep class com.caoccao.javet.** { *; }
-keep interface com.caoccao.javet.** { *; }

# Suppress warnings for JMX classes not available on Android
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn javax.management.NotificationListener

# --------------------------------
# Apache Commons (Compress)
# --------------------------------
-dontwarn com.github.luben.zstd.**
-dontwarn org.tukaani.xz.**

# --------------------------------
# ASM & Bytecode
# --------------------------------
-dontwarn org.objectweb.asm.**

# --------------------------------
# Brotli
# --------------------------------
-dontwarn org.brotli.dec.**

# --------------------------------
# Google Play Services & Firebase
# --------------------------------
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# --------------------------------
# Other Missing Classes
# --------------------------------
-dontwarn java.lang.invoke.MethodHandleProxies
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.lang.model.element.Modifier

# ========================================
# Warning Suppression for KMP
# ========================================
# Suppress warnings that don't affect KMP functionality
-dontwarn kotlinx.coroutines.flow.internal.**
-dontwarn kotlinx.serialization.descriptors.**
-dontwarn kotlin.**

# ========================================
# Notes
# ========================================
# - This configuration is optimized for Kotlin Multiplatform projects
# - It preserves all KMP-specific metadata and platform implementations
# - If you encounter issues with specific features, add targeted -keep rules
# - For smaller APK size, comment out unnecessary -keep rules (but test first)
# - The -assumenosideeffects rules optimize away null checks and debugging code

# ========================================
# EMAS Update SDK
# ========================================
-keepclassmembers class * {
    @com.google.inject.Inject <init>(...);
}
-keepclassmembers class * {
    void *(**On*Event);
}
-keepclassmembers class ** {
    public <init>(android.content.Context);
}
-keepclassmembernames class **.R$* {*;}
-keepclassmembernames class **.R {*;}
-keepclassmembers class **{
    public static final <fields>;
}

-keep class com.taobao.update.** { *; }
-keep class com.alibaba.sdk.android.update.** { *; }
-keep class mtopsdk.** { *; }
-keep class com.taobao.accs.** { *; }

-keepattributes InnerClasses
-keepattributes Signature
-keepattributes EnclosingMethod

-keepclassmembers class * {
    public <init>();
}

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
