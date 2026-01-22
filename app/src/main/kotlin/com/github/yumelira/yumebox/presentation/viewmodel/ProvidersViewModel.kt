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
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.data.repository.ProvidersRepository
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProvidersViewModel(
    private val clashManager: ClashManager,
    private val providersRepository: ProvidersRepository
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

            _uiState.update { it.copy(isLoading = true) }
            val result = providersRepository.queryProviders()
            result.onSuccess { providerList ->
                _providers.value = providerList.sorted()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = MLang.Providers.Message.FetchFailed.format(e.message ?: "Unknown error"))
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateProvider(provider: Provider) {
        val providerKey = "${provider.type}_${provider.name}"
        viewModelScope.launch {
            _uiState.update { it.copy(updatingProviders = it.updatingProviders + providerKey) }
            val result = providersRepository.updateProvider(provider)
            result.onSuccess {
                refreshProviders()
                _uiState.update { it.copy(message = MLang.Providers.Message.UpdateSuccess.format(provider.name)) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = MLang.Providers.Message.UpdateFailed.format(e.message ?: "Unknown error"))
                }
            }
            _uiState.update { it.copy(updatingProviders = it.updatingProviders - providerKey) }
        }
    }

    fun updateAllProviders() {
        viewModelScope.launch {
            val httpProviders = _providers.value.filter { it.vehicleType == Provider.VehicleType.HTTP }
            if (httpProviders.isEmpty()) return@launch

            _uiState.update { it.copy(isUpdatingAll = true) }
            val providerKeys = httpProviders.map { "${it.type}_${it.name}" }.toSet()
            _uiState.update { it.copy(updatingProviders = providerKeys) }

            val result = providersRepository.updateAllProviders(httpProviders)
            result.onSuccess { updateResult ->
                refreshProviders()
                if (updateResult.failedProviders.isEmpty()) {
                    _uiState.update { it.copy(message = MLang.Providers.Message.AllUpdated) }
                } else {
                    _uiState.update {
                        it.copy(
                            error = MLang.Providers.Message.UpdateFailed.format(
                                "Failed providers: ${updateResult.failedProviders.joinToString(", ")}"
                            )
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = MLang.Providers.Message.UpdateFailed.format(e.message ?: "Unknown error"))
                }
            }

            _uiState.update { it.copy(isUpdatingAll = false, updatingProviders = emptySet()) }
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
            _uiState.update { it.copy(updatingProviders = it.updatingProviders + providerKey) }

            val result = providersRepository.uploadProviderFile(context, provider, uri)
            result.onSuccess {
                refreshProviders()
                _uiState.update { it.copy(message = MLang.Providers.Message.UploadSuccess.format(provider.name)) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = MLang.Providers.Message.UploadFailed.format(e.message ?: "Unknown error"))
                }
            }

            _uiState.update { it.copy(updatingProviders = it.updatingProviders - providerKey) }
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
