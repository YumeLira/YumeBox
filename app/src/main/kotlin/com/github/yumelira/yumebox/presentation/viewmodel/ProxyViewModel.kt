package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.repository.OverrideRepository
import com.github.yumelira.yumebox.data.repository.ProxyDisplaySettingsRepository
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupOpenMode
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
        const val PROXY_TESTING_SORT_HOLD_MS = 2200L
    }

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private val _testingGroupNames = MutableStateFlow<Set<String>>(emptySet())
    val testingGroupNames: StateFlow<Set<String>> = _testingGroupNames.asStateFlow()

    val currentMode: StateFlow<TunnelState.Mode> = proxyDisplaySettingsRepository.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TunnelState.Mode.Rule)

    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsRepository.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.SINGLE_DETAILED)

    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsRepository.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)

    val groupOpenMode: StateFlow<ProxyGroupOpenMode> = proxyDisplaySettingsRepository.groupOpenMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyGroupOpenMode.FULL_SCREEN)

    val sheetHeightFraction: StateFlow<Float> = proxyDisplaySettingsRepository.sheetHeightFraction.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PROXY_SHEET_HEIGHT_FRACTION_DEFAULT)

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyFacade.proxyGroups

    private var screenActive = false
    private var externalSelectionSyncJob: Job? = null

    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        combine(proxyGroups, sortMode, testingGroupNames) { groups, mode, testingNames ->
            if (mode == ProxySortMode.DEFAULT) {
                groups
            } else {
                groups.map { group ->
                    val shouldFreezeLatencySort =
                        mode == ProxySortMode.BY_LATENCY && testingNames.contains(group.name)
                    group.copy(
                        proxies = if (shouldFreezeLatencySort) {
                            group.proxies
                        } else {
                            sortProxies(group.proxies, mode)
                        }
                    )
                }
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

    fun setGroupOpenMode(mode: ProxyGroupOpenMode) {
        proxyDisplaySettingsRepository.groupOpenMode.set(mode)
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

    private fun sortProxies(proxies: List<Proxy>, sortMode: ProxySortMode): List<Proxy> = when (sortMode) {
        ProxySortMode.DEFAULT -> proxies
        ProxySortMode.BY_NAME -> proxies.sortedBy { it.name }
        ProxySortMode.BY_LATENCY -> {
            val originalIndex = proxies.withIndex().associate { (index, proxy) -> proxy.name to index }
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
                val delayMillis = if (_testingGroupNames.value.isNotEmpty()) {
                    PROXY_REFRESH_TESTING_MS
                } else {
                    PROXY_REFRESH_IDLE_MS
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
