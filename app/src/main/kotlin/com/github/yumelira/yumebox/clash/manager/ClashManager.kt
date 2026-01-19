package com.github.yumelira.yumebox.clash.manager

import android.content.Context
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.clash.config.RouteConfig
import com.github.yumelira.yumebox.clash.core.ClashCore
import com.github.yumelira.yumebox.clash.exception.toConfigImportException
import com.github.yumelira.yumebox.common.util.SystemProxyHelper
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.repository.ProxyStateRepository
import com.github.yumelira.yumebox.data.repository.SelectionDao
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxyState
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.net.InetSocketAddress

class ClashManager(
    private val context: Context,
    private val workDir: File,
    private val proxyModeProvider: (() -> TunnelState.Mode)? = null
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selectionDao = SelectionDao(context)

    private val currentProfileId = java.util.concurrent.atomic.AtomicReference<String>(null)

    val proxyStateRepository = ProxyStateRepository(
        context = context,
        proxyChainResolver = ProxyChainResolver(),
        profileIdProvider = { currentProfileId.get() }
    )

    private val _proxyState = MutableStateFlow<ProxyState>(ProxyState.Idle)
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    val isRunning: StateFlow<Boolean> = _proxyState.map { it.isRunning }.stateIn(scope, SharingStarted.Eagerly, false)

    val runningMode: StateFlow<RunningMode> = _proxyState.map { state ->
        when (state) {
            is ProxyState.Running -> state.mode
            is ProxyState.Connecting -> state.mode
            else -> RunningMode.None
        }
    }.stateIn(scope, SharingStarted.Eagerly, RunningMode.None)

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _trafficNow = MutableStateFlow(TrafficData.ZERO)
    val trafficNow: StateFlow<TrafficData> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow(TrafficData.ZERO)
    val trafficTotal: StateFlow<TrafficData> = _trafficTotal.asStateFlow()

    private val _tunnelState = MutableStateFlow<TunnelState?>(null)
    val tunnelState: StateFlow<TunnelState?> = _tunnelState.asStateFlow()

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyStateRepository.proxyGroups

    private val _logs = MutableSharedFlow<LogMessage>(replay = 100)
    val logs: SharedFlow<LogMessage> = _logs.asSharedFlow()

    private var monitorJob: Job? = null
    private var logJob: Job? = null

    init {
        workDir.mkdirs()
        startLogSubscription()
    }

    suspend fun loadProfile(profile: Profile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val configDir = getConfigDir(profile)
            if (!configDir.exists() || !configDir.isDirectory) {
                return@withContext Result.failure(IllegalStateException("配置目录不存在: ${profile.name}"))
            }

            val loadResult = ClashCore.loadConfig(
                configDir = configDir, options = ClashCore.LoadOptions(
                    timeoutMs = 30_000L, resetBeforeLoad = true, clearSessionOverride = true
                )
            )

            if (loadResult.isFailure) {
                return@withContext loadResult
            }

            _currentProfile.value = profile
            currentProfileId.set(profile.id)

            proxyModeProvider?.let { provider ->
                runCatching {
                    val mode = provider()
                    val persist = ClashCore.queryOverride(Clash.OverrideSlot.Persist)
                    if (persist.mode != mode) {
                        persist.mode = mode
                        ClashCore.patchOverride(Clash.OverrideSlot.Persist, persist)
                    }
                    val session = ClashCore.queryOverride(Clash.OverrideSlot.Session)
                    session.mode = mode
                    ClashCore.patchOverride(Clash.OverrideSlot.Session, session)
                }
            }

            // 先恢复存储的选择
            val selections = selectionDao.getAllSelections(profile.id)

            // 获取当前组列表（不需要同步，内核刚加载）
            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
            val groupsMap = groupNames.associateWith { name ->
                runCatching { Clash.queryGroup(name, ProxySort.Default) }.getOrNull()
            }

            // 恢复选择
            selections.forEach { (groupName, proxyName) ->
                val group = groupsMap[groupName]
                val proxy = group?.proxies?.find { it.name == proxyName }

                if (group != null && proxy != null && group.type == Proxy.Type.Selector) {
                    runCatching { ClashCore.selectProxy(groupName, proxyName) }
                }
            }

            // 开始同步
            proxyStateRepository.start()

            scope.launch {
                runCatching { proxyStateRepository.syncOnce() }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toConfigImportException())
        }
    }

    private fun getConfigDir(profile: Profile): File {
        return when (profile.type) {
            ProfileType.FILE -> {
                val configFile = File(profile.config)
                configFile.parentFile ?: workDir
            }

            ProfileType.URL -> {
                val importedDir = workDir.parentFile?.resolve("imported") ?: File(workDir, "imported")
                File(importedDir, profile.id)
            }
        }
    }

    suspend fun startTun(
        fd: Int,
        config: Configuration.TunConfig = Configuration.TunConfig(),
        enableIPv6: Boolean = false,
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile =
                _currentProfile.value ?: return@withContext Result.failure(IllegalStateException("请先加载配置"))

            _proxyState.value = ProxyState.Connecting(RunningMode.Tun)

            val gateway = buildString {
                append("${config.gateway}/30")
                if (enableIPv6) {
                    append(",${RouteConfig.TUN_GATEWAY6}/${RouteConfig.TUN_SUBNET_PREFIX6}")
                }
            }

            val portal = buildString {
                append(config.portal)
                if (enableIPv6) {
                    append(",${RouteConfig.TUN_PORTAL6}")
                }
            }

            val dns = buildString {
                if (config.dnsHijacking) {
                    if (enableIPv6) {
                        append("0.0.0.0")
                    } else {
                        // IPv6 关闭时，不劫持 DNS，让系统 DNS 处理（避免 AAAA 查询问题）
                        append(config.dns)
                    }
                } else {
                    append(config.dns)
                    if (enableIPv6) {
                        append(",${RouteConfig.TUN_DNS6}")
                    }
                }
            }

            ClashCore.startTun(
                fd = fd,
                stack = config.stack,
                gateway = gateway,
                portal = portal,
                dns = dns,
                markSocket = markSocket,
                querySocketUid = querySocketUid
            )

            _proxyState.value = ProxyState.Running(profile, RunningMode.Tun)
            startMonitor()

            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            _proxyState.value = ProxyState.Error("TUN启动失败: ${importException.message}", importException)
            Result.failure(importException)
        }
    }

    suspend fun startHttp(
        config: Configuration.HttpConfig = Configuration.HttpConfig()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val profile =
                _currentProfile.value ?: return@withContext Result.failure(IllegalStateException("请先加载配置"))

            _proxyState.value = ProxyState.Connecting(RunningMode.Http(config.address))

            val address = ClashCore.startHttp(config.listenAddress) ?: config.address

            _proxyState.value = ProxyState.Running(profile, RunningMode.Http(address))
            startMonitor()

            Result.success(address)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            _proxyState.value = ProxyState.Error("HTTP启动失败: ${importException.message}", importException)
            Result.failure(importException)
        }
    }

    fun stop() {
        runCatching { _proxyState.value = ProxyState.Stopping }
        runCatching { ClashCore.stopTun() }
        runCatching { ClashCore.stopHttp() }
        runCatching { SystemProxyHelper.clearSystemProxy(context) }

        runCatching {
            proxyStateRepository.stop()
            stopMonitor()
            resetState()
        }.onFailure {
            runCatching { ClashCore.reset() }
            proxyStateRepository.stop()
            stopMonitor()
            resetState()
        }
    }


    private fun startMonitor() {
        stopMonitor()

        monitorJob = scope.launch {
            while (isActive) {
                runCatching {
                    _trafficNow.value = TrafficData.from(ClashCore.queryTrafficNow())
                    _trafficTotal.value = TrafficData.from(ClashCore.queryTrafficTotal())
                    _tunnelState.value = ClashCore.queryTunnelState()
                }
                delay(1000)
            }
        }
    }

    private fun stopMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun startLogSubscription() {
        logJob = scope.launch {
            val channel = ClashCore.subscribeLogcat()
            for (log in channel) {
                if (!log.message.contains("Request interrupted by user") && !log.message.contains("更新延迟")) {
                    _logs.emit(log)
                }
            }
        }
    }

    private fun resetState() {
        _proxyState.value = ProxyState.Idle
        _currentProfile.value = null
        currentProfileId.set(null)
        _trafficNow.value = TrafficData.ZERO
        _trafficTotal.value = TrafficData.ZERO
        _tunnelState.value = null
    }

    suspend fun reloadCurrentProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        val profile = _currentProfile.value
            ?: return@withContext Result.failure(IllegalStateException("没有加载的配置"))
        loadProfile(profile)
    }

    suspend fun refreshProxyGroups(): Result<Unit> {
        return proxyStateRepository.syncFromCore()
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Boolean {
        return proxyStateRepository.selectProxy(groupName, proxyName).getOrDefault(false)
    }

    suspend fun healthCheck(groupName: String): Result<Unit> {
        return proxyStateRepository.testGroupDelay(groupName)
    }

    suspend fun healthCheckAll(): Result<Unit> {
        return proxyStateRepository.testAllDelay()
    }

    override fun close() {
        logJob?.cancel()
        monitorJob?.cancel()
        proxyStateRepository.close()
        scope.cancel("ClashManager closed")
        resetState()
    }
}
