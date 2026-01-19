package com.github.yumelira.yumebox.data.repository

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.data.model.Selection
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

class ProxyStateRepository(
    private val context: Context,
    private val proxyChainResolver: ProxyChainResolver,
    private val profileIdProvider: () -> String? = { null }
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()

    private val selectionDao = SelectionDao(context)
    private val lastSavedStates = ConcurrentHashMap<String, String>()

    init {
        scope.launch {
            proxyGroups.collect { groups ->
                val profileId = profileIdProvider() ?: return@collect

                for (i in groups.indices) {
                    val group = groups[i]
                    if (group.type == Proxy.Type.Selector && group.now.isNotEmpty()) {
                        val name = group.name
                        val last = lastSavedStates[name]
                        if (last != group.now) {
                            lastSavedStates[name] = group.now
                            selectionDao.setSelected(Selection(profileId, name, group.now))
                        }
                    }
                }
            }
        }
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val isActive = AtomicBoolean(false)

    private var autoSyncJob: Job? = null

    fun start(intervalMs: Long = 5000L) {
        if (isActive.getAndSet(true)) return

        autoSyncJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                syncFromCore()
            }
        }
    }

    suspend fun syncOnce() = syncFromCore()

    fun stop() {
        if (!isActive.getAndSet(false)) return

        autoSyncJob?.cancel()
        autoSyncJob = null
        _proxyGroups.value = emptyList()
        _isSyncing.value = false
    }

    suspend fun syncFromCore(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isActive) return@withContext Result.failure(IllegalStateException("未启动"))

        _isSyncing.value = true
        try {
            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
            if (groupNames.isEmpty()) {
                _proxyGroups.value = emptyList()
                return@withContext Result.success(Unit)
            }

            val groups = groupNames.map { name ->
                async {
                    try {
                        val group = Clash.queryGroup(name, ProxySort.Default)
                        ProxyGroupInfo(name = name, type = group.type, proxies = group.proxies, now = group.now)
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            val groupsWithChain = groups.map { group ->
                val chainPath = if (group.now.isNotEmpty()) proxyChainResolver.buildChainPath(group.now, groups) else emptyList()
                group.copy(chainPath = chainPath)
            }

            _proxyGroups.value = groupsWithChain
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("同步失败"))
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val group = _proxyGroups.value.find { it.name == groupName }
            ?: return@withContext Result.failure(IllegalArgumentException("代理组不存在"))

        if (group.type != Proxy.Type.Selector) {
            return@withContext Result.failure(IllegalArgumentException("非Selector类型"))
        }

        val success = Clash.patchSelector(groupName, proxyName)
        if (success) syncFromCore()
        Result.success(success)
    }

    suspend fun testGroupDelay(groupName: String): Result<Unit> = withContext(Dispatchers.IO) {
        Clash.healthCheck(groupName).await()
        delay(500)
        syncFromCore()
        Result.success(Unit)
    }

    suspend fun testAllDelay(): Result<Unit> = withContext(Dispatchers.IO) {
        Clash.healthCheckAll()
        delay(1000)
        syncFromCore()
        Result.success(Unit)
    }

    fun getCachedDelay(nodeName: String): Int? {
        return _proxyGroups.value.asSequence()
            .flatMap { it.proxies.asSequence() }
            .firstOrNull { it.name == nodeName }?.delay
    }

    fun findGroup(groupName: String): ProxyGroupInfo? = _proxyGroups.value.find { it.name == groupName }

    fun findProxy(nodeName: String): Proxy? {
        return _proxyGroups.value.asSequence()
            .flatMap { it.proxies.asSequence() }
            .firstOrNull { it.name == nodeName }
    }

    fun getCurrentSelection(groupName: String): String? = findGroup(groupName)?.now

    fun isSelectableGroup(groupName: String): Boolean = findGroup(groupName)?.type == Proxy.Type.Selector

    fun close() {
        stop()
        scope.cancel()
    }
}
