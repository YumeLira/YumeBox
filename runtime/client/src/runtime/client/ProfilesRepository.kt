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

package com.github.yumelira.yumebox.runtime.client

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.core.data.RepositoryUtils.safeApiCall
import com.github.yumelira.yumebox.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunReloadScheduler
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.remote.IFetchObserver
import com.github.yumelira.yumebox.service.root.RootTunStateStore
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Repository for managing Profile CRUD operations.
 *
 * Communicates with ServiceClient (IPC to VPN service) to perform profile operations. All methods
 * return Result<T> for consistent error handling.
 */
class ProfilesRepository(private val context: Context) {
    private val appContext = context.appContextOrSelf
    private val rootTunStateStore by lazy { RootTunStateStore(appContext) }

    suspend fun createProfile(type: Profile.Type, name: String, source: String = "", ageSecretKey: String? = null): UUID =
        safeApiCall(TAG, "createProfile") {
                Timber.d("Creating profile: type=$type, name=$name")
                ServiceClient.connect(context)
                ServiceClient.profile().create(type, name, source, ageSecretKey)
            }
            .getOrThrow()

    suspend fun cloneProfile(uuid: UUID): UUID =
        safeApiCall(TAG, "cloneProfile") {
                Timber.d("Cloning profile: uuid=$uuid")
                ServiceClient.connect(context)
                ServiceClient.profile().clone(uuid)
            }
            .getOrThrow()

    suspend fun deleteProfile(uuid: UUID) {
        safeApiCall(TAG, "deleteProfile") {
                Timber.d("Deleting profile: uuid=$uuid")
                ServiceClient.connect(context)
                ServiceClient.profile().delete(uuid)
            }
            .getOrThrow()
    }

    suspend fun queryAllProfiles(): List<Profile> =
        withContext(Dispatchers.IO) {
            safeApiCall(TAG, "queryAllProfiles") {
                    ServiceClient.connect(context)
                    ServiceClient.profile().queryAll()
                }
                .getOrThrow()
        }

    suspend fun queryActiveProfile(): Profile? =
        withContext(Dispatchers.IO) {
            safeApiCall(TAG, "queryActiveProfile") {
                    ServiceClient.connect(context)
                    ServiceClient.profile().queryActive()
                }
                .getOrThrow()
        }

    suspend fun queryProfileByUUID(uuid: UUID): Profile? =
        withContext(Dispatchers.IO) {
            safeApiCall(TAG, "queryProfileByUUID") {
                    ServiceClient.connect(context)
                    ServiceClient.profile().queryByUUID(uuid)
                }
                .getOrThrow()
        }

    suspend fun setActiveProfile(uuid: UUID) {
        withContext(Dispatchers.IO) {
            safeApiCall(TAG, "setActiveProfile") {
                    val startedAt = System.currentTimeMillis()
                    Timber.d("Setting active profile: uuid=$uuid")
                    ServiceClient.connect(context)

                    val profile =
                        ServiceClient.profile().queryByUUID(uuid)
                            ?: throw IllegalArgumentException("Profile not found: $uuid")

                    ServiceClient.profile().setActive(profile)

                    notifyRuntimeOverrideChanged()

                    if (isRootTunActive()) {
                        RootTunReloadScheduler.schedule(
                            appContext,
                            RootTunReloadScheduler.Reason.PROFILE_CHANGED,
                        )
                    }

                    Timber.d(
                        "Active profile applied: uuid=$uuid cost=${System.currentTimeMillis() - startedAt}ms"
                    )
                }
                .getOrThrow()
        }
    }

    suspend fun clearActiveProfile(profile: Profile) {
        safeApiCall(TAG, "clearActiveProfile") {
                Timber.d("Clearing active profile: uuid=${profile.uuid}")
                ServiceClient.connect(context)
                ServiceClient.profile().clearActive(profile)
                notifyRuntimeOverrideChanged()
            }
            .getOrThrow()
    }

    suspend fun reorderProfiles(uuids: List<UUID>) {
        safeApiCall(TAG, "reorderProfiles") {
                Timber.d("Reordering profiles: count=${uuids.size}")
                ServiceClient.connect(context)
                ServiceClient.profile().reorder(uuids)
            }
            .getOrThrow()
    }

    suspend fun updateProfile(uuid: UUID, callback: IFetchObserver? = null) {
        safeApiCall(TAG, "updateProfile") {
                Timber.d("Updating profile: uuid=$uuid")
                ServiceClient.connect(context)
                ServiceClient.profile().update(uuid, callback)
            }
            .getOrThrow()
    }

    suspend fun patchProfile(
        uuid: UUID,
        name: String,
        source: String,
        interval: Long,
        updateAgeSecretKey: Boolean = false,
        ageSecretKey: String? = null,
    ) {
        safeApiCall(TAG, "patchProfile") {
                Timber.d("Patching profile: uuid=$uuid")
                ServiceClient.connect(context)
                ServiceClient.profile()
                    .patch(
                        uuid = uuid,
                        name = name,
                        source = source,
                        interval = interval,
                        updateAgeSecretKey = updateAgeSecretKey,
                        ageSecretKey = ageSecretKey,
                    )
            }
            .getOrThrow()
    }

    suspend fun queryAll(): List<Profile> = queryAllProfiles()

    suspend fun queryActive(): Profile? = queryActiveProfile()

    private fun isRootTunActive(): Boolean {
        val status = rootTunStateStore.snapshot()
        return status.state.isActive || status.runtimeReady
    }

    private fun notifyRuntimeOverrideChanged() {
        appContext.sendBroadcast(
            Intent(Intents.actionOverrideChanged(appContext.packageName))
                .setPackage(appContext.packageName)
        )
    }

    companion object {
        private const val TAG = "ProfilesRepository"
    }
}
