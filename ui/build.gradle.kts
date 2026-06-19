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

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.core:core-ktx:${gropify.dep.version.coreKtx}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${gropify.dep.version.lifecycle}")
    implementation("io.github.raamcosta.compose-destinations:core:${gropify.dep.version.composeDestinations}")
    implementation("io.github.panpf.sketch4:sketch-compose:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-http:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-svg:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-animated-gif:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-animated-webp:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-compose-resources:${gropify.dep.version.sketch4}")
    implementation("dev.chrisbanes.haze:haze:${gropify.dep.version.haze}")
    implementation("io.github.kyant0:shapes:1.2.0")
    implementation("top.yukonga.miuix.kmp:miuix-ui:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-preference:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-icons:${gropify.dep.version.miuix}")
}


