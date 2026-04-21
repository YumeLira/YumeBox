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
    kotlin("plugin.serialization")
}


android {
    namespace = gropify.project.namespace.core

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel:${gropify.dep.version.lifecycle}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")
    implementation("com.github.bmoliveira:snake-yaml:v1.18-android")
    implementation("androidx.annotation:annotation-jvm:${gropify.dep.version.annotationJvm}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
}