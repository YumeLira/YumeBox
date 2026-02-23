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
 * Copyright (c)  YumeLira 2025.
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
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.Traffic
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.common.constants.Intents
import kotlinx.coroutines.delay
import com.github.yumelira.yumebox.remote.ServiceClient
import com.github.yumelira.yumebox.remote.VpnPermissionRequired
import com.github.yumelira.yumebox.service.ClashService
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.TunService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * ProxyFacade - proxy management facade
 */
class ProxyFacade(private val context: Context) {
    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val actionServiceRecreated: String get() = Intents.actionServiceRecreated(appContext.packageName)
    private val actionClashStarted: String get() = Intents.actionClashStarted(appContext.packageName)
    private val actionClashStopped: String get() = Intents.actionClashStopped(appContext.packageName)
    private val actionClashRequestStop: String get() = Intents.actionClashRequestStop(appContext.packageName)
    private val actionProfileChanged: String get() = Intents.actionProfileChanged(appContext.packageName)
    private val actionProfileLoaded: String get() = Intents.actionProfileLoaded(appContext.packageName)
    private val actionOverrideChanged: String get() = Intents.actionOverrideChanged(appContext.packageName)

    // Service running state
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Proxy groups
    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()

    // Current profile
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    // Traffic statistics
    private val _trafficNow = MutableStateFlow<Traffic>(0L)
    val trafficNow: StateFlow<Traffic> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow<Traffic>(0L)
    val trafficTotal: StateFlow<Traffic> = _trafficTotal.asStateFlow()

    private var trafficPollingJob: Job? = null

    private val serviceEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                actionClashStarted -> {
                    updateServiceState(true)
                    startTrafficPolling()
                    scope.launch { refreshAllSafely() }
                }
                actionClashStopped -> {
                    updateServiceState(false)
                    stopTrafficPolling()
                    _currentProfile.value = null
                    _proxyGroups.value = emptyList()
                    _trafficNow.value = 0L
                    _trafficTotal.value = 0L
                }
                actionProfileLoaded,
                actionProfileChanged,
                actionOverrideChanged,
                actionServiceRecreated -> {
                    scope.launch { refreshAllSafely() }
                }
            }
        }
    }

    init {
        registerServiceEventReceiver()

        // In single-process mode, avoid touching Clash core when service is not running.
        // Querying proxy groups here would initialize native core eagerly and increase idle RSS.
        if (StatusProvider.serviceRunning) {
            // Best-effort sync when service is already running in current process.
            scope.launch {
                runCatching {
                    ServiceClient.connect(appContext)
                    refreshCurrentProfile()
                    val groups = ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable = false)
                    if (groups.isNotEmpty()) {
                        updateServiceState(true)
                        startTrafficPolling()
                        refreshAll()
                    }
                }.onFailure { e ->
                    Timber.d(e, "Initial proxy state sync skipped")
                }
            }
        }
    }

    private fun startTrafficPolling() {
        if (trafficPollingJob?.isActive == true) return
        trafficPollingJob = scope.launch {
            while (true) {
                runCatching {
                    ServiceClient.connect(appContext)
                    queryTrafficNow()
                    if ((System.currentTimeMillis() / 5000L) % 2L == 0L) {
                        queryTrafficTotal()
                    }
                }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    private fun stopTrafficPolling() {
        trafficPollingJob?.cancel()
        trafficPollingJob = null
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerServiceEventReceiver() {
        val filter = IntentFilter().apply {
            addAction(actionClashStarted)
            addAction(actionClashStopped)
            addAction(actionProfileChanged)
            addAction(actionProfileLoaded)
            addAction(actionOverrideChanged)
            addAction(actionServiceRecreated)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(serviceEventsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(serviceEventsReceiver, filter)
            }
        }.onFailure { e ->
            Timber.w(e, "Failed to register service event receiver")
        }
    }

    /**
     * Start proxy service
     * @param useTun Whether to use TUN mode (VPN)
     * @throws VpnPermissionRequired if VPN permission is needed but not granted
     */
    suspend fun startProxy(useTun: Boolean = false) {
        Timber.i("Starting proxy service, useTun=$useTun")
        ServiceClient.connect(appContext)

        // Ensure an active profile exists before starting clash runtime, otherwise service will
        // be stuck on "loading" and then stop due to ConfigurationModule load failure.
        val activeProfile = ServiceClient.profile().queryActive()
        if (activeProfile == null) {
            Timber.w("No active profile, abort start")
            throw IllegalStateException("No profile selected")
        }

        // Check VPN permission if needed
        if (useTun) {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                Timber.w("VPN permission required")
                throw VpnPermissionRequired(vpnIntent)
            }
        }

        // Start appropriate service
        val serviceIntent = if (useTun) {
            Intent(context, TunService::class.java)
        } else {
            Intent(context, ClashService::class.java)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Readiness check: only mark running after proxy groups are available.
        val ready = withTimeoutOrNull(8_000L) {
            while (true) {
                val groups = withContext(Dispatchers.IO) {
                    runCatching {
                        ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable = false)
                    }.getOrNull()
                }

                if (!groups.isNullOrEmpty()) return@withTimeoutOrNull true

                delay(250)
            }
        } == true

        if (!ready) {
            val groups = runCatching {
                ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable = false)
            }.getOrDefault(emptyList())

            if (groups.isEmpty()) {
                updateServiceState(false)
                stopTrafficPolling()
                _proxyGroups.value = emptyList()
                _trafficNow.value = 0L
                _trafficTotal.value = 0L
                Timber.w("Proxy runtime readiness failed: no proxy groups available")
                throw IllegalStateException("代理启动失败：配置未成功加载（可能是配置校验失败或网络资源下载失败）")
            }
        }

        updateServiceState(true)
        startTrafficPolling()
        refreshAllSafely()
        Timber.i("Proxy service started successfully: ${activeProfile.name}")
    }

    /**
     * Stop proxy service
     */
    suspend fun stopProxy() {
        Timber.i("Stopping proxy service")

        withContext(Dispatchers.IO) {
            runCatching {
                ServiceClient.connect(appContext)
                ServiceClient.clash().requestStop()
            }.onFailure {
                appContext.sendBroadcast(Intent(actionClashRequestStop).setPackage(appContext.packageName))
                context.stopService(Intent(context, TunService::class.java))
                context.stopService(Intent(context, ClashService::class.java))
            }

            runCatching { ServiceClient.disconnect() }
        }

        updateServiceState(false)
        stopTrafficPolling()
        _currentProfile.value = null
        _trafficNow.value = 0L
        _trafficTotal.value = 0L

        Timber.i("Proxy service stopped")
    }

    /**
     * Query all proxy group names
     * @param excludeNotSelectable Exclude non-selectable groups
     * @return List of proxy group names
     */
    suspend fun queryProxyGroupNames(excludeNotSelectable: Boolean = false): List<String> {
        return ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable)
    }

    /**
     * Query proxy group details
     * @param name Group name
     * @param sort Proxy sorting method
     * @return ProxyGroup details
     */
    suspend fun queryProxyGroup(
        name: String,
        sort: ProxySort = ProxySort.Default
    ): ProxyGroup {
        return ServiceClient.clash().queryProxyGroup(name, sort)
    }

    /**
     * Select proxy in a group
     * @param group Group name
     * @param proxyName Proxy name to select
     * @return True if selection successful
     */
    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Selecting proxy: group=$group, proxy=$proxyName")
        val ok = ServiceClient.clash().patchSelector(group, proxyName)
        if (ok) {
            // Clash 会异步更新 selector 的 now 值，稍等再刷新一把 UI
            delay(200)
            refreshProxyGroups()
        }
        return ok
    }

    /**
     * Perform health check on proxy group
     * @param group Group name
     */
    suspend fun healthCheck(group: String, refreshAfter: Boolean = true) {
        Timber.d("Health check: group=$group")
        ServiceClient.clash().healthCheck(group)
        if (refreshAfter) {
            // 延迟测试结果是异步写回的，刷新几次确保 UI 能拿到 delay
            repeat(4) {
                delay(600)
                refreshProxyGroups()
            }
        }
    }

    /**
     * Query current tunnel state
     * @return TunnelState
     */
    suspend fun queryTunnelState(): TunnelState {
        return ServiceClient.clash().queryTunnelState()
    }

    /**
     * Query total traffic
     * @return Total traffic in bytes
     */
    suspend fun queryTrafficTotal(): Long {
        val traffic = ServiceClient.clash().queryTrafficTotal()
        _trafficTotal.value = traffic
        return traffic
    }

    /**
     * Query current traffic (upload/download speed)
     */
    suspend fun queryTrafficNow(): Long {
        val traffic = ServiceClient.clash().queryTrafficNow()
        _trafficNow.value = traffic
        return traffic
    }

    /**
     * Reload current profile configuration
     */
    suspend fun reloadCurrentProfile(): Result<Unit> {
        return runCatching {
            // Query current profile from ProfileManager
            val profileManager = ServiceClient.profile()
            val currentProfile = profileManager.queryActive()
            if (currentProfile != null) {
                // Reload profile to apply configuration changes
                profileManager.setActive(currentProfile)
                _currentProfile.value = currentProfile
                // 等待 service 侧 ConfigurationModule 完成 load，再刷 UI
                delay(600)
                refreshAll()
            }
        }
    }

    /**
     * Update service running state
     */
    fun updateServiceState(isRunning: Boolean) {
        _isRunning.value = isRunning
    }

    /**
     * Refresh proxy groups
     */
    suspend fun refreshProxyGroups() {
        runCatching {
            if (!_isRunning.value) return@runCatching
            
            val groupNames = queryProxyGroupNames(excludeNotSelectable = false)
            val groups = groupNames.map { name ->
                val proxyGroup = queryProxyGroup(name)
                ProxyGroupInfo(
                    name = name,
                    type = proxyGroup.type,
                    proxies = proxyGroup.proxies,
                    now = proxyGroup.now ?: "",
                    icon = proxyGroup.icon
                )
            }
            _proxyGroups.value = groups
        }.onFailure { e ->
            Timber.e(e, "Failed to refresh proxy groups")
        }
    }

    /**
     * Refresh current profile
     */
    suspend fun refreshCurrentProfile() {
        runCatching {
            val profile = ServiceClient.profile().queryActive()
            _currentProfile.value = profile
        }.onFailure { e ->
            Timber.e(e, "Failed to refresh current profile")
        }
    }

    /**
     * Refresh all state
     */
    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        queryTrafficNow()
        queryTrafficTotal()
    }

    private suspend fun refreshAllSafely() {
        runCatching {
            ServiceClient.connect(appContext)
            refreshAll()
        }.onFailure { e ->
            Timber.w(e, "refreshAllSafely failed")
        }
    }

}

