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

package com.github.yumelira.yumebox.substore

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubStoreServiceRequest(
    val frontendPort: Int = 8080,
    val backendPort: Int = 8081,
    val allowLan: Boolean = false,
)

data class SubStoreServiceSnapshot(
    val isRunning: Boolean = false,
    val isStarting: Boolean = false,
) {
    val isActive: Boolean
        get() = isRunning || isStarting
}

object SubStoreServiceController {
    const val DEFAULT_FRONTEND_PORT = 8080
    const val DEFAULT_BACKEND_PORT = 8081

    private const val EXTRA_FRONTEND_PORT = "frontendPort"
    private const val EXTRA_BACKEND_PORT = "backendPort"
    private const val EXTRA_ALLOW_LAN = "allowLan"

    private val _snapshot = MutableStateFlow(SubStoreServiceSnapshot())
    val snapshot: StateFlow<SubStoreServiceSnapshot> = _snapshot.asStateFlow()

    fun startService(context: Context, request: SubStoreServiceRequest) {
        _snapshot.value = SubStoreServiceSnapshot(isRunning = false, isStarting = true)
        val intent =
            Intent(context, SubStoreService::class.java).apply {
                putExtra(EXTRA_FRONTEND_PORT, request.frontendPort)
                putExtra(EXTRA_BACKEND_PORT, request.backendPort)
                putExtra(EXTRA_ALLOW_LAN, request.allowLan)
            }
        runCatching { context.startService(intent) }
            .onFailure {
                _snapshot.value = SubStoreServiceSnapshot()
                throw it
            }
    }

    fun stopService(context: Context) {
        _snapshot.value = SubStoreServiceSnapshot()
        context.stopService(Intent(context, SubStoreService::class.java))
    }

    fun requestFrom(intent: Intent?): SubStoreServiceRequest {
        return SubStoreServiceRequest(
            frontendPort =
                intent?.getIntExtra(EXTRA_FRONTEND_PORT, DEFAULT_FRONTEND_PORT)
                    ?: DEFAULT_FRONTEND_PORT,
            backendPort =
                intent?.getIntExtra(EXTRA_BACKEND_PORT, DEFAULT_BACKEND_PORT)
                    ?: DEFAULT_BACKEND_PORT,
            allowLan = intent?.getBooleanExtra(EXTRA_ALLOW_LAN, false) ?: false,
        )
    }

    internal fun markRunning() {
        _snapshot.value = SubStoreServiceSnapshot(isRunning = true, isStarting = false)
    }

    internal fun markStopped() {
        _snapshot.value = SubStoreServiceSnapshot()
    }
}
