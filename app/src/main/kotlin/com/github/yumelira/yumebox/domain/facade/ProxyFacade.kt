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

class ProxyFacade(
    private val clashManager: ClashManager,
    private val proxyConnectionService: ProxyConnectionService,
    private val selectionDao: SelectionDao
) {

    val proxyState: StateFlow<ProxyState> = clashManager.proxyState

    val isRunning: StateFlow<Boolean> = clashManager.isRunning

    val currentProfile: StateFlow<Profile?> = clashManager.currentProfile

    val trafficNow: StateFlow<TrafficData> = clashManager.trafficNow

    val trafficTotal: StateFlow<TrafficData> = clashManager.trafficTotal

    val tunnelState: StateFlow<TunnelState?> = clashManager.tunnelState

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyStateRepository.proxyGroups

    val isSyncing: StateFlow<Boolean> = clashManager.proxyStateRepository.isSyncing

    val runningMode: StateFlow<RunningMode> = clashManager.runningMode

    val logs = clashManager.logs

    suspend fun startProxy(profileId: String, forceTunMode: Boolean? = null): Result<Intent?> {
        return proxyConnectionService.prepareAndStart(profileId, forceTunMode)
    }

    fun stopProxy() {
        proxyConnectionService.stop(runningMode.value)
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Result<Boolean> {
        val result = clashManager.proxyStateRepository.selectProxy(groupName, proxyName)

        if (result.isSuccess && result.getOrNull() == true) {
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

    suspend fun testDelay(groupName: String): Result<Unit> {
        return clashManager.proxyStateRepository.testGroupDelay(groupName)
    }

    suspend fun testAllDelay(): Result<Unit> {
        return clashManager.proxyStateRepository.testAllDelay()
    }

    fun getCachedDelay(nodeName: String): Int? {
        return clashManager.proxyStateRepository.getCachedDelay(nodeName)
    }

    fun findGroup(groupName: String): ProxyGroupInfo? {
        return clashManager.proxyStateRepository.findGroup(groupName)
    }

    fun getCurrentSelection(groupName: String): String? {
        return clashManager.proxyStateRepository.getCurrentSelection(groupName)
    }

    fun isSelectableGroup(groupName: String): Boolean {
        return clashManager.proxyStateRepository.isSelectableGroup(groupName)
    }

}
