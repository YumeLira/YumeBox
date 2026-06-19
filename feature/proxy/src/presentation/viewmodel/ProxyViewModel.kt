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

package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.presentation.ContractStateViewModel
import com.github.yumelira.yumebox.core.presentation.LoadableState
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.controller.RuntimeOverrideController
import com.github.yumelira.yumebox.data.model.ProxySortMode
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.ProxyGroupSyncPriority
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProxyViewModel(
    private val runtimeOverrideController: RuntimeOverrideController,
    private val proxyFacade: ProxyFacade,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore,
    appSettings: AppSettingsStore,
) :
    ContractStateViewModel<ProxyViewModel.ProxyUiState, ProxyViewModel.ProxyUiEffect>(
        ProxyUiState()
    ) {
    private val _testingGroupNames = MutableStateFlow<Set<String>>(emptySet())
    val testingGroupNames: StateFlow<Set<String>> = _testingGroupNames.asStateFlow()

    private val _testingProxyNames = MutableStateFlow<Set<String>>(emptySet())
    val testingProxyNames: StateFlow<Set<String>> = _testingProxyNames.asStateFlow()

    private val groupSorter = ProxyGroupSorter()

    val sortMode: StateFlow<ProxySortMode> =
        proxyDisplaySettingsStore.sortMode.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ProxySortMode.DEFAULT,
        )

    val singleNodeTest: StateFlow<Boolean> =
        appSettings.singleNodeTest.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true,
        )

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> =
        proxyFacade.proxyGroups
            .map { groups -> groups.filterNot(ProxyGroupInfo::hidden) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeSyncSources = mutableSetOf<String>()

    init {
        proxyFacade.warmUpProxyGroups()
        viewModelScope.launch {
            proxyGroups
                .distinctUntilChangedBy { groups -> groups.map(ProxyGroupInfo::name) }
                .collect { groups -> groupSorter.track(groups) }
        }
    }

    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        groupSorter.bind(scope = viewModelScope, proxyGroups = proxyGroups, sortMode = sortMode)

    fun ensureCoreLoaded(isActive: Boolean, source: String = "proxy_page") {
        val changed =
            if (isActive) {
                activeSyncSources.add(source)
            } else {
                activeSyncSources.remove(source)
            }
        if (!changed) return
        proxyFacade.setProxyGroupSyncPriority(
            priority = if (isActive) ProxyGroupSyncPriority.FAST else ProxyGroupSyncPriority.OFF,
            source = source,
        )
        if (isActive) {
            viewModelScope.launch {
                runCatching {
                        if (proxyGroups.value.isEmpty()) {
                            proxyFacade.refreshProxyGroups()
                        }
                    }
                    .onFailure { error -> if (error is CancellationException) throw error }
            }
        }
    }

    fun refreshGroup(groupName: String) {
        viewModelScope.launch {
            runCatching { proxyFacade.refreshProxyGroup(groupName) }
                .onFailure { error -> if (error is CancellationException) throw error }
        }
    }

    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            setLoading(true)
            clearError()
            val currentGroups = proxyGroups.value
            val testingTargets: Set<String> =
                if (groupName != null) {
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
                    proxyFacade.healthCheck(groupName)
                    PollingTimers.awaitTick(PollingTimerSpecs.ProxyHealthcheckRefresh)
                    proxyFacade.refreshProxyGroup(groupName)
                    showMessage(MLang.Proxy.Testing.RequestSent)
                } else {
                    showMessage(MLang.Proxy.Testing.All)
                    proxyFacade.healthCheckAll()
                    if (currentGroups.isNotEmpty()) {
                        PollingTimers.awaitTick(PollingTimerSpecs.ProxyHealthcheckRefresh)
                        proxyFacade.refreshProxyGroups()
                    }
                }
            }

            setLoading(false)

            if (testingTargets.isNotEmpty()) {
                PollingTimers.awaitTick(PollingTimerSpecs.ProxyTestingSortHold)
                _testingGroupNames.update { it - testingTargets }
            }

            result.exceptionOrNull()?.let { error ->
                showError(MLang.Proxy.Testing.Failed.format(error.message))
            }
        }
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
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
                }
                .onFailure { error -> showError(MLang.Proxy.Selection.Error.format(error.message)) }
        }
    }

    fun testProxyDelay(groupName: String, proxyName: String) {
        viewModelScope.launch {
            _testingProxyNames.update { it + proxyName }
            runCatching { proxyFacade.healthCheckProxy(groupName, proxyName) }
            PollingTimers.awaitTick(PollingTimerSpecs.ProxySwitchFeedback)
            _testingProxyNames.update { it - proxyName }
        }
    }

    private fun showMessage(message: String) {
        postMessage(message, ProxyUiEffect.ShowMessage(message))
    }

    private fun showError(error: String) {
        postError(error, ProxyUiEffect.ShowError(error))
    }

    fun clearError() {
        clearErrorState()
    }

    data class ProxyUiState(
        override val isLoading: Boolean = false,
        override val message: String? = null,
        override val error: String? = null,
    ) : LoadableState<ProxyUiState> {
        override fun withLoading(loading: Boolean): ProxyUiState = copy(isLoading = loading)

        override fun withError(error: String?): ProxyUiState = copy(error = error)

        override fun withMessage(message: String?): ProxyUiState = copy(message = message)
    }

    sealed interface ProxyUiEffect {
        data class ShowMessage(val message: String) : ProxyUiEffect

        data class ShowError(val message: String) : ProxyUiEffect
    }
}
