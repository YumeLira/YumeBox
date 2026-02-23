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
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.FetchStatus
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.data.store.ProfileLinksStorage
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.service.remote.IFetchObserver
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*

class ProfilesViewModel(
    application: Application,
    private val profilesRepository: ProfilesRepository,
    profileLinksStorage: ProfileLinksStorage
) : AndroidViewModel(application) {

    // 链接管理
    val linkOpenMode: Preference<LinkOpenMode> = profileLinksStorage.linkOpenMode
    val links: Preference<List<ProfileLink>> = profileLinksStorage.links
    val defaultLinkId: Preference<String> = profileLinksStorage.defaultLinkId

    fun setOpenMode(mode: LinkOpenMode) = linkOpenMode.set(mode)

    // 配置列表
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    // 激活的配置
    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    // UI 状态
    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    // 下载进度
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    init {
        refreshProfiles()
    }

    /**
     * 刷新配置列表
     */
    fun refreshProfiles() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val allProfiles = profilesRepository.queryAllProfiles()
                val active = profilesRepository.queryActiveProfile()
                
                _profiles.value = allProfiles
                _activeProfile.value = active
                
                Timber.d("Profiles refreshed: ${allProfiles.size} total, active=${active?.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh profiles")
                showError(MLang.ProfilesVM.Message.UpdateFailed.format(e.message ?: "Unknown"))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 创建新配置
     * @param type 配置类型
     * @param name 配置名称
     * @param source 配置来源（URL或文件URI）
     * @param interval 更新间隔（仅对订阅有效）
     * @param fileUri 文件URI（仅对文件类型有效，用于复制文件）
     */
    fun createProfile(
        type: Profile.Type,
        name: String,
        source: String = "",
        interval: Long = 0L,
        fileUri: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val uuid = profilesRepository.createProfile(type, name, source)

                _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.Preparing)

                val observer = IFetchObserver { status ->
                    val percent = if (status.max > 0) {
                        ((status.progress * 100) / status.max)
                    } else 0

                    _downloadProgress.value = DownloadProgress(
                        percent,
                        when (status.action) {
                            FetchStatus.Action.FetchConfiguration -> MLang.ProfilesVM.Progress.Preparing
                            FetchStatus.Action.FetchProviders -> MLang.ProfilesVM.Progress.Verifying
                            FetchStatus.Action.Verifying -> MLang.ProfilesVM.Progress.Verifying
                        }
                    )
                }

                // 对于文件类型，需要先复制文件到pending目录
                if (type == Profile.Type.File && fileUri != null) {
                    copyFileToPendingDir(fileUri, uuid)
                }

                profilesRepository.commitProfile(uuid, observer)
                _downloadProgress.value = DownloadProgress(100, MLang.ProfilesVM.Progress.ImportComplete)

                showMessage(MLang.ProfilesVM.Message.ProfileAdded.format(name))
                refreshProfiles()
                Timber.i("Profile created: $uuid")
            } catch (e: Exception) {
                Timber.e(e, "Failed to create profile")
                showError(MLang.ProfilesVM.Message.AddFailed.format(e.message ?: "Unknown"))
                _downloadProgress.value = null
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 复制文件到pending目录
     */
    private suspend fun copyFileToPendingDir(uri: Uri, uuid: UUID) {
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pendingDir = File(context.filesDir, "pending/${uuid}")
            pendingDir.mkdirs()

            val inputFile = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Failed to open file: $uri")

            val outputFile = File(pendingDir, "config.yaml")
            outputFile.outputStream().use { output ->
                inputFile.copyTo(output)
            }
            Timber.d("File copied to pending dir: ${outputFile.absolutePath}")
        }
    }

    /**
     * 克隆配置
     */
    fun cloneProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val newUuid = profilesRepository.cloneProfile(uuid)
                showMessage(MLang.ProfilesVM.Message.ProfileAdded.format("Clone"))
                refreshProfiles()
                Timber.i("Profile cloned: $uuid -> $newUuid")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clone profile")
                showError(MLang.ProfilesVM.Message.AddFailed.format(e.message ?: "Unknown"))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 删除配置
     */
    fun deleteProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                profilesRepository.deleteProfile(uuid)
                showMessage(MLang.ProfilesVM.Message.ProfileDeleted)
                refreshProfiles()
                Timber.i("Profile deleted: $uuid")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete profile")
                showError(MLang.ProfilesVM.Message.DeleteFailed.format(e.message ?: "Unknown"))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 激活配置
     */
    fun activateProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                profilesRepository.setActiveProfile(uuid)
                showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format("Active"))
                refreshProfiles()
                Timber.i("Profile activated: $uuid")
            } catch (e: Exception) {
                Timber.e(e, "Failed to activate profile")
                showError(MLang.ProfilesVM.Message.ToggleFailed.format(e.message ?: "Unknown"))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 更新配置（重新获取）
     */
    fun updateProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.Preparing)

                val observer = IFetchObserver { status ->
                    val percent = if (status.max > 0) {
                        ((status.progress * 100) / status.max)
                    } else 0

                    _downloadProgress.value = DownloadProgress(
                        percent,
                        when (status.action) {
                            FetchStatus.Action.FetchConfiguration -> MLang.ProfilesVM.Progress.Preparing
                            FetchStatus.Action.FetchProviders -> MLang.ProfilesVM.Progress.Verifying
                            FetchStatus.Action.Verifying -> MLang.ProfilesVM.Progress.Verifying
                        }
                    )
                }

                profilesRepository.updateProfile(uuid, observer)

                _downloadProgress.value = DownloadProgress(100, MLang.ProfilesVM.Progress.ImportComplete)
                showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(uuid.toString()))
                refreshProfiles()
                Timber.i("Profile updated: $uuid")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update profile")
                showError(MLang.ProfilesVM.Message.UpdateFailed.format(e.message ?: "Unknown"))
                _downloadProgress.value = null
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 修改配置元数据
     */
    fun patchProfile(uuid: UUID, name: String, source: String, interval: Long) {
        viewModelScope.launch {
            try {
                setLoading(true)
                profilesRepository.patchProfile(uuid, name, source, interval)
                showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(name))
                refreshProfiles()
                Timber.i("Profile patched: $uuid")
            } catch (e: Exception) {
                Timber.e(e, "Failed to patch profile")
                showError(MLang.ProfilesVM.Message.UpdateFailed.format(e.message ?: "Unknown"))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 从文件导入配置
     */
    fun importProfileFromFile(uri: Uri, name: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.ImportPreparing)
                
                // 创建 File 类型配置
                val uuid = profilesRepository.createProfile(
                    Profile.Type.File,
                    name,
                    uri.toString()
                )
                
                _downloadProgress.value = DownloadProgress(50, MLang.ProfilesVM.Progress.Verifying)
                
                // TODO: 实现文件复制到配置目录的逻辑
                // 当前简化实现，后续需要完善
                
                _downloadProgress.value = DownloadProgress(100, MLang.ProfilesVM.Progress.ImportComplete)
                showMessage(MLang.ProfilesVM.Message.ProfileImported.format(name))
                refreshProfiles()
                Timber.i("Profile imported from file: $uuid")
            } catch (e: Exception) {
                Timber.e(e, "Failed to import profile")
                showError(MLang.ProfilesVM.Message.ImportFailed.format(e.message ?: "Unknown"))
            } finally {
                setLoading(false)
                _downloadProgress.value = null
            }
        }
    }

    /**
     * 重新排序配置
     */
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
                Timber.d("Reorder profiles: $from -> $to")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reorder profiles")
                refreshProfiles()
            }
        }
    }

    /**
     * 切换配置启用状态
     * - 如果当前未激活，则激活此配置
     * - 如果当前已激活，则禁用此配置（激活另一个默认配置或不激活任何配置）
     */
    fun toggleProfileEnabled(uuid: UUID) {
        viewModelScope.launch {
            try {
                val profile = profilesRepository.queryProfileByUUID(uuid)
                    ?: error("Profile not found: $uuid")

                if (profile.active) {
                    // 当前已激活，点击后禁用（激活第一个其他配置或不激活任何配置）
                    val otherProfiles = profiles.value.filter { it.uuid != uuid }
                    if (otherProfiles.isNotEmpty()) {
                        // 激活第一个其他配置
                        profilesRepository.setActiveProfile(otherProfiles.first().uuid)
                    } else {
                        // 没有其他配置，保持当前状态
                        showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(profile.name))
                    }
                } else {
                    // 当前未激活，激活此配置
                    profilesRepository.setActiveProfile(uuid)
                    showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(profile.name))
                }
                refreshProfiles()
                Timber.d("Profile toggled: $uuid, active=${!profile.active}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle profile")
                showError(MLang.ProfilesVM.Message.ToggleFailed.format(e.message ?: "Unknown"))
            }
        }
    }

    fun clearDownloadProgress() {
        _downloadProgress.value = null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}

// UI State
data class ProfilesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

// Download Progress
data class DownloadProgress(
    val percent: Int,
    val message: String
)

