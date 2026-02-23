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

package com.github.yumelira.yumebox.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.repository.NetworkSettingsRepository
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.data.store.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSettingsViewModel(
    application: Application,
    repository: NetworkSettingsRepository,
    private val profilesRepository: ProfilesRepository,
    private val proxyFacade: ProxyFacade,
) : AndroidViewModel(application) {

    private var restartJob: Job? = null

    val proxyMode: Preference<ProxyMode> = repository.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = repository.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = repository.dnsHijack
    val allowBypass: Preference<Boolean> = repository.allowBypass
    val enableIPv6: Preference<Boolean> = repository.enableIPv6
    val systemProxy: Preference<Boolean> = repository.systemProxy
    val tunStack: Preference<TunStack> = repository.tunStack
    val accessControlMode: Preference<AccessControlMode> = repository.accessControlMode
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()


    val serviceState: StateFlow<ServiceState> = proxyFacade.isRunning
        .map { running -> if (running) ServiceState.Running else ServiceState.Stopped }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceState.Stopped)

    val currentProxyMode: StateFlow<ProxyMode> = proxyMode.state


    val uiState: StateFlow<NetworkSettingsUiState> = combine(
        serviceState,
        currentProxyMode
    ) { serviceState, proxyMode ->
        NetworkSettingsUiState(
            serviceState = serviceState,
            currentProxyMode = proxyMode,
            needsRestart = serviceState == ServiceState.Running
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkSettingsUiState()
    )


    fun onProxyModeChange(mode: ProxyMode) {
        proxyMode.set(mode)
    }

    fun onBypassPrivateNetworkChange(enabled: Boolean) {
        bypassPrivateNetwork.set(enabled)
        updateServiceConfig()
    }

    fun onDnsHijackChange(enabled: Boolean) {
        dnsHijack.set(enabled)
        updateServiceConfig()
    }

    fun onAllowBypassChange(enabled: Boolean) {
        allowBypass.set(enabled)
        updateServiceConfig()
    }

    fun onEnableIPv6Change(enabled: Boolean) {
        enableIPv6.set(enabled)
        updateServiceConfig()
    }

    fun onSystemProxyChange(enabled: Boolean) {
        systemProxy.set(enabled)
        updateServiceConfig()
    }

    fun onTunStackChange(stack: TunStack) {
        tunStack.set(stack)
        updateServiceConfig()
    }

    fun onAccessControlModeChange(mode: AccessControlMode) {
        accessControlMode.set(mode)
        updateServiceConfig()
    }


    fun startService(mode: ProxyMode) {
        viewModelScope.launch {
            switchService(mode).onFailure { error ->
                _errors.tryEmit(error.message ?: "Failed to start proxy service")
            }
        }
    }

    fun restartService() {
        viewModelScope.launch {
            if (!proxyFacade.isRunning.value) return@launch
            switchService(proxyMode.value).onFailure { error ->
                _errors.tryEmit(error.message ?: "Failed to restart proxy service")
            }
        }
    }

    private suspend fun switchService(mode: ProxyMode): Result<Unit> = runCatching {
        val hasActiveProfile = withContext(Dispatchers.IO) {
            profilesRepository.queryActiveProfile() != null
        }
        check(hasActiveProfile) { "No active profile selected" }

        if (proxyFacade.isRunning.value) {
            withContext(Dispatchers.IO) {
                proxyFacade.stopProxy()
            }
            delay(SERVICE_RESTART_DELAY_MS)
        }

        withContext(Dispatchers.IO) {
            proxyFacade.startProxy(mode == ProxyMode.Tun)
        }
    }

    private fun updateServiceConfig() {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            delay(RESTART_DEBOUNCE_DELAY_MS)
            if (serviceState.value == ServiceState.Running) {
                restartService()
            }
        }
    }

    companion object {
        private const val RESTART_DEBOUNCE_DELAY_MS = 300L
        private const val SERVICE_RESTART_DELAY_MS = 500L
    }
}

data class NetworkSettingsUiState(
    val serviceState: ServiceState = ServiceState.Stopped,
    val currentProxyMode: ProxyMode = ProxyMode.Tun,
    val needsRestart: Boolean = false
)

enum class ServiceState {
    Running, Stopped
}

