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

package com.github.yumelira.yumebox.service.root

import android.content.Context
import com.tencent.mmkv.MMKV

class RootTunStateStore(context: Context) {
    private val store = MMKV.mmkvWithID(STORE_ID, MMKV.MULTI_PROCESS_MODE)
    @Volatile private var cachedEncoded: String? = null
    @Volatile private var cachedStatus: RootTunStatus? = null

    fun snapshot(): RootTunStatus {
        val encoded = store.decodeString(KEY_STATUS_JSON)
        if (!encoded.isNullOrBlank()) {
            val lastEncoded = cachedEncoded
            val lastStatus = cachedStatus
            if (lastEncoded != null && lastEncoded == encoded && lastStatus != null) {
                return lastStatus
            }
            return runCatching {
                    RootTunJson.Default.decodeFromString(RootTunStatus.serializer(), encoded)
                }
                .getOrElse { legacySnapshot() }
                .also { decoded ->
                    cachedEncoded = encoded
                    cachedStatus = decoded
                }
        }

        return legacySnapshot().also {
            cachedEncoded = null
            cachedStatus = it
        }
    }

    fun isRunning(): Boolean = snapshot().running

    fun updateStatus(status: RootTunStatus) {
        val normalized = status.copy(running = status.state.isActive)
        val encoded = RootTunJson.Default.encodeToString(RootTunStatus.serializer(), normalized)
        store.encode(KEY_STATUS_JSON, encoded)
        store.encode(KEY_RUNNING, normalized.running)
        encodeNullable(KEY_LAST_ERROR, normalized.lastError)
        encodeNullable(KEY_PROFILE_UUID, normalized.profileUuid)
        encodeNullable(KEY_PROFILE_NAME, normalized.profileName)
        cachedEncoded = encoded
        cachedStatus = normalized
    }

    fun markIdle(error: String? = null) {
        updateStatus(
            RootTunStatus(
                state = RootTunState.Idle,
                lastError = error,
                runtimeReady = false,
                controllerReady = true,
            )
        )
    }

    fun clear() {
        store.clearAll()
        cachedEncoded = null
        cachedStatus = null
    }

    private fun legacySnapshot(): RootTunStatus {
        val running = store.decodeBool(KEY_RUNNING, false)
        val state = if (running) RootTunState.Running else RootTunState.Idle
        return RootTunStatus(
            state = state,
            running = state.isActive,
            lastError = store.decodeString(KEY_LAST_ERROR),
            profileUuid = store.decodeString(KEY_PROFILE_UUID),
            profileName = store.decodeString(KEY_PROFILE_NAME),
            runtimeReady = state == RootTunState.Running,
            controllerReady = false,
        )
    }

    private fun encodeNullable(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            store.removeValueForKey(key)
        } else {
            store.encode(key, value)
        }
    }

    companion object {
        private const val STORE_ID = "root_tun_state"
        private const val KEY_STATUS_JSON = "status_json"
        private const val KEY_RUNNING = "running"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_PROFILE_UUID = "profile_uuid"
        private const val KEY_PROFILE_NAME = "profile_name"
    }
}
