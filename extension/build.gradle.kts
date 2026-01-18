@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.build.api.dsl.ApplicationExtension

fun ApplicationExtension.configureExtensionBuildTypes() {
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
}

fun ApplicationExtension.configureExtensionPackaging() {
    packaging {
        jniLibs { useLegacyPackaging = true }
        resources { excludes += listOf("META-INF/**") }
    }
}

fun ApplicationExtension.configureExtensionSplits(abiList: List<String>) {
    splits {
        abi {
            isEnable = true
            reset()
            //noinspection ChromeOsAbiSupport
            include(*abiList.toTypedArray())
            isUniversalApk = false
        }
    }
}

plugins {
    id("com.android.application")
    id("yumebox.base.android")
}

val extensionAbiList = gropify.abi.extension.list.split(",").map { it.trim() }

dependencies {
    implementation("com.caoccao.javet:javet-node-android:5.0.2")
}

android {
    namespace = gropify.project.namespace.extension
    compileSdk = gropify.android.compileSdk

    defaultConfig {
        applicationId = gropify.project.namespace.extension
        minSdk = gropify.android.minSdk
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
    }

    configureExtensionPackaging()
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    configureExtensionBuildTypes()
    configureExtensionSplits(extensionAbiList)
}

tasks.withType<PackageAndroidArtifact>().configureEach {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}
