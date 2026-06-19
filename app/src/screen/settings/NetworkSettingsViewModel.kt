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

package com.github.yumelira.yumebox.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.RootTunDnsMode
import com.github.yumelira.yumebox.data.controller.NetworkSettingsController
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworkSettingsViewModel(
    application: Application,
    settings: NetworkSettingsStore,
    private val controller: NetworkSettingsController,
    private val proxyFacade: ProxyFacade,
) : AndroidViewModel(application) {

    val proxyMode: Preference<ProxyMode> = settings.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = settings.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = settings.dnsHijack
    val allowBypass: Preference<Boolean> = settings.allowBypass
    val enableIPv6: Preference<Boolean> = settings.enableIPv6
    val systemProxy: Preference<Boolean> = settings.systemProxy
    val tunStack: Preference<TunStack> = settings.tunStack
    val rootTunAutoRoute: Preference<Boolean> = settings.rootTunAutoRoute
    val rootTunStrictRoute: Preference<Boolean> = settings.rootTunStrictRoute
    val rootTunAutoRedirect: Preference<Boolean> = settings.rootTunAutoRedirect
    val rootTunDnsMode: Preference<RootTunDnsMode> = settings.rootTunDnsMode
    val accessControlMode: Preference<AccessControlMode> = settings.accessControlMode

    private val rootTunIfName = settings.rootTunIfName
    private val rootTunMtu = settings.rootTunMtu
    private val rootTunFakeIpRange = settings.rootTunFakeIpRange
    private val rootTunFakeIpRange6 = settings.rootTunFakeIpRange6

    private val _rootTunIfNameDraft = MutableStateFlow(rootTunIfName.value)
    val rootTunIfNameDraft: StateFlow<String> = _rootTunIfNameDraft.asStateFlow()

    private val _rootTunMtuDraft = MutableStateFlow(rootTunMtu.value.toString())
    val rootTunMtuDraft: StateFlow<String> = _rootTunMtuDraft.asStateFlow()

    private val _rootTunFakeIpRangeDraft = MutableStateFlow(rootTunFakeIpRange.value)
    val rootTunFakeIpRangeDraft: StateFlow<String> = _rootTunFakeIpRangeDraft.asStateFlow()

    private val _rootTunFakeIpRange6Draft = MutableStateFlow(rootTunFakeIpRange6.value)
    val rootTunFakeIpRange6Draft: StateFlow<String> = _rootTunFakeIpRange6Draft.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val runtimeSnapshot = proxyFacade.runtimeSnapshot

    val serviceState: StateFlow<ServiceState> =
        runtimeSnapshot
            .map { snapshot -> ServiceState.fromPhase(snapshot.phase) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceState.Idle)

    val currentProxyMode: StateFlow<ProxyMode> = proxyMode.state

    val uiState: StateFlow<NetworkSettingsUiState> =
        combine(currentProxyMode, runtimeSnapshot, rootTunDnsMode.state) {
                configuredMode,
                snapshot,
                dnsMode ->
                val effectiveMode = RuntimeStateMapper.resolveDisplayMode(snapshot, configuredMode)
                val activeMode = RuntimeStateMapper.modeForOwner(snapshot.owner)
                NetworkSettingsUiState(
                    serviceState = ServiceState.fromPhase(snapshot.phase),
                    configuredMode = configuredMode,
                    effectiveMode = effectiveMode,
                    needsRestart =
                        snapshot.phase == RuntimePhase.Running && activeMode != configuredMode,
                    showServiceOptions = configuredMode != ProxyMode.Http,
                    showTunOnlyOptions = configuredMode == ProxyMode.Tun,
                    showAccessControlMode = configuredMode != ProxyMode.Http,
                    showRootTunAdvanced = configuredMode == ProxyMode.RootTun,
                    showFakeIpRange =
                        configuredMode == ProxyMode.RootTun && dnsMode == RootTunDnsMode.FakeIp,
                )
            }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NetworkSettingsUiState(),
            )

    private val commonTunOptionsUiState: StateFlow<CommonTunOptionsUiState> =
        combine(bypassPrivateNetwork.state, dnsHijack.state, enableIPv6.state, tunStack.state) {
                bypassPrivateNetwork,
                dnsHijack,
                enableIPv6,
                tunStack ->
                CommonTunOptionsUiState(
                    bypassPrivateNetwork = bypassPrivateNetwork,
                    dnsHijack = dnsHijack,
                    enableIPv6 = enableIPv6,
                    tunStack = tunStack,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    CommonTunOptionsUiState(
                        bypassPrivateNetwork = bypassPrivateNetwork.value,
                        dnsHijack = dnsHijack.value,
                        enableIPv6 = enableIPv6.value,
                        tunStack = tunStack.value,
                    ),
            )

    val tunServiceOptionsUiState: StateFlow<TunServiceOptionsUiState> =
        combine(commonTunOptionsUiState, allowBypass.state, systemProxy.state) {
                common,
                allowBypass,
                systemProxy ->
                TunServiceOptionsUiState(
                    common = common,
                    allowBypass = allowBypass,
                    systemProxy = systemProxy,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    TunServiceOptionsUiState(
                        common =
                            CommonTunOptionsUiState(
                                bypassPrivateNetwork = bypassPrivateNetwork.value,
                                dnsHijack = dnsHijack.value,
                                enableIPv6 = enableIPv6.value,
                                tunStack = tunStack.value,
                            ),
                        allowBypass = allowBypass.value,
                        systemProxy = systemProxy.value,
                    ),
            )

    private val rootTunRoutingUiState =
        combine(
            rootTunAutoRoute.state,
            rootTunStrictRoute.state,
            rootTunAutoRedirect.state,
            rootTunDnsMode.state,
        ) { rootTunAutoRoute, rootTunStrictRoute, rootTunAutoRedirect, rootTunDnsMode ->
            RootTunRoutingUiState(
                rootTunAutoRoute = rootTunAutoRoute,
                rootTunStrictRoute = rootTunStrictRoute,
                rootTunAutoRedirect = rootTunAutoRedirect,
                rootTunDnsMode = rootTunDnsMode,
            )
        }

    private val rootTunDraftsUiState =
        combine(
            rootTunIfNameDraft,
            rootTunMtuDraft,
            rootTunFakeIpRangeDraft,
            rootTunFakeIpRange6Draft,
        ) { rootTunIfNameDraft, rootTunMtuDraft, rootTunFakeIpRangeDraft, rootTunFakeIpRange6Draft
            ->
            RootTunDraftsUiState(
                rootTunIfNameDraft = rootTunIfNameDraft,
                rootTunMtuDraft = rootTunMtuDraft,
                rootTunFakeIpRangeDraft = rootTunFakeIpRangeDraft,
                rootTunFakeIpRange6Draft = rootTunFakeIpRange6Draft,
            )
        }

    val rootTunServiceOptionsUiState: StateFlow<RootTunServiceOptionsUiState> =
        combine(commonTunOptionsUiState, rootTunRoutingUiState, rootTunDraftsUiState) {
                common,
                routing,
                drafts ->
                RootTunServiceOptionsUiState(
                    common = common,
                    rootTunAutoRoute = routing.rootTunAutoRoute,
                    rootTunStrictRoute = routing.rootTunStrictRoute,
                    rootTunAutoRedirect = routing.rootTunAutoRedirect,
                    rootTunDnsMode = routing.rootTunDnsMode,
                    rootTunIfNameDraft = drafts.rootTunIfNameDraft,
                    rootTunMtuDraft = drafts.rootTunMtuDraft,
                    rootTunFakeIpRangeDraft = drafts.rootTunFakeIpRangeDraft,
                    rootTunFakeIpRange6Draft = drafts.rootTunFakeIpRange6Draft,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    RootTunServiceOptionsUiState(
                        common = commonTunOptionsUiState.value,
                        rootTunAutoRoute = rootTunAutoRoute.value,
                        rootTunStrictRoute = rootTunStrictRoute.value,
                        rootTunAutoRedirect = rootTunAutoRedirect.value,
                        rootTunDnsMode = rootTunDnsMode.value,
                        rootTunIfNameDraft = rootTunIfNameDraft.value,
                        rootTunMtuDraft = rootTunMtuDraft.value,
                        rootTunFakeIpRangeDraft = rootTunFakeIpRangeDraft.value,
                        rootTunFakeIpRange6Draft = rootTunFakeIpRange6Draft.value,
                    ),
            )

    fun onProxyModeChange(mode: ProxyMode) {
        controller.setProxyMode(mode)
    }

    fun onBypassPrivateNetworkChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(bypassPrivateNetwork, enabled)
    }

    fun onDnsHijackChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(dnsHijack, enabled)
    }

    fun onAllowBypassChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(allowBypass, enabled)
    }

    fun onEnableIPv6Change(enabled: Boolean) {
        controller.setAndRestartIfNeeded(enableIPv6, enabled)
    }

    fun onSystemProxyChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(systemProxy, enabled)
    }

    fun onTunStackChange(stack: TunStack) {
        controller.setAndRestartIfNeeded(tunStack, stack)
    }

    fun onRootTunAutoRouteChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(rootTunAutoRoute, enabled)
    }

    fun onRootTunStrictRouteChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(rootTunStrictRoute, enabled)
    }

    fun onRootTunAutoRedirectChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(rootTunAutoRedirect, enabled)
    }

    fun onRootTunDnsModeChange(mode: RootTunDnsMode) {
        controller.setAndRestartIfNeeded(rootTunDnsMode, mode)
    }

    fun onAccessControlModeChange(mode: AccessControlMode) {
        controller.setAndRestartIfNeeded(accessControlMode, mode)
    }

    fun onRootTunIfNameDraftChange(value: String) {
        _rootTunIfNameDraft.value = value
    }

    fun commitRootTunIfName() {
        val normalized = _rootTunIfNameDraft.value.trim().ifBlank { DEFAULT_ROOT_TUN_IF_NAME }
        _rootTunIfNameDraft.value = normalized
        controller.commitDraftAndRestart(rootTunIfName, normalized)
    }

    fun onRootTunMtuDraftChange(value: String) {
        _rootTunMtuDraft.value = value
    }

    fun commitRootTunMtu() {
        val parsed = _rootTunMtuDraft.value.trim().toIntOrNull()?.takeIf { it > 0 } ?: return
        _rootTunMtuDraft.value = parsed.toString()
        controller.commitDraftAndRestart(rootTunMtu, parsed)
    }

    fun onRootTunFakeIpRangeDraftChange(value: String) {
        _rootTunFakeIpRangeDraft.value = value
    }

    fun commitRootTunFakeIpRange() {
        val normalized = _rootTunFakeIpRangeDraft.value.trim().ifBlank { DEFAULT_FAKE_IP_RANGE }
        _rootTunFakeIpRangeDraft.value = normalized
        controller.commitDraftAndRestart(rootTunFakeIpRange, normalized)
    }

    fun onRootTunFakeIpRange6DraftChange(value: String) {
        _rootTunFakeIpRange6Draft.value = value
    }

    fun commitRootTunFakeIpRange6() {
        val normalized = _rootTunFakeIpRange6Draft.value.trim().ifBlank { DEFAULT_FAKE_IP_RANGE6 }
        _rootTunFakeIpRange6Draft.value = normalized
        controller.commitDraftAndRestart(rootTunFakeIpRange6, normalized)
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
            if (!RuntimeStateMapper.isActuallyRunning(runtimeSnapshot.value)) return@launch
            switchService(proxyMode.value).onFailure { error ->
                _errors.tryEmit(error.message ?: "Failed to restart proxy service")
            }
        }
    }

    private suspend fun switchService(mode: ProxyMode): Result<Unit> = runCatching {
        controller.startService(mode).getOrThrow()
    }

    companion object {
        private const val DEFAULT_ROOT_TUN_IF_NAME = "Yume"
        private const val DEFAULT_FAKE_IP_RANGE = "198.18.0.1/16"
        private const val DEFAULT_FAKE_IP_RANGE6 = "fc00::/18"
    }
}

data class NetworkSettingsUiState(
    val serviceState: ServiceState = ServiceState.Idle,
    val configuredMode: ProxyMode = ProxyMode.Tun,
    val effectiveMode: ProxyMode = ProxyMode.Tun,
    val needsRestart: Boolean = false,
    val showServiceOptions: Boolean = true,
    val showTunOnlyOptions: Boolean = true,
    val showAccessControlMode: Boolean = true,
    val showRootTunAdvanced: Boolean = false,
    val showFakeIpRange: Boolean = false,
)

data class CommonTunOptionsUiState(
    val bypassPrivateNetwork: Boolean = false,
    val dnsHijack: Boolean = false,
    val enableIPv6: Boolean = false,
    val tunStack: TunStack = TunStack.System,
)

data class TunServiceOptionsUiState(
    val common: CommonTunOptionsUiState = CommonTunOptionsUiState(),
    val allowBypass: Boolean = false,
    val systemProxy: Boolean = false,
)

data class RootTunServiceOptionsUiState(
    val common: CommonTunOptionsUiState = CommonTunOptionsUiState(),
    val rootTunAutoRoute: Boolean = false,
    val rootTunStrictRoute: Boolean = false,
    val rootTunAutoRedirect: Boolean = false,
    val rootTunDnsMode: RootTunDnsMode = RootTunDnsMode.RedirHost,
    val rootTunIfNameDraft: String = "",
    val rootTunMtuDraft: String = "",
    val rootTunFakeIpRangeDraft: String = "",
    val rootTunFakeIpRange6Draft: String = "",
)

private data class RootTunRoutingUiState(
    val rootTunAutoRoute: Boolean,
    val rootTunStrictRoute: Boolean,
    val rootTunAutoRedirect: Boolean,
    val rootTunDnsMode: RootTunDnsMode,
)

private data class RootTunDraftsUiState(
    val rootTunIfNameDraft: String,
    val rootTunMtuDraft: String,
    val rootTunFakeIpRangeDraft: String,
    val rootTunFakeIpRange6Draft: String,
)

enum class ServiceState {
    Idle,
    Starting,
    Running,
    Stopping,
    Failed;

    companion object {
        fun fromPhase(phase: RuntimePhase): ServiceState {
            return when (phase) {
                RuntimePhase.Idle -> Idle
                RuntimePhase.Starting -> Starting
                RuntimePhase.Running -> Running
                RuntimePhase.Stopping -> Stopping
                RuntimePhase.Failed -> Failed
            }
        }
    }
}
