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

package com.github.yumelira.yumebox.service.notification

import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.common.util.formatSpeed
import com.github.yumelira.yumebox.core.model.ProxyGroup

internal data class NotificationPresentation(
    val title: String,
    val content: String,
    val expandedText: String,
    val subText: String? = null,
)

internal object NotificationPresentationFactory {
    fun createRunning(
        profileName: String,
        trafficNow: Long,
        trafficTotal: Long,
    ): NotificationPresentation {
        val speedLine = buildSpeedLine(trafficNow)
        val totalLine = buildTotalLine(trafficTotal)
        return NotificationPresentation(
            title = profileName,
            content = speedLine,
            expandedText = "$speedLine\n$totalLine",
            subText = totalLine,
        )
    }

    fun createStatus(
        profileName: String,
        status: String,
    ): NotificationPresentation {
        return NotificationPresentation(
            title = profileName,
            content = status,
            expandedText = status,
            subText = null,
        )
    }

    fun resolveNodeName(
        queryGroup: (String) -> ProxyGroup?,
        queryGroupNames: () -> List<String>,
    ): String? {
        val startGroup = queryGroup("Proxy")
            ?.takeIf(::isSelectableGroup)
            ?: queryGroupNames()
                .asSequence()
                .mapNotNull(queryGroup)
                .firstOrNull(::isSelectableGroup)
            ?: return null

        val seed = startGroup.now.ifBlank {
            startGroup.proxies.firstOrNull()?.name.orEmpty()
        }
        return resolveSelection(seed, queryGroup, mutableSetOf())
    }

    suspend fun resolveNodeNameSuspend(
        queryGroup: suspend (String) -> ProxyGroup?,
        queryGroupNames: suspend () -> List<String>,
    ): String? {
        val directGroup = queryGroup("Proxy")
        val startGroup = if (directGroup != null && isSelectableGroup(directGroup)) {
            directGroup
        } else {
            var matched: ProxyGroup? = null
            for (name in queryGroupNames()) {
                val group = queryGroup(name)
                if (group != null && isSelectableGroup(group)) {
                    matched = group
                    break
                }
            }
            matched
        } ?: return null

        val seed = startGroup.now.ifBlank {
            startGroup.proxies.firstOrNull()?.name.orEmpty()
        }
        return resolveSelectionSuspend(seed, queryGroup, mutableSetOf())
    }

    private fun resolveSelection(
        selection: String,
        queryGroup: (String) -> ProxyGroup?,
        visited: MutableSet<String>,
    ): String? {
        val normalized = selection.trim()
        if (normalized.isEmpty()) return null
        if (!visited.add(normalized)) return normalized

        val group = queryGroup(normalized)
        if (group != null && isSelectableGroup(group)) {
            val next = group.now.ifBlank {
                group.proxies.firstOrNull()?.name.orEmpty()
            }
            return resolveSelection(next, queryGroup, visited) ?: normalized
        }

        return normalized
    }

    private suspend fun resolveSelectionSuspend(
        selection: String,
        queryGroup: suspend (String) -> ProxyGroup?,
        visited: MutableSet<String>,
    ): String? {
        val normalized = selection.trim()
        if (normalized.isEmpty()) return null
        if (!visited.add(normalized)) return normalized

        val group = queryGroup(normalized)
        if (group != null && isSelectableGroup(group)) {
            val next = group.now.ifBlank {
                group.proxies.firstOrNull()?.name.orEmpty()
            }
            return resolveSelectionSuspend(next, queryGroup, visited) ?: normalized
        }

        return normalized
    }

    private fun buildSpeedLine(trafficNow: Long): String {
        val upNow = decodeTrafficHalf(trafficNow ushr 32)
        val downNow = decodeTrafficHalf(trafficNow and 0xFFFFFFFFL)
        return "下行 ${formatSpeed(downNow)}  上行 ${formatSpeed(upNow)}"
    }

    private fun buildTotalLine(trafficTotal: Long): String {
        val upTotal = decodeTrafficHalf(trafficTotal ushr 32)
        val downTotal = decodeTrafficHalf(trafficTotal and 0xFFFFFFFFL)
        return "总流量 ${formatBytes(upTotal + downTotal)}"
    }

    private fun isSelectableGroup(group: ProxyGroup): Boolean {
        return group.type.group && (group.now.isNotBlank() || group.proxies.isNotEmpty())
    }

    private fun decodeTrafficHalf(encoded: Long): Long {
        val type = (encoded ushr 30) and 0x3L
        val payload = encoded and 0x3FFFFFFFL
        return when (type.toInt()) {
            0 -> payload
            1 -> (payload * 1024L) / 100L
            2 -> (payload * 1024L * 1024L) / 100L
            3 -> (payload * 1024L * 1024L * 1024L) / 100L
            else -> 0L
        }
    }
}
