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

package com.github.yumelira.yumebox.domain.model

import com.github.yumelira.yumebox.data.model.Profile

sealed interface ProxyState {
    data object Idle : ProxyState

    data class Connecting(val mode: RunningMode) : ProxyState

    data class Running(
        val profile: Profile,
        val mode: RunningMode
    ) : ProxyState

    data object Stopping : ProxyState

    data class Error(val message: String, val cause: Throwable? = null) : ProxyState

    val isRunning: Boolean get() = this is Running
}
