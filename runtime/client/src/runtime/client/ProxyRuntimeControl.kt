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

package com.github.yumelira.yumebox.runtime.client

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.runtime.api.RuntimeOwner
import com.github.yumelira.yumebox.runtime.api.appContextOrSelf
import com.github.yumelira.yumebox.runtime.client.manager.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.runtime.service.ClashService
import com.github.yumelira.yumebox.runtime.service.StatusProvider
import com.github.yumelira.yumebox.runtime.service.TunService
import com.github.yumelira.yumebox.runtime.service.root.RootAccessSupport
import com.github.yumelira.yumebox.runtime.service.session.RuntimeServiceLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal class ProxyRuntimeControl(
    context: Context,
    private val clashRequestStopAction: () -> String,
) {
    private val appContext = context.appContextOrSelf

    suspend fun start(owner: RuntimeOwner, mode: ProxyMode) {
        when (owner) {
            RuntimeOwner.RootTun -> {
                RootAccessSupport.requireRootTunAccess(appContext)
                val result = RootTunController.start(appContext)
                if (!result.success) {
                    error(result.error ?: "RootTun start failed")
                }
            }

            RuntimeOwner.LocalTun,
            RuntimeOwner.LocalHttp ->
                RuntimeServiceLauncher.start(
                    context = appContext,
                    mode = mode,
                    source = RuntimeServiceLauncher.SOURCE_UI,
                )

            RuntimeOwner.RemoteController,
            RuntimeOwner.None -> Unit
        }
    }

    suspend fun stop(owner: RuntimeOwner) {
        when (owner) {
            RuntimeOwner.RootTun -> {
                val result = RootTunController.stop(appContext)
                if (!result.success) {
                    error(result.error ?: "RootTun stop failed")
                }
            }

            RuntimeOwner.LocalTun,
            RuntimeOwner.LocalHttp -> stopLocalRuntime()

            RuntimeOwner.RemoteController,
            RuntimeOwner.None -> Unit
        }
    }

    private suspend fun stopLocalRuntime() {
        withContext(Dispatchers.IO) {
            runCatching {
                    ServiceClient.connect(appContext)
                    ServiceClient.clash().requestStop()
                }
                .onFailure {
                    appContext.sendBroadcast(
                        Intent(clashRequestStopAction()).setPackage(appContext.packageName)
                    )
                }
            appContext.stopService(Intent(appContext, TunService::class.java))
            appContext.stopService(Intent(appContext, ClashService::class.java))
            // stopService is async. A start issued while the old instance is still dying gets
            // its command delivered to that instance (onCreate never re-runs) and is swallowed,
            // and the dying instance's late stopSelf can put the replacement down; serialize
            // the handover by waiting until the service is really gone.
            awaitLocalServicesStopped()
        }
    }

    private suspend fun awaitLocalServicesStopped() {
        val deadline = SystemClock.elapsedRealtime() + STOP_HANDOVER_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val alive =
                StatusProvider.isLocalRuntimeServiceAlive(ProxyMode.Tun) ||
                    StatusProvider.isLocalRuntimeServiceAlive(ProxyMode.Http)
            if (!alive) return
            delay(STOP_HANDOVER_POLL_MS)
        }
    }

    private companion object {
        const val STOP_HANDOVER_TIMEOUT_MS = 5_000L
        const val STOP_HANDOVER_POLL_MS = 100L
    }
}
