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

package com.github.yumelira.yumebox.runtime.service.session

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.RemoteControllerStore
import com.github.yumelira.yumebox.runtime.api.Intents
import com.github.yumelira.yumebox.runtime.api.RuntimePhase
import com.github.yumelira.yumebox.runtime.api.appContextOrSelf
import com.github.yumelira.yumebox.runtime.service.ClashService
import com.github.yumelira.yumebox.runtime.service.StatusProvider
import com.github.yumelira.yumebox.runtime.service.TunService

object RuntimeServiceLauncher {
    const val EXTRA_REQUEST_SOURCE = "runtime_request_source"

    const val SOURCE_UI = "ui"
    const val SOURCE_TILE = "tile"
    const val SOURCE_AUTO_RESTART = "auto_restart"
    const val SOURCE_AUTO_RESTART_BOOT = "auto_restart_boot"
    const val SOURCE_AUTO_RESTART_REPLACED = "auto_restart_replaced"
    const val SOURCE_UNKNOWN = "unknown"

    @Synchronized
    fun start(context: Context, mode: ProxyMode, source: String = SOURCE_UNKNOWN) {
        require(mode != ProxyMode.RootTun) { "RuntimeServiceLauncher does not start RootTun" }

        val appContext = context.appContextOrSelf
        val logScope = RuntimeStartupLogStore.scopeForMode(mode)
        val startupLogStore = RuntimeStartupLogStore(appContext, logScope)
        startupLogStore.clear()
        startupLogStore.append(
            "${logScope.tag} launcher: request start source=$source mode=${mode.name}"
        )

        if (RemoteControllerStore.isActive()) {
            startupLogStore.append("${logScope.tag} launcher: skipped, remote controller active")
            return
        }

        // A redundant start against an already-running service would re-mark the persisted
        // phase as Starting with nothing to flip it back to Running afterwards.
        val currentPhase = StatusProvider.queryRuntimePhase(mode)
        if (currentPhase == RuntimePhase.Running) {
            startupLogStore.append("${logScope.tag} launcher: skipped, already running")
            return
        }

        if (StatusProvider.isRuntimeStartingWithinGrace(mode)) {
            startupLogStore.append("${logScope.tag} launcher: skipped, already starting")
            return
        }

        // A Starting phase past the grace window with a live-but-stuck instance would swallow
        // a new command (onCreate never re-runs on an existing instance). Ask it to recreate
        // itself after releasing its old session instead of sending a new token to that instance.
        if (
            StatusProvider.queryRuntimePhase(mode) == RuntimePhase.Starting &&
                StatusProvider.isLocalRuntimeServiceAlive(mode)
        ) {
            startupLogStore.append("${logScope.tag} launcher: stopping stale ${mode.name} runtime")
            appContext.sendBroadcast(
                Intent(Intents.ACTION_CLASH_REQUEST_STOP)
                    .setPackage(appContext.packageName)
                    .putExtra(Intents.EXTRA_RESTART, true)
                    .putExtra(Intents.EXTRA_RUNTIME_MODE, mode.name)
            )
            return
        }

        // Only one local runtime may own the process-wide core; stop a lingering service of
        // the other mode here so every caller (UI, tile, auto restart) gets the exclusion,
        // otherwise the new session tears the core down underneath the old foreground service.
        val otherMode = if (mode == ProxyMode.Tun) ProxyMode.Http else ProxyMode.Tun
        if (StatusProvider.queryRuntimePhase(otherMode).isNotIdle) {
            startupLogStore.append(
                "${logScope.tag} launcher: stopping previous ${otherMode.name} runtime"
            )
            runCatching { appContext.stopService(Intent(appContext, serviceClassFor(otherMode))) }
        }

        val sessionToken = StatusProvider.beginRuntimeSession(mode)

        val intent =
            Intent(appContext, serviceClassFor(mode)).putExtra(EXTRA_REQUEST_SOURCE, source)

        runCatching { appContext.startForegroundService(intent) }
            .onFailure { error ->
                StatusProvider.markRuntimeIdle(mode, sessionToken)
                startupLogStore.append("${logScope.tag} launcher: failed=${error.message}")
                throw error
            }
    }

    @Synchronized
    fun stop(context: Context, mode: ProxyMode) {
        require(mode != ProxyMode.RootTun) { "RuntimeServiceLauncher does not stop RootTun" }

        val appContext = context.appContextOrSelf
        runCatching { appContext.stopService(Intent(appContext, serviceClassFor(mode))) }
        StatusProvider.markRuntimeIdle(mode)
    }

    private fun serviceClassFor(mode: ProxyMode): Class<*> =
        when (mode) {
            ProxyMode.Tun -> TunService::class.java
            ProxyMode.Http -> ClashService::class.java
            ProxyMode.RootTun -> error("unsupported mode")
        }
}
