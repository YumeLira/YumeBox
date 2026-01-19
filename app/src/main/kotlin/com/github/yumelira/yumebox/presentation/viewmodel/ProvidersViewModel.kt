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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ProvidersViewModel(
    private val clashManager: ClashManager
) : ViewModel() {

    private val _providers = MutableStateFlow<List<Provider>>(emptyList())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    val isRunning: StateFlow<Boolean> = clashManager.isRunning

    fun refreshProviders() {
        viewModelScope.launch {
            if (!clashManager.isRunning.value) {
                _providers.value = emptyList()
                return@launch
            }

            try {
                _uiState.update { it.copy(isLoading = true) }
                val providerList = Clash.queryProviders()
                _providers.value = providerList.sorted()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "获取外部资源失败: ${e.message ?: "Unknown error"}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateProvider(provider: Provider) {
        val providerKey = "${provider.type}_${provider.name}"
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(updatingProviders = it.updatingProviders + providerKey) }
                Clash.updateProvider(provider.type, provider.name).await()
                refreshProviders()
                _uiState.update { it.copy(message = "${provider.name} 更新成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "更新失败: ${e.message ?: "Unknown error"}") }
            } finally {
                _uiState.update { it.copy(updatingProviders = it.updatingProviders - providerKey) }
            }
        }
    }

    fun updateAllProviders() {
        viewModelScope.launch {
            val httpProviders = _providers.value.filter { it.vehicleType == Provider.VehicleType.HTTP }
            if (httpProviders.isEmpty()) return@launch

            try {
                _uiState.update { it.copy(isUpdatingAll = true) }
                val providerKeys = httpProviders.map { "${it.type}_${it.name}" }.toSet()
                _uiState.update { it.copy(updatingProviders = providerKeys) }

                val failedProviders = mutableListOf<String>()
                httpProviders.forEach { provider ->
                    try {
                        Clash.updateProvider(provider.type, provider.name).await()
                    } catch (e: Exception) {
                        failedProviders.add(provider.name)
                        Timber.e(e, "Failed to update provider: ${provider.name}")
                    }
                }

                refreshProviders()
                if (failedProviders.isEmpty()) {
                    _uiState.update { it.copy(message = "全部更新完成") }
                } else {
                    _uiState.update {
                        it.copy(
                            error = "更新失败: Failed providers: ${failedProviders.joinToString(", ")}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "更新失败: ${e.message ?: "Unknown error"}") }
            } finally {
                _uiState.update { it.copy(isUpdatingAll = false, updatingProviders = emptySet()) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun uploadProviderFile(context: Context, provider: Provider, uri: Uri) {
        val providerKey = "${provider.type}_${provider.name}"
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(updatingProviders = it.updatingProviders + providerKey) }

                withContext(Dispatchers.IO) {
                    if (provider.path.isBlank()) {
                        throw IllegalStateException("Provider path is empty")
                    }

                    val targetFile = File(provider.path)
                    targetFile.parentFile?.mkdirs()

                    // Validate URI and get file size
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot read file from uri: $uri")

                    // Check file size (limit to 50MB to prevent abuse)
                    val fileSize = inputStream.available()
                    if (fileSize > 50 * 1024 * 1024) {
                        inputStream.close()
                        throw IllegalStateException("File size exceeds 50MB limit")
                    }

                    inputStream.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                refreshProviders()
                _uiState.update { it.copy(message = "${provider.name} 上传成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "上传失败: ${e.message ?: "Unknown error"}") }
            } finally {
                _uiState.update { it.copy(updatingProviders = it.updatingProviders - providerKey) }
            }
        }
    }

    data class ProvidersUiState(
        val isLoading: Boolean = false,
        val isUpdatingAll: Boolean = false,
        val updatingProviders: Set<String> = emptySet(),
        val message: String? = null,
        val error: String? = null
    )
}
