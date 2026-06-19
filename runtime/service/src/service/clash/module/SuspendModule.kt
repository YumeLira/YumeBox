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

package com.github.yumelira.yumebox.service.clash.module

import android.app.Service
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.common.util.ticker
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class SuspendModule(service: Service) : Module<Unit>(service) {
    private var suspended: Boolean? = null

    private fun applySuspend(shouldSuspend: Boolean) {
        if (suspended == shouldSuspend) return

        Clash.suspendCore(shouldSuspend)
        suspended = shouldSuspend
    }

    override suspend fun run() {
        val power = service.getSystemService<PowerManager>()
        val interactive = power?.isInteractive ?: true

        applySuspend(!interactive)

        val screenToggle =
            receiveBroadcast(Channel.CONFLATED) {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        val safetyTicker = ticker(5_000L)

        try {
            while (true) {
                select<Unit> {
                    screenToggle.onReceive {
                        when (it.action) {
                            Intent.ACTION_SCREEN_OFF -> applySuspend(true)
                            Intent.ACTION_SCREEN_ON,
                            Intent.ACTION_USER_PRESENT -> applySuspend(false)
                            else -> Unit
                        }
                    }
                    safetyTicker.onReceive {
                        val shouldSuspend = !(power?.isInteractive ?: true)
                        applySuspend(shouldSuspend)
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                Clash.suspendCore(false)
                suspended = false
            }
        }
    }
}
