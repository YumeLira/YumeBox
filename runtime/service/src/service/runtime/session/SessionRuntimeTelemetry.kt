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

package com.github.yumelira.yumebox.service.runtime.session

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.domain.ConnectionHistoryManager
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.service.root.RootTunJson
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class SessionRuntimeTelemetry(
    private val host: RuntimeHost,
    private val scope: CoroutineScope,
    private val onLogReadyChanged: (Boolean) -> Unit,
) {
    private var logJob: Job? = null
    private var connectionTrackingJob: Job? = null
    private val logSeq = AtomicLong(0L)
    private val recentLogs = ArrayDeque<Pair<Long, String>>()
    private var localLogObserver: ((LogMessage) -> Unit)? = null

    fun setLogObserver(observer: ((LogMessage) -> Unit)?) {
        localLogObserver = observer
    }

    fun queryRecentLogsJson(sinceSeq: Long): RuntimeLogChunk {
        synchronized(recentLogs) {
            val items = recentLogs.filter { it.first > sinceSeq }.map { it.second }
            return RuntimeLogChunk(nextSeq = logSeq.get(), items = items)
        }
    }

    fun isLogStreaming(): Boolean = logJob?.isActive == true

    fun startLogStream(subscribe: () -> ReceiveChannel<LogMessage>) {
        stopLogStream()
        host.onLogReady(false)
        logJob =
            scope.launch(Dispatchers.IO) {
                val receiver = subscribe()
                host.onLogReady(true)
                onLogReadyChanged(true)
                try {
                    while (isActive) {
                        val item = receiver.receive()
                        localLogObserver?.invoke(item)
                        host.onLogItem(item)
                        val encoded =
                            RootTunJson.Default.encodeToString(LogMessage.serializer(), item)
                        val seq = logSeq.incrementAndGet()
                        synchronized(recentLogs) {
                            recentLogs.addLast(seq to encoded)
                            while (recentLogs.size > MAX_BUFFERED_LOGS) {
                                recentLogs.removeFirst()
                            }
                        }
                    }
                } finally {
                    receiver.cancel()
                    host.onLogReady(false)
                    onLogReadyChanged(false)
                }
            }
    }

    fun stopLogStream() {
        logJob?.cancel()
        logJob = null
        synchronized(recentLogs) { recentLogs.clear() }
        host.onLogReady(false)
    }

    fun startConnectionTracking() {
        stopConnectionTracking()
        connectionTrackingJob =
            scope.launch(Dispatchers.IO) {
                PollingTimers.ticks(PollingTimerSpecs.SessionConnectionTracking).collect {
                    runCatching {
                        val snapshot = Clash.queryConnections()
                        ConnectionHistoryManager.updateConnections(snapshot.connections)
                    }
                }
            }
    }

    fun stopConnectionTracking() {
        connectionTrackingJob?.cancel()
        connectionTrackingJob = null
    }

    private companion object {
        private const val MAX_BUFFERED_LOGS = 256
    }
}
