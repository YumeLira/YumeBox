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
      when {
        requested.group == "com.google.protobuf" -> useVersion("3.25.9")
        requested.group == "org.bouncycastle" -> useVersion("1.84")
        requested.group == "org.jdom" && requested.name == "jdom2" -> useVersion("2.0.6.1")
        requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> useVersion("0.9.6")
        requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core" ->
            useVersion("2.22.0")
        requested.group == "org.apache.commons" && requested.name == "commons-lang3" ->
            useVersion("3.20.0")
        requested.group == "io.netty" -> useVersion("4.1.135.Final")
      }
    }
  }
}

plugins {
  `jvm-toolchains`
  id("com.android.application") version "9.0.1" apply false
  id("com.android.library") version "9.0.1" apply false
  kotlin("plugin.serialization") version "2.2.10" apply false
  kotlin("plugin.compose") version "2.3.10" apply false
  id("org.jetbrains.compose") version "1.11.1" apply false
  id("com.google.devtools.ksp") version "2.3.2" apply false
  id("com.mikepenz.aboutlibraries.plugin.android") version "15.0.0-rc01" apply false
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
      compileSdkExtension = androidCompileSdkMinor

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
