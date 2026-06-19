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

package com.github.yumelira.yumebox.domain.model

import com.github.yumelira.yumebox.core.model.Traffic
import com.github.yumelira.yumebox.core.util.decodeTrafficValue

data class TrafficData(val upload: Long, val download: Long) {
    companion object {
        val ZERO = TrafficData(0, 0)

        fun from(traffic: Traffic): TrafficData {
            val upload = decodeTrafficValue(traffic ushr 32)
            val download = decodeTrafficValue(traffic and 0xFFFFFFFFL)
            return TrafficData(upload, download)
        }
    }
}
