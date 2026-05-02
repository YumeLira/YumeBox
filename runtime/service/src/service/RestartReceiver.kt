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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

class RestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
                -> {
                val reason = when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> AutoRestartService.REASON_BOOT_COMPLETED
                    Intent.ACTION_MY_PACKAGE_REPLACED -> AutoRestartService.REASON_PACKAGE_REPLACED
                    else -> "unknown"
                }
                val serviceIntent = Intent(context, AutoRestartService::class.java).putExtra(
                    AutoRestartService.EXTRA_REASON,
                    reason,
                )
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }.onFailure { error ->
                    Timber.e(error, "Start auto-restart service failed")
                }
            }
        }
    }
}
