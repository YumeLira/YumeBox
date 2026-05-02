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

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.common.util.DeviceUtil
import com.github.yumelira.yumebox.common.util.showToastDialog
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.substore.SubStorePaths
import com.github.yumelira.yumebox.substore.SubStoreServiceController
import com.github.yumelira.yumebox.substore.SubStoreServiceRequest
import com.github.yumelira.yumebox.substore.engine.NativeLibraryManager
import com.github.yumelira.yumebox.substore.model.AutoCloseMode
import com.github.yumelira.yumebox.substore.util.SubStoreDownloadClient
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeatureViewModel(
    store: FeatureStore,
    private val application: Application,
    private val downloadClient: SubStoreDownloadClient,
) : ViewModel() {
    val allowLanAccess: Preference<Boolean> = store.allowLanAccess
    val backendPort: Preference<Int> = store.backendPort
    val frontendPort: Preference<Int> = store.frontendPort
    val selectedPanelType: Preference<Int> = store.selectedPanelType
    val panelOpenMode: Preference<LinkOpenMode> = store.panelOpenMode
    val exitUiWhenBackground: Preference<Boolean> = store.exitUiWhenBackground

    private val _autoCloseMode = MutableStateFlow(AutoCloseMode.DISABLED)
    val autoCloseMode: StateFlow<AutoCloseMode> = _autoCloseMode.asStateFlow()

    val serviceRunningState: StateFlow<Boolean> = SubStoreServiceController.snapshot
        .map { it.isActive }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubStoreServiceController.snapshot.value.isActive,
        )

    private var autoCloseJob: Job? = null

    private val _isDownloadingSubStoreFrontend = MutableStateFlow(false)
    val isDownloadingSubStoreFrontend: StateFlow<Boolean> = _isDownloadingSubStoreFrontend.asStateFlow()

    private val _isDownloadingSubStoreBackend = MutableStateFlow(false)
    val isDownloadingSubStoreBackend: StateFlow<Boolean> = _isDownloadingSubStoreBackend.asStateFlow()

    private val _isSubStoreInitialized = MutableStateFlow(false)
    val isSubStoreInitialized: StateFlow<Boolean> = _isSubStoreInitialized.asStateFlow()

    private val _isExtensionInstalled = MutableStateFlow(false)
    val isExtensionInstalled: StateFlow<Boolean> = _isExtensionInstalled.asStateFlow()

    private val _isJavetLoaded = MutableStateFlow(false)
    val isJavetLoaded: StateFlow<Boolean> = _isJavetLoaded.asStateFlow()

    companion object {
        private const val EXTENSION_PACKAGE_NAME = "com.github.yumelira.yumebox.extension"
        private const val JAVET_LIB_NAME = "libjavet-node-android"
    }

    fun startService() {
        if (DeviceUtil.is32BitDevice()) {
            showToast(MLang.Feature.SubStore.Not32Bit)
            return
        }
        if (!checkSubStoreReadiness()) return
        viewModelScope.launch {
            runCatching {
                SubStoreServiceController.startService(
                    context = application,
                    request = SubStoreServiceRequest(
                        backendPort = backendPort.value,
                        frontendPort = frontendPort.value,
                        allowLan = allowLanAccess.value,
                    ),
                )
            }.onSuccess {
                setupAutoCloseTimer()
            }.onFailure { error ->
                showToast(error.message ?: MLang.Util.Error.UnknownError)
            }
        }
    }

    private fun checkSubStoreReadiness(): Boolean {
        return when {
            !_isExtensionInstalled.value -> {
                showToast(MLang.Feature.SubStore.InstallExtension); false
            }

            !_isSubStoreInitialized.value -> {
                showToast(MLang.Feature.SubStore.DownloadSubStoreFirst); false
            }

            !_isJavetLoaded.value -> {
                showToast(MLang.Feature.SubStore.JavetNotReady); false
            }

            else -> true
        }
    }

    fun stopService() {
        viewModelScope.launch {
            cancelAutoCloseTimer()
            SubStoreServiceController.stopService(application)
            _autoCloseMode.value = AutoCloseMode.DISABLED
        }
    }

    fun setAllowLanAccess(allow: Boolean) = allowLanAccess.set(allow)
    fun setAutoCloseMode(mode: AutoCloseMode) {
        _autoCloseMode.value = mode
        if (serviceRunningState.value) {
            cancelAutoCloseTimer()
            setupAutoCloseTimer()
        }
    }

    fun initializeSubStoreStatus() {
        viewModelScope.launch {
            _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
            _isExtensionInstalled.value = checkExtensionInstalled()
            initializeJavetStatus()
        }
    }

    private fun checkExtensionInstalled(): Boolean = runCatching {
        application.packageManager.getApplicationInfo(EXTENSION_PACKAGE_NAME, 0)
        true
    }.getOrDefault(false)

    private fun initializeJavetStatus() {
        if (!_isExtensionInstalled.value) {
            _isJavetLoaded.value = false; return
        }
        NativeLibraryManager.initialize(application)
        _isJavetLoaded.value = if (!NativeLibraryManager.isLibraryAvailable(JAVET_LIB_NAME)) {
            NativeLibraryManager.extractAllLibraries()[JAVET_LIB_NAME] == true
        } else true
    }

    fun refreshExtensionStatus() {
        viewModelScope.launch {
            _isExtensionInstalled.value = checkExtensionInstalled()
            initializeJavetStatus()
        }
    }

    fun setSelectedPanelType(panelType: Int) {
        selectedPanelType.set(panelType)
    }

    fun setPanelOpenMode(mode: LinkOpenMode) = panelOpenMode.set(mode)
    fun setExitUiWhenBackground(enabled: Boolean) = exitUiWhenBackground.set(enabled)

    fun downloadSubStoreFrontend() {
        launchResourceDownload(
            loadingState = _isDownloadingSubStoreFrontend,
            successMessage = MLang.Feature.SubStore.FrontendDownloadSuccess,
            failureMessage = MLang.Feature.SubStore.FrontendDownloadFailed,
        ) {
            SubStorePaths.ensureStructure()
            SubStorePaths.frontendDir.apply { if (!exists()) mkdirs() }
            downloadClient.downloadAndExtract(
                url = "https://github.com/sub-store-org/Sub-Store-Front-End/releases/latest/download/dist.zip",
                targetDir = SubStorePaths.frontendDir,
            )
        }
    }

    fun downloadSubStoreBackend() {
        launchResourceDownload(
            loadingState = _isDownloadingSubStoreBackend,
            successMessage = MLang.Feature.SubStore.BackendDownloadSuccess,
            failureMessage = MLang.Feature.SubStore.BackendDownloadFailed,
        ) {
            SubStorePaths.ensureStructure()
            SubStorePaths.backendDir.apply { if (!exists()) mkdirs() }
            downloadClient.download(
                url = "https://github.com/sub-store-org/Sub-Store/releases/latest/download/sub-store.bundle.js",
                targetFile = SubStorePaths.backendBundle,
            )
        }
    }

    fun downloadSubStoreAll() {
        viewModelScope.launch {
            if (_isDownloadingSubStoreFrontend.value || _isDownloadingSubStoreBackend.value) return@launch
            downloadSubStoreFrontend()
            while (_isDownloadingSubStoreFrontend.value) {
                PollingTimers.awaitTick(
                    PollingTimerSpecs.dynamic(
                        name = "substore_frontend_download_wait",
                        intervalMillis = 200L,
                        initialDelayMillis = 200L,
                    ),
                )
            }
            downloadSubStoreBackend()
        }
    }

    private fun showToast(msg: String) = showToastDialog(msg)

    private fun launchResourceDownload(
        loadingState: MutableStateFlow<Boolean>,
        successMessage: String,
        failureMessage: String,
        action: suspend () -> Boolean,
    ) {
        if (loadingState.value) return
        viewModelScope.launch {
            loadingState.value = true
            runCatching {
                val success = action()
                showToast(if (success) successMessage else failureMessage)
                if (success) {
                    _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
                }
            }.onFailure { error ->
                showToast(MLang.Feature.SubStore.DownloadError.format(error.message ?: MLang.Util.Error.UnknownError))
            }
            loadingState.value = false
        }
    }

    private fun setupAutoCloseTimer() {
        cancelAutoCloseTimer()
        val mode = _autoCloseMode.value
        mode.minutes?.let { minutes ->
            autoCloseJob = viewModelScope.launch {
                val timeoutMillis = minutes * 60 * 1000L
                PollingTimers.awaitTick(
                    PollingTimerSpecs.dynamic(
                        name = "substore_auto_close",
                        intervalMillis = timeoutMillis,
                        initialDelayMillis = timeoutMillis,
                    ),
                )
                showToast(MLang.Feature.ServiceStatus.AutoClosed)
                stopService()
            }
        }
    }

    private fun cancelAutoCloseTimer() {
        autoCloseJob?.cancel()
        autoCloseJob = null
    }
}
