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

package com.github.yumelira.yumebox.service.runtime.session

import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.UiConfiguration

internal class SessionRuntimeQueryCache {
    private var snapshot = SessionRuntimeQuerySnapshot()

    fun clear() {
        snapshot = SessionRuntimeQuerySnapshot()
    }

    fun snapshot(): SessionRuntimeQuerySnapshot = snapshot

    fun replace(
        configuration: UiConfiguration,
        providers: List<Provider>,
        proxyGroups: List<ProxyGroup>,
        trafficNow: Long,
        trafficTotal: Long,
    ) {
        snapshot =
            SessionRuntimeQuerySnapshot(
                configuration = configuration,
                providers = providers,
                proxyGroups = proxyGroups,
                trafficNow = trafficNow,
                trafficTotal = trafficTotal,
            )
    }

    fun updateTrafficNow(trafficNow: Long) {
        snapshot = snapshot.copy(trafficNow = trafficNow)
    }

    fun updateTrafficTotal(trafficTotal: Long) {
        snapshot = snapshot.copy(trafficTotal = trafficTotal)
    }

    fun replaceProxyGroups(proxyGroups: List<ProxyGroup>) {
        snapshot = snapshot.copy(proxyGroups = proxyGroups)
    }

    fun upsertProxyGroup(name: String, proxyGroup: ProxyGroup) {
        val currentGroups = snapshot.proxyGroups
        snapshot =
            snapshot.copy(
                proxyGroups =
                    if (currentGroups.any { it.name == name }) {
                        currentGroups.map { if (it.name == name) proxyGroup else it }
                    } else {
                        currentGroups + proxyGroup
                    }
            )
    }
}

internal data class SessionRuntimeQuerySnapshot(
    val proxyGroups: List<ProxyGroup> = emptyList(),
    val configuration: UiConfiguration = UiConfiguration(),
    val providers: List<Provider> = emptyList(),
    val trafficNow: Long = 0L,
    val trafficTotal: Long = 0L,
)
