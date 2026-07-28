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

@file:Suppress("UnusedSymbol")

package com.github.yumelira.yumebox.runtime.client


import android.content.Context
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.store.RemoteControllerStore
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.runtime.api.*
import com.github.yumelira.yumebox.runtime.client.access.RuntimeAccess
import com.github.yumelira.yumebox.runtime.client.session.RuntimeCoreOps
import com.github.yumelira.yumebox.runtime.client.session.RuntimeGroupHub
import com.github.yumelira.yumebox.runtime.client.session.RuntimeSession
import com.github.yumelira.yumebox.runtime.client.session.RuntimeSessionDeps
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

enum class ProxyGroupSyncPriority {
    OFF,
    SLOW,
    FAST,
}

/**
 * UI-facing runtime facade. Runtime lifecycle/state live in [RuntimeSession]; proxy-group ops live
 * in [RuntimeGroupHub].
 */
class ProxyFacade(
    context: Context,
    private val networkSettingsStorage: NetworkSettingsStore =
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings")),
    private val remoteControllerStore: RemoteControllerStore =
        RemoteControllerStore(MMKVProvider().getMMKV("remote_controller")),
) {
    private companion object {
        const val RUNTIME_PAYLOAD_REFRESH_TICKS = 15
        const val DEFAULT_SYNC_PRIORITY_SOURCE = "default"
    }

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val session: RuntimeSession
    private val coreOps = RuntimeCoreOps(connect = { session.connectBackend() })
    private val groups: RuntimeGroupHub

    private var previewWarmupJob: Job? = null
    private val syncPriorityRequests =
        MutableStateFlow<Map<String, ProxyGroupSyncPriority>>(emptyMap())

    val runtimeSnapshot: StateFlow<RuntimeSnapshot>
    val isRunning: StateFlow<Boolean>
    val isConfigReloading: StateFlow<Boolean>
    val currentProfile: StateFlow<Profile?>
    val trafficNow: StateFlow<Traffic>
    val trafficTotal: StateFlow<Traffic>
    val proxyGroups: StateFlow<List<ProxyGroupInfo>>
    val resolvedPrimaryNode: StateFlow<Proxy?>

    init {
        session =
            RuntimeSession(
                RuntimeSessionDeps(
                    context = context,
                    scope = scope,
                    networkSettingsStorage = networkSettingsStorage,
                    remoteControllerStore = remoteControllerStore,
                    queryTrafficNowAction = { coreOps.queryTrafficNow() },
                    queryTrafficTotalAction = { coreOps.queryTrafficTotal() },
                    onAfterRunning = { refreshAllSafely() },
                    onAfterIdle = { refreshPreviewStateSafely() },
                    onGroupTick = { groups.refreshSafely() },
                    onTrafficTickExtra = { tick ->
                        if (
                            tick % RUNTIME_PAYLOAD_REFRESH_TICKS == 0 &&
                                session.shouldRefreshRuntimePayload(groups.isGroupsEmpty())
                        ) {
                            refreshAllSafely()
                        }
                    },
                    onClearGroups = { reset -> groups.clear(reset) },
                )
            )
        groups =
            RuntimeGroupHub(
                scope = scope,
                session = session,
                coreOps = coreOps,
                isRemoteControllerActive = { session.isRemoteControllerActive() },
            )
        runtimeSnapshot = session.runtimeSnapshot
        isRunning = session.isRunning
        isConfigReloading = session.isConfigReloading
        currentProfile = session.currentProfile
        trafficNow = session.trafficNow
        trafficTotal = session.trafficTotal
        proxyGroups = groups.groups
        resolvedPrimaryNode = groups.resolvedPrimaryNode
        session.bootstrap()
        observeProxyGroupSyncPriority()
    }

    fun isRemoteControllerActive(): Boolean = session.isRemoteControllerActive()

    fun applyRemoteControllerState() = session.applyRemoteControllerState()

    fun setProxyGroupSyncPriority(
        priority: ProxyGroupSyncPriority,
        source: String = DEFAULT_SYNC_PRIORITY_SOURCE,
    ) {
        syncPriorityRequests.update { current ->
            if (priority == ProxyGroupSyncPriority.OFF) {
                current - source
            } else {
                current + (source to priority)
            }
        }
    }

    fun warmUpProxyGroups() {
        if (previewWarmupJob?.isActive == true) return
        previewWarmupJob = launchPreviewWarmup()
    }

    suspend fun awaitProxyGroupWarmUp() {
        previewWarmupJob?.let { existing ->
            when {
                existing.isActive -> {
                    existing.join()
                    return
                }

                existing.isCompleted -> return
            }
    private val _trafficNow = MutableStateFlow<Traffic>(0L)
        val job = launchPreviewWarmup()
        previewWarmupJob = job
    private val _trafficTotal = MutableStateFlow<Traffic>(0L)
    }

    suspend fun reconcileRuntimeState() = session.reconcile()

    suspend fun reloadProxy(mode: RunMode = networkSettingsStorage.runMode.value) =
        session.reload(mode)

    suspend fun startProxy(request: RuntimeStartRequest) = session.start(request)

    suspend fun startProxy(mode: RunMode = networkSettingsStorage.runMode.value) =
        session.start(
            RuntimeStartRequest(
                owner = session.ownership.ownerForMode(mode),
                mode = mode,
            )
        )

    suspend fun stopProxy(request: RuntimeStopRequest) = session.stop(request)

    suspend fun stopProxy(mode: RunMode? = null) =
        session.stop(RuntimeStopRequest(targetMode = mode ?: networkSettingsStorage.runMode.value))

    suspend fun selectProxy(group: String, proxyName: String): Boolean =
        groups.selectProxy(group, proxyName)

    suspend fun healthCheck(group: String) = groups.healthCheck(group)

    suspend fun healthCheckAll() = groups.healthCheckAll()

    suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        groups.healthCheckProxy(group, proxyName)

    suspend fun queryConnections(): ConnectionSnapshot {
        if (!session.snapshotValue().running) {
            return ConnectionSnapshot()
        }
        return coreOps.queryConnections()
    }

    suspend fun queryTrafficTotal(): Long = session.queryTrafficTotal {
        coreOps.queryTrafficTotal()
    }

    suspend fun queryTrafficNow(): Long = session.queryTrafficNow { coreOps.queryTrafficNow() }

    suspend fun refreshProxyGroups() = groups.refreshProxyGroups()

    suspend fun refreshProxyGroup(name: String, sort: ProxySort = ProxySort.Default) =
        groups.refreshProxyGroup(name, sort)

    suspend fun refreshCurrentProfile() {
        if (isRemoteControllerActive()) {
            session.setCurrentProfile(null)
            session.updateProfileReady(null)
            return
        }
        runCatching {
            session.connectBackend()
            val profile = RuntimeAccess.profile().queryActive()
            session.setCurrentProfile(profile)
            session.updateProfileReady(profile)
        }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to refresh current profile")
            }
    }

    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        if (session.snapshotValue().phase == RuntimePhase.Running) {
            queryTrafficNow()
            queryTrafficTotal()
        } else {
            session.setTrafficNow(0L)
            session.setTrafficTotal(0L)
        }
    }

    private fun launchPreviewWarmup(): Job = scope.launch {
        runCatching { refreshProxyGroups() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Warm up proxy groups skipped")
            }
    }

    private suspend fun refreshAllSafely() {
        val snapshot = session.snapshotValue()
        if (
            snapshot.phase != RuntimePhase.Running &&
                snapshot.owner != RuntimeOwner.RemoteController
        ) {
            return
        }
        runCatching { refreshAll() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Refresh runtime data skipped")
            }
    }

    private suspend fun refreshPreviewStateSafely() {
        runCatching {
            refreshCurrentProfile()
            refreshProxyGroups()
        }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Refresh preview data skipped")
            }
    }

    private fun observeProxyGroupSyncPriority() {
        scope.launch {
            combine(session.runtimeSnapshot, syncPriorityRequests) { snapshot, requests ->
                    resolveEffectiveProxyGroupSyncPriority(snapshot, requests)
                }
                .distinctUntilChanged()
                .collect { priority -> session.startGroupPolling(priority) }
        }
    }

    private fun resolveEffectiveProxyGroupSyncPriority(
        snapshot: RuntimeSnapshot,
        requests: Map<String, ProxyGroupSyncPriority>,
    ): ProxyGroupSyncPriority {
        if (
            snapshot.phase != RuntimePhase.Running &&
                snapshot.owner != RuntimeOwner.RemoteController
        ) {
            return ProxyGroupSyncPriority.OFF
        }
        val requested = requests.values.maxByOrNull { it.ordinal } ?: ProxyGroupSyncPriority.OFF
        return if (requested.ordinal > ProxyGroupSyncPriority.SLOW.ordinal) {
            requested
        } else {
            ProxyGroupSyncPriority.SLOW
        }
    }
}
