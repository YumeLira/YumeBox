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

package com.github.yumelira.yumebox.remote

import android.content.Context
import com.github.yumelira.yumebox.service.ClashManager
import com.github.yumelira.yumebox.service.ProfileManager
import com.github.yumelira.yumebox.service.common.util.initializeServiceGlobal
import com.github.yumelira.yumebox.service.remote.IClashManager
import com.github.yumelira.yumebox.service.remote.IProfileManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Local service gateway in single-process mode.
 */
object ServiceClient {
    private val mutex = Mutex()
    private var initialized = false
    private var clashManager: IClashManager? = null
    private var profileManager: IProfileManager? = null

    suspend fun connect(ctx: Context) {
        mutex.withLock {
            val appContext = ctx.applicationContext
            if (initialized && clashManager != null && profileManager != null) {
                return
            }

            try {
                val app = appContext as android.app.Application
                initializeServiceGlobal(app)

                clashManager = ClashManager(appContext)
                profileManager = ProfileManager(appContext)
                initialized = true
                Timber.d(
                    "ServiceClient local gateway initialized in pid=${android.os.Process.myPid()}, process=${android.app.Application.getProcessName()}"
                )
            } catch (e: Exception) {
                initialized = false
                clashManager = null
                profileManager = null
                Timber.e(e, "Failed to initialize local service gateway")
                throw e
            }
        }
    }

    fun disconnect() {
        clashManager = null
        profileManager = null
        initialized = false
    }

    suspend fun clash(): IClashManager {
        return clashManager ?: throw IllegalStateException("ServiceClient not connected")
    }

    suspend fun profile(): IProfileManager {
        return profileManager ?: throw IllegalStateException("ServiceClient not connected")
    }

    fun isConnected(): Boolean = initialized && clashManager != null && profileManager != null
}
