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



package com.github.yumelira.yumebox.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OverrideMetadata(
    val id: String,
    val name: String,
    val description: String? = null,
    val contentType: OverrideContentType = OverrideContentType.Yaml,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Long = 0L,
) {
    companion object {
        const val ID_PREFIX = "cfg-"
        const val LEGACY_SYSTEM_PREFIX = "preset-"

        fun generateId(): String = "$ID_PREFIX${System.currentTimeMillis()}-${(1000..9999).random()}"

        fun create(
            name: String,
            description: String? = null,
            contentType: OverrideContentType = OverrideContentType.Yaml,
        ): OverrideMetadata {
            val now = System.currentTimeMillis()
            return OverrideMetadata(
                id = generateId(),
                name = name,
                description = description,
                contentType = contentType,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun update(
        name: String = this.name,
        description: String? = this.description,
        contentType: OverrideContentType = this.contentType,
    ): OverrideMetadata {
        return copy(
            name = name,
            description = description,
            contentType = contentType,
            updatedAt = System.currentTimeMillis(),
            sortOrder = sortOrder,
        )
    }

    fun duplicateAsUser(): OverrideMetadata {
        val now = System.currentTimeMillis()
        return OverrideMetadata(
            id = generateId(),
            name = "$name (副本)",
            description = description,
            contentType = contentType,
            createdAt = now,
            updatedAt = now,
            sortOrder = 0L,
        )
    }
}

@Serializable
data class MetadataIndex(
    val configs: Map<String, OverrideMetadata> = emptyMap(),
    val profileChains: Map<String, ProfileBinding> = emptyMap(),
) {
    fun getById(id: String): OverrideMetadata? = configs[id]

    fun upsert(metadata: OverrideMetadata): MetadataIndex {
        return copy(configs = configs + (metadata.id to metadata))
    }

    fun remove(id: String): MetadataIndex {
        return copy(configs = configs - id)
    }

    fun removeOverrideFromProfileChains(overrideId: String): MetadataIndex {
        return copy(
            profileChains = profileChains.mapValues { (_, binding) ->
                binding.removeOverride(overrideId)
            },
        )
    }

    fun sanitizeProfileChains(
        predicate: (String) -> Boolean,
    ): MetadataIndex {
        return copy(
            profileChains = profileChains.mapValues { (_, binding) ->
                binding.copy(
                    overrideIds = binding.overrideIds.filter(predicate),
                )
            },
        )
    }

    fun sortedUserMetadata(): List<OverrideMetadata> {
        return configs.values
            .sortedWith(
                compareBy<OverrideMetadata> { if (it.sortOrder > 0L) 0 else 1 }
                    .thenBy { if (it.sortOrder > 0L) it.sortOrder else Long.MAX_VALUE }
                    .thenByDescending(OverrideMetadata::updatedAt)
                    .thenBy(OverrideMetadata::createdAt),
            )
    }

    fun nextUserSortOrder(): Long {
        return sortedUserMetadata()
            .maxOfOrNull(OverrideMetadata::sortOrder)
            ?.plus(1L)
            ?: 1L
    }

    fun normalizeUserSortOrders(): MetadataIndex {
        val updatedConfigs = configs.toMutableMap()
        var hasChanges = false

        sortedUserMetadata().forEachIndexed { index, metadata ->
            val normalizedOrder = index.toLong() + 1L
            if (metadata.sortOrder != normalizedOrder) {
                updatedConfigs[metadata.id] = metadata.copy(sortOrder = normalizedOrder)
                hasChanges = true
            }
        }

        return if (hasChanges) {
            copy(configs = updatedConfigs)
        } else {
            this
        }
    }

    fun getProfileChain(profileId: String): ProfileBinding? = profileChains[profileId]

    fun upsertProfileChain(binding: ProfileBinding): MetadataIndex {
        return copy(profileChains = profileChains + (binding.profileId to binding))
    }

    fun removeProfileChain(profileId: String): MetadataIndex {
        return copy(profileChains = profileChains - profileId)
    }
}
