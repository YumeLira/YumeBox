@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.gradle.api.provider.MapProperty
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

abstract class DownloadGeoFilesTask : DefaultTask() {
    @get:Input
    abstract val assetUrls: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val destinationDir = outputDirectory.get().asFile
        destinationDir.mkdirs()

        assetUrls.get().forEach { (fileName, url) ->
            val outputFile = destinationDir.resolve(fileName)
            runCatching {
                val uri = URI(url)
                uri.toURL().openStream().use { input ->
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                logger.lifecycle("$fileName downloaded to ${outputFile.absolutePath}")
            }.onFailure { error ->
                logger.warn("Failed to download $fileName from $url", error)
            }
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
}

fytxt {
    langSrcs = mapOf(
        "lang" to layout.projectDirectory.dir("../lang"),
    )
    packageName = "dev.oom_wg.purejoy.mlang"
    objectName = "MLang"
    defaultLang = "ZH"
    composeGen = true
    internalClass = false
}

val targetAbi = project.findProperty("android.injected.build.abi") as String?
val mmkvVersion = when (targetAbi) {
    "arm64-v8a", "x86_64" -> "2.2.4"
    else -> "1.3.14"
}
val mmkvDependency = "com.tencent:mmkv:$mmkvVersion"

val appNamespace = gropify.project.namespace.base
val appName = gropify.project.name
val jvmVersionNumber = gropify.project.jvm
val jvmVersion = jvmVersionNumber.toString()
val javaVersion = JavaVersion.toVersion(jvmVersionNumber) ?: JavaVersion.VERSION_17
val appAbiList = gropify.abi.app.list.split(",").map { it.trim() }
val localeList = gropify.locale.app.list.split(",").map { it.trim() }

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
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Specify supported locales
        resourceConfigurations.addAll(localeList)
    }

    sourceSets {
        named("main") {
            // Include fytxt generated code
            kotlin.srcDir("build/generated/fytxt/kotlin/commonMain/kotlin")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvmVersion))
        }
    }

    androidResources {
        // Don't generate automatic locale config, we'll specify locales manually
        generateLocaleConfig = false
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
            val keystore = rootProject.file("signing.properties")
            if (keystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
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

    // Use new androidComponents API instead of deprecated applicationVariants
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

    // Project dependencies
    implementation(project(":core"))
    
    // Compose dependencies (using Jetpack Compose BOM for version management)
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.12.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // Additional Compose libraries
    implementation("top.yukonga.miuix.kmp:miuix:0.7.2")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.1")
    
    // Storage
    implementation(mmkvDependency)
    
    // Dependency Injection
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-android:4.1.1")
    implementation("io.insert-koin:koin-androidx-compose:4.1.1")
    
    // Navigation
    implementation("io.github.raamcosta.compose-destinations:core:2.3.0")
    ksp("io.github.raamcosta.compose-destinations:ksp:2.3.0")
    
    // Network
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-android:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    
    // Utilities
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.caoccao.javet:javet-node-android:5.0.3")
    implementation("com.highcapable.pangutext:pangutext-android:1.0.5")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    implementation("com.google.firebase:firebase-analytics")
    
    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    
    // Camera
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")
    implementation("androidx.camera:camera-core:1.5.2")
    implementation("androidx.camera:camera-video:1.5.2")
    
    // Image Loading
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("io.coil-kt.coil3:coil-svg:3.3.0")
    
    // About Libraries
    implementation("com.mikepenz:aboutlibraries-core:13.2.1")
    implementation("com.mikepenz:aboutlibraries-compose:13.2.1")
    implementation("com.mikepenz:aboutlibraries-compose-m3:13.2.1")
    
    // UI Components
    implementation("sh.calvin.reorderable:reorderable:3.0.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

// Download GeoFiles Task
val geoFilesDownloadDir = layout.projectDirectory.dir("src/main/assets")

val downloadGeoFilesTask = tasks.register<DownloadGeoFilesTask>("downloadGeoFiles") {
    description = "Download GeoIP and GeoSite databases from MetaCubeX"
    group = "build setup"

    val assets = mapOf(
        "geoip.metadb" to gropify.asset.geoip.url,
        "geosite.dat" to gropify.asset.geosite.url,
        "ASN.mmdb" to gropify.asset.asn.url,
    )
    assetUrls.putAll(assets)
    outputDirectory.set(geoFilesDownloadDir)
}

tasks.configureEach {
    when {
        name.startsWith("assemble") || 
        name.startsWith("lintVitalAnalyze") || 
        (name.startsWith("generate") && name.contains("LintVitalReportModel")) -> {
            dependsOn(downloadGeoFilesTask)
        }
    }
}

tasks.withType<MergeSourceSetFolders>().configureEach {
    dependsOn(downloadGeoFilesTask)
}

tasks.register<Delete>("cleanGeoFiles") {
    description = "Clean downloaded GeoIP and GeoSite databases"
    group = "build setup"
    delete(geoFilesDownloadDir)
}

aboutLibraries {
    export {
        outputFile = file("src/main/resources/aboutlibraries.json")
    }
}
