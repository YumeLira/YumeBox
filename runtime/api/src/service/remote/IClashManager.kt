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

package com.github.yumelira.yumebox.service.remote

import com.github.yumelira.yumebox.core.model.*

interface IClashManager {
    fun queryTunnelState(): TunnelState

    fun queryTrafficNow(): Long

    fun queryTrafficTotal(): Long

    fun queryConnections(): ConnectionSnapshot

    fun queryProfileProxyGroupNames(excludeNotSelectable: Boolean): List<String>

    fun queryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup>

    fun queryActiveProfileTunRouteExcludeAddress(): List<String>

    fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup>

    fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String>

    fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup

    fun queryConfiguration(): UiConfiguration

    fun queryProviders(): ProviderList

    fun patchSelector(group: String, name: String): Boolean

    fun closeConnection(id: String): Boolean

    fun closeAllConnections()

    suspend fun healthCheck(group: String)

    suspend fun healthCheckProxy(group: String, proxyName: String): Int

    suspend fun updateProvider(type: Provider.Type, name: String)

    fun requestStop()

    fun setLogObserver(observer: ILogObserver?)
}
