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
 * Copyright (c)  YumeLira & YumeRiMoe 2025 - Present
 *
 */

package com.github.yumelira.yumebox.config

import android.content.Context
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.remote.ServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TunProfileSync(
    private val context: Context,
    private val networkSettingsStore: NetworkSettingsStore,
) {
    suspend fun syncActiveProfile() =
        withContext(Dispatchers.IO) {
            ServiceClient.connect(context)
            val routeExcludeAddress =
                ServiceClient.clash().queryActiveProfileTunRouteExcludeAddress()
            applyRouteExcludeAddress(routeExcludeAddress)
        }

    private fun applyRouteExcludeAddress(routeExcludeAddress: List<String>) {
        networkSettingsStore.tunRouteExcludeAddress.set(routeExcludeAddress)
        networkSettingsStore.bypassPrivateNetwork.set(false)
    }
}
