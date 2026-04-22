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
data class ProfileBinding(
    val profileId: String,
    val overrideIds: List<String> = emptyList(),
) {
    companion object {

        fun disabled(profileId: String): ProfileBinding {
            return ProfileBinding(
                profileId = profileId,
                overrideIds = emptyList(),
            )
        }

        fun withOverrides(profileId: String, overrideIds: List<String>): ProfileBinding {
            return ProfileBinding(
                profileId = profileId,
                overrideIds = overrideIds,
            )
        }

        fun withOverride(profileId: String, overrideId: String): ProfileBinding {
            return ProfileBinding(
                profileId = profileId,
                overrideIds = listOf(overrideId),
            )
        }
    }

    fun addOverride(overrideId: String, index: Int? = null): ProfileBinding {
        if (overrideIds.contains(overrideId)) return this

        val newList = if (index != null) {
            overrideIds.toMutableList().apply {
                add(index.coerceAtMost(size), overrideId)
            }
        } else {
            overrideIds + overrideId
        }

        return copy(overrideIds = newList)
    }

    fun removeOverride(overrideId: String): ProfileBinding {
        return copy(overrideIds = overrideIds - overrideId)
    }

    fun moveOverride(fromIndex: Int, toIndex: Int): ProfileBinding {
        if (fromIndex !in overrideIds.indices || toIndex !in overrideIds.indices) return this
        if (fromIndex == toIndex) return this

        val newList = overrideIds.toMutableList()
        val item = newList.removeAt(fromIndex)
        newList.add(toIndex, item)

        return copy(overrideIds = newList)
    }

    fun setOverrides(overrideIds: List<String>): ProfileBinding {
        return copy(overrideIds = overrideIds)
    }

    fun clearOverrides(): ProfileBinding {
        return copy(overrideIds = emptyList())
    }
}
