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

import com.tencent.mmkv.MMKV

object AutoStartExecutionGate {
    private const val KEY_AUTO_START_EXECUTING_AT = "auto_start_executing_at"
    private const val ACTIVE_WINDOW_MS = 2 * 60 * 1000L

    fun markStarted(serviceCache: MMKV, now: Long = System.currentTimeMillis()) {
        serviceCache.encode(KEY_AUTO_START_EXECUTING_AT, now)
    }

    fun clear(serviceCache: MMKV) {
        serviceCache.removeValueForKey(KEY_AUTO_START_EXECUTING_AT)
    }

    fun isExecuting(serviceCache: MMKV, now: Long = System.currentTimeMillis()): Boolean {
        val startedAt = serviceCache.decodeLong(KEY_AUTO_START_EXECUTING_AT, 0L)
        if (startedAt <= 0L) {
            return false
        }

        val active = now >= startedAt && now - startedAt <= ACTIVE_WINDOW_MS
        if (!active) {
            clear(serviceCache)
        }
        return active
    }
}
