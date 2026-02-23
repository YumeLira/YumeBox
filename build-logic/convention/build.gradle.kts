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
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `kotlin-dsl`
}

val jvmVersionInt = providers.gradleProperty("project.jvm").orNull?.toIntOrNull() ?: 17
val buildLogicGroup = providers.gradleProperty("project.namespace.buildlogic").orNull
    ?: "com.github.yumelira.yumebox.buildlogic"

group = buildLogicGroup

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmVersionInt.toString())
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(jvmVersionInt)
    targetCompatibility = JavaVersion.toVersion(jvmVersionInt)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmVersionInt))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(jvmVersionInt)
}

dependencies {
    listOf(
        "com.android.tools.build:gradle:8.12.3",
        "org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21",
        "org.jetbrains.compose:compose-gradle-plugin:1.9.3",
        "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.3",
    ).forEach(::compileOnly)
}

gradlePlugin {
    plugins {
        register("baseAndroid") {
            id = "yumebox.base.android"
            implementationClass = "plugins.BaseAndroidPlugin"
        }
        register("buildHelpers") {
            id = "yumebox.build.helpers"
            implementationClass = "plugins.BuildHelpersPlugin"
        }
        register("golangConfig") {
            id = "yumebox.golang.config"
            implementationClass = "plugins.GolangConfigPlugin"
        }
        register("golangTasks") {
            id = "yumebox.golang.tasks"
            implementationClass = "plugins.GolangTasksPlugin"
        }
    }
}
