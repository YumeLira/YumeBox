@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.MergeSourceSetFolders
import core.mmkvDependencyNotation
import groovy.json.JsonSlurper
import tasks.DownloadGeoFilesTask

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("yumebox.base.android")
    id("yumebox.build.helpers")
}

private data class EmasConfigValues(
    val appKey: String,
    val appSecret: String,
    val channelId: String,
)

private fun Project.readEmasConfigMap(): Map<*, *> {
    val emasConfigFile = layout.projectDirectory.file("aliyun-emas-services.json").asFile
    return runCatching {
        if (!emasConfigFile.exists()) emptyMap<String, Any?>()
        else {
            val root = JsonSlurper().parse(emasConfigFile) as? Map<*, *> ?: emptyMap<Any, Any>()
            root["config"] as? Map<*, *> ?: emptyMap<Any, Any>()
        }
    }.getOrDefault(emptyMap<String, Any?>())
}

private fun Project.emasConfigValue(
    emasConfigMap: Map<*, *>,
    key: String,
    fallbackProperty: String,
    defaultValue: String = "",
): String {
    val fromJson = emasConfigMap[key]?.toString().orEmpty()
    if (fromJson.isNotBlank()) return fromJson
    return (findProperty(fallbackProperty) as? String)?.takeIf { it.isNotBlank() } ?: defaultValue
}

private fun Project.readEmasConfigValues(): EmasConfigValues {
    val configMap = readEmasConfigMap()
    return EmasConfigValues(
        appKey = emasConfigValue(configMap, "emas.appKey", "emas.appKey"),
        appSecret = emasConfigValue(configMap, "emas.appSecret", "emas.appSecret"),
        channelId = emasConfigValue(configMap, "emas.channelId", "emas.channelId", "official"),
    )
}

private fun DependencyHandlerScope.implementationProjects(vararg modules: String) {
    modules.forEach { implementation(project(it)) }
}

private fun DependencyHandlerScope.implementationAll(vararg deps: String) {
    deps.forEach(::implementation)
}

private fun DependencyHandlerScope.addAppProjectDependencies() {
    implementationProjects(
        ":core",
        ":platform",
        ":common",
        ":di",
        ":locale",
        ":ui",
        ":data:log",
        ":data:settings",
        ":data:proxy",
        ":runtime:api",
        ":runtime:client",
        ":runtime:service",
        ":feature:update",
        ":feature:web",
        ":feature:substore",
        ":feature:proxy",
    )
}

private fun TaskContainer.registerGeoFilesDownloadTask(
    geoFilesDownloadDir: org.gradle.api.file.Directory,
    assets: Map<String, String>,
): TaskProvider<DownloadGeoFilesTask> = register<DownloadGeoFilesTask>("downloadGeoFiles") {
    description = "Download GeoIP and GeoSite databases from MetaCubeX"
    group = "build setup"
    assetUrls.putAll(assets)
    outputDirectory.set(geoFilesDownloadDir)
}

val mmkvDependency = project.mmkvDependencyNotation()

val appNamespace = gropify.project.namespace.base
val appName = gropify.project.name
val androidJvmTarget = gropify.android.jvm.toString()
val appAbiList = gropify.abi.app.list.split(",").map { it.trim() }
val localeList = gropify.locale.app.list.split(",").map { it.trim() }
private val emasConfig = project.readEmasConfigValues()
val geoFilesAssetsDir = layout.buildDirectory.dir("generated/assets/geo/main")
val geoFilesDownloadDir = geoFilesAssetsDir.get()
val geoAssets = mapOf(
    "geoip.metadb" to gropify.asset.geoip.url,
    "geosite.dat" to gropify.asset.geosite.url,
    "ASN.mmdb" to gropify.asset.asn.url,
)
val downloadGeoFilesTask = tasks.registerGeoFilesDownloadTask(geoFilesDownloadDir, geoAssets)

android {
    namespace = appNamespace

    defaultConfig {
        applicationId = appNamespace
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
        manifestPlaceholders["appName"] = appName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Clarity Analytics Configuration
        buildConfigField("String", "CLARITY_PROJECT_ID", "\"${project.findProperty("clarity.projectId") ?: ""}\"")
        buildConfigField("String", "EMAS_APP_KEY", "\"${emasConfig.appKey}\"")
        buildConfigField("String", "EMAS_APP_SECRET", "\"${emasConfig.appSecret}\"")
        buildConfigField("String", "EMAS_CHANNEL_ID", "\"${emasConfig.channelId}\"")

    }

    sourceSets {
        named("main") {
            // Explicitly package local JNI libraries from app/src/main/jniLibs
            jniLibs.srcDir("src/main/jniLibs")
            // Build-generated geo assets merged during packaging/lint
            assets.srcDir(geoFilesAssetsDir)
        }
    }

    compileOptions { isCoreLibraryDesugaringEnabled = true }

    //noinspection WrongGradleMethod
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(androidJvmTarget))
        }
    }

    androidResources {
        // Don't generate automatic locale config, we'll specify locales manually
        generateLocaleConfig = false
        localeFilters.addAll(localeList)
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
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
            vcsInfo.include = false
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
            isUniversalApk = true
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

    // Use new androidComponents API instead of deprecated applicationVariants
    //noinspection WrongGradleMethod
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiName = output.filters.find {
                    it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"
                val buildTypeName = variant.buildType ?: "release"
                // Set correct versionName
                output.versionName.set(gropify.project.version.name)
                // Set APK output file name
                (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName.set(
                    "${appName}-${abiName}-${buildTypeName}.apk"
                )
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Internal modules
    addAppProjectDependencies()

    // Compose dependencies (using Jetpack Compose BOM for version management)
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementationAll(
        "androidx.compose.runtime:runtime",
        "androidx.compose.foundation:foundation",
        "androidx.compose.ui:ui",
        "androidx.compose.ui:ui-tooling-preview",
        "androidx.activity:activity-compose:1.12.4",
    )
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Additional Compose libraries
    val miuixVersion = "0.8.4"
    implementation("top.yukonga.miuix.kmp:miuix:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons:$miuixVersion")
    implementation("dev.chrisbanes.haze:haze:1.7.2")

    // Storage
    implementation(mmkvDependency)

    // Dependency Injection
    implementationAll(
        "io.insert-koin:koin-core:4.1.1",
        "io.insert-koin:koin-android:4.1.1",
        "io.insert-koin:koin-androidx-compose:4.1.1",
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2",
    )

    // Navigation
    implementation("io.github.raamcosta.compose-destinations:core:2.3.0")
    ksp("io.github.raamcosta.compose-destinations:ksp:2.3.0")

    // Utilities
    implementationAll(
        "com.jakewharton.timber:timber:5.0.1",
    )

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.microsoft.clarity:clarity-compose:3.8.1")

    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Camera
    val cameraVersion = "1.5.3"
    implementationAll(
        "androidx.camera:camera-camera2:$cameraVersion",
        "androidx.camera:camera-lifecycle:$cameraVersion",
        "androidx.camera:camera-view:$cameraVersion",
        "androidx.camera:camera-core:$cameraVersion",
        "androidx.camera:camera-video:$cameraVersion",
    )

    // Image Loading
    implementationAll(
        "io.coil-kt.coil3:coil-compose:3.3.0",
        "io.coil-kt.coil3:coil-network-okhttp:3.3.0",
        "io.coil-kt.coil3:coil-svg:3.3.0",
        "io.github.panpf.sketch4:sketch-compose:4.3.1",
        "io.github.panpf.sketch4:sketch-http:4.3.1",
        "io.github.panpf.sketch4:sketch-animated-webp:4.3.1",
    )

    // UI Components
    implementation("sh.calvin.reorderable:reorderable:3.0.0")
    implementation("com.mikepenz:aboutlibraries-core:13.2.1")
    implementation("com.mikepenz:aboutlibraries-compose:13.2.1")

    // Lifecycle
    implementationAll(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0",
        "androidx.lifecycle:lifecycle-runtime-compose:2.10.0",
    )
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

tasks.withType<MergeSourceSetFolders>().configureEach {
    dependsOn(downloadGeoFilesTask)
}

// Lint Vital tasks may read src/main/assets directly and bypass MergeSourceSetFolders,
// so Gradle 9 validation requires an explicit dependency on downloadGeoFiles.
tasks.configureEach {
    if (
        name.startsWith("lintVitalAnalyze") ||
        (name.startsWith("generate") && name.contains("LintVitalReportModel"))
    ) {
        dependsOn(downloadGeoFilesTask)
    }
}

tasks.register<Delete>("cleanGeoFiles") {
    description = "Clean downloaded GeoIP and GeoSite databases"
    group = "build setup"
    delete(geoFilesAssetsDir)
}

aboutLibraries {
    export {
        outputFile = file("src/main/resources/aboutlibraries.json")
    }
}
