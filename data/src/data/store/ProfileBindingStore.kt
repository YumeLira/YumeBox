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



package com.github.yumelira.yumebox.data.store

import android.content.Context
import com.github.yumelira.yumebox.core.util.YamlCodec
import com.github.yumelira.yumebox.data.model.MetadataIndex
import com.github.yumelira.yumebox.data.model.OverrideMetadata
import com.github.yumelira.yumebox.data.model.ProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ProfileBindingStore(
    context: Context,
) : ProfileBindingProvider {
    private val metadataFile = File(context.filesDir, "overrides/metadata.yaml")

    private val bindingsStateFlow = MutableStateFlow<Map<String, ProfileBinding>>(emptyMap())

    init {
        bindingsStateFlow.value = loadBindings()
    }

    override suspend fun getBinding(profileId: String): ProfileBinding? = withContext(Dispatchers.IO) {
        loadBindings()[profileId]
    }

    override fun getBindingFlow(profileId: String): Flow<ProfileBinding?> {
        return bindingsStateFlow.asStateFlow().map { bindings ->
            bindings[profileId]
        }
    }

    override suspend fun setBinding(binding: ProfileBinding) = withContext(Dispatchers.IO) {
        val map = loadBindings().toMutableMap()
        map[binding.profileId] = binding
        saveBindings(map)
    }

    override suspend fun removeBinding(profileId: String) = withContext(Dispatchers.IO) {
        val map = loadBindings().toMutableMap()
        map.remove(profileId)
        saveBindings(map)
    }

    override suspend fun getAllBindings(): List<ProfileBinding> = withContext(Dispatchers.IO) {
        loadBindings().values.toList()
    }

    override fun getAllBindingsFlow(): Flow<List<ProfileBinding>> {
        return bindingsStateFlow.asStateFlow().map { bindings ->
            bindings.values.toList()
        }
    }

    override suspend fun getProfilesUsingOverride(overrideId: String): List<String> = withContext(Dispatchers.IO) {
        loadBindings().values
            .filter { binding ->
                isOverrideApplied(binding, overrideId)
            }
            .map { it.profileId }
    }

    override suspend fun isOverrideInUse(overrideId: String): Boolean = withContext(Dispatchers.IO) {
        loadBindings().values.any { binding ->
            isOverrideApplied(binding, overrideId)
        }
    }

    override suspend fun getOverrideUsageCount(overrideId: String): Int = withContext(Dispatchers.IO) {
        loadBindings().values.count { binding ->
            isOverrideApplied(binding, overrideId)
        }
    }

    override suspend fun addOverride(profileId: String, overrideId: String, index: Int?) {
        val existing = getBinding(profileId)
        val binding = existing?.addOverride(overrideId, index)
            ?: ProfileBinding.withOverride(profileId, overrideId)
        setBinding(binding)
    }

    override suspend fun removeOverride(profileId: String, overrideId: String) {
        val existing = getBinding(profileId) ?: return
        setBinding(existing.removeOverride(overrideId))
    }

    override suspend fun removeOverrideFromAllBindings(overrideId: String) = withContext(Dispatchers.IO) {
        val currentIndex = loadMetadataIndex()
        val updatedIndex = currentIndex.removeOverrideFromProfileChains(overrideId)
        if (updatedIndex != currentIndex) {
            saveMetadataIndex(updatedIndex)
            bindingsStateFlow.value = updatedIndex.profileChains
        }
    }

    override suspend fun clearOverrides(profileId: String) {
        val existing = getBinding(profileId) ?: return
        setBinding(existing.clearOverrides())
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val index = loadMetadataIndex()
        saveMetadataIndex(index.copy(profileChains = emptyMap()))
    }

    suspend fun setOverrides(profileId: String, overrideIds: List<String>) {
        val existing = getBinding(profileId)
        val binding = if (existing != null) {
            existing.setOverrides(overrideIds)
        } else {
            ProfileBinding.withOverrides(profileId, overrideIds)
        }
        setBinding(binding)
    }

    suspend fun moveOverride(profileId: String, fromIndex: Int, toIndex: Int) {
        val existing = getBinding(profileId) ?: return
        setBinding(existing.moveOverride(fromIndex, toIndex))
    }

    private fun loadBindings(): Map<String, ProfileBinding> {
        return try {
            loadMetadataIndex().profileChains
        } catch (error: Exception) {
            Timber.w(error, "Failed to load bindings from metadata.yaml, returning empty map")
            emptyMap()
        }
    }

    private fun saveBindings(map: Map<String, ProfileBinding>) {
        val index = loadMetadataIndex().copy(profileChains = map)
        saveMetadataIndex(index)
        bindingsStateFlow.value = map
    }

    private fun loadMetadataIndex(): MetadataIndex {
        if (!metadataFile.exists()) return MetadataIndex()
        val index = runCatching {
            YamlCodec.decode(MetadataIndex.serializer(), metadataFile.readText())
        }.getOrElse { error ->
            Timber.w(error, "Failed to decode override metadata index")
            MetadataIndex()
        }
        val sanitized = sanitizeMetadataIndex(index)
        if (sanitized != index) {
            saveMetadataIndex(sanitized)
        }
        return sanitized
    }

    private fun saveMetadataIndex(index: MetadataIndex) {
        metadataFile.parentFile?.mkdirs()
        metadataFile.writeText(YamlCodec.encode(MetadataIndex.serializer(), index))
    }

    private fun sanitizeMetadataIndex(index: MetadataIndex): MetadataIndex {
        return index.copy(
            profileChains = index.profileChains.mapValues { (_, binding) ->
                binding.copy(
                    overrideIds = binding.overrideIds.filterNot(::isLegacyPresetOverrideId),
                )
            },
        )
    }

    private fun isLegacyPresetOverrideId(overrideId: String): Boolean {
        return overrideId.startsWith(OverrideMetadata.LEGACY_SYSTEM_PREFIX)
    }

    private fun isOverrideApplied(binding: ProfileBinding, overrideId: String): Boolean {
        return binding.overrideIds.contains(overrideId)
    }

}
