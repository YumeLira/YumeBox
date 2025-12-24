package com.github.yumelira.yumebox.clash.manager

import android.content.Context
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.clash.core.ClashCore
import com.github.yumelira.yumebox.clash.exception.toConfigImportException
import com.github.yumelira.yumebox.common.util.SystemProxyHelper
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.LogMessage
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

    val proxyStateRepository = ProxyStateRepository(context, ProxyChainResolver())

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

            Timber.d("开始加载配置：${profile.name}")

            val loadResult = ClashCore.loadConfig(
                configDir = configDir, options = ClashCore.LoadOptions(
                    timeoutMs = 30_000L, resetBeforeLoad = true, clearSessionOverride = true
                )
            )

            if (loadResult.isFailure) {
                val error = loadResult.exceptionOrNull()
                Timber.e(error, "配置加载失败：${profile.name}")
                return@withContext loadResult
            }

            _currentProfile.value = profile

            proxyModeProvider?.let { provider ->
                try {
                    val mode = provider()
                    val persist = ClashCore.queryOverride(Clash.OverrideSlot.Persist)
                    if (persist.mode != mode) {
                        persist.mode = mode
                        ClashCore.patchOverride(Clash.OverrideSlot.Persist, persist)
                    }
                    val session = ClashCore.queryOverride(Clash.OverrideSlot.Session)
                    session.mode = mode
                    ClashCore.patchOverride(Clash.OverrideSlot.Session, session)
                    Timber.d("应用代理模式：$mode")
                } catch (e: Exception) {
                    Timber.e(e, "应用代理模式失败")
                }
            }

            proxyStateRepository.start()

            scope.launch {
                try {


                    val selections = selectionDao.getAllSelections(profile.id)
                    Timber.d("恢复 ${selections.size} 个节点选择")

                    selections.forEach { (groupName, proxyName) ->
                        runCatching {
                            ClashCore.selectProxy(groupName, proxyName)
                            Timber.d("恢复选择：$groupName -> $proxyName")
                        }.onFailure { e ->
                            Timber.e(e, "恢复选择失败：$groupName -> $proxyName")
                        }
                    }

                    proxyStateRepository.syncFromCore()
                } catch (e: Exception) {
                    Timber.e(e, "恢复节点选择失败")
                }
            }

            scope.launch {
                try {
                    proxyStateRepository.syncFromCore()
                } catch (e: Exception) {
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, "加载配置失败：${profile.name}")
            Result.failure(importException)
        }
    }

    private fun getConfigDir(profile: Profile): File {
        return when (profile.type) {
            ProfileType.FILE -> {
                val configFile = File(profile.config)
                configFile.parentFile ?: configFile.parentFile!!
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
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile =
                _currentProfile.value ?: return@withContext Result.failure(IllegalStateException("请先加载配置"))

            Timber.d("启动 TUN 模式")
            _proxyState.value = ProxyState.Connecting(RunningMode.Tun)

            ClashCore.startTun(
                fd = fd,
                stack = config.stack,
                gateway = "${config.gateway}/30",
                portal = "${config.portal}/30",
                dns = config.dns,
                markSocket = markSocket,
                querySocketUid = querySocketUid
            )

            _proxyState.value = ProxyState.Running(profile, RunningMode.Tun)
            startMonitor()

            Timber.d("TUN 模式启动成功")
            Result.success(Unit)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, "TUN 模式启动失败")
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

            Timber.d("启动 HTTP 代理模式")
            _proxyState.value = ProxyState.Connecting(RunningMode.Http(config.address))

            val address = ClashCore.startHttp(config.listenAddress) ?: config.address

            _proxyState.value = ProxyState.Running(profile, RunningMode.Http(address))
            startMonitor()

            Timber.d("HTTP 代理模式启动成功：$address")
            Result.success(address)
        } catch (e: Exception) {
            val importException = e.toConfigImportException()
            Timber.e(importException, "HTTP 代理模式启动失败")
            _proxyState.value = ProxyState.Error("HTTP启动失败: ${importException.message}", importException)
            Result.failure(importException)
        }
    }

    fun stop() {
        runCatching {
            _proxyState.value
            _proxyState.value = ProxyState.Stopping

            try {
                ClashCore.stopTun()
            } catch (e: Exception) {
            }

            try {
                ClashCore.stopHttp()
            } catch (e: Exception) {
            }

            try {
                SystemProxyHelper.clearSystemProxy(context)
            } catch (e: Exception) {
            }

            try {
                ClashCore.reset()
            } catch (e: Exception) {
            }

            proxyStateRepository.stop()

            stopMonitor()

            resetState()

        }.onFailure { _ ->
            try {
                ClashCore.reset()
            } catch (_: Exception) {
            }
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

                }.onFailure { e ->
                    Timber.e(e, "监控任务执行失败")
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
            try {
                val channel = ClashCore.subscribeLogcat()
                for (log in channel) {
                    if (!log.message.contains("Request interrupted by user") && !log.message.contains("更新延迟")) {
                        _logs.emit(log)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "日志订阅错误")
            }
        }
    }

    private fun resetState() {
        _proxyState.value = ProxyState.Idle
        _currentProfile.value = null
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
