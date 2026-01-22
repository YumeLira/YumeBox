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

import com.github.yumelira.yumebox.common.util.ByteFormatter
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
enum class ProfileType {
    URL, FILE,
}

@Serializable
data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "我的订阅",
    val provider: String = "未知服务商",
    val enabled: Boolean = true,
    val plan: String? = null,
    val expireAt: Long? = null,
    val usedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val remainingBytes: Long? = null,
    val subscribeUrl: String? = null,
    val updateUrl: String? = null,
    val exportUrl: String? = null,
    val lastUpdatedAt: Long? = null,
    val autoUpdate: Boolean = false
)

@Serializable
data class Profile(
    val id: String,
    val name: String = "",
    val config: String = "",
    val remoteUrl: String? = null,
    val type: ProfileType = ProfileType.URL,
    val enabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val provider: String? = null,
    val expireAt: Long? = null,
    val usedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val lastUpdatedAt: Long? = null,
    val order: Int = 0,
) {
    fun getDisplayProvider(): String = when (type) {
        ProfileType.URL -> provider ?: MLang.Component.ProfileCard.RemoteSubscription
        ProfileType.FILE -> MLang.Component.ProfileCard.LocalFile
    }

    fun getInfoText(): String = when (type) {
        ProfileType.URL -> {
            buildString {
                if (totalBytes != null && totalBytes > 0) {
                    val usedPercent = usedBytes * 100 / totalBytes
                    append(
                        MLang.Component.ProfileCard.Traffic.format(
                            ByteFormatter.format(usedBytes),
                            ByteFormatter.format(totalBytes),
                            usedPercent.toInt()
                        )
                    )
                } else if (usedBytes > 0) {
                    append(MLang.Component.ProfileCard.UsedTraffic.format(ByteFormatter.format(usedBytes)))
                } else {
                    append(MLang.Component.ProfileCard.ClickToUpdate)
                }

                expireAt?.let { expire ->
                    val expireDate =
                        java.time.Instant.ofEpochMilli(expire).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val now = java.time.LocalDate.now()
                    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, expireDate)

                    if (isNotEmpty()) append("\n")

                    if (daysLeft > 0) {
                        append(MLang.Component.ProfileCard.ExpireAt.format(expireDate, daysLeft.toInt()))
                    } else if (daysLeft == 0L) {
                        append(MLang.Component.ProfileCard.ExpireToday)
                    } else {
                        append(MLang.Component.ProfileCard.Expired.format(expireDate))
                    }
                }

                lastUpdatedAt?.let { updated ->
                    append("|${getRelativeTimeString(updated)}")
                }
            }
        }

        ProfileType.FILE -> MLang.Component.ProfileCard.LocalConfig
    }

    private fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)

        return when {
            diff < 60 * 1000 -> MLang.Component.ProfileCard.JustNow
            minutes < 60 -> MLang.Component.ProfileCard.MinutesAgo.format(minutes.toInt())
            hours < 24 -> MLang.Component.ProfileCard.HoursAgo.format(hours.toInt())
            else -> {
                val days = diff / (1000 * 60 * 60 * 24)
                MLang.Component.ProfileCard.DaysAgo.format(days.toInt())
            }
        }
    }

    fun shouldShowUpdateButton(): Boolean = type == ProfileType.URL

}
