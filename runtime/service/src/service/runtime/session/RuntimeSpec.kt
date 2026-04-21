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



package com.github.yumelira.yumebox.service.runtime.session

import com.github.yumelira.yumebox.core.model.OverrideSpec
import com.github.yumelira.yumebox.core.model.RootTunConfig
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import kotlinx.serialization.Serializable

@Serializable
data class RuntimeSpec(
    val owner: RuntimeOwner,
    val profileUuid: String,
    val profileName: String,
    val profileDir: String,
    val runtimeConfigPath: String = "",
    val overrideSpecs: List<OverrideSpec> = emptyList(),
    val rootTunConfig: RootTunConfig? = null,
    val staticPlanFingerprint: String = "",
    val transportFingerprint: String = "",
    val effectiveFingerprint: String = "",
    val profileFingerprint: String = "",
)

@Serializable
data class RuntimeOperationResult(
    val success: Boolean,
    val error: String? = null,
)

@Serializable
data class RuntimeLogChunk(
    val nextSeq: Long = 0L,
    val items: List<String> = emptyList(),
)
