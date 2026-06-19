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

package com.github.yumelira.yumebox.data.controller

import com.github.yumelira.yumebox.core.model.ConnectionInfo
import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.AppTrafficDeltaRecord
import com.github.yumelira.yumebox.data.model.ConnectionTrafficBaseline
import com.github.yumelira.yumebox.data.model.TrafficStatisticsBuckets
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import com.github.yumelira.yumebox.domain.model.TrafficData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class AppTrafficStatisticsCollector(
    private val isRunningFlow: Flow<Boolean>,
    private val currentProfileId: () -> String?,
    private val trafficStatisticsStore: TrafficStatisticsStore,
    private val appIdentityResolver: AppIdentityResolver,
    private val queryTrafficTotal: suspend () -> TrafficData,
    private val queryConnections: suspend () -> ConnectionSnapshot,
    private val queryActiveProfileId: suspend () -> String?,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private var collectionJob: Job? = null
    private var monitoringJob: Job? = null
    private val connectionBaselines = linkedMapOf<String, ConnectionTrafficBaseline>()
    private var lastTotalUpload = NO_BASELINE
    private var lastTotalDownload = NO_BASELINE
    private var lastProfileId: String? = null

    init {
        startCollection()
    }

    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            isRunningFlow.collectLatest { isRunning ->
                monitoringJob?.cancel()
                if (isRunning) {
                    monitoringJob = startTrafficMonitoring(this)
                } else {
                    resetBaselines()
                }
            }
        }
    }

    private fun startTrafficMonitoring(parentScope: CoroutineScope): Job {
        return parentScope.launch {
            lastTotalUpload = trafficStatisticsStore.getLastTrafficUpload()
            lastTotalDownload = trafficStatisticsStore.getLastTrafficDownload()
            lastProfileId = trafficStatisticsStore.getLastProfileId()
            connectionBaselines.clear()
            PollingTimers.ticks(PollingTimerSpecs.TrafficStatsCollection).collect {
                runCatching { collectTrafficData() }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.tag(TAG).e(error, "App traffic collection failed")
                    }
            }
        }
    }

    private suspend fun collectTrafficData() {
        val totalTraffic = queryTrafficTotal()
        val snapshot = queryConnections()
        val timestamp = System.currentTimeMillis()
        val currentProfileId =
            currentProfileId() ?: runCatching { queryActiveProfileId() }.getOrNull()

        if (lastTotalUpload < 0L || lastTotalDownload < 0L) {
            initializeTotals(
                totalTraffic = totalTraffic,
                profileId = currentProfileId,
                forcePersist = true,
            )
            bootstrapConnectionBaselines(snapshot.connections)
            return
        }

        if (currentProfileId != lastProfileId) {
            initializeTotals(
                totalTraffic = totalTraffic,
                profileId = currentProfileId,
                forcePersist = true,
            )
            bootstrapConnectionBaselines(snapshot.connections)
            return
        }

        if (connectionBaselines.isEmpty()) {
            recordUnattributedDeltaIfNeeded(
                timestamp = timestamp,
                currentUpload = totalTraffic.upload,
                currentDownload = totalTraffic.download,
            )
            initializeTotals(
                totalTraffic = totalTraffic,
                profileId = currentProfileId,
                forcePersist = true,
            )
            bootstrapConnectionBaselines(snapshot.connections)
            return
        }

        if (totalTraffic.upload < lastTotalUpload || totalTraffic.download < lastTotalDownload) {
            initializeTotals(
                totalTraffic = totalTraffic,
                profileId = currentProfileId,
                forcePersist = true,
            )
            bootstrapConnectionBaselines(snapshot.connections)
            return
        }

        val trafficDeltas = linkedMapOf<String, AppTrafficDeltaRecord>()
        val attributedTotals =
            collectConnectionDeltas(
                connections = snapshot.connections,
                trafficDeltas = trafficDeltas,
            )
        val totalUploadDelta = totalTraffic.upload - lastTotalUpload
        val totalDownloadDelta = totalTraffic.download - lastTotalDownload
        val unattributedUpload = (totalUploadDelta - attributedTotals.upload).coerceAtLeast(0L)
        val unattributedDownload =
            (totalDownloadDelta - attributedTotals.download).coerceAtLeast(0L)
        if (unattributedUpload > 0L || unattributedDownload > 0L) {
            trafficDeltas[TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY] =
                TrafficStatisticsBuckets.buildUnattributedRecord(
                    uploadDelta = unattributedUpload,
                    downloadDelta = unattributedDownload,
                )
        }

        if (trafficDeltas.isNotEmpty()) {
            trafficStatisticsStore.recordAppTrafficBatch(
                timestamp = timestamp,
                records = trafficDeltas.values.toList(),
            )
        }
        initializeTotals(
            totalTraffic = totalTraffic,
            profileId = currentProfileId,
            forcePersist = false,
        )
    }

    private fun collectConnectionDeltas(
        connections: List<ConnectionInfo>,
        trafficDeltas: MutableMap<String, AppTrafficDeltaRecord>,
    ): TrafficData {
        val activeIds = hashSetOf<String>()
        var attributedUpload = 0L
        var attributedDownload = 0L

        connections.forEach { connection ->
            activeIds += connection.id
            val (uploadDelta, downloadDelta) =
                collectConnectionDelta(connection = connection, trafficDeltas = trafficDeltas)
            attributedUpload += uploadDelta
            attributedDownload += downloadDelta
        }

        connectionBaselines.keys.retainAll(activeIds)
        return TrafficData(attributedUpload, attributedDownload)
    }

    private fun collectConnectionDelta(
        connection: ConnectionInfo,
        trafficDeltas: MutableMap<String, AppTrafficDeltaRecord>,
    ): TrafficData {
        val baseline = connectionBaselines[connection.id]
        val identity = resolveIdentity(connection, baseline)

        val updatedBaseline =
            ConnectionTrafficBaseline(
                id = connection.id,
                upload = connection.upload,
                download = connection.download,
                appKey = identity.appKey,
                packageName = identity.packageName,
                appName = identity.appName,
            )

        if (baseline == null) {
            connectionBaselines[connection.id] = updatedBaseline
            return TrafficData.ZERO
        }

        if (connection.upload < baseline.upload || connection.download < baseline.download) {
            connectionBaselines[connection.id] = updatedBaseline
            return TrafficData.ZERO
        }

        val uploadDelta = connection.upload - baseline.upload
        val downloadDelta = connection.download - baseline.download

        if (uploadDelta > 0L || downloadDelta > 0L) {
            val routeKey = resolveRouteKey(connection)
            val routeLabel = resolveRouteLabel(connection, routeKey)
            val trafficKey = buildTrafficBucketKey(identity.appKey, routeKey)
            val current = trafficDeltas[trafficKey]
            trafficDeltas[trafficKey] =
                AppTrafficDeltaRecord(
                    appKey = identity.appKey,
                    packageName = identity.packageName ?: current?.packageName,
                    appName = identity.appName.ifBlank { current?.appName.orEmpty() },
                    uploadDelta = (current?.uploadDelta ?: 0L) + uploadDelta,
                    downloadDelta = (current?.downloadDelta ?: 0L) + downloadDelta,
                    routeKey = routeKey ?: current?.routeKey,
                    routeLabel = routeLabel ?: current?.routeLabel,
                )
        }

        connectionBaselines[connection.id] = updatedBaseline
        return TrafficData(uploadDelta, downloadDelta)
    }

    private fun resolveIdentity(
        connection: ConnectionInfo,
        baseline: ConnectionTrafficBaseline?,
    ): AppIdentity {
        baseline?.let {
            return AppIdentity(
                appKey = it.appKey,
                packageName = it.packageName,
                appName = it.appName,
            )
        }
        return appIdentityResolver.resolve(connection.metadata)
    }

    private fun bootstrapConnectionBaselines(connections: List<ConnectionInfo>) {
        connectionBaselines.clear()
        connections.forEach { connection ->
            val identity = appIdentityResolver.resolve(connection.metadata)
            connectionBaselines[connection.id] =
                ConnectionTrafficBaseline(
                    id = connection.id,
                    upload = connection.upload,
                    download = connection.download,
                    appKey = identity.appKey,
                    packageName = identity.packageName,
                    appName = identity.appName,
                )
        }
    }

    private fun initializeTotals(
        totalTraffic: TrafficData,
        profileId: String?,
        forcePersist: Boolean,
    ) {
        lastTotalUpload = totalTraffic.upload
        lastTotalDownload = totalTraffic.download
        lastProfileId = profileId
        trafficStatisticsStore.setLastTraffic(
            upload = totalTraffic.upload,
            download = totalTraffic.download,
            profileId = profileId,
            forcePersist = forcePersist,
        )
    }

    private fun recordUnattributedDeltaIfNeeded(
        timestamp: Long,
        currentUpload: Long,
        currentDownload: Long,
    ) {
        if (currentUpload < lastTotalUpload || currentDownload < lastTotalDownload) {
            return
        }
        val uploadDelta = currentUpload - lastTotalUpload
        val downloadDelta = currentDownload - lastTotalDownload
        if (uploadDelta <= 0L && downloadDelta <= 0L) {
            return
        }
        trafficStatisticsStore.recordAppTrafficBatch(
            timestamp = timestamp,
            records =
                listOf(
                    TrafficStatisticsBuckets.buildUnattributedRecord(
                        uploadDelta = uploadDelta,
                        downloadDelta = downloadDelta,
                    )
                ),
        )
    }

    private fun resetBaselines() {
        trafficStatisticsStore.flushNow()
        connectionBaselines.clear()
        lastTotalUpload = NO_BASELINE
        lastTotalDownload = NO_BASELINE
        lastProfileId = null
    }

    private fun buildTrafficBucketKey(appKey: String, routeKey: String?): String {
        return if (routeKey.isNullOrBlank()) appKey else "$appKey::$routeKey"
    }

    private fun resolveRouteKey(connection: ConnectionInfo): String {
        return connection.chains.lastOrNull()?.takeIf(String::isNotBlank)
            ?: connection.providerChains.lastOrNull()?.takeIf(String::isNotBlank)
            ?: connection.rulePayload.takeIf(String::isNotBlank)
            ?: connection.rule.takeIf(String::isNotBlank)
            ?: TrafficStatisticsBuckets.UNATTRIBUTED_ROUTE_KEY
    }

    private fun resolveRouteLabel(connection: ConnectionInfo, routeKey: String?): String {
        return connection.chains.lastOrNull()?.takeIf(String::isNotBlank)
            ?: connection.providerChains.lastOrNull()?.takeIf(String::isNotBlank)
            ?: routeKey
            ?: TrafficStatisticsBuckets.UNATTRIBUTED_ROUTE_NAME
    }

    fun stop() {
        trafficStatisticsStore.flushNow()
        monitoringJob?.cancel()
        monitoringJob = null
        collectionJob?.cancel()
        collectionJob = null
        connectionBaselines.clear()
    }

    companion object {
        private const val TAG = "AppTrafficStatsCollector"
        private const val NO_BASELINE = -1L
    }
}
