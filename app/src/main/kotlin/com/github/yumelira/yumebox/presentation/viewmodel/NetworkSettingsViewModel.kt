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

package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.service.NetworkServiceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworkSettingsViewModel(
    application: Application,
    storage: NetworkSettingsStorage,
) : AndroidViewModel(application) {


    private val serviceManager = NetworkServiceManager(application)


    val proxyMode: Preference<ProxyMode> = storage.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = storage.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = storage.dnsHijack
    val allowBypass: Preference<Boolean> = storage.allowBypass
    val enableIPv6: Preference<Boolean> = storage.enableIPv6
    val systemProxy: Preference<Boolean> = storage.systemProxy
    val tunStack: Preference<TunStack> = storage.tunStack
    val accessControlMode: Preference<AccessControlMode> = storage.accessControlMode


    val serviceState = serviceManager.serviceState
    val currentProxyMode = serviceManager.proxyMode


    val uiState: StateFlow<NetworkSettingsUiState> = combine(
        serviceState,
        currentProxyMode
    ) { serviceState, proxyMode ->
        NetworkSettingsUiState(
            serviceState = serviceState,
            currentProxyMode = proxyMode,
            needsRestart = serviceState == NetworkServiceManager.ServiceState.Running
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


    fun startService(proxyMode: ProxyMode) {
        serviceManager.startService(proxyMode)
    }

    fun restartService() {
        serviceManager.restartService()
    }

    private fun updateServiceConfig() {
        viewModelScope.launch {
            val serviceConfig = NetworkServiceManager.ServiceConfig(
                bypassPrivateNetwork = bypassPrivateNetwork.value,
                dnsHijacking = dnsHijack.value,
                allowBypass = allowBypass.value,
                allowIpv6 = enableIPv6.value,
                systemProxy = systemProxy.value,
                tunStackMode = tunStack.value.name,
                accessControlMode = accessControlMode.value
            )
            serviceManager.updateServiceConfig(serviceConfig)
        }
    }

}

data class NetworkSettingsUiState(
    val serviceState: NetworkServiceManager.ServiceState = NetworkServiceManager.ServiceState.Stopped,
    val currentProxyMode: ProxyMode = ProxyMode.Tun,
    val needsRestart: Boolean = false
)
