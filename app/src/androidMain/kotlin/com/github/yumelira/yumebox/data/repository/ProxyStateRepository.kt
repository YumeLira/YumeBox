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

/**
 * 代理状态仓库
 *
 * 职责：
 * 1. 从核心同步代理组状态（按需同步，不自动轮询）
 * 2. 提供代理组状态的观察接口
 * 3. 处理节点选择（仅 Selector 类型）
 * 4. 处理延迟测试
 */
class ProxyStateRepository(
    private val context: Context,
    private val proxyChainResolver: ProxyChainResolver
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 代理组状态流
    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()

    // 同步状态
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // 是否已启动
    private val isActive = AtomicBoolean(false)

    // 自动同步任务
    private var autoSyncJob: Job? = null

    /**
     * 启动仓库并开始自动同步
     *
     * @param intervalMs 自动同步间隔（毫秒），默认 5 秒
     */
    fun start(intervalMs: Long = 5000L) {
        if (isActive.getAndSet(true)) {
            return
        }

        // 启动自动同步
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

    /**
     * 停止仓库
     */
    fun stop() {
        if (!isActive.getAndSet(false)) {
            return
        }

        // 停止自动同步
        autoSyncJob?.cancel()
        autoSyncJob = null

        // 清空状态
        _proxyGroups.value = emptyList()
        _isSyncing.value = false
    }

    /**
     * 从核心同步代理组状态
     *
     * 这是唯一获取代理组数据的方法
     */
    suspend fun syncFromCore(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isActive) {
            return@withContext Result.failure(IllegalStateException("仓库未启动"))
        }

        try {
            _isSyncing.value = true

            // 从核心查询所有代理组名称
            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)

            if (groupNames.isEmpty()) {
                _proxyGroups.value = emptyList()
                return@withContext Result.success(Unit)
            }

            // 并发查询所有代理组详情
            val groups = groupNames.map { name ->
                async {
                    try {
                        val group = Clash.queryGroup(name, ProxySort.Default)
                        ProxyGroupInfo(
                            name = name,
                            type = group.type,
                            proxies = group.proxies,
                            now = group.now // 当前选中的节点
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "查询代理组失败: $name")
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            // 为所有代理组计算代理链路径
            val groupsWithChain = groups.map { group ->
                val chainPath = if (group.now.isNotBlank()) {
                    proxyChainResolver.buildChainPath(group.now, groups)
                } else {
                    emptyList()
                }
                group.copy(chainPath = chainPath)
            }

            // 更新状态
            _proxyGroups.value = groupsWithChain

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "同步代理组状态失败")
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * 选择代理节点
     *
     * 注意：只有 Selector 类型的代理组才能选择节点
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @return 是否成功
     */
    suspend fun selectProxy(groupName: String, proxyName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 检查代理组类型
            val group = _proxyGroups.value.find { it.name == groupName }
            if (group == null) {
                Timber.w("代理组不存在: $groupName")
                return@withContext Result.failure(IllegalArgumentException("代理组不存在"))
            }

            if (group.type != Proxy.Type.Selector) {
                Timber.w("代理组类型不支持选择节点: $groupName (${group.type})")
                return@withContext Result.failure(IllegalArgumentException("只有 Selector 类型的代理组才能选择节点"))
            }

            // 调用核心 API 设置节点
            val success = Clash.patchSelector(groupName, proxyName)

            if (success) {
                // 立即同步状态，更新当前选中的节点
                syncFromCore()
            }

            Result.success(success)
        } catch (e: Exception) {
            Timber.e(e, "选择代理节点异常: $groupName -> $proxyName")
            Result.failure(e)
        }
    }

    /**
     * 测试代理组延迟
     *
     * @param groupName 代理组名称
     */
    suspend fun testGroupDelay(groupName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 触发健康检查
            Clash.healthCheck(groupName).await()

            // 等待测试完成
            delay(500)

            // 同步最新状态（获取更新后的延迟值）
            syncFromCore()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "测试代理组延迟失败: $groupName")
            Result.failure(e)
        }
    }

    /**
     * 测试所有代理组延迟
     */
    suspend fun testAllDelay(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 触发全部健康检查
            Clash.healthCheckAll()

            // 等待测试完成（所有组测试需要更长时间）
            delay(1000)

            // 同步最新状态（获取更新后的延迟值）
            syncFromCore()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "测试所有代理组延迟失败")
            Result.failure(e)
        }
    }

    /**
     * 获取节点延迟
     *
     * @param nodeName 节点名称
     * @return 延迟值，null 表示未找到
     */
    fun getCachedDelay(nodeName: String): Int? {
        return _proxyGroups.value
            .flatMap { it.proxies }
            .find { it.name == nodeName }
            ?.delay
    }

    /**
     * 查找代理组
     *
     * @param groupName 代理组名称
     * @return 代理组信息，null 表示未找到
     */
    fun findGroup(groupName: String): ProxyGroupInfo? {
        return _proxyGroups.value.find { it.name == groupName }
    }

    /**
     * 查找代理节点
     *
     * @param nodeName 节点名称
     * @return 代理节点信息，null 表示未找到
     */
    fun findProxy(nodeName: String): Proxy? {
        return _proxyGroups.value
            .flatMap { it.proxies }
            .find { it.name == nodeName }
    }

    /**
     * 获取当前选中的节点
     *
     * @param groupName 代理组名称
     * @return 当前选中的节点名称
     */
    fun getCurrentSelection(groupName: String): String? {
        return findGroup(groupName)?.now
    }

    /**
     * 检查代理组是否可选择节点
     *
     * @param groupName 代理组名称
     * @return 是否可选择
     */
    fun isSelectableGroup(groupName: String): Boolean {
        val group = findGroup(groupName)
        return group?.type == Proxy.Type.Selector
    }

    /**
     * 清理资源
     */
    fun close() {
        stop()
        scope.cancel("ProxyStateRepository closed")
    }
}
