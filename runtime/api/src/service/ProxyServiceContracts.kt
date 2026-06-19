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

package com.github.yumelira.yumebox.service

import com.github.yumelira.yumebox.service.common.constants.Intents

object ProxyServiceContracts {
    val ACTION_PROXY_STARTED: String
        get() = Intents.ACTION_CLASH_STARTED

    val ACTION_PROXY_STOPPED: String
        get() = Intents.ACTION_CLASH_STOPPED

    val ACTION_PROXY_GROUPS_UPDATED: String
        get() = Intents.ACTION_PROXY_GROUPS_UPDATED

    val ACTION_PROFILE_LOADED: String
        get() = Intents.ACTION_PROFILE_LOADED

    val ACTION_PROFILE_CHANGED: String
        get() = Intents.ACTION_PROFILE_CHANGED

    val ACTION_REQUEST_STOP: String
        get() = Intents.ACTION_CLASH_REQUEST_STOP

    val ACTION_PATCH_SELECTOR: String
        get() = Intents.ACTION_PATCH_SELECTOR

    const val EXTRA_GROUP_NAME = Intents.EXTRA_GROUP_NAME
    const val EXTRA_PROXY_NAME = Intents.EXTRA_PROXY_NAME
    const val EXTRA_PROFILE_ID = Intents.EXTRA_PROFILE_ID
    const val EXTRA_START_PROXY = Intents.EXTRA_START_PROXY

    val ACTION_PATCH_OVERRIDE: String
        get() = Intents.ACTION_PATCH_OVERRIDE

    val ACTION_CLEAR_OVERRIDE: String
        get() = Intents.ACTION_CLEAR_OVERRIDE

    const val EXTRA_OVERRIDE_SLOT = Intents.EXTRA_OVERRIDE_SLOT
    const val EXTRA_OVERRIDE_CONFIG = Intents.EXTRA_OVERRIDE_CONFIG

    val ACTION_HEALTH_CHECK: String
        get() = Intents.ACTION_HEALTH_CHECK

    val ACTION_HEALTH_CHECK_ALL: String
        get() = Intents.ACTION_HEALTH_CHECK_ALL

    const val EXTRA_HEALTH_CHECK_GROUP = Intents.EXTRA_HEALTH_CHECK_GROUP

    fun intentSelf(action: String, packageName: String? = null): android.content.Intent {
        return android.content.Intent(action).apply {
            if (!packageName.isNullOrBlank()) {
                setPackage(packageName)
            }
        }
    }
}
