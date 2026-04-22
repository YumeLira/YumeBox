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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.controller.ActiveProfileOverrideReloader
import com.github.yumelira.yumebox.data.controller.OverrideResolver
import com.github.yumelira.yumebox.data.model.OverrideConfig
import com.github.yumelira.yumebox.data.model.OverrideContentType
import com.github.yumelira.yumebox.data.model.OverrideMetadata
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import com.github.yumelira.yumebox.data.store.ProfileBindingProvider
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class OverrideConfigViewModel(
    private val configRepo: OverrideConfigStore,
    private val resolver: OverrideResolver,
    private val bindingProvider: ProfileBindingProvider,
    private val activeProfileOverrideReloader: ActiveProfileOverrideReloader,
) : ViewModel() {

    companion object {
        private const val TAG = "OverrideConfigViewModel"
    }

    private val _configs = MutableStateFlow<List<OverrideConfig>>(emptyList())
    val configs: StateFlow<List<OverrideConfig>> = _configs.asStateFlow()

    private val _userConfigs = MutableStateFlow<List<OverrideConfig>>(emptyList())
    val userConfigs: StateFlow<List<OverrideConfig>> = _userConfigs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _usageCountMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usageCountMap: StateFlow<Map<String, Int>> = _usageCountMap.asStateFlow()

    private val _pendingRevealConfigId = MutableStateFlow<String?>(null)
    val pendingRevealConfigId: StateFlow<String?> = _pendingRevealConfigId.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            bindingProvider.getAllBindingsFlow().collectLatest {
                loadUsageCounts()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val users = configRepo.getUserConfigs()
                _userConfigs.value = users
                _configs.value = users
                loadUsageCounts()
            } catch (error: Exception) {
                Timber.tag(TAG).e(error, "Failed to load overrides")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getConfigById(id: String): OverrideConfig? {
        return _configs.value.find { it.id == id }
    }

    fun getConfigContent(configId: String): String? {
        return configRepo.getConfigContent(configId)
    }

    fun saveConfigContent(configId: String, content: String): Boolean {
        val saved = configRepo.saveConfigContent(configId, content)
        if (!saved) return false

        viewModelScope.launch {
            activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(configId)
            refresh()
        }
        return true
    }

    fun createConfig(
        name: String,
        description: String? = null,
        contentType: OverrideContentType,
    ) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val config = OverrideConfig(
                    id = OverrideMetadata.generateId(),
                    name = name,
                    description = description,
                    contentType = contentType,
                    content = "",
                    createdAt = now,
                    updatedAt = now,
                )
                configRepo.save(config)
                _pendingRevealConfigId.value = config.id
                refresh()
            }.onFailure { error ->
                Timber.tag(TAG).e(error, "Failed to create override")
            }
        }
    }

    fun deleteConfig(id: String) {
        viewModelScope.launch {
            runCatching {
                val shouldResyncRuntime = activeProfileOverrideReloader.isActiveProfileUsingOverride(id)
                val deleted = configRepo.delete(id)
                if (deleted && shouldResyncRuntime) {
                    activeProfileOverrideReloader.reapplyActiveProfileOverride()
                }
                refresh()
            }.onFailure { error ->
                Timber.tag(TAG).e(error, "Failed to delete override")
            }
        }
    }

    fun duplicateConfig(id: String) {
        viewModelScope.launch {
            runCatching {
                val duplicated = configRepo.duplicate(id)
                if (duplicated != null) {
                    _pendingRevealConfigId.value = duplicated.id
                }
                refresh()
            }.onFailure { error ->
                Timber.tag(TAG).e(error, "Failed to duplicate override")
            }
        }
    }

    fun reorderUserConfigs(
        fromIndex: Int,
        toIndex: Int,
    ) {
        viewModelScope.launch {
            val currentConfigs = _userConfigs.value
            if (fromIndex !in currentConfigs.indices || fromIndex == toIndex) return@launch

            val reorderedConfigs = currentConfigs.toMutableList().also { configs ->
                val moving = configs.removeAt(fromIndex)
                configs.add(toIndex.coerceIn(0, configs.size), moving)
            }
            _userConfigs.value = reorderedConfigs
            _configs.value = reorderedConfigs

            runCatching {
                configRepo.reorderUserConfigs(reorderedConfigs.map(OverrideConfig::id))
            }.onFailure { error ->
                Timber.tag(TAG).e(error, "Failed to reorder overrides")
            }
            refresh()
        }
    }

    fun importConfig(
        content: String,
        sourceName: String?,
    ): Result<OverrideConfig> {
        val contentType = OverrideContentType.fromFileName(sourceName)
            ?: return Result.failure(IllegalArgumentException(MLang.Override.Import.Failed.format("仅支持 YAML 或 JS")))
        if (contentType == OverrideContentType.JavaScript && content.isBlank()) {
            return Result.failure(IllegalArgumentException(MLang.Override.Import.Failed.format("JS 文件为空")))
        }

        val config = OverrideConfig(
            id = OverrideMetadata.generateId(),
            name = normalizeImportedConfigSourceName(sourceName) ?: MLang.Override.Save.ImportDefaultName,
            description = null,
            contentType = contentType,
            content = content,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )

        viewModelScope.launch {
            runCatching {
                configRepo.save(config)
                _pendingRevealConfigId.value = config.id
                refresh()
            }.onFailure { error ->
                Timber.tag(TAG).e(error, "Failed to import override")
            }
        }
        return Result.success(config)
    }

    suspend fun isConfigInUse(id: String): Boolean {
        return resolver.isOverrideInUse(id)
    }

    fun consumePendingRevealConfig(configId: String) {
        if (_pendingRevealConfigId.value == configId) {
            _pendingRevealConfigId.value = null
        }
    }

    private suspend fun loadUsageCounts() {
        val countMap = mutableMapOf<String, Int>()
        _configs.value.forEach { config ->
            countMap[config.id] = resolver.getOverrideUsageCount(config.id)
        }
        _usageCountMap.value = countMap
    }
}

internal fun normalizeImportedConfigSourceName(sourceName: String?): String? {
    var normalizedName = sourceName
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: return null

    val removableSuffixes = listOf(".yaml", ".yml", ".js")
    while (true) {
        val matchedSuffix = removableSuffixes.firstOrNull { suffix ->
            normalizedName.length > suffix.length && normalizedName.endsWith(suffix, ignoreCase = true)
        } ?: break
        normalizedName = normalizedName.dropLast(matchedSuffix.length).trimEnd()
    }

    return normalizedName.takeIf(String::isNotBlank)
}
