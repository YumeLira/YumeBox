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

package com.github.yumeyucca.yumebox.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.core.model.TunDnsMode
import com.github.yumeyucca.yumebox.data.controller.NetworkSettingsController
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.data.model.TunStack
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.data.store.Preference
import com.github.yumeyucca.yumebox.runtime.service.core.EbpfBridgeProcess
import com.github.yumeyucca.yumebox.runtime.service.core.KernelManager
import com.github.yumeyucca.yumebox.runtime.service.root.EbpfCgroupSupport
import com.github.yumeyucca.yumebox.runtime.service.root.RootAccessSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs both the run-mode picker ([NetworkSettingsScreen]) and the VpnService options page
 * ([VpnServiceOptionsScreen]). The TCP/IP stack is fixed to gVisor and the parallel HTTP run mode
 * is gone, so this only exposes the configured mode plus the VpnService knobs.
 */
class NetworkSettingsViewModel(
    application: Application,
    settings: NetworkSettingsStore,
    private val controller: NetworkSettingsController,
) : AndroidViewModel(application) {
    val runMode: Preference<RunMode> = settings.runMode
    val bypassPrivateNetwork: Preference<Boolean> = settings.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = settings.dnsHijack
    val allowBypass: Preference<Boolean> = settings.allowBypass
    val enableIPv6: Preference<Boolean> = settings.enableIPv6
    val systemProxy: Preference<Boolean> = settings.systemProxy
    val disableAllOverride: Preference<Boolean> = settings.disableAllOverride
    val accessControlMode: Preference<AccessControlMode> = settings.accessControlMode

    // Root Tun geometry — its own service-config sub-page ([TunServiceOptionsScreen]).
    val tunStack: Preference<TunStack> = settings.tunStack
    val tunAutoRoute: Preference<Boolean> = settings.tunAutoRoute
    val tunStrictRoute: Preference<Boolean> = settings.tunStrictRoute
    val tunAutoRedirect: Preference<Boolean> = settings.tunAutoRedirect
    val tunDnsMode: Preference<TunDnsMode> = settings.tunDnsMode
    val tunIfName: Preference<String> = settings.tunIfName
    val tunMtu: Preference<Int> = settings.tunMtu
    // Root availability, probed once on open — gates the Tun card (greyed when false, no
    // toast).
    // Probing constructs the libsu shell, so a rooted device may surface its su prompt here; that
    // grant
    // flow is intended. A non-rooted device fast-fails to false and the root cards stay disabled.
    private val _rootAvailable = MutableStateFlow(false)
    val rootAvailable: StateFlow<Boolean> = _rootAvailable.asStateFlow()
    private val _ebpfAvailable = MutableStateFlow(false)
    val ebpfAvailable: StateFlow<Boolean> = _ebpfAvailable.asStateFlow()
    private val _kernels = MutableStateFlow<List<KernelManager.Kernel>>(emptyList())
    private val _kernelStatus = MutableStateFlow("No downloaded kernel installed")
    private val _kernelBusy = MutableStateFlow(false)
    private val _activeKernelId = MutableStateFlow(KernelManager.BUNDLED_ALPHA_ID)
    private val _installedKernelCommits = MutableStateFlow<Map<String, String>>(emptyMap())

    data class NetworkSettingsScreenState(
        val runMode: RunMode = RunMode.VpnService,
        val disableAllOverride: Boolean = false,
        val accessControlMode: AccessControlMode = AccessControlMode.ALLOW_ALL,
        val rootAvailable: Boolean = false,
        val ebpfAvailable: Boolean = false,
        val kernels: List<KernelManager.Kernel> = emptyList(),
        val kernelStatus: String = "No downloaded kernel installed",
        val kernelBusy: Boolean = false,
        val activeKernelId: String = KernelManager.BUNDLED_ALPHA_ID,
        val installedKernelCommits: Map<String, String> = emptyMap(),
    )

    val networkScreenState: StateFlow<NetworkSettingsScreenState> =
        combine(
            combine(
                runMode.state,
                disableAllOverride.state,
                accessControlMode.state,
                rootAvailable,
                ebpfAvailable,
            ) { mode, disableOverride, accessMode, root, ebpf ->
                NetworkSettingsScreenState(
                    runMode = mode,
                    disableAllOverride = disableOverride,
                    accessControlMode = accessMode,
                    rootAvailable = root,
                    ebpfAvailable = ebpf,
                )
            },
            combine(
                _kernels,
                _kernelStatus,
                _kernelBusy,
                _activeKernelId,
                _installedKernelCommits,
            ) { availableKernels, status, busy, active, installedCommits ->
                KernelUiSelection(availableKernels, status, busy, active, installedCommits)
            },
        ) { base, kernel ->
            base.copy(
                kernels = kernel.kernels,
                kernelStatus = kernel.status,
                kernelBusy = kernel.busy,
                activeKernelId = kernel.active,
                installedKernelCommits = kernel.installedCommits,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                NetworkSettingsScreenState(
                    runMode = runMode.value,
                    disableAllOverride = disableAllOverride.value,
                    accessControlMode = accessControlMode.value,
                    rootAvailable = false,
                    ebpfAvailable = false,
                    kernels = emptyList(),
                    kernelStatus = _kernelStatus.value,
                    kernelBusy = false,
                    activeKernelId = KernelManager.BUNDLED_ALPHA_ID,
                ),
            )

    data class TunOptionsScreenState(
        val ifName: String = "",
        val mtu: Int = 0,
        val stack: TunStack = TunStack.System,
        val autoRoute: Boolean = false,
        val strictRoute: Boolean = false,
        val autoRedirect: Boolean = false,
        val dnsMode: TunDnsMode = TunDnsMode.FakeIp,
        val enableIPv6: Boolean = false,
    )

    val tunOptionsScreenState: StateFlow<TunOptionsScreenState> =
        combine(
            combine(
                tunIfName.state,
                tunMtu.state,
                tunStack.state,
                tunAutoRoute.state,
                tunStrictRoute.state,
            ) { ifName, mtu, stack, autoRoute, strictRoute ->
                TunOptionsScreenState(
                    ifName = ifName,
                    mtu = mtu,
                    stack = stack,
                    autoRoute = autoRoute,
                    strictRoute = strictRoute,
                )
            },
            combine(tunAutoRedirect.state, tunDnsMode.state, enableIPv6.state) { autoRedirect,
                                                                                 dnsMode,
                                                                                 ipv6 ->
                Triple(autoRedirect, dnsMode, ipv6)
            },
        ) { base, extra ->
            base.copy(
                autoRedirect = extra.first,
                dnsMode = extra.second,
                enableIPv6 = extra.third,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                TunOptionsScreenState(
                    ifName = tunIfName.value,
                    mtu = tunMtu.value,
                    stack = tunStack.value,
                    autoRoute = tunAutoRoute.value,
                    strictRoute = tunStrictRoute.value,
                    autoRedirect = tunAutoRedirect.value,
                    dnsMode = tunDnsMode.value,
                    enableIPv6 = enableIPv6.value,
                ),
            )

    init {
        _activeKernelId.value = KernelManager.activeKernelId(getApplication())
        viewModelScope.launch {
            refreshInstalledKernelCommits()
            val root = RootAccessSupport.evaluateAsync(getApplication()).canStartRoot
            _rootAvailable.value = root
            val cgroupPath = EbpfCgroupSupport.rootCgroupPath()
            _ebpfAvailable.value =
                root &&
                        cgroupPath != null &&
                        withContext(Dispatchers.IO) {
                            EbpfBridgeProcess.isCapabilityAvailable(getApplication(), cgroupPath)
                        }
        }
    }

    val uiState: StateFlow<NetworkSettingsUiState> =
        runMode.state
            .map { NetworkSettingsUiState(configuredMode = it) }
            .distinctUntilChanged()
            .stateInWhileSubscribed(viewModelScope, NetworkSettingsUiState())

    val tunServiceOptionsUiState: StateFlow<TunServiceOptionsUiState> =
        combine(
            bypassPrivateNetwork.state,
            dnsHijack.state,
            enableIPv6.state,
            allowBypass.state,
            systemProxy.state,
        ) { bypassPrivateNetwork, dnsHijack, enableIPv6, allowBypass, systemProxy ->
            TunServiceOptionsUiState(
                common =
                    CommonTunOptionsUiState(
                        bypassPrivateNetwork = bypassPrivateNetwork,
                        dnsHijack = dnsHijack,
                        enableIPv6 = enableIPv6,
                    ),
                allowBypass = allowBypass,
                systemProxy = systemProxy,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                TunServiceOptionsUiState(
                    common =
                        CommonTunOptionsUiState(
                            bypassPrivateNetwork = bypassPrivateNetwork.value,
                            dnsHijack = dnsHijack.value,
                            enableIPv6 = enableIPv6.value,
                        ),
                    allowBypass = allowBypass.value,
                    systemProxy = systemProxy.value,
                ),
            )

    /**
     * Selects the run mode — the single mode key across the runtime. The root Tun card
     * are disabled in the UI when [rootAvailable] is false, so reaching here for a root mode
     * already implies root was granted.
     */
    fun onRunModeChange(mode: RunMode) {
        controller.setRunMode(mode)
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

    fun onTunStackChange(value: TunStack) = controller.setAndRestartIfNeeded(tunStack, value)

    fun onTunAutoRouteChange(enabled: Boolean) =
        controller.setAndRestartIfNeeded(tunAutoRoute, enabled)

    fun onTunStrictRouteChange(enabled: Boolean) =
        controller.setAndRestartIfNeeded(tunStrictRoute, enabled)

    fun onTunAutoRedirectChange(enabled: Boolean) =
        controller.setAndRestartIfNeeded(tunAutoRedirect, enabled)

    fun onTunDnsModeChange(value: TunDnsMode) = controller.setAndRestartIfNeeded(tunDnsMode, value)

    fun onTunIfNameChange(value: String) {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) controller.setAndRestartIfNeeded(tunIfName, trimmed)
    }

    fun onTunMtuChange(value: Int) {
        if (value in 576..9000) controller.setAndRestartIfNeeded(tunMtu, value)
    }

    fun onDisableAllOverrideChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(disableAllOverride, enabled)
    }

    fun onAccessControlModeChange(mode: AccessControlMode) {
        controller.setAndRestartIfNeeded(accessControlMode, mode)
    }

    fun refreshKernels() {
        if (_kernelBusy.value) return
        viewModelScope.launch {
            _kernelBusy.value = true
            runCatching { KernelManager.fetchIndex() }
                .onSuccess { index ->
                    _kernels.value = index.kernels
                    _activeKernelId.value = KernelManager.activeKernelId(getApplication())
                    _kernelStatus.value = "Select kernels to download"
                }
                .onFailure { error -> _kernelStatus.value = "Kernel index unavailable: ${error.message}" }
            _kernelBusy.value = false
        }
    }

    fun downloadKernels(ids: Set<String>, onFinished: (Boolean) -> Unit = {}) {
        if (_kernelBusy.value || ids.isEmpty()) return
        val selected = _kernels.value.filter { it.id in ids }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _kernelBusy.value = true
            var completed = 0
            var failed = false
            selected.forEach { kernel ->
                if (!failed) {
                    _kernelStatus.value = "Downloading ${kernel.name}"
                    runCatching { KernelManager.install(getApplication(), kernel) }
                        .onFailure { error ->
                            failed = true
                            _kernelStatus.value = "Kernel install failed: ${error.message}"
                        }
                }
                if (!failed) completed++
            }
            if (!failed) _kernelStatus.value = "$completed kernel(s) downloaded and verified"
            if (!failed) refreshInstalledKernelCommits()
            _kernelBusy.value = false
            onFinished(!failed)
        }
    }

    fun selectKernel(id: String) {
        if (id == KernelManager.BUNDLED_ALPHA_ID) {
            runCatching { KernelManager.activate(getApplication(), id) }
                .onSuccess {
                    _activeKernelId.value = id
                    _kernelStatus.value = "Builtin selected. Restart the service to load it."
                    controller.requestRestartIfRunning()
                }
            return
        }
        val kernel = _kernels.value.firstOrNull { it.id == id }
        if (kernel == null && KernelManager.isInstalled(getApplication(), id)) {
            runCatching { KernelManager.activate(getApplication(), id) }
                .onSuccess {
                    _activeKernelId.value = id
                    _kernelStatus.value = "$id selected. Restart the service to load it."
                    controller.requestRestartIfRunning()
                }
            return
        }
        if (kernel == null) {
            viewModelScope.launch {
                _kernelBusy.value = true
                runCatching { KernelManager.fetchIndex() }
                    .onSuccess { index ->
                        _kernels.value = index.kernels
                        _kernelBusy.value = false
                        selectKernel(id)
                    }
                    .onFailure { error -> _kernelStatus.value = "Kernel index unavailable: ${error.message}" }
                _kernelBusy.value = false
            }
            return
        }
        if (_kernelBusy.value) return
        if (KernelManager.isInstalled(getApplication(), id)) {
            runCatching { KernelManager.activate(getApplication(), id) }
                .onSuccess {
                    _activeKernelId.value = id
                    _kernelStatus.value = "${kernel.name} selected. Restart the service to load it."
                    controller.requestRestartIfRunning()
                }
            return
        }
        viewModelScope.launch {
            _kernelBusy.value = true
            _kernelStatus.value = "Downloading ${kernel.name}"
            runCatching { KernelManager.install(getApplication(), kernel) }
                .onSuccess {
                    KernelManager.activate(getApplication(), kernel.id)
                    refreshInstalledKernelCommits()
                    _activeKernelId.value = kernel.id
                    _kernelStatus.value = "${kernel.name} selected. Restart the service to load it."
                }
                .onFailure { error -> _kernelStatus.value = "Kernel install failed: ${error.message}" }
            _kernelBusy.value = false
        }
    }

    private suspend fun refreshInstalledKernelCommits() {
        _installedKernelCommits.value =
            withContext(Dispatchers.IO) {
                listOf("alpha", "meta", "smart").mapNotNull { id ->
                    KernelManager.installedCommit(getApplication(), id)?.let { id to it }
                }.toMap()
            }
    }
}

private data class KernelUiSelection(
    val kernels: List<KernelManager.Kernel>,
    val status: String,
    val busy: Boolean,
    val active: String,
    val installedCommits: Map<String, String>,
)

data class NetworkSettingsUiState(val configuredMode: RunMode = RunMode.VpnService)

data class CommonTunOptionsUiState(
    val bypassPrivateNetwork: Boolean = false,
    val dnsHijack: Boolean = false,
    val enableIPv6: Boolean = false,
)

data class TunServiceOptionsUiState(
    val common: CommonTunOptionsUiState = CommonTunOptionsUiState(),
    val allowBypass: Boolean = false,
    val systemProxy: Boolean = false,
)
