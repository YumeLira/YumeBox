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

package com.github.yumelira.yumebox.domain.facade

import android.content.Intent
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.Selection
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.repository.SelectionDao
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxyState
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * 代理外观
 *
 * 为 UI 层提供统一的代理操作接口
 *
 * 重构说明：
 * - 代理组状态完全由 ProxyStateRepository 管理
 * - 移除了手动刷新接口
 * - 所有操作都是"设置"而非"刷新"
 * - UI 只需观察状态流即可
 */
class ProxyFacade(
    private val clashManager: ClashManager,
    private val proxyConnectionService: ProxyConnectionService,
    private val selectionDao: SelectionDao
) {
    // ========== 状态流 ==========

    /**
     * 代理状态
     */
    val proxyState: StateFlow<ProxyState> = clashManager.proxyState

    /**
     * 是否正在运行
     */
    val isRunning: StateFlow<Boolean> = clashManager.isRunning

    /**
     * 当前配置
     */
    val currentProfile: StateFlow<Profile?> = clashManager.currentProfile

    /**
     * 实时流量
     */
    val trafficNow: StateFlow<TrafficData> = clashManager.trafficNow

    /**
     * 总流量
     */
    val trafficTotal: StateFlow<TrafficData> = clashManager.trafficTotal

    /**
     * 隧道状态
     */
    val tunnelState: StateFlow<TunnelState?> = clashManager.tunnelState

    /**
     * 代理组信息（自动同步，无需手动刷新）
     */
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyStateRepository.proxyGroups

    /**
     * 是否正在同步
     */
    val isSyncing: StateFlow<Boolean> = clashManager.proxyStateRepository.isSyncing

    /**
     * 运行模式
     */
    val runningMode: StateFlow<RunningMode> = clashManager.runningMode

    /**
     * 日志流
     */
    val logs = clashManager.logs

    // ========== 代理控制 ==========

    /**
     * 启动代理
     *
     * @param profileId 配置 ID
     * @param forceTunMode 是否强制 TUN 模式
     * @return 启动结果，成功时可能返回需要的 Intent（VPN 权限请求）
     */
    suspend fun startProxy(profileId: String, forceTunMode: Boolean? = null): Result<Intent?> {
        return proxyConnectionService.prepareAndStart(profileId, forceTunMode)
    }

    /**
     * 停止代理
     */
    fun stopProxy() {
        proxyConnectionService.stop(runningMode.value)
    }

    // ========== 代理节点操作 ==========

    /**
     * 选择代理节点
     *
     * 注意：选择后状态会自动同步，无需手动刷新
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @return 是否成功
     */
    suspend fun selectProxy(groupName: String, proxyName: String): Result<Boolean> {
        val result = clashManager.proxyStateRepository.selectProxy(groupName, proxyName)

        if (result.isSuccess && result.getOrNull() == true) {
            // 保存选择到数据库
            val profile = currentProfile.value
            if (profile != null) {
                try {
                    selectionDao.setSelected(Selection(profile.id, groupName, proxyName))
                } catch (e: Exception) {
                    Timber.e(e, "保存节点选择失败")
                }
            }
        }

        return result
    }

    /**
     * 测试代理组延迟
     *
     * 注意：测试完成后状态会自动同步，无需手动刷新
     *
     * @param groupName 代理组名称
     * @return 测试结果
     */
    suspend fun testDelay(groupName: String): Result<Unit> {
        return clashManager.proxyStateRepository.testGroupDelay(groupName)
    }

    /**
     * 测试所有代理组延迟
     *
     * 注意：测试完成后状态会自动同步，无需手动刷新
     *
     * @return 测试结果
     */
    suspend fun testAllDelay(): Result<Unit> {
        return clashManager.proxyStateRepository.testAllDelay()
    }

    /**
     * 获取节点延迟（从缓存）
     *
     * @param nodeName 节点名称
     * @return 延迟值，null 表示未找到
     */
    fun getCachedDelay(nodeName: String): Int? {
        return clashManager.proxyStateRepository.getCachedDelay(nodeName)
    }

    // ========== 代理组查询 ==========

    /**
     * 查找代理组
     *
     * @param groupName 代理组名称
     * @return 代理组信息，null 表示未找到
     */
    fun findGroup(groupName: String): ProxyGroupInfo? {
        return clashManager.proxyStateRepository.findGroup(groupName)
    }

    /**
     * 获取当前选中的节点
     *
     * @param groupName 代理组名称
     * @return 当前选中的节点名称
     */
    fun getCurrentSelection(groupName: String): String? {
        return clashManager.proxyStateRepository.getCurrentSelection(groupName)
    }

    /**
     * 检查代理组是否可选择节点
     *
     * @param groupName 代理组名称
     * @return 是否可选择（仅 Selector 类型）
     */
    fun isSelectableGroup(groupName: String): Boolean {
        return clashManager.proxyStateRepository.isSelectableGroup(groupName)
    }

}
