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

package com.github.yumelira.yumebox.runtime.client

import android.content.Context
import com.github.yumelira.yumebox.remote.ServiceClient
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.remote.IFetchObserver
import timber.log.Timber
import java.util.*

/**
 * ProfilesRepository - profile management repository
 */
class ProfilesRepository(private val context: Context) {

    /**
     * Create a new profile
     * @param type Profile type
     * @param name Profile name
     * @param source Profile source (URL or file path)
     * @return UUID of created profile
     */
    suspend fun createProfile(
        type: Profile.Type,
        name: String,
        source: String = ""
    ): UUID {
        Timber.d("Creating profile: type=$type, name=$name")
        ServiceClient.connect(context)
        return ServiceClient.profile().create(type, name, source)
    }

    /**
     * Clone an existing profile
     * @param uuid UUID of profile to clone
     * @return UUID of cloned profile
     */
    suspend fun cloneProfile(uuid: UUID): UUID {
        Timber.d("Cloning profile: uuid=$uuid")
        ServiceClient.connect(context)
        return ServiceClient.profile().clone(uuid)
    }

    /**
     * Delete a profile
     * @param uuid UUID of profile to delete
     */
    suspend fun deleteProfile(uuid: UUID) {
        Timber.d("Deleting profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().delete(uuid)
    }

    /**
     * Query all profiles
     * @return List of all profiles
     */
    suspend fun queryAllProfiles(): List<Profile> {
        ServiceClient.connect(context)
        return ServiceClient.profile().queryAll()
    }

    /**
     * Query active profile
     * @return Active profile or null
     */
    suspend fun queryActiveProfile(): Profile? {
        ServiceClient.connect(context)
        return ServiceClient.profile().queryActive()
    }

    /**
     * Query profile by UUID
     * @param uuid Profile UUID
     * @return Profile or null if not found
     */
    suspend fun queryProfileByUUID(uuid: UUID): Profile? {
        ServiceClient.connect(context)
        return ServiceClient.profile().queryByUUID(uuid)
    }

    /**
     * Set active profile
     * @param uuid UUID of profile to activate
     */
    suspend fun setActiveProfile(uuid: UUID) {
        Timber.d("Setting active profile: uuid=$uuid")
        ServiceClient.connect(context)
        
        val profile = ServiceClient.profile().queryByUUID(uuid)
            ?: error("Profile not found: $uuid")

        // 允许“立即启用”：如果当前 profile 还在 pending（例如刚导入/编辑但未应用），
        // 在启动代理前先尝试 apply 到 imported，避免 UI 落后和切换到旧配置。
        if (profile.pending && !profile.imported) {
            ServiceClient.profile().commit(profile.uuid, null)
        }
        
        ServiceClient.profile().setActive(profile)
    }

    suspend fun clearActiveProfile(profile: Profile) {
        Timber.d("Clearing active profile: uuid=${profile.uuid}")
        ServiceClient.connect(context)
        ServiceClient.profile().clearActive(profile)
    }

    suspend fun reorderProfiles(uuids: List<UUID>) {
        Timber.d("Reordering profiles: count=${uuids.size}")
        ServiceClient.connect(context)
        ServiceClient.profile().reorder(uuids)
    }

    /**
     * Update profile (fetch latest config)
     * @param uuid UUID of profile to update
     * @param callback Fetch progress observer
     */
    suspend fun updateProfile(uuid: UUID, callback: IFetchObserver? = null) {
        Timber.d("Updating profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().update(uuid, callback)
    }

    /**
     * Patch profile metadata
     * @param uuid Profile UUID
     * @param name New name
     * @param source New source
     * @param interval Update interval
     */
    suspend fun patchProfile(
        uuid: UUID,
        name: String,
        source: String,
        interval: Long
    ) {
        Timber.d("Patching profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().patch(uuid, name, source, interval)
    }

    /**
     * Commit profile changes
     * @param uuid Profile UUID
     * @param callback Fetch progress observer
     */
    suspend fun commitProfile(uuid: UUID, callback: IFetchObserver? = null) {
        Timber.d("Committing profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().commit(uuid, callback)
    }

    /**
     * Release profile (unlock editing)
     * @param uuid Profile UUID
     */
    suspend fun releaseProfile(uuid: UUID) {
        Timber.d("Releasing profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().release(uuid)
    }

    /**
     * Query all profiles (别名，兼容旧代码)
     */
    suspend fun queryAll(): List<Profile> {
        return queryAllProfiles()
    }

    /**
     * Query active profile (别名，兼容旧代码)
     */
    suspend fun queryActive(): Profile? {
        return queryActiveProfile()
    }
}

