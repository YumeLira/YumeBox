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

package com.github.yumelira.yumebox.core.util

import android.annotation.SuppressLint


@SuppressLint("DefaultLocale")
private fun trafficString(scaled: Long): String {
    return when {
        scaled > 1024 * 1024 * 1024 * 100L -> {
            val data = scaled / 1024 / 1024 / 1024

            String.format("%.2f GiB", data.toFloat() / 100)
        }

        scaled > 1024 * 1024 * 100L -> {
            val data = scaled / 1024 / 1024

            String.format("%.2f MiB", data.toFloat() / 100)
        }

        scaled > 1024 * 100L -> {
            val data = scaled / 1024

            String.format("%.2f KiB", data.toFloat() / 100)
        }

        else -> {
            "$scaled Bytes"
        }
    }
}

private fun scaleTraffic(value: Long): Long {
    val type = (value ushr 30) and 0x3
    val data = value and 0x3FFFFFFF

    return when (type) {
        0L -> data
        1L -> data * 1024
        2L -> data * 1024 * 1024
        3L -> data * 1024 * 1024 * 1024
        else -> throw IllegalArgumentException("invalid value type")
    }
}
