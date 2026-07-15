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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.Traffic
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.store.RemoteControllerStore
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.runtime.api.Intents
import com.github.yumelira.yumebox.runtime.api.Profile
import com.github.yumelira.yumebox.runtime.api.RootTunStatus
import com.github.yumelira.yumebox.runtime.api.RuntimeOwner
import com.github.yumelira.yumebox.runtime.api.RuntimePhase
import com.github.yumelira.yumebox.runtime.api.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.api.VpnPermissionRequired
import com.github.yumelira.yumebox.runtime.api.appContextOrSelf
import com.github.yumelira.yumebox.runtime.client.manager.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunBootstrapCoordinator
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.runtime.service.StatusProvider
import com.github.yumelira.yumebox.runtime.service.root.RootTunStatusFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

enum class ProxyGroupSyncPriority {
    OFF,
    SLOW,
    FAST,
}

// Established runtime facade seam (UI-facing single entry point); splitting is tracked separately.
@Suppress("LargeClass")
class ProxyFacade(
    private val context: Context,
    private val networkSettingsStorage: NetworkSettingsStore =
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings")),
    private val remoteControllerStore: RemoteControllerStore =
        RemoteControllerStore(MMKVProvider().getMMKV("remote_controller")),
) {
    private companion object {
        const val TRAFFIC_TOTAL_POLL_TICKS = 10
        const val RUNTIME_PAYLOAD_REFRESH_TICKS = 15
        const val DEFAULT_SYNC_PRIORITY_SOURCE = "default"
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
        const val CONTROLLER_SWITCH_STOP_TIMEOUT_MS = 4000L
        const val CONTROLLER_SWITCH_STOP_POLL_MS = 100L
    }

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun isRemoteControllerActive(): Boolean =
        remoteControllerStore.controllerEnabled.value && remoteControllerStore.activeBackend() != null

    private val remoteClashManager: com.github.yumelira.yumebox.runtime.api.IClashManager by lazy {
        com.github.yumelira.yumebox.runtime.client.manager.HttpClashManager { remoteControllerStore.activeBackend() }
    }

    private suspend fun resolveClashManager(): com.github.yumelira.yumebox.runtime.api.IClashManager =
        if (isRemoteControllerActive()) {
            remoteClashManager
        } else {
            connectCurrentBackend()
            ServiceClient.clash()
        }

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

    private val groupStore =
        ProxyGroupStore(
            isRuntimeRunning = { _runtimeSnapshot.value.phase == RuntimePhase.Running },
            onGroupsReady = ::updateGroupsReady,
        )
    val proxyGroups: StateFlow<List<ProxyGroupInfo>>
        get() = groupStore.groups
    val resolvedPrimaryNode: StateFlow<Proxy?>
        get() = groupStore.resolvedPrimaryNode

    private val rootTunBootstrap =
        RootTunBootstrapCoordinator(appContext, scope) { status ->
            applyRootTunStatus(status)
            publishRuntimeSnapshot(
                ProxyRuntimeOwnership.activeSnapshot(
                    owner = RuntimeOwner.RootTun,
                    configuredMode = networkSettingsStorage.proxyMode.value,
                    rootStatus = status,
                    localPhase = RuntimePhase.Idle,
                )
            )
            if (status.state == RuntimePhase.Running) {
                startTrafficPolling()
                refreshAllSafely()
                true
            } else {
                false
            }
        }

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _trafficNow = MutableStateFlow(0L)
    val trafficNow: StateFlow<Traffic> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow(0L)
    val trafficTotal: StateFlow<Traffic> = _trafficTotal.asStateFlow()

    private var trafficPollingJob: Job? = null
    private var proxyGroupSyncJob: Job? = null
    private var previewWarmupJob: Job? = null
    private val refreshProxyGroupsMutex = Mutex()
    private val operationMutex = Mutex()
    private val controllerSwitchMutex = Mutex()
    private val syncPriorityRequests =
        MutableStateFlow<Map<String, ProxyGroupSyncPriority>>(emptyMap())
    private var activeProxyGroupSyncPriority = ProxyGroupSyncPriority.OFF
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
        observeRemoteController()
    }

    private fun observeRemoteController() {
        scope.launch {
            remoteControllerStore.controllerEnabled.state.collect { applyRemoteControllerState() }
        }
    }

    fun applyRemoteControllerState() {
        scope.launch { controllerSwitchMutex.withLock { applyRemoteControllerStateLocked() } }
    }

    private suspend fun applyRemoteControllerStateLocked() {
        if (isRemoteControllerActive()) {
            val snapshot = _runtimeSnapshot.value
            if (
                snapshot.owner != RuntimeOwner.RemoteController ||
                    snapshot.phase != RuntimePhase.Running
            ) {
                stopLocalRuntimeForControllerSwitch()
                publishRuntimeSnapshot(
                    RuntimeSnapshot(
                        owner = RuntimeOwner.RemoteController,
                        phase = RuntimePhase.Running,
                        targetMode = networkSettingsStorage.proxyMode.value,
                        generation = nextGeneration(),
                        startedAt = System.currentTimeMillis(),
                    )
                )
            }
            startTrafficPolling()
            refreshAllSafely()
        } else if (_runtimeSnapshot.value.owner == RuntimeOwner.RemoteController) {
            // controller mode turned off -> return to normal local/root reconciliation
            reconcileRuntimeState()
        }
    }

    private fun markRemoteControllerLost(error: Throwable) {
        val snapshot = _runtimeSnapshot.value
        if (snapshot.owner != RuntimeOwner.RemoteController) return

        publishRuntimeSnapshot(
            snapshot.copy(
                phase = RuntimePhase.Failed,
                trafficReady = false,
                lastError = error.message ?: error::class.simpleName ?: "remote backend lost",
                generation = nextGeneration(),
            )
        )
        _trafficNow.value = 0L
        _trafficTotal.value = 0L
    }

    private fun markRemoteControllerOnline() {
        val snapshot = _runtimeSnapshot.value
        if (snapshot.owner != RuntimeOwner.RemoteController || snapshot.phase == RuntimePhase.Running) {
            return
        }

        publishRuntimeSnapshot(
            snapshot.copy(
                phase = RuntimePhase.Running,
                lastError = null,
                generation = nextGeneration(),
                startedAt = snapshot.startedAt ?: System.currentTimeMillis(),
            )
        )
    }

    private suspend fun stopLocalRuntimeForControllerSwitch() {
        runCatching {
            val owner = detectActiveOwner()
            if (
                owner == RuntimeOwner.LocalTun ||
                    owner == RuntimeOwner.LocalHttp ||
                    owner == RuntimeOwner.RootTun
            ) {
                Timber.i("Controller switch: stopping local runtime owner=$owner")
                runtimeControl.stop(owner)
                stopTrafficPolling()
                awaitLocalRuntimeFullyStopped(owner)
            }
        }.onFailure { error ->
            Timber.w(error, "Failed to stop local runtime on controller switch")
        }
    }

    // stopService is async; wait until the local service is really gone, then reconcile persisted state.
    private suspend fun awaitLocalRuntimeFullyStopped(owner: RuntimeOwner) {
        val mode = localModeForOwner(owner)
        if (mode != null) {
            val deadline = System.currentTimeMillis() + CONTROLLER_SWITCH_STOP_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                if (!StatusProvider.isLocalRuntimeServiceAlive(mode)) break
                delay(CONTROLLER_SWITCH_STOP_POLL_MS)
            }
        }
        StatusProvider.reconcilePersistedRuntimeState()
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
        if (isRemoteControllerActive()) {
            applyRemoteControllerState()
            return
        }
        operationMutex.withLock {
            val configuredMode = networkSettingsStorage.proxyMode.value
            StatusProvider.reconcilePersistedRuntimeState()
            val shouldBootstrapRootTun = rootTunBootstrap.shouldBootstrap()
            val rootStatus = resolveObservedRootTunStatus()
            applyRootTunStatus(rootStatus)
            val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

            if (owner == RuntimeOwner.None) {
                stopTrafficPolling()
                clearRuntimeState(resetGroups = false)
                // Surface the retained failure record; otherwise the next reconcile wipes the
                // stop reason the service broadcast a moment ago and the failure goes silent.
                publishRuntimeSnapshot(
                    RuntimeStateMapper.idleSnapshot(
                        configuredMode,
                        lastError = StatusProvider.queryRuntimeLastError(configuredMode),
                    )
                )
                if (shouldBootstrapRootTun) {
                    rootTunBootstrap.schedule()
                } else {
                    rootTunBootstrap.stop()
                }
                refreshPreviewStateSafely()
                return
            }

            if (owner != RuntimeOwner.RootTun) {
                rootTunBootstrap.stop()
            }
            if (owner == RuntimeOwner.RootTun) {
                rootTunBootstrap.ensureServiceAttached(rootStatus)
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
                rootTunBootstrap.schedule()
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

    private fun launchPreviewWarmup(): Job = scope.launch {
        runCatching { refreshProxyGroups() }
            .onFailure { error -> Timber.d(error, "Warm up proxy groups skipped") }
    }

    suspend fun startProxy(mode: ProxyMode = networkSettingsStorage.proxyMode.value) {
        if (isRemoteControllerActive()) {
            Timber.i("Ignoring startProxy: remote controller mode active")
            return
        }
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
                        state = RuntimePhase.Starting
                    )
                )
                rootTunBootstrap.schedule()
                handleRuntimeStarted(forceOwner = RuntimeOwner.RootTun)
            }
        }
    }

    suspend fun stopProxy(mode: ProxyMode? = null) {
        if (isRemoteControllerActive()) {
            Timber.i("Ignoring stopProxy: remote controller mode active")
            return
        }
        val targetMode = mode ?: networkSettingsStorage.proxyMode.value

        operationMutex.withLock { stopProxyInternal(targetMode) }
    }

    suspend fun queryProxyGroupNames(excludeNotSelectable: Boolean = false): List<String> =
        resolveClashManager().queryProxyGroupNames(excludeNotSelectable)

    suspend fun queryProfileProxyGroups(excludeNotSelectable: Boolean = false): List<ProxyGroup> {
        return resolveClashManager().queryProfileProxyGroups(excludeNotSelectable)
    }

    suspend fun queryProxyGroup(name: String, sort: ProxySort = ProxySort.Default): ProxyGroup =
        resolveClashManager().queryProxyGroup(name, sort)

    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Select proxy: group=$group proxy=$proxyName")
        val ok = resolveClashManager().patchSelector(group, proxyName)
        if (ok) {
            // Optimistically reflect the user's pick immediately. For a Selector group the user's
            // choice IS authoritative, so set the group's `now` right away instead of waiting for
            // the core to commit it (a slow URLTest can delay `now` past the refresh window, which
            // would otherwise keep the highlight stale until the next periodic sync). The changed
            // `now` makes the summary differ so publishProxyGroups actually republishes. Only do
            // this when the group is already cached; otherwise rely on the refresh below.
            val cachedGroup = groupStore.groups.value.find { it.name == group }
            if (cachedGroup != null && cachedGroup.now != proxyName) {
                val optimisticGroups = groupStore.upsert(cachedGroup.copy(now = proxyName))
                groupStore.publish(optimisticGroups)
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
        resolveClashManager().healthCheck(group)
        Timber.d("Health check dispatched: group=%s", group)
        scheduleRuntimeGroupRefresh(group, PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
        scheduleRuntimeProxyGroupsRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckAll() {
        Timber.d("Health check all request")
        if (_runtimeSnapshot.value.owner == RuntimeOwner.RootTun) {
            // No healthCheckAll on the seam; iterate groups through the routed manager.
            val manager = resolveClashManager()
            manager
                .queryAllProxyGroups(excludeNotSelectable = false)
                .map { it.name }
                .forEach { groupName ->
                    manager.healthCheck(groupName)
                    scheduleRuntimeGroupRefresh(
                        groupName,
                        PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                    )
                }
        } else if (isRemoteControllerActive()) {
            groupStore.groups.value
                .map { it.name }
                .forEach { groupName ->
                    remoteClashManager.healthCheck(groupName)
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
        val delay = resolveClashManager().healthCheckProxy(group, proxyName)
        Timber.d("Health check proxy done: group=%s proxy=%s delay=%s", group, proxyName, delay)
        refreshProxyGroup(group)
        scheduleRuntimeProxyGroupsRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        return delay
    }

    suspend fun queryTunnelState(): TunnelState = resolveClashManager().queryTunnelState()

    suspend fun queryConnections(): ConnectionSnapshot {
        if (!_runtimeSnapshot.value.running) {
            return ConnectionSnapshot()
        }
        return resolveClashManager().queryConnections()
    }

    suspend fun queryTrafficTotal(): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficTotal.value = 0L
            return 0L
        }
        val snapshot = _runtimeSnapshot.value
        val traffic =
            runCatching { resolveClashManager().queryTrafficTotal() }
                .getOrElse { error ->
                    if (snapshot.owner == RuntimeOwner.RemoteController) {
                        markRemoteControllerLost(error)
                    }
                    throw error
                }
        _trafficTotal.value = traffic
        updateTrafficReady()
        if (snapshot.owner == RuntimeOwner.RemoteController) {
            markRemoteControllerOnline()
        }
        return traffic
    }

    suspend fun queryTrafficNow(): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficNow.value = 0L
            return 0L
        }
        val snapshot = _runtimeSnapshot.value
        val traffic =
            runCatching { resolveClashManager().queryTrafficNow() }
                .getOrElse { error ->
                    if (snapshot.owner == RuntimeOwner.RemoteController) {
                        markRemoteControllerLost(error)
                    }
                    throw error
                }
        _trafficNow.value = traffic
        updateTrafficReady()
        if (snapshot.owner == RuntimeOwner.RemoteController) {
            markRemoteControllerOnline()
        }
        return traffic
    }

    suspend fun reloadCurrentProfile(): Result<Unit> = runCatching {
        if (isRemoteControllerActive()) return Result.success(Unit)
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

                            resolveClashManager()
                                .queryAllProxyGroups(excludeNotSelectable = false)
                                .map(groupStore::toInfo)
                        }
                        .getOrElse { error ->
                            Timber.e(error, "Failed to refresh proxy groups")
                            missingLocalRuntime = isMissingLocalRuntime(snapshot)
                            null
                        }
                }

            if (groups != null) {
                if (snapshot.owner == RuntimeOwner.RemoteController) {
                    markRemoteControllerOnline()
                }
                groupStore.publish(groups)
            } else if (missingLocalRuntime) {
                handleMissingLocalRuntime(snapshot, "runtime backend unavailable")
            } else if (snapshot.owner == RuntimeOwner.RemoteController) {
                markRemoteControllerLost(IllegalStateException("remote backend unavailable"))
            }
        }
    }

    suspend fun refreshProxyGroup(name: String, sort: ProxySort = ProxySort.Default) {
        if (!_runtimeSnapshot.value.running) {
            if (groupStore.groups.value.isEmpty()) {
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

                            groupStore.toInfo(resolveClashManager().queryProxyGroup(name, sort))
                        }
                        .getOrElse { error ->
                            Timber.e(error, "Failed to refresh proxy group: %s", name)
                            null
                        }
                } ?: return

            val updatedGroups = groupStore.upsert(updatedGroup)
            groupStore.publish(updatedGroups)
        }
    }

    suspend fun refreshCurrentProfile() {
        if (isRemoteControllerActive()) {
            _currentProfile.value = null
            updateProfileReady(null)
            return
        }
        when {
            _runtimeSnapshot.value.owner == RuntimeOwner.RootTun &&
                _runtimeSnapshot.value.phase == RuntimePhase.Running -> {
                val status = currentRootTunStatus()
                applyRootTunStatus(status)
                refreshRootCurrentProfile(status)
            }

            else -> {
                runCatching {
                        // Ensure the local gateway is connected first (controller mode never
                        // initializes it), otherwise queryActive() throws "ServiceClient not
                        // connected" right after leaving controller mode.
                        connectCurrentBackend()
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
            rootTunBootstrap.stop()
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
            rootTunBootstrap.stop()
            applyRootTunStatus(
                RootTunStatus(
                    state = RuntimePhase.Stopping
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
                ContextCompat.registerReceiver(
                    appContext,
                    serviceEventsReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            }
            .onFailure { error -> Timber.w(error, "Failed to register service event receiver") }
    }

    private fun initializeRuntimeSnapshot() {
        if (isRemoteControllerActive()) {
            // Pure-remote mode restores a synthetic Running snapshot on cold start.
            applyRemoteControllerState()
            return
        }
        val configuredMode = networkSettingsStorage.proxyMode.value
        clearLegacyRuntimeCaches()
        StatusProvider.reconcilePersistedRuntimeState()
        val persistedRootStatus = RootTunStatusFlow.current(appContext)
        val shouldBootstrapRootTun = rootTunBootstrap.shouldBootstrap(persistedRootStatus)
        val rootStatus =
            persistedRootStatus.takeIf { it.isSessionActive } ?: RootTunStatus()
        applyRootTunStatus(rootStatus)
        val owner = ProxyRuntimeOwnership.detectOwner(rootStatus, ::isLocalSessionActive)

        if (owner == RuntimeOwner.None) {
            clearRuntimeState(resetGroups = false)
            publishRuntimeSnapshot(
                RuntimeStateMapper.idleSnapshot(
                    configuredMode,
                    lastError = StatusProvider.queryRuntimeLastError(configuredMode),
                )
            )
            if (shouldBootstrapRootTun) {
                rootTunBootstrap.schedule()
            } else {
                rootTunBootstrap.stop()
            }
            scope.launch { refreshPreviewStateSafely() }
            return
        }

        if (owner != RuntimeOwner.RootTun) {
            rootTunBootstrap.stop()
        }
        if (owner == RuntimeOwner.RootTun) {
            rootTunBootstrap.ensureServiceAttached(rootStatus)
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
            rootTunBootstrap.schedule()
            scope.launch { rootTunBootstrap.reconcileSafely() }
        }
    }

    private fun detectActiveOwner(): RuntimeOwner {
        StatusProvider.reconcilePersistedRuntimeState()
        return ProxyRuntimeOwnership.detectOwner(_rootTunStatus.value, ::isLocalSessionActive)
    }

    private suspend fun resolveObservedRootTunStatus(): RootTunStatus {
        val shouldProbeRuntime =
            rootTunBootstrap.shouldBootstrap() || _runtimeSnapshot.value.owner == RuntimeOwner.RootTun
        if (!shouldProbeRuntime) {
            return RootTunStatus()
        }

        return runCatching { RootTunController.queryStatus(appContext) }
            .onSuccess { status -> RootTunStatusFlow.update(status) }
            .getOrElse { error ->
                Timber.d(error, "RootTun live status unavailable during runtime reconcile")
                RootTunStatusFlow.current(appContext)
            }
    }

    private fun isRootSessionActive(): Boolean = _rootTunStatus.value.isSessionActive

    private fun isLocalSessionActive(mode: ProxyMode?): Boolean {
        if (mode == null) return false
        return StatusProvider.isRuntimeActive(mode)
    }

    private fun localRuntimePhaseForOwner(owner: RuntimeOwner): RuntimePhase {
        val localMode = localModeForOwner(owner) ?: return RuntimePhase.Idle
        return StatusProvider.queryRuntimePhase(localMode)
    }

    private fun localRuntimeStartedAtForOwner(owner: RuntimeOwner): Long? {
        val localMode = localModeForOwner(owner) ?: return null
        return StatusProvider.queryRuntimeStartedAt(localMode)
            ?: _runtimeSnapshot.value.startedAt?.takeIf { _runtimeSnapshot.value.owner == owner }
    }

    private fun localModeForOwner(owner: RuntimeOwner): ProxyMode? =
        RuntimeStateMapper.modeForOwner(owner)?.takeIf { it != ProxyMode.RootTun }

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
        if (isRemoteControllerActive()) {
            applyRemoteControllerState()
            return
        }
        val configuredMode = networkSettingsStorage.proxyMode.value
        val generation = nextGeneration()
        rootTunBootstrap.stop()

        if (!isRootSessionActive()) {
            val status = RootTunStatusFlow.current(appContext)
            if (status.state.isActiveOrStopping) {
                RootTunStatusFlow.markIdle(reason ?: status.lastError)
            }
            applyRootTunStatus(RootTunStatusFlow.current(appContext))
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
        if (isRemoteControllerActive()) {
            applyRemoteControllerState()
            return
        }
        val generation = nextGeneration()
        rootTunBootstrap.stop()
        if (!isRootSessionActive()) {
            RootTunStatusFlow.markIdle(error)
            applyRootTunStatus(RootTunStatusFlow.current(appContext))
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
        val snapshot = _runtimeSnapshot.value
        if (snapshot.phase != RuntimePhase.Running && snapshot.owner != RuntimeOwner.RemoteController) {
            return
        }
        runCatching { refreshAll() }
            .onFailure { error ->
                if (snapshot.owner == RuntimeOwner.RemoteController) {
                    markRemoteControllerLost(error)
                }
                Timber.d(error, "Refresh runtime data skipped")
            }
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
                groupStore.groups.value.isEmpty() ||
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
        if (snapshot.phase != RuntimePhase.Running && snapshot.owner != RuntimeOwner.RemoteController) {
            return ProxyGroupSyncPriority.OFF
        }
        val requested = requests.values.maxByOrNull { it.ordinal } ?: ProxyGroupSyncPriority.OFF
        return if (requested.ordinal > ProxyGroupSyncPriority.SLOW.ordinal) {
            requested
        } else {
            ProxyGroupSyncPriority.SLOW
        }
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
        val snapshot = _runtimeSnapshot.value
        if (snapshot.phase != RuntimePhase.Running && snapshot.owner != RuntimeOwner.RemoteController) {
            return
        }
        runCatching { refreshProxyGroups() }
            .onFailure { error ->
                if (snapshot.owner == RuntimeOwner.RemoteController) {
                    markRemoteControllerLost(error)
                }
                Timber.d(error, "Runtime proxy group sync skipped")
            }
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

    private suspend fun currentRootTunStatus(): RootTunStatus =
        runCatching { RootTunController.queryStatus(appContext) }
            .getOrElse { RootTunStatusFlow.current(appContext) }

    private fun clearLegacyRuntimeCaches() {
        StatusProvider.clearLegacyStateFiles()
        StatusProvider.reconcilePersistedRuntimeState()
        val rootStatus = RootTunStatusFlow.current(appContext)
        if (!rootStatus.state.isActiveOrStopping && !rootStatus.runtimeReady) {
            // The root process owns the durable store now; only reset the in-process view.
            RootTunStatusFlow.update(RootTunStatus())
        }
        applyRootTunStatus(RootTunStatus())
    }

    private fun isMissingLocalRuntime(snapshot: RuntimeSnapshot): Boolean {
        if (
            snapshot.owner == RuntimeOwner.RootTun ||
                snapshot.owner == RuntimeOwner.None ||
                snapshot.owner == RuntimeOwner.RemoteController
        ) {
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
            .onSuccess { groups -> groupStore.publish(groups) }
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
        if (isRemoteControllerActive()) {
            return resolveClashManager()
                .queryAllProxyGroups(excludeNotSelectable = false)
                .map(groupStore::toInfo)
        }
        // The local gateway is connected lazily; in controller mode it is never initialized, so a
        // preview query taken right after leaving controller mode would otherwise throw
        // "ServiceClient not connected" on the line below and the groups would stay empty. Connect
        // first so the local preview path is self-sufficient regardless of prior controller state.
        connectCurrentBackend()
        val activeProfile =
            ServiceClient.profile().queryActive().also {
                _currentProfile.value = it
                updateProfileReady(it)
            }

        if (activeProfile == null) {
            return emptyList()
        }
        val groups =
            ServiceClient.clash()
                .queryProfileProxyGroups(excludeNotSelectable = false)
                .map(groupStore::toInfo)

        return groups
    }

    private fun clearRuntimeState(resetGroups: Boolean = true) {
        _currentProfile.value = null
        groupStore.clear(resetGroups)
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
