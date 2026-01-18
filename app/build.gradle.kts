@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import java.util.Properties

data class AndroidMainPaths(val root: String) {
    val kotlin = "$root/kotlin"
    val res = "$root/res"
    val assets = "$root/assets"
    val manifest = "$root/AndroidManifest.xml"
    val aboutLibrariesJson = "$res/aboutlibraries.json"
}

fun DependencyHandler.addAll(configuration: String, dependencies: Iterable<Any>) {
    dependencies.forEach { add(configuration, it) }
}

fun ApplicationExtension.configureAndroidMain(paths: AndroidMainPaths) {
    sourceSets {
        named("main") {
            java.srcDirs(paths.kotlin)
            res.setSrcDirs(listOf(paths.res))
            assets.setSrcDirs(listOf(paths.assets))
            manifest.srcFile(paths.manifest)
        }
    }
}

fun ApplicationExtension.configureSigningConfig() {
    val keystore = rootProject.file("signing.properties")
    if (!keystore.exists()) return

    signingConfigs {
        create("release") {
            val prop = Properties().also { props ->
                keystore.inputStream().use { stream -> props.load(stream) }
            }
            storeFile = rootProject.file("release.keystore")
            storePassword = prop.getProperty("keystore.password")!!
            keyAlias = prop.getProperty("key.alias")!!
            keyPassword = prop.getProperty("key.password")!!
        }
    }
}

fun ApplicationExtension.configureBuildTypes() {
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            isJniDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

fun ApplicationExtension.configureAbiSplits(appAbiList: List<String>) {
    splits {
        abi {
            //noinspection WrongGradleMethod
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            //noinspection ChromeOsAbiSupport
            include(*appAbiList.toTypedArray())
            isUniversalApk = false
        }
    }
}

fun ApplicationExtension.configurePackaging() {
    packaging {
        jniLibs {
            excludes += listOf("lib/**/libjavet*.so")
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "Sub-Store/**",
                "**/*.kotlin_builtins",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "META-INF/**",
                "index.*.bin",
            )
        }
    }
}

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("dev.oom-wg.purejoy.fyl.fytxt")
    id("yumebox.geo.assets")
}

val androidMainPaths = AndroidMainPaths("src/androidMain")

fytxt {
    langSrcs = mapOf("lang" to layout.projectDirectory.dir("../lang"))
    packageName = "dev.oom_wg.purejoy.mlang"
    objectName = "MLang"
    defaultLang = "ZH"
    composeGen = true
    internalClass = false
}

val appNamespace = gropify.project.namespace.base
val appName = gropify.project.name
val jvmVersionNumber = gropify.project.jvm
val javaVersion = JavaVersion.toVersion(jvmVersionNumber) ?: JavaVersion.VERSION_17
val appAbiList = gropify.abi.app.list.split(",").map { it.trim() }
val localeList = gropify.locale.app.list.split(",").map { it.trim() }
val targetAbi = project.findProperty("android.injected.build.abi") as String?
val mmkvVersion = when (targetAbi) {
    "arm64-v8a", "x86_64" -> "2.2.4"
    else -> "1.3.14"
}
val mmkvDependency = "com.tencent:mmkv:$mmkvVersion"

android {
    namespace = appNamespace
    compileSdk = gropify.android.compileSdk

    defaultConfig {
        applicationId = appNamespace
        minSdk = gropify.android.minSdk
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
        manifestPlaceholders["appName"] = appName
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    configureAndroidMain(androidMainPaths)

    kotlin {
        jvmToolchain(jvmVersionNumber)
    }

    androidResources {
        localeFilters += localeList
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    configureSigningConfig()
    configureBuildTypes()
    configureAbiSplits(appAbiList)
    configurePackaging()
}

geoAssets {
    val assets = mapOf(
        "geoip.metadb" to gropify.asset.geoip.url,
        "geosite.dat" to gropify.asset.geosite.url,
        "ASN.mmdb" to gropify.asset.asn.url,
    )
    assetUrls.putAll(assets)
    outputDirectory.set(layout.projectDirectory.dir(androidMainPaths.assets))
}

dependencies {
    addAll(
        "implementation",
        listOf(
            project(":core"),
            compose.runtime,
            compose.foundation,
            compose.ui,
            compose.components.uiToolingPreview,
            compose.preview,
            "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0",
            "androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0",
            "androidx.lifecycle:lifecycle-runtime-compose:2.10.0",
            "androidx.activity:activity-compose:1.12.2",
            "top.yukonga.miuix.kmp:miuix:0.7.2",
            "dev.chrisbanes.haze:haze-materials:1.7.1",
            mmkvDependency,
            "io.insert-koin:koin-core:4.1.1",
            "io.insert-koin:koin-android:4.1.1",
            "io.insert-koin:koin-androidx-compose:4.1.1",
            "io.github.raamcosta.compose-destinations:core:2.3.0",
            "com.squareup.okhttp3:okhttp:5.3.2",
            "com.jakewharton.timber:timber:5.0.1",
            "com.caoccao.javet:javet-node-android:5.0.2",
            "com.highcapable.pangutext:pangutext-android:1.0.5",
            "org.apache.commons:commons-compress:1.28.0",
            project.dependencies.platform("com.google.firebase:firebase-bom:34.6.0"),
            "com.google.firebase:firebase-crashlytics-ndk",
            "com.google.firebase:firebase-analytics",
            "com.google.mlkit:barcode-scanning:17.3.0",
            "androidx.camera:camera-camera2:1.5.2",
            "androidx.camera:camera-lifecycle:1.5.2",
            "androidx.camera:camera-view:1.5.2",
            "androidx.camera:camera-core:1.5.2",
            "androidx.camera:camera-video:1.5.2",
            "io.ktor:ktor-client-core:3.3.3",
            "io.ktor:ktor-client-android:3.3.3",
            "io.ktor:ktor-client-content-negotiation:3.3.3",
            "io.ktor:ktor-serialization-kotlinx-json:3.3.3",
            "io.coil-kt.coil3:coil-compose:3.3.0",
            "io.coil-kt.coil3:coil-network-okhttp:3.3.0",
            "io.coil-kt.coil3:coil-svg:3.3.0",
            "com.mikepenz:aboutlibraries-core:13.2.1",
            "com.mikepenz:aboutlibraries-compose:13.2.1",
            "com.mikepenz:aboutlibraries-compose-m3:13.2.1",
            "sh.calvin.reorderable:reorderable:2.5.0",
        ),
    )

    addAll(
        "coreLibraryDesugaring",
        listOf("com.android.tools:desugar_jdk_libs:2.1.5"),
    )

    addAll(
        "debugImplementation",
        listOf(compose.uiTooling),
    )

    addAll(
        "ksp",
        listOf("io.github.raamcosta.compose-destinations:ksp:2.3.0"),
    )
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

aboutLibraries {
    export {
        outputFile = file(androidMainPaths.aboutLibrariesJson)
    }
}
