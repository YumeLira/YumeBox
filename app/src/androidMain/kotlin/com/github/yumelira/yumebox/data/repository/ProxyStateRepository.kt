package com.github.yumelira.yumebox.data.repository

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ProxyStateRepository(
    private val context: Context,
    private val proxyChainResolver: ProxyChainResolver
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val isActive = AtomicBoolean(false)

    private var autoSyncJob: Job? = null

    fun start(intervalMs: Long = 5000L) {
        if (isActive.getAndSet(true)) {
            return
        }

        autoSyncJob = scope.launch {
            while (isActive) {
                try {
                    delay(intervalMs)
                    syncFromCore()
                } catch (e: Exception) {
                    Timber.e(e, "自动同步失败")
                }
            }
        }
    }

    fun stop() {
        if (!isActive.getAndSet(false)) {
            return
        }

        autoSyncJob?.cancel()
        autoSyncJob = null

        _proxyGroups.value = emptyList()
        _isSyncing.value = false
    }

    suspend fun syncFromCore(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isActive) {
            return@withContext Result.failure(IllegalStateException("仓库未启动"))
        }

        try {
            _isSyncing.value = true

            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)

            if (groupNames.isEmpty()) {
                _proxyGroups.value = emptyList()
                return@withContext Result.success(Unit)
            }

            val groups = groupNames.map { name ->
                async {
                    try {
                        val group = Clash.queryGroup(name, ProxySort.Default)
                        ProxyGroupInfo(
                            name = name,
                            type = group.type,
                            proxies = group.proxies,
                            now = group.now
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "查询代理组失败: $name")
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            val groupsWithChain = groups.map { group ->
                val chainPath = if (group.now.isNotBlank()) {
                    proxyChainResolver.buildChainPath(group.now, groups)
                } else {
                    emptyList()
                }
                group.copy(chainPath = chainPath)
            }

            _proxyGroups.value = groupsWithChain

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "同步代理组状态失败")
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val group = _proxyGroups.value.find { it.name == groupName }
            if (group == null) {
                Timber.w("代理组不存在: $groupName")
                return@withContext Result.failure(IllegalArgumentException("代理组不存在"))
            }

            if (group.type != Proxy.Type.Selector) {
                Timber.w("代理组类型不支持选择节点: $groupName (${group.type})")
                return@withContext Result.failure(IllegalArgumentException("只有 Selector 类型的代理组才能选择节点"))
            }

            val success = Clash.patchSelector(groupName, proxyName)

            if (success) {
                syncFromCore()
            }

            Result.success(success)
        } catch (e: Exception) {
            Timber.e(e, "选择代理节点异常: $groupName -> $proxyName")
            Result.failure(e)
        }
    }

    suspend fun testGroupDelay(groupName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Clash.healthCheck(groupName).await()

            delay(500)

            syncFromCore()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "测试代理组延迟失败: $groupName")
            Result.failure(e)
        }
    }

    suspend fun testAllDelay(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Clash.healthCheckAll()

            delay(1000)

            syncFromCore()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "测试所有代理组延迟失败")
            Result.failure(e)
        }
    }

    fun getCachedDelay(nodeName: String): Int? {
        return _proxyGroups.value
            .flatMap { it.proxies }
            .find { it.name == nodeName }
            ?.delay
    }

    fun findGroup(groupName: String): ProxyGroupInfo? {
        return _proxyGroups.value.find { it.name == groupName }
    }

    fun findProxy(nodeName: String): Proxy? {
        return _proxyGroups.value
            .flatMap { it.proxies }
            .find { it.name == nodeName }
    }

    fun getCurrentSelection(groupName: String): String? {
        return findGroup(groupName)?.now
    }

    fun isSelectableGroup(groupName: String): Boolean {
        val group = findGroup(groupName)
        return group?.type == Proxy.Type.Selector
    }

    fun close() {
        stop()
        scope.cancel("ProxyStateRepository closed")
    }
}
