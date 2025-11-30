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
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plus.yumeyuka.yumebox.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import plus.yumeyuka.yumebox.clash.manager.ClashManager
import plus.yumeyuka.yumebox.data.model.Profile
import plus.yumeyuka.yumebox.data.model.ProfileType
import plus.yumeyuka.yumebox.data.model.Subscription
import plus.yumeyuka.yumebox.data.store.ProfilesStore
import dev.oom_wg.purejoy.mlang.MLang
import java.io.IOException
import java.util.*

data class ProfileInfo(val profile: Profile, val subscription: Subscription? = null)

class ProfilesViewModel(
    application: Application,
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    val profiles: StateFlow<List<Profile>> = profilesStore.profiles

    init {
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(1000)
            cleanupOrphanedFiles()
        }
    }

    private suspend fun cleanupOrphanedFiles() {
        runCatching {
            val activeIds = profilesStore.profiles.value.map { it.id }.toSet()
            val importedDir = getApplication<Application>().filesDir.resolve("imported")
            if (!importedDir.exists() || !importedDir.isDirectory) return
            importedDir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && file.name !in activeIds -> file.deleteRecursively()
                    file.isDirectory && file.name in activeIds -> {
                        val cfg = java.io.File(file, "config.yaml")
                        if (cfg.exists() && cfg.length() <= 10) cfg.delete()
                    }
                }
            }
        }.onFailure { timber.log.Timber.e(it, "cleanupOrphanedFiles failed") }
    }

    fun addProfile(profile: Profile) {
        viewModelScope.launch {
            runCatching { profilesStore.addProfile(profile); showMessage(MLang.ProfilesVM.Message.ProfileAdded.format(profile.name)) }
                .onFailure { e -> timber.log.Timber.e(e, "addProfile failed"); showError(MLang.ProfilesVM.Message.AddFailed.format(e.message)) }
        }
    }

    suspend fun downloadProfile(profile: Profile, saveToDb: Boolean = true): Profile? {
        if (profile.type != ProfileType.URL && profile.type != ProfileType.FILE) {
            showError(MLang.ProfilesVM.Error.OnlyUrlOrFile)
            return null
        }
        val isUrl = profile.type == ProfileType.URL
        val remoteUrl = profile.remoteUrl
        if (isUrl && remoteUrl.isNullOrBlank()) {
            showError(MLang.ProfilesVM.Error.EmptyUrl); return null
        }

        return runCatching {
            _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.Preparing)
            val subscriptionInfo = if (isUrl) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        plus.yumeyuka.yumebox.common.util.DownloadUtil.downloadWithSubscriptionInfo(
                            remoteUrl!!, java.io.File.createTempFile("temp_${profile.id}", ".yaml")
                        ).second
                    }.getOrNull()
                }
            } else null

            val result = clashManager.downloadProfileOnly(
                profile = profile, forceDownload = true,
                onProgress = { msg, progress -> _downloadProgress.value = DownloadProgress(progress, msg) }
            )

            if (result.isSuccess) {
                _downloadProgress.value = DownloadProgress(100, MLang.ProfilesVM.Progress.DownloadComplete)
                val configFilePath = result.getOrThrow()
                var updated = if (saveToDb) {
                    (profilesStore.profiles.value.find { it.id == profile.id } ?: profile)
                        .copy(updatedAt = System.currentTimeMillis(), config = configFilePath)
                } else profile.copy(updatedAt = System.currentTimeMillis(), config = configFilePath)

                subscriptionInfo?.filename?.let { fileName ->
                    val nameWithoutExt = if (fileName.contains(".")) {
                        fileName.substringBeforeLast(".")
                    } else {
                        fileName
                    }

                    updated = updated.copy(name = nameWithoutExt)
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
                if (saveToDb) profilesStore.updateProfile(updated)
                updated
            } else {
                cleanupProfileDir(profile.id)
                _downloadProgress.value = null
                showError(MLang.ProfilesVM.Progress.DownloadFailed.format(result.exceptionOrNull()?.message))
                null
            }
        }.getOrElse { e ->
            cleanupProfileDir(profile.id)
            _downloadProgress.value = null
            timber.log.Timber.e(e, "downloadProfile failed")
            showError(MLang.ProfilesVM.Progress.DownloadFailed.format(e.message))
            null
        }
    }

    private suspend fun cleanupProfileDir(profileId: String) = withContext(Dispatchers.IO) {
        getApplication<Application>().filesDir.resolve("imported/$profileId").takeIf { it.exists() }
            ?.deleteRecursively()
    }

    fun clearDownloadProgress() {
        _downloadProgress.value = null
    }

    suspend fun importProfileFromFile(uri: Uri, name: String, saveToDb: Boolean = true): Profile? {
        return runCatching {
            setLoading(true)
            _downloadProgress.value = DownloadProgress(0, MLang.ProfilesVM.Progress.ImportPreparing)
            val profileId = UUID.randomUUID().toString()
            val profileDir =
                java.io.File(getApplication<Application>().filesDir, "imported/$profileId").apply { mkdirs() }
            _downloadProgress.value = DownloadProgress(10, MLang.ProfilesVM.Progress.CopyingFile)

            val destFile = java.io.File(profileDir, "config.yaml")
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException(MLang.ProfilesVM.Error.CannotReadFile)

            _downloadProgress.value = DownloadProgress(30, MLang.ProfilesVM.Progress.Verifying)
            val profile = Profile(
                id = profileId, name = name, type = ProfileType.FILE,
                config = destFile.absolutePath, createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(), lastUpdatedAt = System.currentTimeMillis()
            )

            val result = clashManager.downloadProfileOnly(
                profile = profile, forceDownload = true,
                onProgress = { msg, p -> _downloadProgress.value = DownloadProgress(30 + (p * 0.7).toInt(), msg) }
            )

            if (result.isSuccess) {
                _downloadProgress.value = DownloadProgress(100, MLang.ProfilesVM.Progress.ImportComplete)
                if (saveToDb) {
                    profilesStore.addProfile(profile); showMessage(MLang.ProfilesVM.Message.ProfileImported.format(name))
                }
                profile
            } else {
                profileDir.deleteRecursively()
                _downloadProgress.value = null
                showError(MLang.ProfilesVM.Progress.ImportFailed.format(result.exceptionOrNull()?.message))
                null
            }
        }.getOrElse { e ->
            _downloadProgress.value = null
            timber.log.Timber.e(e, "importProfileFromFile failed")
            showError(MLang.ProfilesVM.Message.ImportFailed.format(e.message))
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
                showMessage(MLang.ProfilesVM.Message.ProfileDeleted)
            }.onFailure { e -> timber.log.Timber.e(e, "removeProfile failed"); showError(MLang.ProfilesVM.Message.DeleteFailed.format(e.message)) }
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            runCatching { profilesStore.updateProfile(profile); showMessage(MLang.ProfilesVM.Message.ProfileUpdated.format(profile.name)) }
                .onFailure { e -> timber.log.Timber.e(e, "updateProfile failed"); showError(MLang.ProfilesVM.Message.UpdateFailed.format(e.message)) }
        }
    }

    fun exportProfile(profile: Profile): String = profile.config

    fun toggleProfileEnabled(profileId: String, enabled: Boolean, onProfileEnabled: ((Profile) -> Unit)? = null) {
        viewModelScope.launch {
            runCatching {
                val profiles = profilesStore.profiles.value
                val target = profiles.find { it.id == profileId } ?: throw Exception(MLang.ProfilesVM.Error.ProfileNotExist)
                val updated = if (enabled) {
                    profiles.map { if (it.id == profileId) it.copy(enabled = true) else it.copy(enabled = false) }
                } else {
                    profiles.map { if (it.id == profileId) it.copy(enabled = false) else it }
                }
                updated.forEach { profilesStore.updateProfile(it) }
                if (enabled) {
                    profilesStore.updateLastUsedProfileId(profileId)
                    onProfileEnabled?.invoke(target)
                }
            }.onFailure { e -> timber.log.Timber.e(e, "toggleProfileEnabled failed"); showError(MLang.ProfilesVM.Message.ToggleFailed.format(e.message)) }
        }
    }

    fun renewSubscription(profile: Profile) = showMessage(MLang.ProfilesVM.Message.RenewNotImplemented)

    fun updateProfileSubscriptionInfo(
        profileId: String, provider: String? = null, expireAt: Long? = null,
        usedBytes: Long = 0L, totalBytes: Long? = null
    ) {
        viewModelScope.launch {
            runCatching {
                val target = profilesStore.profiles.value.find { it.id == profileId }
                    ?: throw Exception(MLang.ProfilesVM.Error.ProfileNotExist)
                profilesStore.updateProfile(
                    target.copy(
                        provider = provider ?: target.provider, expireAt = expireAt ?: target.expireAt,
                        usedBytes = usedBytes, totalBytes = totalBytes ?: target.totalBytes,
                        lastUpdatedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                    )
                )
                showMessage(MLang.ProfilesVM.Message.SubscriptionUpdated)
            }.onFailure { e -> timber.log.Timber.e(e, "updateProfileSubscriptionInfo failed"); showError(MLang.ProfilesVM.Message.SubscriptionUpdateFailed.format(e.message)) }
        }
    }

    private fun setLoading(loading: Boolean) = _uiState.update { it.copy(isLoading = loading) }
    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }
    fun showError(error: String) = _uiState.update { it.copy(error = error) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    data class ConfigUiState(val isLoading: Boolean = false, val message: String? = null, val error: String? = null)
    data class DownloadProgress(val progress: Int, val message: String)
}
