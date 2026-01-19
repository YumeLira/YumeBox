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
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.clash.downloadProfile
import com.github.yumelira.yumebox.clash.exception.ConfigImportException
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.data.store.ProfileLinksStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*


class ProfilesViewModel(
    application: Application,
    private val profilesStore: ProfilesStore,
    profileLinksStorage: ProfileLinksStorage,
) : AndroidViewModel(application) {

    // 链接管理
    val linkOpenMode: Preference<LinkOpenMode> = profileLinksStorage.linkOpenMode
    val links: Preference<List<ProfileLink>> = profileLinksStorage.links
    val defaultLinkId: Preference<String> = profileLinksStorage.defaultLinkId

    fun setOpenMode(mode: LinkOpenMode) = linkOpenMode.set(mode)
    
    fun setDefaultLink(linkId: String) = defaultLinkId.set(linkId)

    fun addLink(link: ProfileLink) = links.set(links.value + link)

    fun updateLink(linkId: String, name: String, url: String) {
        links.set(links.value.map { link ->
            if (link.id == linkId) link.copy(name = name, url = url)
            else link
        })
    }

    fun removeLink(linkId: String) {
        links.set(links.value.filterNot { it.id == linkId })
        // 如果删除的是默认链接,清空默认链接
        if (defaultLinkId.value == linkId) {
            defaultLinkId.set("")
        }
    }

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    val profiles: StateFlow<List<Profile>> = profilesStore.profiles

    // 防重复下载的profile ID集合
    private val downloadingProfiles = mutableSetOf<String>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // 立即清理孤立文件，无需延迟
            cleanupOrphanedFiles()
        }
    }

    private fun cleanupOrphanedFiles() {
        runCatching {
            val activeIds = profilesStore.profiles.value.map { it.id }.toSet()
            val importedDir = getApplication<Application>().filesDir.resolve("imported")
            if (!importedDir.exists() || !importedDir.isDirectory) return

            importedDir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && file.name !in activeIds -> {
                        file.deleteRecursively()
                    }

                    file.isDirectory && file.name in activeIds -> {
                        val cfg = java.io.File(file, "config.yaml")
                        if (cfg.exists() && cfg.length() <= 10) {
                            cfg.delete()
                        }
                        // 清理临时文件
                        file.listFiles()?.forEach { subFile ->
                            if (subFile.name != "config.yaml") {
                                subFile.delete()
                            }
                        }
                    }

                    file.isFile && (file.name.endsWith(".yaml") || file.name.endsWith(".yml")) -> {
                        file.delete()
                    }
                }
            }
        }.onFailure { timber.log.Timber.e(it, "cleanupOrphanedFiles failed") }
    }

    fun addProfile(profile: Profile) {
        viewModelScope.launch {
            runCatching {
                val maxOrder = profilesStore.profiles.value.maxOfOrNull { it.order } ?: -1
                val profileWithOrder = profile.copy(order = maxOrder + 1)
                profilesStore.addProfile(profileWithOrder)
                showMessage("配置已添加: ${profile.name}")
            }.onFailure { e ->
                timber.log.Timber.e(e, "addProfile failed")
                showError("添加配置失败: ${e.message}")
            }
        }
    }

    suspend fun downloadProfile(profile: Profile, saveToDb: Boolean = true): Profile? {
        if (profile.id in downloadingProfiles) {
            timber.log.Timber.w("Profile ${profile.id} 正在下载中，跳过重复下载")
            return null
        }

        if (profile.type != ProfileType.URL && profile.type != ProfileType.FILE) {
            showError("只有 URL 或 本地文件 类型的配置才需要下载")
            return null
        }

        val isUrl = profile.type == ProfileType.URL
        val remoteUrl = profile.remoteUrl
        if (isUrl && remoteUrl.isNullOrBlank()) {
            showError("订阅链接为空，无法下载")
            return null
        }

        downloadingProfiles.add(profile.id)

        return try {
            _downloadProgress.value = DownloadProgress(0, "准备下载...")

            val subscriptionInfo = if (isUrl) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        com.github.yumelira.yumebox.common.util.DownloadUtil.downloadWithSubscriptionInfo(
                            remoteUrl!!, java.io.File.createTempFile("temp_${profile.id}", ".yaml")
                        ).second
                    }.getOrNull()
                }
            } else null

            val result = downloadProfile(
                profile = profile,
                workDir = getApplication<Application>().filesDir.resolve("clash"),
                force = true,
                onProgress = { msg, progress ->
                    _downloadProgress.value = DownloadProgress(progress, msg)
                }
            )

            if (result.isSuccess) {
                _downloadProgress.value = DownloadProgress(100, "下载完成")
                val configFilePath = result.getOrThrow()
                val existingProfile = if (saveToDb) {
                    profilesStore.profiles.value.find { it.id == profile.id }
                } else null

                var updated = existingProfile?.copy(updatedAt = System.currentTimeMillis(), config = configFilePath)
                    ?: profile.copy(updatedAt = System.currentTimeMillis(), config = configFilePath)

                subscriptionInfo?.filename?.let { fileName ->
                    val nameWithoutExt = if (fileName.contains(".")) {
                        fileName.substringBeforeLast(".")
                    } else {
                        fileName
                    }

                    val defaultNames = "新配置"
                    if (updated.name.isBlank() || updated.name in defaultNames || updated.name.startsWith("temp_")) {
                        updated = updated.copy(name = nameWithoutExt)
                    }
                }

                subscriptionInfo?.let { info ->
                    updated = updated.copy(
                        provider = info.title ?: updated.provider,
                        expireAt = info.expire ?: updated.expireAt,
                        usedBytes = info.upload + info.download,
                        totalBytes = if (info.total > 0) info.total else updated.totalBytes,
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                }

                if (updated.lastUpdatedAt == null) {
                    updated = updated.copy(lastUpdatedAt = System.currentTimeMillis())
                }

                if (saveToDb) {
                    if (existingProfile != null) {
                        profilesStore.updateProfile(updated)
                    } else {
                        profilesStore.addProfile(updated)
                    }
                }
                updated
            } else {
                _downloadProgress.value = null
                val error = result.exceptionOrNull()
                val errorMsg = if (error is ConfigImportException) {
                    error.userFriendlyMessage
                } else {
                    error?.message ?: "下载失败"
                }
                showError(errorMsg)
                null
            }
        } catch (e: Exception) {
            _downloadProgress.value = null
            val errorMsg = if (e is ConfigImportException) {
                e.userFriendlyMessage
            } else {
                "下载失败: ${e.message}"
            }
            showError(errorMsg)
            null
        } finally {
            downloadingProfiles.remove(profile.id)
        }
    }


    fun clearDownloadProgress() {
        _downloadProgress.value = null
    }

    suspend fun importProfileFromFile(uri: Uri, name: String, saveToDb: Boolean = true): Profile? {
        return runCatching {
            setLoading(true)
            _downloadProgress.value = DownloadProgress(0, "准备导入文件...")
            val profileId = UUID.randomUUID().toString()
            val profileDir =
                java.io.File(getApplication<Application>().filesDir, "imported/$profileId").apply { mkdirs() }
            _downloadProgress.value = DownloadProgress(10, "正在复制文件...")

            val destFile = java.io.File(profileDir, "config.yaml")
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("无法读取文件")

            _downloadProgress.value = DownloadProgress(30, "正在验证配置...")
            // 注意：这里不设置 lastUpdatedAt，等验证成功后再设置
            val profile = Profile(
                id = profileId, name = name, type = ProfileType.FILE,
                config = destFile.absolutePath, createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = downloadProfile(
                profile = profile,
                workDir = getApplication<Application>().filesDir.resolve("clash"),
                force = true,
                onProgress = { msg, p ->
                    _downloadProgress.value = DownloadProgress(30 + (p * 0.7).toInt().coerceIn(0, 70), msg)
                }
            )

            if (result.isSuccess) {
                _downloadProgress.value = DownloadProgress(100, "导入完成")
                val configPath = result.getOrThrow()
                val updatedProfile = profile.copy(
                    config = configPath,
                    lastUpdatedAt = System.currentTimeMillis()
                )
                if (saveToDb) {
                    profilesStore.addProfile(updatedProfile)
                    showMessage("配置已导入: $name")
                }
                updatedProfile
            } else {
                withContext(Dispatchers.IO) {
                    profileDir.deleteRecursively()
                }
                _downloadProgress.value = null
                val error = result.exceptionOrNull()
                val errorMsg = if (error is ConfigImportException) {
                    error.userFriendlyMessage
                } else {
                    "导入失败: ${error?.message}"
                }
                showError(errorMsg)
                null
            }
        }.getOrElse { e ->
            _downloadProgress.value = null
            val errorMsg = if (e is ConfigImportException) {
                e.userFriendlyMessage
            } else {
                "导入配置失败: ${e.message}"
            }
            showError(errorMsg)
            null
        }.also { setLoading(false) }
    }

    fun removeProfile(profileId: String) {
        viewModelScope.launch {
            runCatching {
                profilesStore.removeProfile(profileId)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().filesDir.resolve("imported/$profileId")
                        .takeIf { it.exists() }?.deleteRecursively()
                }
                showMessage("配置已删除")
            }.onFailure { e -> timber.log.Timber.e(e, "removeProfile failed"); showError("删除配置失败: ${e.message}") }
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            runCatching {
                profilesStore.updateProfile(profile)
                showMessage("配置已更新: ${profile.name}")
            }.onFailure { e ->
                timber.log.Timber.e(e, "updateProfile failed")
                showError("更新配置失败: ${e.message}")
            }
        }
    }

    fun toggleProfileEnabled(profile: Profile, enabled: Boolean, onProfileEnabled: ((Profile) -> Unit)? = null) {
        viewModelScope.launch {
            runCatching {
                val profiles = profilesStore.profiles.value
                val updated = if (enabled) {
                    profiles.map { if (it.id == profile.id) it.copy(enabled = true) else it.copy(enabled = false) }
                } else {
                    profiles.map { if (it.id == profile.id) it.copy(enabled = false) else it }
                }
                updated.forEach { profilesStore.updateProfile(it) }
                if (enabled) {
                    profilesStore.updateLastUsedProfileId(profile.id)
                    onProfileEnabled?.invoke(profile.copy(enabled = true))
                }
            }.onFailure { e ->
                timber.log.Timber.e(e, "toggleProfileEnabled failed")
                showError("切换状态失败: ${e.message}")
            }
        }
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    private fun showError(error: String) = _uiState.update { it.copy(error = error) }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun reorderProfiles(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            runCatching {
                val currentList = profiles.value.toMutableList()
                if (fromIndex !in currentList.indices || toIndex !in currentList.indices) {
                    return@launch
                }
                
                val movedItem = currentList.removeAt(fromIndex)
                currentList.add(toIndex, movedItem)
                
                profilesStore.reorderProfiles(currentList)
            }.onFailure { e ->
                timber.log.Timber.e(e, "reorderProfiles failed")
                showError("排序失败: ${e.message}")
            }
        }
    }

    data class ConfigUiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )

    data class DownloadProgress(
        val progress: Int,
        val message: String
    )
}
