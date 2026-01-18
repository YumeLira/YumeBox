@file:Suppress("UnstableApiUsage")

import java.util.*

val androidMainDir = "src/androidMain"
val androidMainKotlinDir = "$androidMainDir/kotlin"
val androidMainResDir = "$androidMainDir/res"
val androidMainAssetsDir = "$androidMainDir/assets"
val androidMainManifest = "$androidMainDir/AndroidManifest.xml"

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

    sourceSets {
        named("main") {
            java.srcDirs(androidMainKotlinDir)
            res {
                setSrcDirs(listOf(androidMainResDir))
            }
            assets {
                setSrcDirs(listOf(androidMainAssetsDir))
            }
            manifest.srcFile(androidMainManifest)
        }
    }

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

    signingConfigs {
        val keystore = rootProject.file("signing.properties")
        if (keystore.exists()) {
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

geoAssets {
    val assets = mapOf(
        "geoip.metadb" to gropify.asset.geoip.url,
        "geosite.dat" to gropify.asset.geosite.url,
        "ASN.mmdb" to gropify.asset.asn.url,
    )
    assetUrls.putAll(assets)
    outputDirectory.set(layout.projectDirectory.dir(androidMainAssetsDir))
}

dependencies {
    implementation(project(":core"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)
    implementation(compose.preview)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("top.yukonga.miuix.kmp:miuix:0.7.2")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.1")
    implementation(mmkvDependency)
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-android:4.1.1")
    implementation("io.insert-koin:koin-androidx-compose:4.1.1")
    implementation("io.github.raamcosta.compose-destinations:core:2.3.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.caoccao.javet:javet-node-android:5.0.2")
    implementation("com.highcapable.pangutext:pangutext-android:1.0.5")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation(project.dependencies.platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")
    implementation("androidx.camera:camera-core:1.5.2")
    implementation("androidx.camera:camera-video:1.5.2")
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-android:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("io.coil-kt.coil3:coil-svg:3.3.0")
    implementation("com.mikepenz:aboutlibraries-core:13.2.1")
    implementation("com.mikepenz:aboutlibraries-compose:13.2.1")
    implementation("com.mikepenz:aboutlibraries-compose-m3:13.2.1")
    implementation("sh.calvin.reorderable:reorderable:2.5.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    debugImplementation(compose.uiTooling)
    ksp("io.github.raamcosta.compose-destinations:ksp:2.3.0")
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

aboutLibraries {
    export {
        outputFile = file("$androidMainResDir/aboutlibraries.json")
    }
}
