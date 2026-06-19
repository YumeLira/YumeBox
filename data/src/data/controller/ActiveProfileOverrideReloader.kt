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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.data.controller

import com.github.yumelira.yumebox.data.store.ProfileBindingProvider
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import timber.log.Timber

class ActiveProfileOverrideReloader(
    private val queryActiveProfile: suspend () -> Profile?,
    private val bindingProvider: ProfileBindingProvider,
    private val overrideService: OverrideService,
) {
    suspend fun reapplyActiveProfileOverride(): Boolean {
        val activeProfile = queryActiveProfile() ?: return true
        val applied = overrideService.applyOverride(activeProfile.uuid.toString())
        if (!applied) {
            Timber.e("Failed to reapply active profile override: profile=%s", activeProfile.uuid)
        }
        return applied
    }

    suspend fun reapplyActiveProfileIfUsingOverride(overrideId: String): Boolean {
        val activeProfile = queryActiveProfile() ?: return true
        val binding = bindingProvider.getBinding(activeProfile.uuid.toString()) ?: return true
        if (!binding.overrideIds.contains(overrideId)) {
            return true
        }

        val applied = overrideService.applyOverride(activeProfile.uuid.toString())
        if (!applied) {
            Timber.e(
                "Failed to reapply active profile override after config change: profile=%s override=%s",
                activeProfile.uuid,
                overrideId,
            )
        }
        return applied
    }

    suspend fun isActiveProfileUsingOverride(overrideId: String): Boolean {
        val activeProfile = queryActiveProfile() ?: return false
        val binding = bindingProvider.getBinding(activeProfile.uuid.toString()) ?: return false
        return binding.overrideIds.contains(overrideId)
    }
}
