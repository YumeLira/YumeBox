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



package com.github.yumelira.yumebox.core.util

import android.annotation.SuppressLint
import com.github.yumelira.yumebox.core.model.Traffic

@SuppressLint("DefaultLocale")
private fun trafficString(scaled: Long): String {
    return when {
        scaled >= 1024L * 1024L * 1024L -> {
            String.format("%.2f GiB", scaled.toDouble() / (1024.0 * 1024.0 * 1024.0))
        }

        scaled >= 1024L * 1024L -> {
            String.format("%.2f MiB", scaled.toDouble() / (1024.0 * 1024.0))
        }

        scaled >= 1024L -> {
            String.format("%.2f KiB", scaled.toDouble() / 1024.0)
        }

        else -> {
            "$scaled Bytes"
        }
    }
}

fun decodeTrafficValue(value: Long): Long {
    val type = (value ushr 30) and 0x3
    val payload = value and 0x3FFFFFFFL

    return when (type) {
        0L -> payload
        1L -> (payload * 1024L) / 100L
        2L -> (payload * 1024L * 1024L) / 100L
        3L -> (payload * 1024L * 1024L * 1024L) / 100L
        else -> 0L
    }
}

private fun scaleTraffic(value: Long): Long = decodeTrafficValue(value)

fun Traffic.trafficUpload(): String {
    return trafficString(scaleTraffic(this ushr 32))
}

fun Traffic.trafficDownload(): String {
    return trafficString(scaleTraffic(this and 0xFFFFFFFFL))
}

fun Traffic.trafficTotal(): String {
    val upload = scaleTraffic(this ushr 32)
    val download = scaleTraffic(this and 0xFFFFFFFFL)

    return trafficString(upload + download)
}
