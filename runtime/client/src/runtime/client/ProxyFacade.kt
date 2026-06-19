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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.remote.ServiceClient
import com.github.yumelira.yumebox.remote.VpnPermissionRequired
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.service.LocalRuntimePhase
import com.github.yumelira.yumebox.service.RootTunService
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.root.RootTunStateStore
import com.github.yumelira.yumebox.service.root.RootTunStatus
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import com.tencent.mmkv.MMKV
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

enum class ProxyGroupSyncPriority {
    OFF,
    SLOW,
    FAST,
}

class ProxyFacade(private val context: Context) {
    private companion object {
        const val TRAFFIC_TOTAL_POLL_TICKS = 10
        const val RUNTIME_PAYLOAD_REFRESH_TICKS = 15
        const val DEFAULT_SYNC_PRIORITY_SOURCE = "default"
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
        const val ROOT_TUN_BOOTSTRAP_ATTEMPTS = 20
        const val ROOT_TUN_BOOTSTRAP_DELAY_MS = 300L
    }

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val networkSettingsStorage by lazy {
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings"))
    }
    private val rootTunStateStore by lazy { RootTunStateStore(appContext) }
    private val runtimeControl = ProxyRuntimeControl(appContext) { actionClashRequestStop }
    private val _rootTunStatus = MutableStateFlow(RootTunStatus())
    val rootTunStatus: StateFlow<RootTunStatus> = _rootTunStatus.asStateFlow()
    private val _runtimeSnapshot =
        MutableStateFlow(RuntimeStateMapper.idleSnapshot(networkSettingsStorage.proxyMode.value))
    val runtimeSnapshot: StateFlow<RuntimeSnapshot> = _runtimeSnapshot.asStateFlow()

    private val actionServiceRecreated: String
        get() = Intents.actionServiceRecreated(appContext.packageName)

    private val actionClashStarted: String
        get() = Intents.actionClashStarted(appContext.packageName)

    private val actionClashStopped: String
        get() = Intents.actionClashStopped(appContext.packageName)

    private val actionClashRequestStop: String
        get() = Intents.actionClashRequestStop(appContext.packageName)

    private val actionProfileChanged: String
        get() = Intents.actionProfileChanged(appContext.packageName)

    private val actionProfileLoaded: String
        get() = Intents.actionProfileLoaded(appContext.packageName)

    private val actionOverrideChanged: String
        get() = Intents.actionOverrideChanged(appContext.packageName)

    private val actionRootRuntimeFailed: String
        get() = Intents.actionRootRuntimeFailed(appContext.packageName)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()
    private val _resolvedPrimaryNode = MutableStateFlow<Proxy?>(null)
    val resolvedPrimaryNode: StateFlow<Proxy?> = _resolvedPrimaryNode.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _trafficNow = MutableStateFlow(0L)
    val trafficNow: StateFlow<Traffic> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow(0L)
    val trafficTotal: StateFlow<Traffic> = _trafficTotal.asStateFlow()

    private var trafficPollingJob: Job? = null
    private var proxyGroupSyncJob: Job? = null
    private var previewWarmupJob: Job? = null
    private var rootTunBootstrapJob: Job? = null
    private val refreshProxyGroupsMutex = Mutex()
    private val operationMutex = Mutex()
    private val syncPriorityRequests =
        MutableStateFlow<Map<String, ProxyGroupSyncPriority>>(emptyMap())
    private var activeProxyGroupSyncPriority = ProxyGroupSyncPriority.OFF
    private var lastProxyGroupsSummary: String? = null
    private var generationCounter = 0L

    private val serviceEventsReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action ?: return) {
                    actionClashStarted -> {
                        scope.launch { reconcileAndRefreshRuntimeState() }
                    }

                    actionClashStopped -> {
                        scope.launch {
                            handleRuntimeStopped(intent.getStringExtra(Intents.EXTRA_STOP_REASON))
                        }
                    }

                    actionProfileLoaded,
                    actionProfileChanged,
                    actionOverrideChanged,
                    actionServiceRecreated -> {
                        scope.launch { reconcileAndRefreshRuntimeState() }
                    }

                    actionRootRuntimeFailed -> {
                        val error = intent.getStringExtra("error")
                        Timber.w("Root runtime failed: $error")
                        scope.launch { handleRuntimeFailure(error) }
                    }
                }
            }
        }

    init {
        registerServiceEventReceiver()
        observeProxyGroupSyncPriority()
        initializeRuntimeSnapshot()
    }

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
        }

        val job = launchPreviewWarmup()
        previewWarmupJob = job
        job.join()
    }

    suspend fun reconcileRuntimeState() {
        operationMutex.withLock {
            val configuredMode = networkSettingsStorage.proxyMode.value
            StatusProvider.reconcilePersistedRuntimeState()
            val shouldBootstrapRootTun = shouldBootstrapRootTunRuntime()
            val rootStatus = resolveObservedRootTunStatus()
            applyRootTunStatus(rootStatus)
            val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

            if (owner == RuntimeOwner.None) {
                stopTrafficPolling()
                clearRuntimeState(resetGroups = false)
                publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(configuredMode))
                if (shouldBootstrapRootTun) {
                    scheduleRootTunBootstrap()
                } else {
                    stopRootTunBootstrap()
                }
                refreshPreviewStateSafely()
                return
            }

            if (owner != RuntimeOwner.RootTun) {
                stopRootTunBootstrap()
            }
            if (owner == RuntimeOwner.RootTun) {
                ensureRootTunServiceAttached(rootStatus)
            }

            publishRuntimeSnapshot(
                ProxyRuntimeOwnership.activeSnapshot(
                    owner = owner,
                    configuredMode = configuredMode,
                    rootStatus = rootStatus,
                    localPhase = localRuntimePhaseForOwner(owner),
                    localStartedAt = localRuntimeStartedAtForOwner(owner),
                )
            )

            if (_runtimeSnapshot.value.phase.running) {
                startTrafficPolling()
                refreshAllSafely()
            } else {
                stopTrafficPolling()
                refreshPreviewStateSafely()
            }
            if (owner == RuntimeOwner.RootTun) {
                scheduleRootTunBootstrap()
            }
        }
    }

    private suspend fun reconcileAndRefreshRuntimeState() {
        reconcileRuntimeState()
        if (_runtimeSnapshot.value.phase == RuntimePhase.Running) {
            refreshAllSafely()
        } else {
            refreshPreviewStateSafely()
        }
    }

    private fun launchPreviewWarmup(): Job {
        return scope.launch {
            runCatching { refreshProxyGroups() }
                .onFailure { error -> Timber.d(error, "Warm up proxy groups skipped") }
        }
    }

    suspend fun startProxy(mode: ProxyMode = networkSettingsStorage.proxyMode.value) {
        Timber.i("Start proxy: mode=$mode")
        ServiceClient.connect(appContext)

        val activeProfile = ServiceClient.profile().queryActive()
        check(activeProfile != null) { "No profile selected" }

        if (mode == ProxyMode.Tun) {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                throw VpnPermissionRequired(vpnIntent)
            }
        }

        operationMutex.withLock {
            val targetOwner = ProxyRuntimeOwnership.ownerForMode(mode)
            val currentOwner =
                detectActiveOwner().takeIf { it != RuntimeOwner.None }
                    ?: _runtimeSnapshot.value.owner
            if (currentOwner != RuntimeOwner.None) {
                stopProxyInternal(targetMode = mode, completeImmediately = true)
            }

            val generation = nextGeneration()

            clearRuntimeState(resetGroups = false)
            _currentProfile.value = activeProfile
            publishRuntimeSnapshot(
                ProxyRuntimeOwnership.startingSnapshot(
                    owner = targetOwner,
                    targetMode = mode,
                    profile = activeProfile,
                    generation = generation,
                )
            )

            runCatching { runtimeControl.start(targetOwner, mode) }
                .onFailure { error ->
                    clearRuntimeState(resetGroups = false)
                    publishRuntimeSnapshot(
                        RuntimeStateMapper.idleSnapshot(
                            configuredMode = mode,
                            generation = generation,
                            lastError = error.message,
                        )
                    )
                    stopTrafficPolling()
                    scope.launch { refreshPreviewStateSafely() }
                    throw error
                }
            if (targetOwner == RuntimeOwner.RootTun) {
                applyRootTunStatus(
                    RootTunStatus(
                        state = com.github.yumelira.yumebox.service.root.RootTunState.Starting
                    )
                )
                scheduleRootTunBootstrap()
                handleRuntimeStarted(forceOwner = RuntimeOwner.RootTun)
            }
        }
    }

    suspend fun stopProxy(mode: ProxyMode? = null) {
        val targetMode = mode ?: networkSettingsStorage.proxyMode.value

        operationMutex.withLock { stopProxyInternal(targetMode) }
    }

    suspend fun queryProxyGroupNames(excludeNotSelectable: Boolean = false): List<String> {
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            return RootTunController.queryProxyGroupNames(appContext, excludeNotSelectable)
        }
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable)
    }

    suspend fun queryProfileProxyGroups(excludeNotSelectable: Boolean = false): List<ProxyGroup> {
        connectCurrentBackend()
        return ServiceClient.clash().queryProfileProxyGroups(excludeNotSelectable)
    }

    suspend fun queryProxyGroup(name: String, sort: ProxySort = ProxySort.Default): ProxyGroup {
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            return RootTunController.queryProxyGroup(appContext, name, sort)
        }
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroup(name, sort)
    }

    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Select proxy: group=$group proxy=$proxyName")
        val ok =
            if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
                RootTunController.patchSelector(appContext, group, proxyName)
            } else {
                connectCurrentBackend()
                ServiceClient.clash().patchSelector(group, proxyName)
            }
        if (ok) {
            // Optimistically reflect the user's pick immediately. For a Selector group the user's
            // choice IS authoritative, so set the group's `now` right away instead of waiting for
            // the core to commit it (a slow URLTest can delay `now` past the refresh window, which
            // would otherwise keep the highlight stale until the next periodic sync). The changed
            // `now` makes the summary differ so publishProxyGroups actually republishes. Only do
            // this when the group is already cached; otherwise rely on the refresh below.
            val cachedGroup = _proxyGroups.value.find { it.name == group }
            if (cachedGroup != null && cachedGroup.now != proxyName) {
                val optimisticGroups = updateCachedProxyGroup(cachedGroup.copy(now = proxyName))
                publishProxyGroups(optimisticGroups)
            }
            PollingTimers.awaitTick(
                PollingTimerSpecs.dynamic(
                    name = "proxy_select_refresh",
                    intervalMillis = 200L,
                    initialDelayMillis = 200L,
                )
            )
            refreshProxyGroup(group)
            scheduleRuntimeProxyGroupsRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        }
        return ok
    }

    suspend fun healthCheck(group: String) {
        Timber.d("Health check request: group=%s", group)
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.healthCheck(appContext, group)
        } else {
            connectCurrentBackend()
            ServiceClient.clash().healthCheck(group)
        }
        Timber.d("Health check dispatched: group=%s", group)
        scheduleRuntimeGroupRefresh(group, PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
        scheduleRuntimeProxyGroupsRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckAll() {
        Timber.d("Health check all request")
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.queryAllProxyGroups(appContext, excludeNotSelectable = false)
                .map { it.name }
                .forEach { groupName ->
                    RootTunController.healthCheck(appContext, groupName)
                    scheduleRuntimeGroupRefresh(
                        groupName,
                        PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                    )
                }
        } else {
            connectCurrentBackend()
            Clash.healthCheckAll()
        }
        scheduleRuntimeProxyGroupsRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("Health check proxy request: group=%s proxy=%s", group, proxyName)
        val delay =
            if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
                RootTunController.healthCheckProxy(appContext, group, proxyName).toIntOrNull() ?: 0
            } else {
                connectCurrentBackend()
                ServiceClient.clash().healthCheckProxy(group, proxyName)
            }
        Timber.d("Health check proxy done: group=%s proxy=%s delay=%s", group, proxyName, delay)
        refreshProxyGroup(group)
        scheduleRuntimeProxyGroupsRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        return delay
    }

    suspend fun queryTunnelState(): TunnelState {
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            return RootTunController.queryTunnelState(appContext)
        }
        connectCurrentBackend()
        return ServiceClient.clash().queryTunnelState()
    }

    suspend fun queryConnections(): ConnectionSnapshot {
        if (!_runtimeSnapshot.value.running) {
            return ConnectionSnapshot()
        }
        return if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            RootTunController.queryConnections(appContext)
        } else {
            connectCurrentBackend()
            ServiceClient.clash().queryConnections()
        }
    }

    suspend fun queryTrafficTotal(): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficTotal.value = 0L
            return 0L
        }
        val traffic =
            if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
                RootTunController.queryTrafficTotal(appContext)
            } else {
                connectCurrentBackend()
                ServiceClient.clash().queryTrafficTotal()
            }
        _trafficTotal.value = traffic
        updateTrafficReady()
        return traffic
    }

    suspend fun queryTrafficNow(): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficNow.value = 0L
            return 0L
        }
        val traffic =
            if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
                RootTunController.queryTrafficNow(appContext)
            } else {
                connectCurrentBackend()
                ServiceClient.clash().queryTrafficNow()
            }
        _trafficNow.value = traffic
        updateTrafficReady()
        return traffic
    }

    suspend fun reloadCurrentProfile(): Result<Unit> {
        return runCatching {
            val profileManager = ServiceClient.profile()
            val currentProfile = profileManager.queryActive()
            if (currentProfile != null) {
                profileManager.setActive(currentProfile)
                _currentProfile.value = currentProfile
                PollingTimers.awaitTick(
                    PollingTimerSpecs.dynamic(
                        name = "runtime_profile_reload_refresh",
                        intervalMillis = 600L,
                        initialDelayMillis = 600L,
                    )
                )
                refreshAll()
            }
        }
    }

    fun updateServiceState(isRunning: Boolean) {
        _isRunning.value = isRunning
    }

    suspend fun refreshProxyGroups() {
        refreshProxyGroupsMutex.withLock {
            val snapshot = _runtimeSnapshot.value
            var missingLocalRuntime = false
            val groups =
                withContext(Dispatchers.IO) {
                    runCatching {
                            if (!snapshot.running) {
                                return@runCatching queryPreviewProxyGroups()
                            }

                            if (snapshot.owner == RuntimeOwner.RootTun && !isRootSessionActive()) {
                                error("RootTun runtime not ready")
                            }

                            if (snapshot.owner == RuntimeOwner.RootTun) {
                                RootTunController.queryAllProxyGroups(
                                        context = appContext,
                                        excludeNotSelectable = false,
                                    )
                                    .map(::toProxyGroupInfo)
                            } else {
                                connectCurrentBackend()
                                ServiceClient.clash()
                                    .queryAllProxyGroups(excludeNotSelectable = false)
                                    .map(::toProxyGroupInfo)
                            }
                        }
                        .getOrElse { error ->
                            Timber.e(error, "Failed to refresh proxy groups")
                            missingLocalRuntime = isMissingLocalRuntime(snapshot)
                            null
                        }
                }

            if (groups != null) {
                publishProxyGroups(groups)
            } else if (missingLocalRuntime) {
                handleMissingLocalRuntime(snapshot, "runtime backend unavailable")
            }
        }
    }

    suspend fun refreshProxyGroup(name: String, sort: ProxySort = ProxySort.Default) {
        if (!_runtimeSnapshot.value.running) {
            if (_proxyGroups.value.isEmpty()) {
                refreshProxyGroups()
            }
            return
        }

        refreshProxyGroupsMutex.withLock {
            val snapshot = _runtimeSnapshot.value
            val updatedGroup =
                withContext(Dispatchers.IO) {
                    runCatching {
                            if (snapshot.owner == RuntimeOwner.RootTun && !isRootSessionActive()) {
                                error("RootTun runtime not ready")
                            }

                            if (snapshot.owner == RuntimeOwner.RootTun) {
                                toProxyGroupInfo(
                                    RootTunController.queryProxyGroup(appContext, name, sort)
                                )
                            } else {
                                connectCurrentBackend()
                                toProxyGroupInfo(ServiceClient.clash().queryProxyGroup(name, sort))
                            }
                        }
                        .getOrElse { error ->
                            Timber.e(error, "Failed to refresh proxy group: %s", name)
                            null
                        }
                } ?: return

            val updatedGroups = updateCachedProxyGroup(updatedGroup)
            publishProxyGroups(updatedGroups)
        }
    }

    suspend fun refreshCurrentProfile() {
        when {
            _runtimeSnapshot.value.owner == RuntimeOwner.RootTun &&
                _runtimeSnapshot.value.phase == RuntimePhase.Running -> {
                val status = currentRootTunStatus()
                applyRootTunStatus(status)
                refreshRootCurrentProfile(status)
            }

            else -> {
                runCatching {
                        val profile = ServiceClient.profile().queryActive()
                        _currentProfile.value = profile
                        updateProfileReady(profile)
                    }
                    .onFailure { error -> Timber.e(error, "Failed to refresh current profile") }
            }
        }
    }

    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        if (_runtimeSnapshot.value.phase == RuntimePhase.Running) {
            queryTrafficNow()
            queryTrafficTotal()
        } else {
            _trafficNow.value = 0L
            _trafficTotal.value = 0L
        }
    }

    private suspend fun stopProxyInternal(
        targetMode: ProxyMode,
        completeImmediately: Boolean = false,
    ) {
        val owner =
            detectActiveOwner().takeIf { it != RuntimeOwner.None } ?: _runtimeSnapshot.value.owner
        val generation = nextGeneration()

        if (owner == RuntimeOwner.None) {
            stopRootTunBootstrap()
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(
                RuntimeStateMapper.idleSnapshot(targetMode, generation = generation)
            )
            stopTrafficPolling()
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        val previousSnapshot = _runtimeSnapshot.value
        publishRuntimeSnapshot(
            previousSnapshot.copy(
                owner = owner,
                phase = RuntimePhase.Stopping,
                targetMode = targetMode,
                profileReady = false,
                groupsReady = false,
                trafficReady = false,
                lastError = null,
                generation = generation,
            )
        )

        runCatching { runtimeControl.stop(owner) }
            .onFailure {
                publishRuntimeSnapshot(previousSnapshot)
                throw it
            }
        if (owner == RuntimeOwner.RootTun) {
            stopRootTunBootstrap()
            applyRootTunStatus(
                RootTunStatus(
                    state = com.github.yumelira.yumebox.service.root.RootTunState.Stopping
                )
            )
        }

        stopTrafficPolling()
        if (!completeImmediately) {
            return
        }

        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(targetMode, generation = generation))
        scope.launch { refreshPreviewStateSafely() }
    }

    private fun startTrafficPolling() {
        if (trafficPollingJob?.isActive == true) return
        trafficPollingJob = scope.launch {
            var tick = 0
            PollingTimers.ticks(PollingTimerSpecs.RuntimeTrafficPolling).collect {
                val snapshot = _runtimeSnapshot.value
                if (!snapshot.running) {
                    return@collect
                }

                runCatching {
                        queryTrafficNow()
                        if (tick % TRAFFIC_TOTAL_POLL_TICKS == 0) {
                            queryTrafficTotal()
                        }
                    }
                    .onFailure { error -> Timber.d(error, "Traffic polling skipped") }
                tick++

                if (tick % RUNTIME_PAYLOAD_REFRESH_TICKS == 0 && shouldRefreshRuntimePayload()) {
                    refreshAllSafely()
                }
            }
        }
    }

    private fun stopTrafficPolling() {
        trafficPollingJob?.cancel()
        trafficPollingJob = null
    }

    private fun stopProxyGroupSync() {
        proxyGroupSyncJob?.cancel()
        proxyGroupSyncJob = null
    }

    private fun stopRootTunBootstrap() {
        rootTunBootstrapJob?.cancel()
        rootTunBootstrapJob = null
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerServiceEventReceiver() {
        val filter =
            IntentFilter().apply {
                addAction(actionClashStarted)
                addAction(actionClashStopped)
                addAction(actionProfileChanged)
                addAction(actionProfileLoaded)
                addAction(actionOverrideChanged)
                addAction(actionServiceRecreated)
                addAction(actionRootRuntimeFailed)
            }
        runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(
                        serviceEventsReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED,
                    )
                } else {
                    appContext.registerReceiver(serviceEventsReceiver, filter)
                }
            }
            .onFailure { error -> Timber.w(error, "Failed to register service event receiver") }
    }

    private fun initializeRuntimeSnapshot() {
        val configuredMode = networkSettingsStorage.proxyMode.value
        clearLegacyRuntimeCaches()
        StatusProvider.reconcilePersistedRuntimeState()
        val persistedRootStatus = rootTunStateStore.snapshot()
        val shouldBootstrapRootTun = shouldBootstrapRootTunRuntime(persistedRootStatus)
        val rootStatus =
            persistedRootStatus.takeIf(::shouldAttachRootTunForegroundService) ?: RootTunStatus()
        applyRootTunStatus(rootStatus)
        val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

        if (owner == RuntimeOwner.None) {
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(RuntimeStateMapper.idleSnapshot(configuredMode))
            if (shouldBootstrapRootTun) {
                scheduleRootTunBootstrap()
            } else {
                stopRootTunBootstrap()
            }
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        if (owner != RuntimeOwner.RootTun) {
            stopRootTunBootstrap()
        }
        if (owner == RuntimeOwner.RootTun) {
            ensureRootTunServiceAttached(rootStatus)
        }

        publishRuntimeSnapshot(
            ProxyRuntimeOwnership.activeSnapshot(
                owner = owner,
                configuredMode = configuredMode,
                rootStatus = rootStatus,
                localPhase = localRuntimePhaseForOwner(owner),
                localStartedAt = localRuntimeStartedAtForOwner(owner),
            )
        )
        if (_runtimeSnapshot.value.phase.running) {
            startTrafficPolling()
            scope.launch { refreshAllSafely() }
        } else {
            stopTrafficPolling()
            scope.launch { refreshPreviewStateSafely() }
        }
        if (owner == RuntimeOwner.RootTun) {
            scheduleRootTunBootstrap()
            scope.launch { reconcileRootTunRuntimeStateSafely() }
        }
    }

    private fun detectActiveOwner(): RuntimeOwner {
        StatusProvider.reconcilePersistedRuntimeState()
        return ProxyRuntimeOwnership.detectOwner(_rootTunStatus.value, ::isLocalSessionActive)
    }

    private suspend fun resolveObservedRootTunStatus(): RootTunStatus {
        val shouldProbeRuntime =
            shouldBootstrapRootTunRuntime() || _runtimeSnapshot.value.owner == RuntimeOwner.RootTun
        if (!shouldProbeRuntime) {
            return RootTunStatus()
        }

        return runCatching { RootTunController.queryStatus(appContext) }
            .onSuccess { status -> rootTunStateStore.updateStatus(status) }
            .getOrElse { error ->
                Timber.d(error, "RootTun live status unavailable during runtime reconcile")
                rootTunStateStore.snapshot()
            }
    }

    private fun shouldBootstrapRootTunRuntime(
        status: RootTunStatus = rootTunStateStore.snapshot()
    ): Boolean {
        return shouldAttachRootTunForegroundService(status)
    }

    private fun shouldAttachRootTunForegroundService(status: RootTunStatus): Boolean {
        return status.state.isActive || status.runtimeReady
    }

    private fun isRootSessionActive(): Boolean {
        val status = _rootTunStatus.value
        return status.state.isActive || status.runtimeReady
    }

    private fun isLocalSessionActive(mode: ProxyMode?): Boolean {
        if (mode == null) return false
        return StatusProvider.isRuntimeActive(mode)
    }

    private fun localRuntimePhaseForOwner(owner: RuntimeOwner): LocalRuntimePhase {
        val localMode = localModeForOwner(owner) ?: return LocalRuntimePhase.Idle
        return StatusProvider.queryRuntimePhase(localMode)
    }

    private fun localRuntimeStartedAtForOwner(owner: RuntimeOwner): Long? {
        val localMode = localModeForOwner(owner) ?: return null
        return StatusProvider.queryRuntimeStartedAt(localMode)
            ?: _runtimeSnapshot.value.startedAt?.takeIf { _runtimeSnapshot.value.owner == owner }
    }

    private fun localModeForOwner(owner: RuntimeOwner): ProxyMode? {
        return when (owner) {
            RuntimeOwner.LocalTun -> ProxyMode.Tun
            RuntimeOwner.LocalHttp -> ProxyMode.Http
            RuntimeOwner.RootTun,
            RuntimeOwner.None -> null
        }
    }

    private suspend fun handleRuntimeStarted(forceOwner: RuntimeOwner? = null) {
        val currentSnapshot = _runtimeSnapshot.value
        val owner =
            forceOwner
                ?: currentSnapshot.owner.takeIf { it != RuntimeOwner.None }
                ?: detectActiveOwner()
        if (owner == RuntimeOwner.None) return

        publishRuntimeSnapshot(
            ProxyRuntimeOwnership.startedSnapshot(
                current = currentSnapshot,
                owner = owner,
                configuredMode = networkSettingsStorage.proxyMode.value,
            )
        )
        startTrafficPolling()
        refreshAllSafely()
    }

    private suspend fun handleRuntimeStopped(reason: String?) {
        val configuredMode = networkSettingsStorage.proxyMode.value
        val generation = nextGeneration()
        stopRootTunBootstrap()

        if (!isRootSessionActive()) {
            val status = rootTunStateStore.snapshot()
            if (status.state.isActive) {
                rootTunStateStore.markIdle(reason ?: status.lastError)
            }
            applyRootTunStatus(rootTunStateStore.snapshot())
        }

        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = configuredMode,
                generation = generation,
                lastError = reason,
            )
        )
        stopTrafficPolling()
        scope.launch { refreshPreviewStateSafely() }
    }

    private fun handleRuntimeFailure(error: String?) {
        val generation = nextGeneration()
        stopRootTunBootstrap()
        if (!isRootSessionActive()) {
            rootTunStateStore.markIdle(error)
            applyRootTunStatus(rootTunStateStore.snapshot())
        }
        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.proxyMode.value,
                generation = generation,
                lastError = error ?: "root runtime failed",
            )
        )
        stopTrafficPolling()
        scope.launch { refreshPreviewStateSafely() }
    }

    private suspend fun refreshAllSafely() {
        if (_runtimeSnapshot.value.phase != RuntimePhase.Running) {
            return
        }
        runCatching { refreshAll() }
            .onFailure { error -> Timber.d(error, "Refresh runtime data skipped") }
    }

    private suspend fun refreshPreviewStateSafely() {
        runCatching {
                refreshCurrentProfile()
                refreshProxyGroups()
            }
            .onFailure { error -> Timber.d(error, "Refresh preview data skipped") }
    }

    private fun shouldRefreshRuntimePayload(): Boolean {
        val snapshot = _runtimeSnapshot.value
        return snapshot.phase == RuntimePhase.Running &&
            (!snapshot.profileReady ||
                !snapshot.groupsReady ||
                _proxyGroups.value.isEmpty() ||
                _currentProfile.value == null)
    }

    private fun observeProxyGroupSyncPriority() {
        scope.launch {
            combine(_runtimeSnapshot, syncPriorityRequests) { snapshot, requests ->
                    resolveEffectiveProxyGroupSyncPriority(snapshot, requests)
                }
                .distinctUntilChanged()
                .collect { priority -> restartProxyGroupSyncLoop(priority) }
        }
    }

    private fun resolveEffectiveProxyGroupSyncPriority(
        snapshot: RuntimeSnapshot,
        requests: Map<String, ProxyGroupSyncPriority>,
    ): ProxyGroupSyncPriority {
        if (snapshot.phase != RuntimePhase.Running) {
            return ProxyGroupSyncPriority.OFF
        }
        val requested = requests.values.maxByOrNull { it.ordinal } ?: ProxyGroupSyncPriority.OFF
        return if (requested.ordinal > ProxyGroupSyncPriority.SLOW.ordinal) requested
        else ProxyGroupSyncPriority.SLOW
    }

    private fun restartProxyGroupSyncLoop(priority: ProxyGroupSyncPriority) {
        if (activeProxyGroupSyncPriority == priority && proxyGroupSyncJob?.isActive == true) {
            return
        }
        activeProxyGroupSyncPriority = priority
        stopProxyGroupSync()
        if (priority == ProxyGroupSyncPriority.OFF) {
            return
        }

        val timerSpec =
            when (priority) {
                ProxyGroupSyncPriority.FAST -> PollingTimerSpecs.RuntimeProxyGroupSyncFast
                ProxyGroupSyncPriority.SLOW -> PollingTimerSpecs.RuntimeProxyGroupSyncSlow
                ProxyGroupSyncPriority.OFF -> return
            }
        proxyGroupSyncJob = scope.launch {
            PollingTimers.ticks(timerSpec).collect { refreshRuntimeProxyGroupsSafely() }
        }
    }

    private suspend fun refreshRuntimeProxyGroupsSafely() {
        if (_runtimeSnapshot.value.phase != RuntimePhase.Running) {
            return
        }
        runCatching { refreshProxyGroups() }
            .onFailure { error -> Timber.d(error, "Runtime proxy group sync skipped") }
    }

    private fun scheduleRuntimeGroupRefresh(groupName: String, delayMillis: Long = 0L) {
        if (groupName.isBlank()) return
        scope.launch {
            awaitDelay(delayMillis, "runtime_proxy_group_refresh_$groupName")
            runCatching { refreshProxyGroup(groupName) }
                .onFailure { error ->
                    Timber.d(error, "Deferred proxy group refresh skipped: %s", groupName)
                }
        }
    }

    private fun scheduleRuntimeProxyGroupsRefresh(delayMillis: Long = 0L) {
        scope.launch {
            awaitDelay(delayMillis, "runtime_proxy_groups_refresh")
            refreshRuntimeProxyGroupsSafely()
        }
    }

    private suspend fun awaitDelay(delayMillis: Long, name: String) {
        if (delayMillis <= 0L) {
            return
        }
        PollingTimers.awaitTick(
            PollingTimerSpecs.dynamic(
                name = name,
                intervalMillis = delayMillis,
                initialDelayMillis = delayMillis,
            )
        )
    }

    private suspend fun currentRootTunStatus(): RootTunStatus {
        return runCatching { RootTunController.queryStatus(appContext) }
            .getOrElse { rootTunStateStore.snapshot() }
    }

    private suspend fun reconcileRootTunRuntimeStateSafely() {
        runCatching {
                val persistedStatus = rootTunStateStore.snapshot()
                if (!shouldAttachRootTunForegroundService(persistedStatus)) {
                    return@runCatching
                }
                ensureRootTunServiceAttached(persistedStatus)
                val status = RootTunController.queryStatus(appContext)
                rootTunStateStore.updateStatus(status)
                applyRootTunStatus(status)
                val configuredMode = networkSettingsStorage.proxyMode.value
                publishRuntimeSnapshot(
                    ProxyRuntimeOwnership.activeSnapshot(
                        owner = RuntimeOwner.RootTun,
                        configuredMode = configuredMode,
                        rootStatus = status,
                        localPhase = LocalRuntimePhase.Idle,
                    )
                )
                if (status.state == com.github.yumelira.yumebox.service.root.RootTunState.Running) {
                    startTrafficPolling()
                    refreshAllSafely()
                }
            }
            .onFailure { error -> Timber.d(error, "RootTun bootstrap reconcile skipped") }
    }

    private fun ensureRootTunServiceAttached(status: RootTunStatus = rootTunStateStore.snapshot()) {
        if (!shouldAttachRootTunForegroundService(status)) {
            return
        }
        runCatching { RootTunService.start(appContext) }
            .onFailure { error -> Timber.d(error, "Attach RootTun foreground service skipped") }
    }

    private fun scheduleRootTunBootstrap() {
        if (rootTunBootstrapJob?.isActive == true) return
        rootTunBootstrapJob = scope.launch {
            repeat(ROOT_TUN_BOOTSTRAP_ATTEMPTS) { attempt ->
                val persistedStatus = rootTunStateStore.snapshot()
                if (!shouldAttachRootTunForegroundService(persistedStatus)) {
                    return@launch
                }

                val status =
                    runCatching {
                            ensureRootTunServiceAttached(persistedStatus)
                            RootTunController.queryStatus(appContext)
                        }
                        .getOrNull()

                if (status != null) {
                    rootTunStateStore.updateStatus(status)
                    applyRootTunStatus(status)
                    val configuredMode = networkSettingsStorage.proxyMode.value
                    publishRuntimeSnapshot(
                        ProxyRuntimeOwnership.activeSnapshot(
                            owner = RuntimeOwner.RootTun,
                            configuredMode = configuredMode,
                            rootStatus = status,
                            localPhase = LocalRuntimePhase.Idle,
                        )
                    )
                    if (
                        status.state ==
                            com.github.yumelira.yumebox.service.root.RootTunState.Running
                    ) {
                        startTrafficPolling()
                        refreshAllSafely()
                        return@launch
                    }
                }

                if (attempt < ROOT_TUN_BOOTSTRAP_ATTEMPTS - 1) {
                    delay(ROOT_TUN_BOOTSTRAP_DELAY_MS)
                }
            }
        }
    }

    private fun clearLegacyRuntimeCaches() {
        StatusProvider.clearLegacyStateFiles()
        StatusProvider.reconcilePersistedRuntimeState()
        val rootStatus = rootTunStateStore.snapshot()
        if (!rootStatus.state.isActive && !rootStatus.runtimeReady) {
            runCatching { rootTunStateStore.clear() }
        }
        applyRootTunStatus(RootTunStatus())
        if (!StatusProvider.serviceRunning) {
            runCatching { MMKV.mmkvWithID("runtime_snapshot", MMKV.MULTI_PROCESS_MODE).clearAll() }
        }
    }

    private fun isMissingLocalRuntime(snapshot: RuntimeSnapshot): Boolean {
        if (snapshot.owner == RuntimeOwner.RootTun || snapshot.owner == RuntimeOwner.None) {
            return false
        }
        val mode = RuntimeStateMapper.modeForOwner(snapshot.owner) ?: return false
        return !StatusProvider.isLocalRuntimeServiceAlive(mode)
    }

    private suspend fun handleMissingLocalRuntime(snapshot: RuntimeSnapshot, reason: String?) {
        val mode = RuntimeStateMapper.modeForOwner(snapshot.owner) ?: return
        StatusProvider.markRuntimeIdle(mode)
        clearRuntimeState(resetGroups = false)
        publishRuntimeSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.proxyMode.value,
                generation = nextGeneration(),
                lastError = reason,
            )
        )
        stopTrafficPolling()
        runCatching { queryPreviewProxyGroups() }
            .onSuccess { groups -> publishProxyGroups(groups) }
            .onFailure { error ->
                Timber.d(error, "Fallback preview refresh skipped after stale runtime reset")
            }
    }

    private fun applyRootTunStatus(status: RootTunStatus) {
        _rootTunStatus.value = status
    }

    private fun publishRuntimeSnapshot(snapshot: RuntimeSnapshot) {
        val normalized = snapshot.copy(running = snapshot.phase.running)
        _runtimeSnapshot.value = normalized
        _isRunning.value = normalized.running
    }

    private fun nextGeneration(): Long {
        generationCounter += 1L
        return generationCounter
    }

    private suspend fun connectCurrentBackend() {
        ServiceClient.connect(appContext)
    }

    private suspend fun refreshRootCurrentProfile(status: RootTunStatus) {
        runCatching {
                connectCurrentBackend()
                val profile =
                    status.profileUuid
                        ?.takeIf { it.isNotBlank() }
                        ?.let { uuid -> ServiceClient.profile().queryByUUID(UUID.fromString(uuid)) }
                        ?: ServiceClient.profile().queryActive()

                if (profile != null) {
                    _currentProfile.value = profile
                }
                updateProfileReady(profile)
            }
            .onFailure { error -> Timber.d(error, "Failed to refresh root current profile") }
    }

    private suspend fun queryPreviewProxyGroups(): List<ProxyGroupInfo> {
        val activeProfile =
            ServiceClient.profile().queryActive().also {
                _currentProfile.value = it
                updateProfileReady(it)
            }

        if (activeProfile == null) {
            return emptyList()
        }
        connectCurrentBackend()
        val groups =
            ServiceClient.clash()
                .queryProfileProxyGroups(excludeNotSelectable = false)
                .map(::toProxyGroupInfo)

        return groups
    }

    private fun publishProxyGroups(groups: List<ProxyGroupInfo>) {
        val summary = summarizeProxyGroups(groups)
        if (summary != lastProxyGroupsSummary) {
            _proxyGroups.value = groups
            lastProxyGroupsSummary = summary
        }
        updateGroupsReady(groups.isNotEmpty())
        updateResolvedPrimaryNode(groups)
    }

    private fun toProxyGroupInfo(group: ProxyGroup): ProxyGroupInfo {
        return ProxyGroupInfo(
            name = group.name,
            type = group.type,
            proxies = group.proxies,
            now = group.now.trim(),
            icon = group.icon,
            hidden = group.hidden,
        )
    }

    private fun updateCachedProxyGroup(updated: ProxyGroupInfo): List<ProxyGroupInfo> {
        val currentGroups = _proxyGroups.value
        if (currentGroups.isEmpty()) return listOf(updated)
        if (currentGroups.none { it.name == updated.name }) {
            return currentGroups + updated
        }
        return currentGroups.map { group -> if (group.name == updated.name) updated else group }
    }

    private fun summarizeProxyGroups(groups: List<ProxyGroupInfo>): String {
        return groups.joinToString(separator = "\n") { group ->
            buildString {
                append(group.name)
                append('|')
                append(group.type.name)
                append('|')
                append(group.now)
                append('|')
                append(group.hidden)
                append('|')
                append(group.proxies.size)
                group.proxies.forEach { proxy ->
                    append('|')
                    append(proxy.name)
                    append(':')
                    append(proxy.type.name)
                    append(':')
                    append(proxy.isGroup)
                    append(':')
                    append(proxy.delay)
                }
            }
        }
    }

    private fun updateResolvedPrimaryNode(groups: List<ProxyGroupInfo>) {
        if (_runtimeSnapshot.value.phase != RuntimePhase.Running || groups.isEmpty()) {
            _resolvedPrimaryNode.value = null
            return
        }
        val mainGroup =
            groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
        val targetNode = mainGroup?.now?.trim().orEmpty()
        _resolvedPrimaryNode.value =
            targetNode.takeIf(String::isNotEmpty)?.let { resolveProxyNode(it, groups) }
    }

    private fun resolveProxyNode(
        nodeName: String,
        groups: List<ProxyGroupInfo>,
        visited: MutableSet<String> = linkedSetOf(),
    ): Proxy? {
        if (!visited.add(nodeName)) {
            return null
        }

        val group = groups.firstOrNull { it.name == nodeName }
        if (group != null) {
            val groupNow = group.now.trim()
            return groupNow
                .takeIf { it.isNotEmpty() }
                ?.let { resolveProxyNode(it, groups, visited) }
        }

        groups.forEach { proxyGroup ->
            val proxy = proxyGroup.proxies.firstOrNull { it.name == nodeName } ?: return@forEach
            if (proxy.isProxyGroup) {
                val nextGroup = groups.firstOrNull { it.name == proxy.name } ?: return null
                val nextNode = nextGroup.now.trim()
                return nextNode
                    .takeIf { it.isNotEmpty() }
                    ?.let { resolveProxyNode(it, groups, visited) }
            }
            return proxy
        }
        return null
    }

    private fun clearRuntimeState(resetGroups: Boolean = true) {
        _currentProfile.value = null
        if (resetGroups) {
            _proxyGroups.value = emptyList()
            lastProxyGroupsSummary = null
        }
        _resolvedPrimaryNode.value = null
        _trafficNow.value = 0L
        _trafficTotal.value = 0L
    }

    private fun updateProfileReady(profile: Profile?) {
        val snapshot = _runtimeSnapshot.value
        publishRuntimeSnapshot(
            snapshot.copy(
                profileReady = profile != null,
                profileUuid = profile?.uuid?.toString() ?: snapshot.profileUuid,
                profileName = profile?.name ?: snapshot.profileName,
            )
        )
    }

    private fun updateGroupsReady(ready: Boolean) {
        publishRuntimeSnapshot(_runtimeSnapshot.value.copy(groupsReady = ready))
    }

    private fun updateTrafficReady() {
        if (!_runtimeSnapshot.value.trafficReady) {
            publishRuntimeSnapshot(_runtimeSnapshot.value.copy(trafficReady = true))
        }
    }

}
