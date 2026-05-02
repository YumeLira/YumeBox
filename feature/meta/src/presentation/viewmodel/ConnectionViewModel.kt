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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.domain.ConnectionHistoryManager
import com.github.yumelira.yumebox.core.model.ConnectionInfo
import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.remote.ServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

enum class ConnectionSort {
    Time,
    Upload,
    Download,
    Host,
}

enum class ConnectionTab {
    ACTIVE,
    CLOSED,
}

data class ConnectionState(
    val snapshot: ConnectionSnapshot? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val sortBy: ConnectionSort = ConnectionSort.Time,
    val selectedTab: ConnectionTab = ConnectionTab.ACTIVE,
    val error: String? = null,
) {
    val totalConnections: Int get() = snapshot?.connections?.size ?: 0
}

class ConnectionViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    val filteredConnections: StateFlow<List<ConnectionInfo>> = state
        .map(::buildFilteredConnections)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private var pollingJob: Job? = null
    private var _isPolling = false

    fun startPolling() {
        if (_isPolling) return
        _isPolling = true

        pollingJob = viewModelScope.launch {
            refreshConnections(showRefreshing = false)
            PollingTimers.ticks(PollingTimerSpecs.ConnectionsPolling).collect {
                try {
                    refreshConnections(showRefreshing = true)
                } catch (error: Exception) {
                    Timber.w(error, "Failed to poll connections")
                    _state.update { it.copy(error = error.message, isRefreshing = false) }
                }
            }
        }
    }

    fun stopPolling() {
        _isPolling = false
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setSortBy(sort: ConnectionSort) {
        _state.update { it.copy(sortBy = sort) }
    }

    fun setTab(tab: ConnectionTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    suspend fun closeConnection(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                ServiceClient.clash().closeConnection(id)
            }.onFailure { error ->
                Timber.w(error, "Failed to close connection: %s", id)
                _state.update { it.copy(error = error.message) }
            }.getOrDefault(false)
        }.also {
            refreshConnections(showRefreshing = true)
        }
    }

    private suspend fun refreshConnections(showRefreshing: Boolean) {
        if (showRefreshing) {
            _state.update { current ->
                current.copy(isRefreshing = true)
            }
        }
        withContext(Dispatchers.IO) {
            try {
                val snapshot = ServiceClient.clash().queryConnections()
                ConnectionHistoryManager.updateConnections(snapshot.connections)
                _state.update {
                    it.copy(
                        snapshot = snapshot,
                        error = null,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            } catch (error: Exception) {
                Timber.w(error, "Failed to query connections")
                _state.update {
                    it.copy(
                        error = error.message,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    private fun buildFilteredConnections(currentState: ConnectionState): List<ConnectionInfo> {
        val connections = when (currentState.selectedTab) {
            ConnectionTab.ACTIVE -> currentState.snapshot?.connections ?: emptyList()
            ConnectionTab.CLOSED -> ConnectionHistoryManager.getClosedConnections()
        }

        val filtered = if (currentState.searchQuery.isEmpty()) {
            connections
        } else {
            val query = currentState.searchQuery.lowercase()
            connections.filter { conn ->
                val host = connectionDisplayTarget(conn).lowercase()
                val process = conn.metadata["process"]?.jsonPrimitive?.content?.lowercase() ?: ""
                val chains = conn.chains.joinToString(" ").lowercase()
                val rule = conn.rule.lowercase()

                host.contains(query) ||
                    process.contains(query) ||
                    chains.contains(query) ||
                    rule.contains(query)
            }
        }

        val sorted = if (currentState.selectedTab == ConnectionTab.ACTIVE) {
            when (currentState.sortBy) {
                ConnectionSort.Time -> filtered.sortedByDescending { it.start }
                ConnectionSort.Upload -> filtered.sortedByDescending { it.upload }
                ConnectionSort.Download -> filtered.sortedByDescending { it.download }
                ConnectionSort.Host -> filtered.sortedBy { connectionDisplayTarget(it) }
            }
        } else {
            filtered
        }
        return sorted
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

private fun connectionDisplayTarget(connection: ConnectionInfo): String {
    val metadata = connection.metadata
    val host = metadata["host"]?.jsonPrimitive?.content.orEmpty()
    val destinationIp = metadata["destinationIP"]?.jsonPrimitive?.content.orEmpty()
    val destinationPort = metadata["destinationPort"]?.jsonPrimitive?.content.orEmpty()
    val sourceIp = metadata["sourceIP"]?.jsonPrimitive?.content.orEmpty()
    val sourcePort = metadata["sourcePort"]?.jsonPrimitive?.content.orEmpty()
    return when {
        host.isNotBlank() && destinationPort.isNotBlank() -> "$host:$destinationPort"
        host.isNotBlank() -> host
        destinationIp.isNotBlank() && destinationPort.isNotBlank() -> "$destinationIp:$destinationPort"
        destinationIp.isNotBlank() -> destinationIp
        sourceIp.isNotBlank() && sourcePort.isNotBlank() -> "$sourceIp:$sourcePort"
        else -> sourceIp
    }
}
