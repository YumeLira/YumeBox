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

package com.github.yumelira.yumebox.service.common.util

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class SocketOwnerResolver(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val packageManager = context.packageManager
    private val packageCache = ConcurrentHashMap<Int, String>()

    fun queryOwner(protocol: Int, source: InetSocketAddress, target: InetSocketAddress): String {
        if (Build.VERSION.SDK_INT < 29) {
            return encode(-1, "")
        }

        val uid = resolveUid(protocol, source, target)
        if (uid < 0) {
            return encode(-1, "")
        }

        return encode(uid, resolvePackageName(uid))
    }

    private fun encode(uid: Int, packageName: String): String {
        return "$uid\t$packageName"
    }

    private fun resolveUid(
        protocol: Int,
        source: InetSocketAddress,
        target: InetSocketAddress,
    ): Int {
        return runCatching { connectivity?.getConnectionOwnerUid(protocol, source, target) ?: -1 }
            .getOrDefault(-1)
    }

    private fun resolvePackageName(uid: Int): String {
        packageCache[uid]?.let {
            return it
        }

        val packageName =
            runCatching { packageManager.getPackagesForUid(uid)?.firstOrNull().orEmpty() }
                .getOrElse { "" }

        if (packageName.isNotEmpty()) {
            packageCache[uid] = packageName
        }

        return packageName
    }
}
