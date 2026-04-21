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

plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.editor"

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets {
        getByName("main") {
            assets.directories.apply {
                clear()
                add("assets")
            }
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":locale"))
    implementation(project(":ui"))

    // Sora Editor
    implementation(platform("io.github.rosemoe:editor-bom:${gropify.dep.version.soraEditor}"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:editor-lsp")
    implementation("io.github.rosemoe:language-textmate")
    implementation("io.github.rosemoe:language-treesitter")

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:${gropify.dep.version.activityCompose}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${gropify.dep.version.lifecycle}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-android:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-androidx-compose:${gropify.dep.version.koin}")
    implementation("io.github.raamcosta.compose-destinations:core:${gropify.dep.version.composeDestinations}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("top.yukonga.miuix.kmp:miuix-ui:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-preference:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-icons:${gropify.dep.version.miuix}")
}
