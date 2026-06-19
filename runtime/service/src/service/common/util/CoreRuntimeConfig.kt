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
import com.github.yumelira.yumebox.core.Clash
import com.tencent.mmkv.MMKV

object CoreRuntimeConfig {
    private const val SETTINGS_STORE_ID = "settings"
    private const val CUSTOM_USER_AGENT_KEY = "customUserAgent"

    fun applyCustomUserAgentIfPresent(context: Context) {
        val settings = MMKV.mmkvWithID(SETTINGS_STORE_ID, MMKV.MULTI_PROCESS_MODE)
        val userAgent = settings.decodeString(CUSTOM_USER_AGENT_KEY, "").orEmpty()
        if (userAgent.isNotBlank()) {
            Clash.setCustomUserAgent(userAgent)
        }
    }
}
