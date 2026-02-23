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

package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.DailyTrafficSummary
import com.github.yumelira.yumebox.data.model.ProfileTrafficUsage
import com.github.yumelira.yumebox.data.model.TimeSlot
import com.github.yumelira.yumebox.data.model.TrafficSlotData
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.util.*

class TrafficStatisticsStore(private val mmkv: MMKV) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val KEY_DAILY_SUMMARIES = "daily_summaries"
        private const val KEY_PROFILE_USAGES = "profile_usages"
        private const val KEY_LAST_TRAFFIC_UPLOAD = "last_traffic_upload"
        private const val KEY_LAST_TRAFFIC_DOWNLOAD = "last_traffic_download"
        private const val KEY_LAST_PROFILE_ID = "last_profile_id"
        private const val MAX_DAYS_TO_KEEP = 90
    }

    private val _dailySummaries = MutableStateFlow<Map<Long, DailyTrafficSummary>>(emptyMap())
    val dailySummaries: StateFlow<Map<Long, DailyTrafficSummary>> = _dailySummaries.asStateFlow()

    private val _profileUsages = MutableStateFlow<Map<String, ProfileTrafficUsage>>(emptyMap())
    val profileUsages: StateFlow<Map<String, ProfileTrafficUsage>> = _profileUsages.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        mmkv.decodeString(KEY_DAILY_SUMMARIES)?.let { jsonStr ->
            runCatching {
                val summaries: Map<Long, DailyTrafficSummary> = json.decodeFromString(jsonStr)
                _dailySummaries.value = summaries
            }
        }

        mmkv.decodeString(KEY_PROFILE_USAGES)?.let { jsonStr ->
            runCatching {
                val usages: Map<String, ProfileTrafficUsage> = json.decodeFromString(jsonStr)
                _profileUsages.value = usages
            }
        }
    }

    private fun saveDailySummaries() {
        runCatching {
            val jsonStr = json.encodeToString(_dailySummaries.value)
            mmkv.encode(KEY_DAILY_SUMMARIES, jsonStr)
        }
    }

    private fun saveProfileUsages() {
        runCatching {
            val jsonStr = json.encodeToString(_profileUsages.value)
            mmkv.encode(KEY_PROFILE_USAGES, jsonStr)
        }
    }


    fun recordTraffic(uploadDelta: Long, downloadDelta: Long, profileId: String? = null, profileName: String? = null) {
        if (uploadDelta <= 0 && downloadDelta <= 0) return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val todayKey = getDayKey(calendar)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val slotIndex = TimeSlot.fromHour(currentHour).ordinal

        val currentSummaries = _dailySummaries.value.toMutableMap()
        val todaySummary = currentSummaries[todayKey] ?: DailyTrafficSummary(
            dateMillis = todayKey,
            totalUpload = 0L,
            totalDownload = 0L,
            hourlyData = emptyMap()
        )

        val currentHourlyData = todaySummary.hourlyData.toMutableMap()
        val currentSlot = currentHourlyData[slotIndex] ?: TrafficSlotData(slotIndex, 0L, 0L)

        val updatedSlot = currentSlot.copy(
            upload = currentSlot.upload + uploadDelta,
            download = currentSlot.download + downloadDelta
        )
        currentHourlyData[slotIndex] = updatedSlot

        val updatedSummary = todaySummary.copy(
            totalUpload = todaySummary.totalUpload + uploadDelta,
            totalDownload = todaySummary.totalDownload + downloadDelta,
            hourlyData = currentHourlyData
        )

        currentSummaries[todayKey] = updatedSummary
        _dailySummaries.value = cleanOldData(currentSummaries)
        saveDailySummaries()

        if (!profileId.isNullOrBlank() && !profileName.isNullOrBlank()) {
            recordProfileTraffic(profileId, profileName, uploadDelta, downloadDelta)
        }
    }

    fun recordProfileTraffic(
        profileId: String,
        profileName: String,
        uploadDelta: Long,
        downloadDelta: Long
    ) {
        if (uploadDelta <= 0 && downloadDelta <= 0) return

        val currentUsages = _profileUsages.value.toMutableMap()
        val currentUsage = currentUsages[profileId] ?: ProfileTrafficUsage(
            profileId = profileId,
            profileName = profileName,
            totalUpload = 0L,
            totalDownload = 0L
        )

        val updatedUsage = currentUsage.copy(
            profileName = profileName,
            totalUpload = currentUsage.totalUpload + uploadDelta,
            totalDownload = currentUsage.totalDownload + downloadDelta
        )

        currentUsages[profileId] = updatedUsage
        _profileUsages.value = currentUsages
        saveProfileUsages()
    }

    fun getTodaySummary(): DailyTrafficSummary {
        val todayKey = getDayKey(Calendar.getInstance())
        return _dailySummaries.value[todayKey] ?: DailyTrafficSummary.EMPTY
    }

    fun getYesterdaySummary(): DailyTrafficSummary {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayKey = getDayKey(calendar)
        return _dailySummaries.value[yesterdayKey] ?: DailyTrafficSummary.EMPTY
    }

    fun getDailySummaries(days: Int): List<DailyTrafficSummary> {
        val result = mutableListOf<DailyTrafficSummary>()

        repeat(days) { i ->
            val targetCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayKey = getDayKey(targetCalendar)
            val summary = _dailySummaries.value[dayKey] ?: DailyTrafficSummary(
                dateMillis = dayKey,
                totalUpload = 0L,
                totalDownload = 0L
            )
            result.add(summary)
        }

        return result.reversed()
    }

    fun getTodayHourlyData(): List<TrafficSlotData> {
        val todaySummary = getTodaySummary()
        return TimeSlot.entries.map { slot ->
            todaySummary.hourlyData[slot.ordinal] ?: TrafficSlotData(slot.ordinal, 0L, 0L)
        }
    }

    fun getProfileUsagesSorted(): List<ProfileTrafficUsage> {
        return _profileUsages.value.values
            .sortedByDescending { it.totalBytes }
    }

    fun clearAll() {
        _dailySummaries.value = emptyMap()
        _profileUsages.value = emptyMap()
        mmkv.remove(KEY_DAILY_SUMMARIES)
        mmkv.remove(KEY_PROFILE_USAGES)
    }

    fun setLastTraffic(upload: Long, download: Long, profileId: String? = null) {
        mmkv.encode(KEY_LAST_TRAFFIC_UPLOAD, upload)
        mmkv.encode(KEY_LAST_TRAFFIC_DOWNLOAD, download)
        if (profileId != null) {
            mmkv.encode(KEY_LAST_PROFILE_ID, profileId)
        }
    }

    fun getLastTrafficUpload(): Long = mmkv.decodeLong(KEY_LAST_TRAFFIC_UPLOAD, 0L)
    fun getLastTrafficDownload(): Long = mmkv.decodeLong(KEY_LAST_TRAFFIC_DOWNLOAD, 0L)
    fun getLastProfileId(): String? = mmkv.decodeString(KEY_LAST_PROFILE_ID)

    private fun getDayKey(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun cleanOldData(data: MutableMap<Long, DailyTrafficSummary>): Map<Long, DailyTrafficSummary> {
        val cutoffTime = System.currentTimeMillis() - (MAX_DAYS_TO_KEEP * 24 * 60 * 60 * 1000L)
        return data.filterKeys { it >= cutoffTime }
    }
}
