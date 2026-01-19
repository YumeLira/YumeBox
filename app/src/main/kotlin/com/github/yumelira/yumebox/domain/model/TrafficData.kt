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

import com.github.yumelira.yumebox.core.model.Traffic

data class TrafficData(
    val upload: Long,
    val download: Long
) {
    companion object {
        val ZERO = TrafficData(0, 0)

        fun from(traffic: Traffic): TrafficData {
            val upload = decodeHalf(traffic ushr 32)
            val download = decodeHalf(traffic and 0xFFFFFFFFL)
            return TrafficData(upload, download)
        }

        private fun decodeHalf(encoded: Long): Long {
            val type = (encoded ushr 30) and 0x3L
            val data = (encoded and 0x3FFFFFFFL) / 100.0
            return when (type.toInt()) {
                0 -> data.toLong()
                1 -> (data * 1024.0).toLong()
                2 -> (data * 1024.0 * 1024.0).toLong()
                3 -> (data * 1024.0 * 1024.0 * 1024.0).toLong()
                else -> 0L
            }
        }
    }
}
