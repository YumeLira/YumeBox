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

package com.github.yumelira.yumebox.domain.facade

import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.ProfilesStore
import kotlinx.coroutines.flow.StateFlow

class ProfilesRepository(
    private val profilesStore: ProfilesStore
) {
    val profiles: StateFlow<List<Profile>> = profilesStore.profiles
    val enabledProfile: StateFlow<Profile?> = profilesStore.enabledProfile
    val recommendedProfile: StateFlow<Profile?> = profilesStore.recommendedProfile

    fun getAllProfiles(): List<Profile> = profilesStore.getAllProfiles()
    fun getEnabledProfile(): Profile? = profilesStore.getEnabledProfile()
    fun getRecommendedProfile(): Profile? = profilesStore.getRecommendedProfile()
    fun hasEnabledProfile(): Boolean = profilesStore.hasEnabledProfile()
}
