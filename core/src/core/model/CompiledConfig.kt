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

package com.github.yumelira.yumebox.core.model

import kotlinx.serialization.Serializable

@Serializable data class OverrideSpec(val path: String, val ext: String)

@Serializable
data class CompileRequest(
    val schemaVersion: Int = 1,
    val profileUuid: String,
    val profileDir: String,
    val profilePath: String,
    val overrides: List<OverrideSpec> = emptyList(),
    val outputPath: String,
    val ageSecretKey: String? = null,
)

@Serializable
data class CompileResult(
    val success: Boolean,
    val fingerprint: String = "",
    val finalYaml: String = "",
    val warnings: List<String> = emptyList(),
    val error: String? = null,
)

@Serializable
data class NativeInspectResult(
    val success: Boolean,
    val payload: String = "",
    val error: String? = null,
)

@Serializable
data class CompileRawSummary(
    val success: Boolean,
    val fingerprint: String = "",
    val warnings: List<String> = emptyList(),
    val error: String? = null,
)
