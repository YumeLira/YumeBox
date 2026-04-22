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

import com.github.yumelira.yumebox.data.model.ProfileBinding
import kotlinx.coroutines.flow.Flow

interface ProfileBindingProvider {

    suspend fun getBinding(profileId: String): ProfileBinding?

    fun getBindingFlow(profileId: String): Flow<ProfileBinding?>

    suspend fun setBinding(binding: ProfileBinding)

    suspend fun removeBinding(profileId: String)

    suspend fun getAllBindings(): List<ProfileBinding>

    fun getAllBindingsFlow(): Flow<List<ProfileBinding>>

    suspend fun getProfilesUsingOverride(overrideId: String): List<String>

    suspend fun isOverrideInUse(overrideId: String): Boolean

    suspend fun getOverrideUsageCount(overrideId: String): Int

    suspend fun addOverride(profileId: String, overrideId: String, index: Int? = null)

    suspend fun removeOverride(profileId: String, overrideId: String)

    suspend fun removeOverrideFromAllBindings(overrideId: String)

    suspend fun clearOverrides(profileId: String)
}
