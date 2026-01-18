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

plugins {
    `kotlin-dsl`
}

data class PluginSpec(val name: String, val id: String, val implementationClass: String)

val jvmVersionInt = providers.gradleProperty("project.jvm").orNull?.toIntOrNull() ?: 17
val buildLogicGroup = providers.gradleProperty("project.namespace.buildlogic").orNull
    ?: "com.github.yumelira.yumebox.buildlogic"

val agpVersion = "9.0.0"
val kotlinVersion = "2.3.0"
val composePluginVersion = "1.9.3"
val kspVersion = "2.3.3"

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

dependencies {
    compileOnly("com.android.tools.build:gradle:$agpVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:$composePluginVersion")
    compileOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
}

gradlePlugin {
    plugins {
        val pluginSpecs = listOf(
            PluginSpec("baseAndroid", "yumebox.base.android", "plugins.BaseAndroidPlugin"),
            PluginSpec("golangConfig", "yumebox.golang.config", "plugins.GolangConfigPlugin"),
            PluginSpec("golangTasks", "yumebox.golang.tasks", "plugins.GolangTasksPlugin"),
            PluginSpec("geoAssets", "yumebox.geo.assets", "plugins.GeoAssetsPlugin"),
        )
        pluginSpecs.forEach { spec ->
            register(spec.name) {
                id = spec.id
                implementationClass = spec.implementationClass
            }
        }
    }
}
