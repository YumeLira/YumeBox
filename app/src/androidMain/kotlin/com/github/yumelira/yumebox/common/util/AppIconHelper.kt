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

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

object AppIconHelper {
    private const val MAIN_ACTIVITY_ALIAS = "com.github.yumelira.yumebox.MainActivityAlias"

    fun hideIcon(context: Context) {
        runCatching {
            val componentName = ComponentName(context.packageName, MAIN_ACTIVITY_ALIAS)
            val currentState = context.packageManager.getComponentEnabledSetting(componentName)
            if (currentState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                return
            }

            val mainActivityComponent =
                ComponentName(context.packageName, "com.github.yumelira.yumebox.MainActivity")
            context.packageManager.setComponentEnabledSetting(
                mainActivityComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )

            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }.onFailure { e ->
            Timber.w(e, "Failed to hide app icon")
        }
    }

    fun showIcon(context: Context) {
        runCatching {
            val componentName = ComponentName(context.packageName, MAIN_ACTIVITY_ALIAS)
            val currentState = context.packageManager.getComponentEnabledSetting(componentName)
            if (currentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || currentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                return
            }

            val mainActivityComponent =
                ComponentName(context.packageName, "com.github.yumelira.yumebox.MainActivity")
            context.packageManager.setComponentEnabledSetting(
                mainActivityComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )

            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }.onFailure { e ->
            Timber.w(e, "Failed to show app icon")
        }
    }

    fun toggleIcon(context: Context, hide: Boolean) {
        if (hide) {
            hideIcon(context)
        } else {
            showIcon(context)
        }
    }
}