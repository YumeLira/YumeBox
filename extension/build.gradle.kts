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

import com.android.build.gradle.tasks.PackageAndroidArtifact

plugins {
    id("com.android.application")
}

dependencies {
    implementation("com.caoccao.javet:javet-node-android:${gropify.dep.version.javetNodeAndroid}")
}

android {
    namespace = gropify.project.namespace.extension

    defaultConfig {
        applicationId = gropify.project.namespace.extension
        minSdk = gropify.android.minSdk
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.apply {
                clear()
                add("jniLibs")
            }
        }
    }

    tasks.withType<PackageAndroidArtifact>().configureEach {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf("META-INF/**")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
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
            isEnable = true
            reset()
            val abiList = (gropify.abi.extension.list ?: "arm64-v8a,x86_64")
                .split(',').map { it.trim() }.filter { it.isNotEmpty() }
            include(*abiList.toTypedArray())
            isUniversalApk = false
        }
    }
}


