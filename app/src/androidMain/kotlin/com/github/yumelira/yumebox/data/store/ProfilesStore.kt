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

package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.Profile
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfilesStore(
    mmkv: MMKV,
    private val scope: CoroutineScope
) : MMKVPreference(externalMmkv = mmkv) {

    private val _profiles: Preference<List<Profile>> by jsonListFlow(
        default = emptyList(),
        decode = { str -> decodeFromString(str) },
        encode = { value -> encodeToString(value) }
    )

    var lastUsedProfileId: String by str(default = "")

    val profiles: StateFlow<List<Profile>> = _profiles.state

    val enabledProfile: StateFlow<Profile?> = _profiles.state
        .map { list -> list.firstOrNull { it.enabled } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), getEnabledProfile())

    val recommendedProfile: StateFlow<Profile?> = _profiles.state
        .map { list -> list.firstOrNull { it.enabled } ?: list.firstOrNull() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), getRecommendedProfile())

    fun getAllProfiles(): List<Profile> = _profiles.value

    suspend fun addProfile(profile: Profile) {
        _profiles.add(profile)
    }

    suspend fun removeProfile(id: String) {
        _profiles.remove { it.id == id }
    }

    suspend fun updateProfile(profile: Profile) {
        _profiles.update({ it.id == profile.id }) { profile }
    }

    fun updateLastUsedProfileId(profileId: String) {
        lastUsedProfileId = profileId
    }

    fun getEnabledProfile(): Profile? = _profiles.value.firstOrNull { it.enabled }

    fun getRecommendedProfile(): Profile? = getEnabledProfile() ?: _profiles.value.firstOrNull()

    fun hasEnabledProfile(): Boolean = _profiles.value.any { it.enabled }
}
