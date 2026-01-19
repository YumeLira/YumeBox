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
        ProfileType.URL -> provider ?: "远程订阅"
        ProfileType.FILE -> "本地文件"
    }

    fun getInfoText(): String = when (type) {
        ProfileType.URL -> {
            buildString {
                if (totalBytes != null && totalBytes > 0) {
                    val usedPercent = usedBytes * 100 / totalBytes
                    append(
                        "流量: ${com.github.yumelira.yumebox.common.util.ByteFormatter.format(usedBytes)}/${
                            com.github.yumelira.yumebox.common.util.ByteFormatter.format(
                                totalBytes
                            )
                        } ($usedPercent%)"
                    )
                } else if (usedBytes > 0) {
                    append("已用流量: ${com.github.yumelira.yumebox.common.util.ByteFormatter.format(usedBytes)}")
                } else {
                    append("点击更新")
                }

                expireAt?.let { expire ->
                    val expireDate =
                        java.time.Instant.ofEpochMilli(expire).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val now = java.time.LocalDate.now()
                    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, expireDate)

                    if (isNotEmpty()) append("\n")

                    if (daysLeft > 0) {
                        append("到期于: $expireDate (剩余 ${daysLeft}天)")
                    } else if (daysLeft == 0L) {
                        append("今日到期")
                    } else {
                        append("已过期: $expireDate")
                    }
                }

                lastUpdatedAt?.let { updated ->
                    append("|${getRelativeTimeString(updated)}")
                }
            }
        }

        ProfileType.FILE -> "本地配置"
    }

    private fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)

        return when {
            diff < 60 * 1000 -> "刚刚"
            minutes < 60 -> "$minutes 分钟前"
            hours < 24 -> "$hours 小时前"
            else -> {
                val days = diff / (1000 * 60 * 60 * 24)
                "$days 天前"
            }
        }
    }

    fun shouldShowUpdateButton(): Boolean = type == ProfileType.URL

}
