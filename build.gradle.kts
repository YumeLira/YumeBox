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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

buildscript {
    configurations.classpath {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "com.google.protobuf" -> useVersion("3.25.9")
                "org.bouncycastle" -> useVersion("1.84")
                "org.jdom" -> if (requested.name == "jdom2") useVersion("2.0.6.1")
                "org.bitbucket.b_c" -> if (requested.name == "jose4j") useVersion("0.9.6")
                "com.fasterxml.jackson.core" ->
                    if (requested.name == "jackson-core") useVersion("2.22.0")
                "org.apache.commons" -> if (requested.name == "commons-lang3") useVersion("3.20.0")
                "io.netty" -> useVersion("4.1.136.Final")
            }
        }
    }
}

plugins {
    `jvm-toolchains`
    id("com.android.application") version "9.3.1" apply false
    id("com.android.library") version "9.3.1" apply false
    kotlin("jvm") version "2.4.10" apply false
    kotlin("plugin.serialization") version "2.4.10" apply false
    kotlin("plugin.compose") version "2.4.10" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("androidx.room") version "2.8.4" apply false
    id("com.mikepenz.aboutlibraries.plugin.android") version "15.0.4" apply false
    id("com.diffplug.spotless") version "8.9.0" apply false
}

// AGP-created tool configurations (unified-test-platform-*, androidLintTool) and test
// classpaths resolve independently of the buildscript classpath above, so the same
// vulnerable-version floors must be enforced on project configurations as well.
allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "org.bouncycastle" -> useVersion("1.84")
                "io.netty" -> useVersion("4.1.136.Final")
                "org.apache.httpcomponents" -> if (requested.name == "httpclient") useVersion("4.5.14")
                "org.apache.commons" -> if (requested.name == "commons-lang3") useVersion("3.20.0")
            }
        }
    }
}

val androidCompileSdk = providers.gradleProperty("android.compileSdk").map(String::toInt).get()
val androidCompileSdkMinor =
    providers.gradleProperty("android.compileSdkMinor").map(String::toInt).orElse(0).get()
val androidMinSdk = providers.gradleProperty("android.minSdk").map(String::toInt).get()
val androidJvm =
    providers
        .gradleProperty("android.jvm")
        .orElse(providers.gradleProperty("project.jvm"))
        .orElse("17")
        .get()
val androidJvmVersion = androidJvm.toInt()
val androidNdkVersion = providers.gradleProperty("android.ndkVersion").orNull.orEmpty()

subprojects {
    apply(plugin = "jvm-toolchains")

    val javaToolchainService = extensions.getByType(JavaToolchainService::class.java)

    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(
            javaToolchainService.compilerFor {
                languageVersion.set(JavaLanguageVersion.of(androidJvmVersion))
            }
        )
    }

    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension>("android") {
            compileSdk = androidCompileSdk
            compileSdkMinor = androidCompileSdkMinor

            if (androidNdkVersion.isNotBlank()) {
                ndkVersion = androidNdkVersion
            }

            defaultConfig { minSdk = androidMinSdk }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(androidJvm)
                targetCompatibility = JavaVersion.toVersion(androidJvm)
            }

            sourceSets {
                getByName("main") {
                    kotlin.directories.apply {
                        clear()
                        add("src")
                    }
                    res.directories.apply {
                        clear()
                        add("res")
                    }
                    assets.directories.apply {
                        clear()
                        add("assets")
                    }
                    aidl.directories.apply {
                        clear()
                        add("aidl")
                    }
                    resources.directories.apply {
                        clear()
                        add("resources")
                    }
                    if (project.file("AndroidManifest.xml").isFile) {
                        manifest.srcFile("AndroidManifest.xml")
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension>("android") {
            compileSdk = androidCompileSdk
            compileSdkMinor = androidCompileSdkMinor

            if (androidNdkVersion.isNotBlank()) {
                ndkVersion = androidNdkVersion
            }

            defaultConfig { minSdk = androidMinSdk }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(androidJvm)
                targetCompatibility = JavaVersion.toVersion(androidJvm)
            }

            buildFeatures { buildConfig = false }

            packaging { jniLibs { useLegacyPackaging = true } }

            sourceSets {
                getByName("main") {
                    kotlin.directories.apply {
                        clear()
                        add("src")
                    }
                    res.directories.apply {
                        clear()
                        add("res")
                    }
                    assets.directories.apply {
                        clear()
                        add("assets")
                    }
                    aidl.directories.apply {
                        clear()
                        add("aidl")
                    }
                    resources.directories.apply {
                        clear()
                        add("resources")
                    }
                    if (project.file("AndroidManifest.xml").isFile) {
                        manifest.srcFile("AndroidManifest.xml")
                    }
                }
            }
        }
    }
}
