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

package com.github.yumelira.yumebox.service.runtime.session

import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.service.ClashService
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.TunService
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf

object RuntimeServiceLauncher {
    const val EXTRA_REQUEST_SOURCE = "runtime_request_source"

    const val SOURCE_UI = "ui"
    const val SOURCE_TILE = "tile"
    const val SOURCE_AUTO_RESTART = "auto_restart"
    const val SOURCE_AUTO_RESTART_BOOT = "auto_restart_boot"
    const val SOURCE_AUTO_RESTART_REPLACED = "auto_restart_replaced"
    const val SOURCE_UNKNOWN = "unknown"

    fun start(context: Context, mode: ProxyMode, source: String = SOURCE_UNKNOWN) {
        require(mode != ProxyMode.RootTun) { "RuntimeServiceLauncher does not start RootTun" }

        val appContext = context.appContextOrSelf
        val store = RuntimeStartupLogStore(appContext, RuntimeStartupLogStore.scopeForMode(mode))
        store.clear()
        store.append(
            "${RuntimeStartupLogStore.scopeForMode(mode).tag} launcher: request start source=$source mode=${mode.name}"
        )

        if (mode == ProxyMode.Tun && StatusProvider.isTunStarting()) {
            store.append("LOCAL_TUN launcher: skipped, already starting")
            return
        }

        if (mode == ProxyMode.Tun) {
            StatusProvider.markTunStarting()
        }
        StatusProvider.markRuntimeStarting(mode)

        val intent =
            Intent(
                    appContext,
                    when (mode) {
                        ProxyMode.Tun -> TunService::class.java
                        ProxyMode.Http -> ClashService::class.java
                        ProxyMode.RootTun -> error("unsupported mode")
                    },
                )
                .putExtra(EXTRA_REQUEST_SOURCE, source)

        runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            }
            .onFailure { error ->
                if (mode == ProxyMode.Tun) {
                    StatusProvider.clearTunStarting()
                }
                StatusProvider.markRuntimeIdle(mode)
                store.append(
                    "${RuntimeStartupLogStore.scopeForMode(mode).tag} launcher: failed=${error.message}"
                )
                throw error
            }
    }
}
