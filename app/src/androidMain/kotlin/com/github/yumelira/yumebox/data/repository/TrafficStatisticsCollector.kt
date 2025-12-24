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

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import kotlinx.coroutines.*
import timber.log.Timber

class TrafficStatisticsCollector(
    private val clashManager: ClashManager,
    private val trafficStatisticsStore: TrafficStatisticsStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "TrafficStatisticsCollector"
        private const val COLLECTION_INTERVAL_MS = 5000L
    }

    private var collectionJob: Job? = null
    private var lastTotalUpload: Long = 0L
    private var lastTotalDownload: Long = 0L
    private var lastProfileId: String? = null

    init {
        startCollection()
    }

    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            clashManager.isRunning.collect { isRunning ->
                if (isRunning) {
                    startTrafficMonitoring()
                } else {
                    resetLastValues()
                }
            }
        }
    }

    private fun CoroutineScope.startTrafficMonitoring() {
        launch {
            lastTotalUpload = trafficStatisticsStore.getLastTrafficUpload()
            lastTotalDownload = trafficStatisticsStore.getLastTrafficDownload()
            lastProfileId = trafficStatisticsStore.getLastProfileId()

            while (isActive && clashManager.isRunning.value) {
                runCatching {
                    collectTrafficData()
                    delay(COLLECTION_INTERVAL_MS)
                }.onFailure { e ->
                    if (e is CancellationException) throw e
                    Timber.tag("TrafficStatisticsCollec").e(e, "流量数据收集失败")
                    delay(COLLECTION_INTERVAL_MS)
                }
            }
        }
    }

    private fun collectTrafficData() {
        val trafficTotal = clashManager.trafficTotal.value
        val currentUpload = trafficTotal.upload
        val currentDownload = trafficTotal.download
        val currentProfile = clashManager.currentProfile.value
        val currentProfileId = currentProfile?.id
        val currentProfileName = currentProfile?.name

        if (lastTotalUpload == 0L && lastTotalDownload == 0L) {
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            lastProfileId = currentProfileId
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        if (currentProfileId != lastProfileId) {
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            lastProfileId = currentProfileId
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        if (currentUpload < lastTotalUpload || currentDownload < lastTotalDownload) {
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        val uploadDelta = currentUpload - lastTotalUpload
        val downloadDelta = currentDownload - lastTotalDownload

        if (uploadDelta > 0 || downloadDelta > 0) {
            trafficStatisticsStore.recordTraffic(
                uploadDelta,
                downloadDelta,
                currentProfileId,
                currentProfileName
            )
            Timber.tag("TrafficStatisticsCollec")
                .v("记录流量: 上传=$uploadDelta, 下载=$downloadDelta, 配置=$currentProfileName")
        }

        lastTotalUpload = currentUpload
        lastTotalDownload = currentDownload
        trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
    }

    private fun resetLastValues() {
        lastTotalUpload = 0L
        lastTotalDownload = 0L
        lastProfileId = null
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }
}
