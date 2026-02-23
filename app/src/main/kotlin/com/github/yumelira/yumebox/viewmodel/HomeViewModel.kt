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
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.data.repository.NetworkInfoService
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    appSettingsStorage: AppSettingsStorage,
    private val networkInfoService: NetworkInfoService,
    private val proxyChainResolver: ProxyChainResolver
) : AndroidViewModel(application) {

    // State flows for profiles
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()
    
    private val _recommendedProfile = MutableStateFlow<Profile?>(null)
    val recommendedProfile: StateFlow<Profile?> = _recommendedProfile.asStateFlow()

    private val _profilesLoaded = MutableStateFlow(false)
    val profilesLoaded: StateFlow<Boolean> = _profilesLoaded.asStateFlow()
    
    // 检查是否有激活的配置
    val hasEnabledProfile: Flow<Boolean> = profiles.map { list -> 
        list.any { it.active } 
    }

    val isRunning = proxyFacade.isRunning
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val proxyGroups = proxyFacade.proxyGroups

    val oneWord: StateFlow<String> = appSettingsStorage.oneWord.state
    val oneWordAuthor: StateFlow<String> = appSettingsStorage.oneWordAuthor.state

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _displayRunning = MutableStateFlow(false)
    val displayRunning: StateFlow<Boolean> = _displayRunning.asStateFlow()

    private val _isToggling = MutableStateFlow(false)
    val isToggling: StateFlow<Boolean> = _isToggling.asStateFlow()

    private val _vpnPrepareIntent = MutableSharedFlow<Intent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val vpnPrepareIntent = _vpnPrepareIntent.asSharedFlow()

    private val _speedHistory = MutableStateFlow<List<Long>>(emptyList())
    val speedHistory: StateFlow<List<Long>> = _speedHistory.asStateFlow()

    private val mainProxyNode: StateFlow<com.github.yumelira.yumebox.core.model.Proxy?> =
        combine(isRunning, proxyGroups) { running, groups ->
            if (!running || groups.isEmpty()) return@combine null
            val mainGroup = groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
            if (mainGroup != null && mainGroup.now.isNotBlank()) {
                proxyChainResolver.resolveEndNode(mainGroup.now, groups)
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerName: StateFlow<String?> =
        mainProxyNode.map { it?.name }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedServerPing: StateFlow<Int?> = mainProxyNode.map { node ->
        node?.delay?.takeIf { d -> d > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ipMonitoringState: StateFlow<IpMonitoringState> = isRunning.flatMapLatest { running ->
        if (running) {
            networkInfoService.startIpMonitoring(isRunning)
        } else {
            flowOf(IpMonitoringState.Loading)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IpMonitoringState.Loading)

    init {
        refreshProfiles()
        syncDisplayState()
        startSpeedSampling()
        observeProfileChanges()
    }

    private fun refreshProfiles() {
        viewModelScope.launch {
            try {
                val allProfiles = profilesRepository.queryAllProfiles()
                val active = profilesRepository.queryActiveProfile()
                _profiles.value = allProfiles
                _recommendedProfile.value = active
                _profilesLoaded.value = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh profiles")
                _profilesLoaded.value = true
            }
        }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            proxyFacade.currentProfile.collect {
                // active profile can change even when proxy isn't running
                refreshProfiles()
            }
        }
    }

    private fun syncDisplayState() {
        viewModelScope.launch {
            isRunning.collect { running ->
                if (!_isToggling.value) {
                    _displayRunning.value = running
                }
                if (running == _displayRunning.value) {
                    _isToggling.value = false
                }
            }
        }
    }

    suspend fun reloadProfile() {
        try {
            setLoading(true)
            
            // 查询活动配置
            val activeProfile = profilesRepository.queryActiveProfile()
            if (activeProfile == null) {
                showError(MLang.Home.Message.ConfigSwitchFailed.format(MLang.ProfilesVM.Error.ProfileNotExist))
                return
            }

            // 先更新配置文件（下载/导入）
            profilesRepository.updateProfile(activeProfile.uuid)

            // 热重载：不重启服务，直接让 service 侧重新 load 当前 profile
            proxyFacade.reloadCurrentProfile().getOrThrow()
            showMessage(MLang.Home.Message.ConfigSwitched)
        } catch (e: Exception) {
            Timber.e(e, "Failed to reload profile")
            showError(MLang.Home.Message.ConfigSwitchFailed.format(e.message))
        } finally {
            setLoading(false)
        }
    }

    fun startProxy(profileId: String, useTunMode: Boolean? = null) {
        if (_isToggling.value) return

        viewModelScope.launch {
            _isToggling.value = true
            _displayRunning.value = true
            _uiState.update {
                it.copy(
                    isStartingProxy = true,
                    loadingProgress = MLang.Home.Message.Preparing
                )
            }

            try {
                // 设置活动配置
                if (profileId.isNotBlank()) {
                    profilesRepository.setActiveProfile(java.util.UUID.fromString(profileId))
                }
                
                // 启动代理
                val useTun = useTunMode ?: false
                proxyFacade.startProxy(useTun)
                
                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                _isToggling.value = false
            } catch (e: com.github.yumelira.yumebox.remote.VpnPermissionRequired) {
                // VPN 权限需要请求
                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                _vpnPrepareIntent.emit(e.intent)
                _displayRunning.value = false
                _isToggling.value = false
                Timber.i("VPN permission required")
            } catch (e: Exception) {
                _displayRunning.value = false
                _isToggling.value = false
                _uiState.update { it.copy(isStartingProxy = false, loadingProgress = null) }
                Timber.e(e, "Failed to start proxy")
                showError(MLang.Home.Message.StartFailed.format(e.message))
            }
        }
    }

    fun stopProxy() {
        if (_isToggling.value) return

        viewModelScope.launch {
            _isToggling.value = true
            _displayRunning.value = false
            setLoading(true)

            try {
                proxyFacade.stopProxy()
                showMessage(MLang.Home.Message.ProxyStopped)
                _isToggling.value = false
            } catch (e: Exception) {
                _displayRunning.value = true
                _isToggling.value = false
                Timber.e(e, "Failed to stop proxy")
                showError(MLang.Home.Message.StopFailed.format(e.message))
            }

            setLoading(false)
        }
    }

    private fun startSpeedSampling(sampleLimit: Int = 24) {
        viewModelScope.launch {
            flow {
                while (true) {
                    val t = proxyFacade.trafficNow.value
                    val d = TrafficData.from(t)
                    emit((d.upload + d.download).coerceAtLeast(0L))
                    kotlinx.coroutines.delay(1000L)
                }
            }.catch { }.collect { sample ->
                _speedHistory.update { old ->
                    buildList(sampleLimit) {
                        repeat((sampleLimit - old.size - 1).coerceAtLeast(0)) { add(0L) }
                        addAll(old.takeLast(sampleLimit - 1))
                        add(sample)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }

    data class HomeUiState(
        val isLoading: Boolean = false,
        val isStartingProxy: Boolean = false,
        val loadingProgress: String? = null,
        val message: String? = null,
        val error: String? = null
    )
}

