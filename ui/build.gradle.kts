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

plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.github.yumelira.yumebox.core.ui"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":locale"))
    implementation(project(":data"))
    implementation(project(":runtime:api"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.destinations.core)
    implementation(libs.sketch.compose)
    implementation(libs.sketch.http)
    implementation(libs.sketch.svg)
    implementation(libs.sketch.animated.gif)
    implementation(libs.sketch.animated.webp)
    implementation(libs.sketch.compose.resources)
    implementation(libs.haze)
    implementation(libs.shapes)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
}


