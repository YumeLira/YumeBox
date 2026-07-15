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

package com.github.yumelira.yumebox.runtime.client.manager

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.ProviderList
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.model.UiConfiguration
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.runtime.api.IClashManager
import com.github.yumelira.yumebox.runtime.api.ILogObserver
import com.github.yumelira.yumebox.runtime.api.Intents
import com.github.yumelira.yumebox.runtime.api.appContextOrSelf
import com.github.yumelira.yumebox.runtime.service.ClashService
import com.github.yumelira.yumebox.runtime.service.StatusProvider
import com.github.yumelira.yumebox.runtime.service.TunService
import com.github.yumelira.yumebox.runtime.service.config.ServiceStore
import com.github.yumelira.yumebox.runtime.service.session.CompiledConfigPipeline
import com.github.yumelira.yumebox.runtime.service.session.RuntimeProxyGroupResolver
import com.github.yumelira.yumebox.runtime.service.session.RuntimeSpec
import com.github.yumelira.yumebox.runtime.service.session.SessionRuntimeSpecFactory
import com.github.yumelira.yumebox.runtime.service.util.sendBroadcastSelf
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * [IClashManager] over the in-process Go core (local TUN/HTTP runtimes). Live queries hit
 * [Clash] directly; profile-preview queries resolve through the compiled-config pipeline.
 */
internal class LocalClashManager(context: Context) : IClashManager {
    private val appContext = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val store = ServiceStore()
    private val compiledConfigPipeline = CompiledConfigPipeline(appContext)
    private val proxyGroupResolver = RuntimeProxyGroupResolver(compiledConfigPipeline)
    private val runtimeSpecFactory = SessionRuntimeSpecFactory(appContext)
    private val networkSettings = MMKV.mmkvWithID("network_settings", MMKV.MULTI_PROCESS_MODE)
    private var logReceiver: ReceiveChannel<LogMessage>? = null

    override fun queryTunnelState(): TunnelState = Clash.queryTunnelState()

    override fun queryTrafficNow(): Long =
        if (!StatusProvider.serviceRunning) 0L else Clash.queryTrafficNow()

    override fun queryTrafficTotal(): Long =
        if (!StatusProvider.serviceRunning) 0L else Clash.queryTrafficTotal()

    override fun queryConnections(): ConnectionSnapshot = Clash.queryConnections()

    override fun queryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        if (store.activeProfile == null) return emptyList()
        val spec = configuredSpec()
        return runBlocking(Dispatchers.Default) {
            proxyGroupResolver.resolvedGroups(spec, excludeNotSelectable, enrichLive = false)
        }
    }

    override fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        runBlocking(Dispatchers.Default) {
            proxyGroupResolver.resolvedGroups(activeRuntimeSpec(), excludeNotSelectable)
        }

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> =
        runBlocking(Dispatchers.Default) {
            proxyGroupResolver.resolvedGroupNames(activeRuntimeSpec(), excludeNotSelectable)
        }

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup =
        Clash.queryGroup(name, proxySort)

    override fun queryConfiguration(): UiConfiguration = Clash.queryConfiguration()

    override fun queryProviders(): ProviderList = ProviderList(Clash.queryProviders())

    override fun patchSelector(group: String, name: String): Boolean =
        Clash.patchSelector(group, name)

    override fun closeConnection(id: String): Boolean = Clash.closeConnection(id)

    override fun closeAllConnections() = Clash.closeAllConnections()

    override suspend fun healthCheck(group: String) {
        Clash.healthCheck(group).await()
    }

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        val json = Clash.healthCheckProxy(proxyName).await()
        return Json.parseToJsonElement(json).jsonObject["delay"]?.jsonPrimitive?.int ?: -1
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        Clash.updateProvider(type, name).await()
    }

    override fun requestStop() {
        runCatching { appContext.sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP)) }

        runCatching {
            appContext.stopService(Intent(appContext, TunService::class.java))
            appContext.stopService(Intent(appContext, ClashService::class.java))
        }

        // Fallback for an orphaned core only. With a live service the session owns the core
        // teardown (owner-token guarded); resetting the core here would yank it out from
        // under the stop already in flight.
        if (
            !StatusProvider.isLocalRuntimeServiceAlive(ProxyMode.Tun) &&
                !StatusProvider.isLocalRuntimeServiceAlive(ProxyMode.Http)
        ) {
            runCatching {
                Clash.stopHttp()
                Clash.stopTun()
                Clash.reset()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun setLogObserver(observer: ILogObserver?) {
        synchronized(this) {
            logReceiver?.apply {
                cancel()
                Clash.forceGc()
            }

            if (observer != null) {
                logReceiver =
                    Clash.subscribeLogcat().also { receiver ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                while (isActive) {
                                    observer.newItem(receiver.receive())
                                }
                            } catch (_: CancellationException) {} catch (error: Exception) {
                                // fault barrier: core log stream / observer may fail arbitrarily;
                                // the receive loop must end gracefully, not crash the process.
                                Timber.w(error, "Log observer failed")
                            } finally {
                                withContext(NonCancellable) {
                                    receiver.cancel()
                                    Clash.forceGc()
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun configuredSpec(): RuntimeSpec =
        when (configuredProxyMode()) {
            ProxyMode.RootTun -> runtimeSpecFactory.createRootTunSpec()
            ProxyMode.Http -> runtimeSpecFactory.createHttpSpec()
            ProxyMode.Tun -> runtimeSpecFactory.createTunSpec()
        }

    private fun activeRuntimeSpec(): RuntimeSpec? {
        val activeProfile = store.activeProfile ?: return null
        return configuredSpec().takeIf { it.profileUuid == activeProfile.toString() }
    }

    private fun configuredProxyMode(): ProxyMode {
        val raw =
            networkSettings.decodeString("proxyMode", ProxyMode.Tun.name) ?: ProxyMode.Tun.name
        return runCatching { ProxyMode.valueOf(raw) }.getOrDefault(ProxyMode.Tun)
    }
}
