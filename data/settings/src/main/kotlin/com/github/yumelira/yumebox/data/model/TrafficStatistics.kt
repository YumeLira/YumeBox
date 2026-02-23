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

package com.github.yumelira.yumebox.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TrafficRecord(
    val timestamp: Long,
    val upload: Long,
    val download: Long,
    val profileId: String? = null
) {
    val total: Long get() = upload + download
}

enum class TimeSlot(val startHour: Int, val endHour: Int, val label: String) {
    SLOT_0_4(0, 4, "0-4"),
    SLOT_4_8(4, 8, "4-8"),
    SLOT_8_12(8, 12, "8-12"),
    SLOT_12_16(12, 16, "12-16"),
    SLOT_16_20(16, 20, "16-20"),
    SLOT_20_24(20, 24, "20-24");

    companion object {
        fun fromHour(hour: Int): TimeSlot {
            return entries.first { hour >= it.startHour && hour < it.endHour }
        }
    }
}

@Serializable
data class DailyTrafficSummary(
    val dateMillis: Long,
    val totalUpload: Long,
    val totalDownload: Long,
    val hourlyData: Map<Int, TrafficSlotData> = emptyMap()
) {
    val total: Long get() = totalUpload + totalDownload

    companion object {
        val EMPTY = DailyTrafficSummary(0L, 0L, 0L)
    }
}

@Serializable
data class TrafficSlotData(
    val slotIndex: Int,
    val upload: Long,
    val download: Long
) {
    val total: Long get() = upload + download
}

@Serializable
data class ProfileTrafficUsage(
    val profileId: String,
    val profileName: String,
    val totalUpload: Long,
    val totalDownload: Long
) {
    val totalBytes: Long get() = totalUpload + totalDownload
}

enum class StatisticsTimeRange(val days: Int) {
    TODAY(1),
    WEEK(7);

    val label: String
        get() = when (this) {
            TODAY -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.TimeRange.Today
            WEEK -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.TimeRange.Week
        }
}

enum class ChartGranularity {
    HOURLY,
    DAILY;

    val label: String
        get() = when (this) {
            HOURLY -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.Chart.Hourly
            DAILY -> dev.oom_wg.purejoy.mlang.MLang.TrafficStatistics.Chart.Daily
        }
}
