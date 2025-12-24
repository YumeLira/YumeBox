package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Represents the view model for proxy-related UI interactions.
 *
 * This class exposes various states and actions related to proxy management, including
 * current mode, display mode, sort mode, selected group index, and proxy groups. It also
 * provides methods for patching mode, testing delay, setting display and sort modes,
 * selecting a proxy, and sorting proxies.
 */
class ProxyViewModel(
    private val clashManager: ClashManager,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore
) : ViewModel() {

    /**
     * Internal UI state object, maintaining the state of the UI.
     *
     * This object represents the current state of the user interface, including
     * loading progress, messages, and error states.
     */
    private val _uiState = MutableStateFlow(ProxyUiState())
    /**
     * The state of the user interface, exposing loading status, error message and a success message.
     */
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    /**
     * Current state of the tunnel mode. This flow is initialized with a default initial state and
     * shares all new state with other subscribers.
     */
    val currentMode: StateFlow<TunnelState.Mode> = proxyDisplaySettingsStore.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TunnelState.Mode.Rule)

    /**
     * The current display mode for the proxy display settings.
     * This field is a state flow that reflects the current mode.
     */
    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsStore.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.DOUBLE_SIMPLE)

    /**
     * The current sorting mode of proxies.
     */
    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsStore.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)

    /**
     * Index of the currently selected group.
     *
     * This value is used to store the currently selected group index.
     * It is a mutable state flow, allowing it to be updated from anywhere in the application.
     */
    private val _selectedGroupIndex = MutableStateFlow(0)
    /**
     * The index of the selected group.
     *
     * This property exposes the current selected group index as a StateFlow, allowing subscribers to observe and react to changes.
     */
    val selectedGroupIndex: StateFlow<Int> = _selectedGroupIndex.asStateFlow()

    /**
     *
     */
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyGroups


    /**
     * A StateFlow containing a list of sorted proxy group information.
     * It is updated dynamically based on the current sort mode.
     *
     * @property value The list of sorted proxy group information.
     */
    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        combine(proxyGroups, sortMode) { groups, mode ->
            if (mode == ProxySortMode.DEFAULT) {
                groups
            } else {
                groups.map { group ->
                    group.copy(proxies = sortProxies(group.proxies, mode))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Switches the current proxy mode to the specified mode.
     *
     * After switching the mode, the current profile is reloaded, and the proxy groups are refreshed.
     * If an error occurs during the switching process, an error message is shown to the user.
     *
     * @param mode The new proxy mode to switch to.
     */
    fun patchMode(mode: TunnelState.Mode) {
        proxyDisplaySettingsStore.proxyMode.set(mode)
        viewModelScope.launch {
            runCatching {
                val persistOverride = Clash.queryOverride(Clash.OverrideSlot.Persist)
                persistOverride.mode = mode
                Clash.patchOverride(Clash.OverrideSlot.Persist, persistOverride)

                val sessionOverride = Clash.queryOverride(Clash.OverrideSlot.Session)
                sessionOverride.mode = mode
                Clash.patchOverride(Clash.OverrideSlot.Session, sessionOverride)

                clashManager.reloadCurrentProfile()
                delay(100)
                val modeName = when (mode) {
                    TunnelState.Mode.Direct -> MLang.Proxy.Mode.Direct
                    TunnelState.Mode.Global -> MLang.Proxy.Mode.Global
                    TunnelState.Mode.Rule -> MLang.Proxy.Mode.Rule
                    else -> MLang.Proxy.Mode.Unknown
                }
                showMessage(MLang.Proxy.Mode.Switched.format(modeName))
                clashManager.refreshProxyGroups()
            }.onFailure { e ->
                Timber.e(e, "代理模式切换失败：$mode")
                showError(MLang.Proxy.Mode.SwitchFailed.format(e.message))
            }
        }
    }

    /**
     * Begins an asynchronous delay test.
     *
     * This method checks the delay of proxies in the specified [groupName] or all groups if no group name is provided.
     *
     * If the test is successful, a message is shown in the UI indicating that the request was sent.
     * If the test fails, an error message is shown in the UI indicating the reason for the failure.
     *
     * @param groupName The name of the group to test, or null to test all groups.
     */
    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            try {
                setLoading(true)
                clearError()

                if (groupName != null) {
                    showMessage(MLang.Proxy.Testing.Group.format(groupName))
                    val result = clashManager.healthCheck(groupName)
                    if (result.isSuccess) {
                        showMessage(MLang.Proxy.Testing.RequestSent)
                    } else {
                        showError(MLang.Proxy.Testing.Failed.format(result.exceptionOrNull()?.message))
                    }
                } else {
                    showMessage(MLang.Proxy.Testing.All)
                    val result = clashManager.healthCheckAll()
                    if (result.isFailure) {
                        showError(MLang.Proxy.Testing.Failed.format(result.exceptionOrNull()?.message))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "延迟测试异常")
                showError(MLang.Proxy.Testing.Failed.format(e.message))
            } finally {
                setLoading(false)
            }
        }
    }


    /**
     * Selects a group by the given [index].
     * Ensures the [index] is within the bounds of [proxyGroups] list.
     * Updates the current selected group's index accordingly.
     *
     * @param index The index of the group to select
     */
    fun setSelectedGroup(index: Int) {
        val groups = proxyGroups.value
        _selectedGroupIndex.value = index.coerceIn(0, groups.size - 1)
    }


    /**
     * Sets the display mode for the proxy display settings.
     *
     * @param mode The display mode to be set. One of ProxyDisplayMode enum values.
     */
    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyDisplaySettingsStore.displayMode.set(mode)
    }

    /**
     *
     */
    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
    }

    /**
     * Select a proxy node in the specified group.
     *
     * @param groupName the name of the group to select from
     * @param proxyName the name of the proxy node to select
     */
    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                val success = clashManager.selectProxy(groupName, proxyName)
                if (success) {
                    showMessage(MLang.Proxy.Selection.Switched.format(proxyName))
                } else {
                    showError(MLang.Proxy.Selection.Failed)
                }
            } catch (e: Exception) {
                showError(MLang.Proxy.Selection.Error.format(e.message))
            }
        }
    }

    /**
     * Sorts the given list of proxies based on the specified sort mode.
     *
     * @param proxies The list of proxies to sort.
     * @param sortMode The mode to sort the proxies by. Can be one of [ProxySortMode.DEFAULT], [ProxySortMode.BY_NAME] or [ProxySortMode.BY_LATENCY].
     * @return The sorted list of proxies.
     */
    private fun sortProxies(proxies: List<Proxy>, sortMode: ProxySortMode): List<Proxy> = when (sortMode) {
        ProxySortMode.DEFAULT -> proxies
        ProxySortMode.BY_NAME -> proxies.sortedBy { it.name }
        ProxySortMode.BY_LATENCY -> proxies.sortedWith(compareBy { if (it.delay > 0) it.delay else Int.MAX_VALUE })
    }

    /**
     * Sets the loading state of the UI.
     *
     * This function updates the current UI state to reflect the new loading state.
     *
     * @param loading True to set the UI to a loading state, false to clear the loading state.*/
    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    /**
     * Displays a message in the UI.
     *
     * @param message The message to be displayed.
     */
    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    /**
     * Displays an error message to the user.
     *
     * @param error The error message to be displayed.
     */
    private fun showError(error: String) {
        _uiState.update { it.copy(error = error) }
    }

    /**
     * Clears the error message in the UI state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Represents the state of the UI, including loading status, messages, and errors.
     */
    data class ProxyUiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}
