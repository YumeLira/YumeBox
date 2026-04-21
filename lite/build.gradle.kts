/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
}

val appAbiList =
    gropify.abi.app.list.split(',').map { it.trim() }.filter { it.isNotEmpty() }

val geoFilesAssetsDir = rootProject.layout.buildDirectory.dir("generated/assets/geo")

android {
    namespace = "${gropify.project.namespace.base}.lite"

    defaultConfig {
        applicationId = "${gropify.project.namespace.base}.lite"
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
        manifestPlaceholders["appName"] = "${gropify.project.name} Lite"
    }

    compileOptions {
        val javaVer = gropify.android.jvm
        sourceCompatibility = JavaVersion.toVersion(javaVer)
        targetCompatibility = JavaVersion.toVersion(javaVer)
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        getByName("main") {
            kotlin.directories.apply {
                clear()
                add("src")
            }
            res.directories.apply {
                clear()
                addAll(
                    listOf(
                        "res",
                        "../app/res",
                    )
                )
            }
            assets.directories.apply {
                clear()
                addAll(
                    listOf(
                        "assets",
                        "../app/assets",
                        geoFilesAssetsDir.get().asFile.invariantSeparatorsPath,
                    )
                )
            }
            aidl.directories.apply {
                clear()
                add("aidl")
            }
            resources.directories.apply {
                clear()
                addAll(
                    listOf(
                        "resources",
                        "../app/resources",
                    )
                )
            }
            jniLibs.directories.apply {
                clear()
                add("../jniLibs")
            }
            if (project.file("AndroidManifest.xml").isFile) {
                manifest.srcFile("AndroidManifest.xml")
            }
        }
    }

    androidResources {
        generateLocaleConfig = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            include(*appAbiList.toTypedArray())
            isUniversalApk = true
        }
    }

    packaging {
        jniLibs {
            excludes += listOf(
                "lib/**/libjavet*.so",
            )
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        val keystore = rootProject.file("signing.properties")
        if (keystore.exists()) {
            create("release") {
                val prop = Properties().apply { keystore.inputStream().use(::load) }
                storeFile = rootProject.file("release.keystore")
                storePassword = prop.getProperty("keystore.password")!!
                keyAlias = prop.getProperty("key.alias")!!
                keyPassword = prop.getProperty("key.password")!!
            }
        }
    }

    if (signingConfigs.findByName("release") != null) {
        buildTypes.named("release").configure {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiName = output.filters.find {
                    it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"
                val buildTypeName = variant.buildType ?: "release"
                (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName.set(
                    "${gropify.project.name}-lite-${abiName}-${buildTypeName}.apk"
                )
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${gropify.dep.version.desugarJdkLibs}")

    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":runtime:api"))
    implementation(project(":runtime:client"))
    implementation(project(":runtime:service"))
    implementation(project(":feature:proxy"))

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:${gropify.dep.version.activityCompose}")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("top.yukonga.miuix.kmp:miuix-ui:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-preference:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-icons:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:${gropify.dep.version.miuix}")
    implementation("dev.chrisbanes.haze:haze:${gropify.dep.version.haze}")

    val mmkv64 = gropify.dep.version.mmkv64
    val mmkv32 = gropify.dep.version.mmkv32
    val injectedAbi = findProperty("android.injected.build.abi") as? String
    val mmkvVersion = if (injectedAbi in listOf("arm64-v8a", "x86_64")) mmkv64 else mmkv32
    //noinspection NewerVersionAvailable
    implementation("com.tencent:mmkv:$mmkvVersion")
    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-android:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-androidx-compose:${gropify.dep.version.koin}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")

    implementation("io.github.raamcosta.compose-destinations:core:${gropify.dep.version.composeDestinations}")
    ksp("io.github.raamcosta.compose-destinations:ksp:${gropify.dep.version.composeDestinations}")

    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("org.tukaani:xz:1.12")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${gropify.dep.version.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${gropify.dep.version.lifecycle}")
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}
