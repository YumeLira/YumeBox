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

package com.github.yumelira.yumebox.screen.importconfig

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.FetchStatus
import com.github.yumelira.yumebox.core.presentation.AndroidContractStateViewModel
import com.github.yumelira.yumebox.core.presentation.LoadableState
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.config.TunProfileSync
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.service.remote.IFetchObserver
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID

class ImportConfigViewModel(
    application: Application,
    private val profilesRepository: ProfilesRepository,
    private val proxyFacade: ProxyFacade,
    private val tunProfileSync: TunProfileSync,
) : AndroidContractStateViewModel<ProfilesUiState, ImportConfigViewModel.ProfilesUiEffect>(
    application,
    ProfilesUiState(),
) {
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    val isRunning: StateFlow<Boolean> = proxyFacade.isRunning

    init {
        refreshProfiles()
    }

    fun refreshProfiles() {
        viewModelScope.launch {
            try {
                applyLoading(true)
                _profiles.value = profilesRepository.queryAllProfiles()
                _activeProfile.value = profilesRepository.queryActiveProfile()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to refresh lite profiles")
                showError(MLang.ProfilesVM.Message.UpdateFailed.format(error.message ?: "Unknown"))
            } finally {
                applyLoading(false)
            }
        }
    }

    fun createProfile(
        type: Profile.Type,
        name: String,
        source: String = "",
        interval: Long = 0L,
        fileUri: Uri? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            var createdUuid: UUID? = null
            try {
                applyLoading(true)
                val uuid = profilesRepository.createProfile(type, name, source)
                createdUuid = uuid
                _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.Preparing)

                if (type == Profile.Type.File && fileUri != null) {
                    copyFileToImportedDir(fileUri, uuid)
                }

                profilesRepository.updateProfile(uuid, IFetchObserver { status ->
                    _downloadProgress.value = status.toDownloadProgress()
                })
                _downloadProgress.value = DownloadProgress(
                    percent = 100,
                    message = MLang.ProfilesVM.Progress.ImportComplete,
                    isCompleted = true,
                )
                showMessage(MLang.ProfilesVM.Message.ProfileAdded.format(name))
                refreshProfiles()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to create lite profile")
                createdUuid?.let { uuid -> runCatching { profilesRepository.deleteProfile(uuid) } }
                refreshProfiles()
                showError(MLang.ProfilesVM.Message.AddFailed.format(error.message ?: "Unknown"))
                _downloadProgress.value = null
            } finally {
                applyLoading(false)
                onComplete?.invoke()
            }
        }
    }

    fun patchProfile(
        uuid: UUID,
        name: String,
        source: String,
        interval: Long,
        onComplete: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            try {
                applyLoading(true)
                profilesRepository.patchProfile(uuid, name, source, interval)
                showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(name))
                refreshProfiles()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to update lite profile")
                showError(MLang.ProfilesVM.Message.UpdateFailed.format(error.message ?: "Unknown"))
                _downloadProgress.value = null
            } finally {
                applyLoading(false)
                onComplete?.invoke()
            }
        }
    }

    fun updateProfile(
        uuid: UUID,
        onComplete: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            try {
                applyLoading(true)
                _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.Preparing)
                profilesRepository.updateProfile(uuid, IFetchObserver { status ->
                    _downloadProgress.value = status.toDownloadProgress()
                })
                _downloadProgress.value = DownloadProgress(
                    percent = 100,
                    message = MLang.ProfilesVM.Progress.ImportComplete,
                    isCompleted = true,
                )
                showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(uuid.toString()))
                refreshProfiles()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to update lite profile")
                showError(MLang.ProfilesVM.Message.UpdateFailed.format(error.message ?: "Unknown"))
                _downloadProgress.value = null
            } finally {
                applyLoading(false)
                onComplete?.invoke()
            }
        }
    }

    fun updateAllUrlProfiles(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val targets = _profiles.value.filter { it.type == Profile.Type.Url }
                for (profile in targets) {
                    _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.Preparing)
                    profilesRepository.updateProfile(profile.uuid, IFetchObserver { status ->
                        _downloadProgress.value = status.toDownloadProgress()
                    })
                }
                if (targets.isNotEmpty()) {
                    _downloadProgress.value = DownloadProgress(
                        percent = 100,
                        message = MLang.ProfilesVM.Progress.ImportComplete,
                        isCompleted = true,
                    )
                    showMessage(MLang.ProfilesPage.Action.UpdateAll)
                }
                refreshProfiles()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to update all lite url profiles")
                showError(MLang.ProfilesVM.Message.UpdateFailed.format(error.message ?: "Unknown"))
                _downloadProgress.value = null
            } finally {
                onComplete?.invoke()
            }
        }
    }

    fun deleteProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                applyLoading(true)
                profilesRepository.deleteProfile(uuid)
                showMessage(MLang.ProfilesVM.Message.ProfileDeleted)
                refreshProfiles()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to delete lite profile")
                showError(MLang.ProfilesVM.Message.DeleteFailed.format(error.message ?: "Unknown"))
            } finally {
                applyLoading(false)
            }
        }
    }

    fun reorderProfiles(from: Int, to: Int) {
        viewModelScope.launch {
            try {
                val current = _profiles.value
                if (from !in current.indices || to !in current.indices || from == to) return@launch
                val reordered = current.toMutableList()
                val moved = reordered.removeAt(from)
                reordered.add(to, moved)
                _profiles.value = reordered
                profilesRepository.reorderProfiles(reordered.map { it.uuid })
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                refreshProfiles()
            }
        }
    }

    fun toggleProfileEnabled(uuid: UUID, stopService: Boolean) {
        viewModelScope.launch {
            try {
                val profile = profilesRepository.queryProfileByUUID(uuid)
                    ?: error("Profile not found: $uuid")
                if (profile.active) {
                    profilesRepository.clearActiveProfile(profile)
                    tunProfileSync.syncActiveProfile()
                    if (stopService) {
                        proxyFacade.stopProxy()
                    }
                } else {
                    profilesRepository.setActiveProfile(uuid)
                    tunProfileSync.syncActiveProfile()
                    if (proxyFacade.isRunning.value) {
                        proxyFacade.startProxy(ProxyMode.Tun)
                    }
                }
                showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(profile.name))
                refreshProfiles()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to toggle lite profile")
                showError(MLang.ProfilesVM.Message.ToggleFailed.format(error.message ?: "Unknown"))
            }
        }
    }

    fun clearDownloadProgress() {
        _downloadProgress.value = null
    }

    fun clearError() {
        clearErrorState()
    }

    fun clearMessage() {
        clearMessageState()
    }

    private suspend fun copyFileToImportedDir(uri: Uri, uuid: UUID) {
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val importedDir = File(context.filesDir, "imported/${uuid}")
            importedDir.mkdirs()
            val outputFile = File(importedDir, "config.yaml")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Failed to open file: $uri")
        }
    }

    private fun applyLoading(loading: Boolean) {
        super.setLoading(loading)
    }

    private fun showError(message: String) {
        postError(message, ProfilesUiEffect.ShowError(message))
    }

    private fun showMessage(message: String) {
        postMessage(message, ProfilesUiEffect.ShowMessage(message))
    }

    sealed interface ProfilesUiEffect {
        data class ShowMessage(val message: String) : ProfilesUiEffect
        data class ShowError(val message: String) : ProfilesUiEffect
    }
}

data class ProfilesUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    override val message: String? = null,
) : LoadableState<ProfilesUiState> {
    override fun withLoading(loading: Boolean): ProfilesUiState = copy(isLoading = loading)
    override fun withError(error: String?): ProfilesUiState = copy(error = error)
    override fun withMessage(message: String?): ProfilesUiState = copy(message = message)
}

data class DownloadProgress(
    val percent: Int?,
    val message: String,
    val isCompleted: Boolean = false,
)

private fun FetchStatus.toDownloadProgress(): DownloadProgress {
    val percent = if (max > 0) ((progress * 100) / max).coerceIn(0, 100) else null
    val detail = args.firstOrNull().orEmpty().trim()
    val message = when (action) {
        FetchStatus.Action.FetchConfiguration -> {
            if (percent == null || percent <= 5) MLang.ProfilesVM.Progress.Preparing
            else detail.ifBlank { MLang.ProfilesPage.Progress.Downloading }
        }
        FetchStatus.Action.FetchProviders -> if (detail.isNotBlank()) detail else ""
        FetchStatus.Action.Verifying -> detail.ifBlank { MLang.ProfilesVM.Progress.Verifying }
    }
    return DownloadProgress(percent = percent, message = message)
}
