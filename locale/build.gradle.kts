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
    namespace = "com.github.yumelira.yumebox.core.locale"

    sourceSets {
        getByName("main") {
            kotlin.directories.apply {
                clear()
                addAll(
                    listOf(
                        "src",
                        "build/generated/fytxt/kotlin/commonMain/kotlin",
                    )
                )
            }
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
}
