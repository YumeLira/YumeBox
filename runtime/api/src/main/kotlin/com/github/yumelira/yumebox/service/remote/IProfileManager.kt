package com.github.yumelira.yumebox.service.remote

import com.github.yumelira.yumebox.service.runtime.entity.Profile
import java.util.*

interface IProfileManager {
    suspend fun create(type: Profile.Type, name: String, source: String = ""): UUID
    suspend fun clone(uuid: UUID): UUID
    suspend fun commit(uuid: UUID, callback: IFetchObserver? = null)
    suspend fun release(uuid: UUID)
    suspend fun delete(uuid: UUID)
    suspend fun patch(uuid: UUID, name: String, source: String, interval: Long)
    suspend fun update(uuid: UUID, callback: IFetchObserver? = null)
    suspend fun queryByUUID(uuid: UUID): Profile?
    suspend fun queryAll(): List<Profile>
    suspend fun queryActive(): Profile?
    suspend fun setActive(profile: Profile)
    suspend fun clearActive(profile: Profile)
    suspend fun reorder(uuids: List<UUID>)
}
