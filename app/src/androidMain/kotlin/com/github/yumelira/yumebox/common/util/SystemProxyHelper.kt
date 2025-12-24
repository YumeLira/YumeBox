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

package com.github.yumelira.yumebox.common.util

import android.content.Context
import android.os.Build
import timber.log.Timber

object SystemProxyHelper {

    private const val TAG = "SystemProxyHelper"

    fun clearSystemProxy(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                clearSystemProxyQ(context)
            } else {
                clearSystemProxyLegacy(context)
            }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "清理系统代理失败: ${e.message}")
        }
    }

    @Suppress("NewApi")
    private fun clearSystemProxyQ(context: Context) {
        runCatching {
            val proxyHost = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")
            if (proxyHost != null || proxyPort != null) {
                getSystemProxy(context)
            }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Android 10+ 代理清除失败: ${e.message}")
        }
    }

    private fun clearSystemProxyLegacy(context: Context) {
        runCatching {
            getSystemProxy(context)
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "旧版本代理清除失败: ${e.message}")
        }
    }

    private fun getSystemProxy(context: Context) {
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
        System.clearProperty("https.proxyHost")
        System.clearProperty("https.proxyPort")
        System.clearProperty("socksProxyHost")
        System.clearProperty("socksProxyPort")
    }
}