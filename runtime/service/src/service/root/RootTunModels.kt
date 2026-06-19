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

package com.github.yumelira.yumebox.service.root

import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.UiConfiguration
import com.github.yumelira.yumebox.service.runtime.session.RuntimeLogChunk
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object RootTunJson {
    val Default = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

@Serializable
enum class RootTunState {
    Idle,
    Starting,
    Running,
    Stopping,
    Failed;

    val isActive: Boolean
        get() = this == Starting || this == Running || this == Stopping

    val isRecovering: Boolean
        get() = this == Starting || this == Stopping
}

@Serializable data class RootTunStartRequest(val source: String = "")

@Serializable data class RootTunOperationResult(val success: Boolean, val error: String? = null)

@Serializable
data class RootTunStatus(
    val state: RootTunState = RootTunState.Idle,
    val running: Boolean = state.isActive,
    val lastError: String? = null,
    val profileUuid: String? = null,
    val profileName: String? = null,
    val runtimeReady: Boolean = false,
    val controllerReady: Boolean = false,
    val startedAt: Long? = null,
    val staticPlanFingerprint: String? = null,
    val transportFingerprint: String? = null,
    val overrideFingerprint: String? = null,
    val profileFingerprint: String? = null,
)

@Serializable
data class RootTunRuntimeSnapshot(
    val status: RootTunStatus = RootTunStatus(),
    val profileUuid: String? = null,
    val profileName: String? = null,
    val proxyGroups: List<ProxyGroup> = emptyList(),
    val configuration: UiConfiguration = UiConfiguration(),
    val providers: List<Provider> = emptyList(),
    val trafficNow: Long = 0L,
    val trafficTotal: Long = 0L,
    val transportFingerprint: String = "",
    val overrideFingerprint: String = "",
    val profileFingerprint: String = "",
)

@Serializable
data class RootTunLogChunk(val nextSeq: Long = 0L, val items: List<String> = emptyList()) {
    companion object {
        fun from(value: RuntimeLogChunk): RootTunLogChunk {
            return RootTunLogChunk(nextSeq = value.nextSeq, items = value.items)
        }
    }
}
