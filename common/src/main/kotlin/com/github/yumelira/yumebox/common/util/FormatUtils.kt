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

object ByteFormatter {

    private const val KB = 1024L
    private const val MB = KB * 1024
    private const val GB = MB * 1024
    private const val TB = GB * 1024
    private const val PB = TB * 1024

    private const val KB_D = 1024.0
    private const val MB_D = KB_D * 1024
    private const val GB_D = MB_D * 1024
    private const val TB_D = GB_D * 1024
    private const val PB_D = TB_D * 1024

    @JvmStatic
    fun format(bytes: Long, decimals: Int? = null): String {
        val value = bytes.coerceAtLeast(0L)
        return when {
            value < KB -> "$value B"
            value < MB -> formatValue(value / KB_D, "KB", decimals ?: 1)
            value < GB -> formatValue(value / MB_D, "MB", decimals ?: 1)
            value < TB -> formatValue(value / GB_D, "GB", decimals ?: 2)
            value < PB -> formatValue(value / TB_D, "TB", decimals ?: 2)
            else -> formatValue(value / PB_D, "PB", decimals ?: 2)
        }
    }

    @JvmStatic
    fun formatSpeed(bytesPerSecond: Long): String {
        val value = bytesPerSecond.coerceAtLeast(0L)
        return when {
            value < KB -> "$value B/s"
            value < MB -> formatValue(value / KB_D, "KB/s", 1)
            value < GB -> formatValue(value / MB_D, "MB/s", 1)
            else -> formatValue(value / GB_D, "GB/s", 2)
        }
    }

    @JvmStatic
    fun formatForDisplay(bytes: Long, isSpeed: Boolean = false): Pair<String, String> {
        val value = bytes.coerceAtLeast(0L)
        val suffix = if (isSpeed) "/s" else ""
        return when {
            value < KB -> Pair("$value", "B$suffix")
            value < MB -> {
                val num = value / KB_D
                Pair(if (num < 10) "%.1f".format(num) else "%.0f".format(num), "KB$suffix")
            }

            value < GB -> {
                val num = value / MB_D
                Pair(if (num < 10) "%.1f".format(num) else "%.0f".format(num), "MB$suffix")
            }

            else -> Pair("%.2f".format(value / GB_D), "GB$suffix")
        }
    }

    private fun formatValue(value: Double, unit: String, decimals: Int): String {
        return "%.${decimals}f $unit".format(value)
    }
}

fun formatBytes(bytes: Long): String = ByteFormatter.format(bytes)
fun formatSpeed(bytesPerSecond: Long): String = ByteFormatter.formatSpeed(bytesPerSecond)
fun formatBytesForDisplay(bytes: Long): Pair<String, String> = ByteFormatter.formatForDisplay(bytes, isSpeed = true)
