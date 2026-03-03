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

package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.repository.OverrideRepository
import com.github.yumelira.yumebox.data.repository.ProxyDisplaySettingsRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import com.github.yumelira.yumebox.domain.model.PROXY_SHEET_HEIGHT_FRACTION_DEFAULT
import com.github.yumelira.yumebox.domain.model.normalizeProxySheetHeightFraction
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProxyViewModel(
    private val overrideRepository: OverrideRepository,
    private val proxyFacade: ProxyFacade,
    private val proxyDisplaySettingsRepository: ProxyDisplaySettingsRepository,
) : ViewModel() {
    private companion object {
        const val PROXY_REFRESH_IDLE_MS = 1500L
        const val PROXY_REFRESH_TESTING_MS = 400L
        const val PROXY_REFRESH_PREVIEW_MS = 10_000L
        const val PROXY_TESTING_SORT_HOLD_MS = 2200L
    }

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private val _testingGroupNames = MutableStateFlow<Set<String>>(emptySet())
    val testingGroupNames: StateFlow<Set<String>> = _testingGroupNames.asStateFlow()
    private val _groupOriginalOrder = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val currentMode: StateFlow<TunnelState.Mode> = proxyDisplaySettingsRepository.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TunnelState.Mode.Rule)

    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsRepository.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.SINGLE_DETAILED)

    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsRepository.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)

    val sheetHeightFraction: StateFlow<Float> = proxyDisplaySettingsRepository.sheetHeightFraction.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PROXY_SHEET_HEIGHT_FRACTION_DEFAULT)

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyFacade.proxyGroups

    private var screenActive = false
    private var externalSelectionSyncJob: Job? = null

    init {
        viewModelScope.launch {
            proxyGroups.collect { groups ->
                _groupOriginalOrder.update { current ->
                    updateGroupOrderCache(current, groups)
                }
            }
        }
    }

    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        combine(
            proxyGroups,
            sortMode,
            _groupOriginalOrder,
        ) { groups, mode, originalOrderCache ->
            groups.map { group ->
                val originalOrder = originalOrderCache[group.name].orEmpty()
                group.copy(
                    proxies = sortProxies(
                        proxies = group.proxies,
                        sortMode = mode,
                        originalOrder = originalOrder,
                    )
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun ensureCoreLoaded(isActive: Boolean) {
        if (screenActive == isActive) return
        screenActive = isActive
        if (isActive) {
            startExternalSelectionSync()
        } else {
            stopExternalSelectionSync()
        }
    }

    fun patchMode(mode: TunnelState.Mode) {
        val previousMode = proxyDisplaySettingsRepository.proxyMode.value
        proxyDisplaySettingsRepository.proxyMode.set(mode)
        viewModelScope.launch {
            val persistError = overrideRepository.updatePersist { it.copy(mode = mode) }.exceptionOrNull()
            if (persistError != null) {
                proxyDisplaySettingsRepository.proxyMode.set(previousMode)
                showError(MLang.Proxy.Mode.SwitchFailed.format(persistError.message))
                return@launch
            }

            val sessionError = overrideRepository.updateSession { it.copy(mode = mode) }.exceptionOrNull()
            if (sessionError != null) {
                proxyDisplaySettingsRepository.proxyMode.set(previousMode)
                showError(MLang.Proxy.Mode.SwitchFailed.format(sessionError.message))
                return@launch
            }

            val reloadError = proxyFacade.reloadCurrentProfile().exceptionOrNull()
            if (reloadError != null) {
                proxyDisplaySettingsRepository.proxyMode.set(previousMode)
                showError(MLang.Proxy.Mode.SwitchFailed.format(reloadError.message))
                return@launch
            }

            delay(500)
            showMessage(MLang.Proxy.Mode.Switched.format(mode.toModeName()))
        }
    }

    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            setLoading(true)
            clearError()
            val currentGroups = proxyGroups.value
            val testingTargets: Set<String> = if (groupName != null) {
                setOf(groupName)
            } else {
                currentGroups.mapTo(linkedSetOf()) { it.name }
            }
            if (testingTargets.isNotEmpty()) {
                _testingGroupNames.update { it + testingTargets }
            }

            val result = runCatching {
                if (groupName != null) {
                    showMessage(MLang.Proxy.Testing.Group.format(groupName))
                    proxyFacade.healthCheck(groupName, refreshAfter = false)
                    showMessage(MLang.Proxy.Testing.RequestSent)
                } else {
                    showMessage(MLang.Proxy.Testing.All)
                    var firstError: Throwable? = null
                    currentGroups.forEach { group ->
                        runCatching {
                            proxyFacade.healthCheck(group.name, refreshAfter = false)
                        }.onFailure { error ->
                            if (firstError == null) {
                                firstError = error
                            }
                        }
                    }
                    firstError?.let { throw it }
                }
            }

            if (testingTargets.isNotEmpty()) {
                delay(PROXY_TESTING_SORT_HOLD_MS)
                _testingGroupNames.update { it - testingTargets }
            }
            setLoading(false)

            result.exceptionOrNull()?.let { error ->
                showError(MLang.Proxy.Testing.Failed.format(error.message))
            }
        }
    }

    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyDisplaySettingsRepository.displayMode.set(mode)
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsRepository.sortMode.set(mode)
    }

    fun setSheetHeightFraction(value: Float) {
        proxyDisplaySettingsRepository.sheetHeightFraction.set(normalizeProxySheetHeightFraction(value))
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            runCatching {
                val success = proxyFacade.selectProxy(groupName, proxyName)
                if (success) {
                    showMessage(MLang.Proxy.Selection.Switched.format(proxyName))
                } else {
                    showError(MLang.Proxy.Selection.Failed)
                }
            }.onFailure { error ->
                showError(MLang.Proxy.Selection.Error.format(error.message))
            }
        }
    }

    private fun sortProxies(
        proxies: List<Proxy>,
        sortMode: ProxySortMode,
        originalOrder: List<String>,
    ): List<Proxy> = when (sortMode) {
        ProxySortMode.DEFAULT -> reorderByNameSequence(proxies, originalOrder)
        ProxySortMode.BY_NAME -> {
            val originalIndex = originalOrder.withIndex().associate { (index, name) -> name to index }
            proxies.sortedWith(
                compareBy<Proxy>(
                    { it.name.lowercase() },
                    { originalIndex[it.name] ?: Int.MAX_VALUE },
                )
            )
        }
        ProxySortMode.BY_LATENCY -> {
            val originalIndex = originalOrder.withIndex().associate { (index, name) -> name to index }
            proxies.sortedWith(
                compareBy<Proxy>(
                    { proxy ->
                        when {
                            proxy.delay > 0 -> proxy.delay
                            proxy.delay < 0 -> Int.MAX_VALUE - 1
                            else -> Int.MAX_VALUE
                        }
                    },
                    { proxy -> originalIndex[proxy.name] ?: Int.MAX_VALUE }
                )
            )
        }
    }

    private fun reorderByNameSequence(
        proxies: List<Proxy>,
        orderedNames: List<String>,
    ): List<Proxy> {
        if (proxies.isEmpty()) return proxies
        if (orderedNames.isEmpty()) return proxies

        val proxyByName = proxies.associateBy { it.name }
        val consumed = HashSet<String>(proxies.size)
        val reordered = ArrayList<Proxy>(proxies.size)

        orderedNames.forEach { name ->
            val proxy = proxyByName[name] ?: return@forEach
            reordered += proxy
            consumed += name
        }
        proxies.forEach { proxy ->
            if (consumed.add(proxy.name)) reordered += proxy
        }
        return reordered
    }

    private fun updateGroupOrderCache(
        current: Map<String, List<String>>,
        groups: List<ProxyGroupInfo>,
    ): Map<String, List<String>> {
        val next = current.toMutableMap()
        var changed = false
        val activeGroupNames = groups.mapTo(HashSet(groups.size)) { it.name }

        if (next.keys.removeAll { it !in activeGroupNames }) {
            changed = true
        }

        groups.forEach { group ->
            val latestNames = group.proxies.map { it.name }
            val previous = next[group.name]
            val merged = if (previous == null) {
                latestNames
            } else {
                mergeStableOrder(previous, latestNames)
            }
            if (previous != merged) {
                next[group.name] = merged
                changed = true
            }
        }

        return if (changed) next else current
    }

    private fun mergeStableOrder(
        previousOrder: List<String>,
        latestNames: List<String>,
    ): List<String> {
        if (previousOrder.isEmpty()) return latestNames
        if (latestNames.isEmpty()) return emptyList()

        val latestSet = latestNames.toHashSet()
        val merged = ArrayList<String>(latestNames.size)
        previousOrder.forEach { name ->
            if (name in latestSet) merged += name
        }
        latestNames.forEach { name ->
            if (name !in merged) merged += name
        }
        return merged
    }

    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun showError(error: String) {
        _uiState.update { it.copy(error = error) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun startExternalSelectionSync() {
        if (externalSelectionSyncJob?.isActive == true) return
        externalSelectionSyncJob = viewModelScope.launch {
            while (true) {
                runCatching { proxyFacade.refreshProxyGroups() }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                    }
                val delayMillis = when {
                    !proxyFacade.isRunning.value -> PROXY_REFRESH_PREVIEW_MS
                    _testingGroupNames.value.isNotEmpty() -> PROXY_REFRESH_TESTING_MS
                    else -> PROXY_REFRESH_IDLE_MS
                }
                delay(delayMillis)
            }
        }
    }

    private fun stopExternalSelectionSync() {
        externalSelectionSyncJob?.cancel()
        externalSelectionSyncJob = null
    }

    override fun onCleared() {
        stopExternalSelectionSync()
        super.onCleared()
    }

    private fun TunnelState.Mode.toModeName(): String = when (this) {
        TunnelState.Mode.Direct -> MLang.Proxy.Mode.Direct
        TunnelState.Mode.Global -> MLang.Proxy.Mode.Global
        TunnelState.Mode.Rule -> MLang.Proxy.Mode.Rule
        else -> MLang.Proxy.Mode.Unknown
    }

    data class ProxyUiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}
