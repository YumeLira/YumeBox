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



package com.github.yumelira.yumebox.service

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.remote.IClashManager
import com.github.yumelira.yumebox.service.remote.ILogObserver
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.session.CompiledConfigPipeline
import com.github.yumelira.yumebox.service.runtime.session.SessionRuntimeSpecFactory
import com.github.yumelira.yumebox.service.runtime.util.mergeProxyGroupNames
import com.github.yumelira.yumebox.service.runtime.util.sendBroadcastSelf
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.File

class ClashManager(private val context: Context) : IClashManager, CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val store = ServiceStore()
    private val compiledConfigPipeline = CompiledConfigPipeline(context)
    private val runtimeSpecFactory = SessionRuntimeSpecFactory(context)
    private val networkSettings = MMKV.mmkvWithID("network_settings", MMKV.MULTI_PROCESS_MODE)
    private var logReceiver: ReceiveChannel<LogMessage>? = null

    override fun queryTunnelState(): TunnelState {
        return Clash.queryTunnelState()
    }

    override fun queryTrafficNow(): Long {
        if (!StatusProvider.serviceRunning) return 0L
        return Clash.queryTrafficNow()
    }

    override fun queryTrafficTotal(): Long {
        if (!StatusProvider.serviceRunning) return 0L
        return Clash.queryTrafficTotal()
    }

    override fun queryConnections(): ConnectionSnapshot {
        return Clash.queryConnections()
    }

    override fun queryProfileProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        return queryProfileProxyGroups(excludeNotSelectable).map(ProxyGroup::name)
    }

    override fun queryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        if (store.activeProfile == null) return emptyList()
        val spec = when (configuredProxyMode()) {
            ProxyMode.RootTun -> runtimeSpecFactory.createRootTunSpec()
            ProxyMode.Http -> runtimeSpecFactory.createHttpSpec()
            ProxyMode.Tun -> runtimeSpecFactory.createTunSpec()
        }
        return runBlocking(Dispatchers.Default) {
            compiledConfigPipeline.previewGroups(spec, excludeNotSelectable)
        }
    }

    override fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        val groupNames = resolveRuntimeProxyGroupNames(excludeNotSelectable)
        return groupNames.mapNotNull(::queryRuntimeProxyGroupOrNull)
    }

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        return resolveRuntimeProxyGroupNames(excludeNotSelectable)
    }

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup {
        return Clash.queryGroup(name, proxySort)
    }

    override fun queryConfiguration(): UiConfiguration {
        return Clash.queryConfiguration()
    }

    override fun queryProviders(): ProviderList {
        return ProviderList(Clash.queryProviders())
    }

    override fun patchSelector(group: String, name: String): Boolean {
        return Clash.patchSelector(group, name).also { patched ->
            val current = store.activeProfile ?: return@also

            if (!patched) {
                SelectionDao.remove(current, group)
                return@also
            }

            val patchedGroup = runCatching { Clash.queryGroup(group, ProxySort.Default) }.getOrNull()
            if (patchedGroup?.type == Proxy.Type.Selector) {
                SelectionDao.upsertManualSelection(current, group, name)
            } else {
                SelectionDao.remove(current, group)
            }
        }
    }

    override fun closeConnection(id: String): Boolean {
        return Clash.closeConnection(id)
    }

    override fun closeAllConnections() {
        Clash.closeAllConnections()
    }

    override fun requestStop() {
        runCatching {
            context.sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP))
        }

        runCatching {
            context.stopService(Intent(context, TunService::class.java))
            context.stopService(Intent(context, ClashService::class.java))
        }

        runCatching {
            Clash.stopHttp()
            Clash.stopTun()
            Clash.reset()
        }
    }

    override suspend fun healthCheck(group: String) {
        Timber.d("ClashManager healthCheck: group=%s", group)
        return Clash.healthCheck(group).await()
    }

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("ClashManager healthCheckProxy: group=%s proxy=%s", group, proxyName)
        val json = Clash.healthCheckProxy(proxyName).await()
        val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
        return jsonElement.jsonObject["delay"]?.jsonPrimitive?.int ?: -1
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        return Clash.updateProvider(type, name).await()
    }

    private fun configuredProxyMode(): ProxyMode {
        val raw = networkSettings.decodeString("proxyMode", ProxyMode.Tun.name) ?: ProxyMode.Tun.name
        return runCatching { ProxyMode.valueOf(raw) }.getOrDefault(ProxyMode.Tun)
    }

    private fun resolveRuntimeProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        val runtimeNames = Clash.queryGroupNames(excludeNotSelectable)
        val activeProfile = store.activeProfile ?: return runtimeNames
        val spec = when (configuredProxyMode()) {
            ProxyMode.RootTun -> runtimeSpecFactory.createRootTunSpec()
            ProxyMode.Http -> runtimeSpecFactory.createHttpSpec()
            ProxyMode.Tun -> runtimeSpecFactory.createTunSpec()
        }
        if (spec.profileUuid != activeProfile.toString()) {
            return runtimeNames
        }

        val runtimeFile = File(spec.runtimeConfigPath)
        if (!runtimeFile.isFile) {
            return runtimeNames
        }
        val expectedNames = runCatching {
            Clash.inspectCompiledGroups(
                runtimeFile.readText(),
                File(spec.profileDir),
                excludeNotSelectable,
            ).map(ProxyGroup::name)
        }.getOrDefault(emptyList())
        if (expectedNames.isEmpty()) {
            return runtimeNames
        }

        return mergeProxyGroupNames(expectedNames, runtimeNames)
    }

    private fun queryRuntimeProxyGroupOrNull(groupName: String): ProxyGroup? {
        val group = Clash.queryGroup(groupName, ProxySort.Default)
        if (group.name.isBlank()) {
            return null
        }
        return if (group.type == Proxy.Type.Unknown && group.proxies.isEmpty() && group.now.isBlank() && group.icon.isNullOrBlank()) {
            null
        } else {
            group
        }
    }

    override fun setLogObserver(observer: ILogObserver?) {
        synchronized(this) {
            logReceiver?.apply {
                cancel()

                Clash.forceGc()
            }

            if (observer != null) {
                logReceiver = Clash.subscribeLogcat().also { receiver ->
                    launch {
                        try {
                            while (isActive) {
                                observer.newItem(receiver.receive())
                            }
                        } catch (_: CancellationException) {

                        } catch (error: Exception) {
                            Log.w("UI crashed", error)
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
}
