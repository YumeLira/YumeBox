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
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.service.ClashService
import com.github.yumelira.yumebox.service.TunService
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.root.RootAccessSupport
import com.github.yumelira.yumebox.service.runtime.session.RuntimeServiceLauncher
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import kotlinx.coroutines.Dispatchers
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
        }
    }
}
