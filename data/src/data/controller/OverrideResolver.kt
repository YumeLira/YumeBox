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



package com.github.yumelira.yumebox.data.controller

import com.github.yumelira.yumebox.core.model.OverrideSpec
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import com.github.yumelira.yumebox.data.store.ProfileBindingProvider
import com.github.yumelira.yumebox.data.model.OverrideMetadata
import com.github.yumelira.yumebox.data.model.ProfileBinding
import java.util.*

class OverrideResolver(
    private val configStore: OverrideConfigStore,
    private val bindingProvider: ProfileBindingProvider,
) {

    suspend fun resolveIds(profileId: UUID): List<String> {
        val binding = bindingProvider.getBinding(profileId.toString())
        return resolveBindingIds(binding)
    }

    suspend fun resolveIds(profileId: String): List<String> {
        val binding = bindingProvider.getBinding(profileId)
        return resolveBindingIds(binding)
    }

    suspend fun resolveSpecs(overrideIds: List<String>): List<OverrideSpec> {
        return resolveOrderedSpecs(overrideIds)
    }

    suspend fun getProfilesUsingOverride(overrideId: String): List<String> {
        return bindingProvider.getProfilesUsingOverride(overrideId)
    }

    suspend fun isOverrideInUse(overrideId: String): Boolean {
        return bindingProvider.isOverrideInUse(overrideId)
    }

    suspend fun getOverrideUsageCount(overrideId: String): Int {
        return bindingProvider.getOverrideUsageCount(overrideId)
    }

    suspend fun getBinding(profileId: String) = bindingProvider.getBinding(profileId)

    suspend fun bindOverride(profileId: String, overrideId: String, index: Int? = null) {
        bindingProvider.addOverride(profileId, overrideId, index)
    }

    suspend fun setOverrides(profileId: String, overrideIds: List<String>) {
        val binding = bindingProvider.getBinding(profileId)
        if (binding != null) {
            bindingProvider.setBinding(binding.setOverrides(overrideIds))
        } else {
            bindingProvider.setBinding(ProfileBinding.withOverrides(profileId, overrideIds))
        }
    }

    suspend fun clearBinding(profileId: String) {
        bindingProvider.removeBinding(profileId)
    }

    private suspend fun resolveBindingIds(binding: ProfileBinding?): List<String> {
        if (binding == null) {
            return emptyList()
        }
        return buildList {
            binding.overrideIds.forEach { overrideId ->
                if (isLegacyPresetOverrideId(overrideId) || OverrideConfigStore.isInternalRuntimeConfig(overrideId)) {
                    return@forEach
                }
                if (configStore.getConfigFilePath(overrideId) != null) {
                    add(overrideId)
                }
            }
        }.distinct()
    }

    private suspend fun resolveOrderedSpecs(
        overrideIds: List<String>,
    ): List<OverrideSpec> {
        return overrideIds.mapNotNull { overrideId ->
            val config = configStore.getById(overrideId) ?: return@mapNotNull null
            val file = configStore.getConfigFilePath(overrideId) ?: return@mapNotNull null
            OverrideSpec(
                path = file.absolutePath,
                ext = config.contentType.extension,
            )
        }
    }

    private fun isLegacyPresetOverrideId(overrideId: String): Boolean {
        return overrideId.startsWith(OverrideMetadata.LEGACY_SYSTEM_PREFIX)
    }
}
