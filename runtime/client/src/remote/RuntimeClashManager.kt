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

package com.github.yumelira.yumebox.remote

import android.content.Context
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.remote.IClashManager
import com.github.yumelira.yumebox.service.remote.ILogObserver
import com.github.yumelira.yumebox.service.root.RootTunRuntimeRecovery
import com.github.yumelira.yumebox.service.root.RootTunStateStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

class RuntimeClashManager(context: Context, private val local: IClashManager) : IClashManager {
    private val appContext = context.appContextOrSelf
    private val rootTunStateStore by lazy { RootTunStateStore(appContext) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var rootLogJob: Job? = null
    private var rootLogSeq: Long = 0L

    override fun queryTunnelState(): TunnelState {
        return queryWithRuntime(
            rootCall = { runBlocking { RootTunController.queryTunnelState(appContext) } },
            localCall = { local.queryTunnelState() },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryTrafficNow(): Long {
        return queryWithRuntime(
            rootCall = { runBlocking { RootTunController.queryTrafficNow(appContext) } },
            localCall = { local.queryTrafficNow() },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryTrafficTotal(): Long {
        return queryWithRuntime(
            rootCall = { runBlocking { RootTunController.queryTrafficTotal(appContext) } },
            localCall = { local.queryTrafficTotal() },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryConnections(): ConnectionSnapshot {
        return queryWithRuntime(
            rootCall = { runBlocking { RootTunController.queryConnections(appContext) } },
            localCall = { local.queryConnections() },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryProfileProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        return local.queryProfileProxyGroupNames(excludeNotSelectable)
    }

    override fun queryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        return local.queryProfileProxyGroups(excludeNotSelectable)
    }

    override fun queryActiveProfileTunRouteExcludeAddress(): List<String> {
        return local.queryActiveProfileTunRouteExcludeAddress()
    }

    override fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        return queryWithRuntime(
            rootCall = {
                runBlocking {
                    RootTunController.queryAllProxyGroups(appContext, excludeNotSelectable)
                }
            },
            localCall = { local.queryAllProxyGroups(excludeNotSelectable) },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        return queryWithRuntime(
            rootCall = {
                runBlocking {
                    RootTunController.queryProxyGroupNames(appContext, excludeNotSelectable)
                }
            },
            localCall = { local.queryProxyGroupNames(excludeNotSelectable) },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup {
        return queryWithRuntime(
            rootCall = {
                runBlocking { RootTunController.queryProxyGroup(appContext, name, proxySort) }
            },
            localCall = { local.queryProxyGroup(name, proxySort) },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryConfiguration(): UiConfiguration {
        return queryWithRuntime(
            rootCall = { runBlocking { RootTunController.queryConfiguration(appContext) } },
            localCall = { local.queryConfiguration() },
            fallbackOnRootFailure = false,
        )
    }

    override fun queryProviders(): ProviderList {
        val providers =
            queryWithRuntime(
                rootCall = { runBlocking { RootTunController.queryProviders(appContext) } },
                localCall = { local.queryProviders().toList() },
                fallbackOnRootFailure = false,
            )
        return ProviderList(providers)
    }

    override fun patchSelector(group: String, name: String): Boolean {
        return queryWithRuntime(
            rootCall = { runBlocking { RootTunController.patchSelector(appContext, group, name) } },
            localCall = { local.patchSelector(group, name) },
            fallbackOnRootFailure = false,
        )
    }

    override fun closeConnection(id: String): Boolean {
        return queryWithRuntime(
            rootCall = { runBlocking { RootTunController.closeConnection(appContext, id) } },
            localCall = { local.closeConnection(id) },
            fallbackOnRootFailure = false,
        )
    }

    override fun closeAllConnections() {
        queryWithRuntime(
            rootCall = { runBlocking { RootTunController.closeAllConnections(appContext) } },
            localCall = { local.closeAllConnections() },
            fallbackOnRootFailure = false,
        )
    }

    override suspend fun healthCheck(group: String) {
        queryWithRuntimeSuspend(
            rootCall = { RootTunController.healthCheck(appContext, group) },
            localCall = { local.healthCheck(group) },
            fallbackOnRootFailure = false,
        )
    }

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        return queryWithRuntimeSuspend(
            rootCall = {
                val payload = RootTunController.healthCheckProxy(appContext, group, proxyName)
                val json = kotlinx.serialization.json.Json.parseToJsonElement(payload)
                json.jsonObject["delay"]?.jsonPrimitive?.int ?: -1
            },
            localCall = { local.healthCheckProxy(group, proxyName) },
            fallbackOnRootFailure = false,
        )
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        queryWithRuntimeSuspend(
            rootCall = { RootTunController.updateProvider(appContext, type, name) },
            localCall = { local.updateProvider(type, name) },
            fallbackOnRootFailure = false,
        )
    }

    override fun requestStop() {
        queryWithRuntime(
            rootCall = { runBlocking { RootTunController.requestStop(appContext) } },
            localCall = { local.requestStop() },
            fallbackOnRootFailure = false,
        )
    }

    override fun setLogObserver(observer: ILogObserver?) {
        if (useRootRuntime()) {
            local.setLogObserver(null)
            rootLogJob?.cancel()
            if (observer == null) {
                rootLogSeq = 0L
                return
            }
            rootLogJob = scope.launch {
                PollingTimers.ticks(PollingTimerSpecs.RuntimeRootLogPolling).collect {
                    runCatching {
                            val chunk = RootTunController.queryRecentLogs(appContext, rootLogSeq)
                            if (chunk.items.isNotEmpty()) {
                                chunk.items.forEach { raw ->
                                    observer.newItem(
                                        com.github.yumelira.yumebox.service.root.RootTunJson.Default
                                            .decodeFromString(LogMessage.serializer(), raw)
                                    )
                                }
                            }
                            rootLogSeq = chunk.nextSeq
                        }
                        .onFailure { error -> Timber.d(error, "Root runtime log polling skipped") }
                }
            }
        } else {
            rootLogJob?.cancel()
            rootLogSeq = 0L
            local.setLogObserver(observer)
        }
    }

    private fun useRootRuntime(): Boolean {
        val status = rootTunStateStore.snapshot()
        return status.state.isActive || status.runtimeReady
    }

    private inline fun <T> queryWithRuntime(
        rootCall: () -> T,
        localCall: () -> T,
        fallbackOnRootFailure: Boolean = true,
    ): T {
        if (!useRootRuntime()) {
            return localCall()
        }
        return try {
            rootCall()
        } catch (error: Throwable) {
            handleRootRuntimeFailure(error)
            if (fallbackOnRootFailure) localCall() else throw error
        }
    }

    private suspend inline fun <T> queryWithRuntimeSuspend(
        crossinline rootCall: suspend () -> T,
        crossinline localCall: suspend () -> T,
        fallbackOnRootFailure: Boolean = true,
    ): T {
        if (!useRootRuntime()) {
            return localCall()
        }
        return try {
            rootCall()
        } catch (error: Throwable) {
            handleRootRuntimeFailure(error)
            if (fallbackOnRootFailure) localCall() else throw error
        }
    }

    private fun handleRootRuntimeFailure(error: Throwable) {
        if (RootTunRuntimeRecovery.isBinderConnectionFailure(error)) {
            rootLogJob?.cancel()
            rootLogJob = null
            rootLogSeq = 0L
            RootTunRuntimeRecovery.handleBinderGone(
                appContext,
                RootTunRuntimeRecovery.binderFailureReason(error),
            )
            Timber.w(error, "Root runtime binder died")
            return
        }
        Timber.w(error, "Root runtime query failed")
    }
}
