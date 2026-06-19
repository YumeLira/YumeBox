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

package com.github.yumelira.yumebox.core.util

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Session-scoped gate for auto start behavior.
 *
 * Manual pause blocks auto start only within current app process lifetime. Foreground auto actions
 * also run at most once per process lifetime. After process restart, state resets naturally.
 */
object AutoStartSessionGate {
    private val manualPaused = AtomicBoolean(false)
    private val foregroundAutoActionsInFlight = AtomicBoolean(false)
    private val foregroundAutoActionsHandled = AtomicBoolean(false)

    fun markManualPaused() {
        manualPaused.set(true)
    }

    fun clearManualPaused() {
        manualPaused.set(false)
    }

    fun shouldSkipAutoStart(): Boolean = manualPaused.get()

    fun tryBeginForegroundAutoActions(): Boolean {
        if (foregroundAutoActionsHandled.get()) {
            return false
        }
        return foregroundAutoActionsInFlight.compareAndSet(false, true)
    }

    fun finishForegroundAutoActions(markHandled: Boolean) {
        foregroundAutoActionsInFlight.set(false)
        if (markHandled) {
            foregroundAutoActionsHandled.set(true)
        }
    }
}
