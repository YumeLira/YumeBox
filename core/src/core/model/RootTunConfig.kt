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

package com.github.yumelira.yumebox.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RootTunConfig(
    val ifName: String = "yume",
    val mtu: Int = 9000,
    val stack: String = "system",
    val inet4Address: List<String> = listOf("172.19.0.1/30"),
    val inet6Address: List<String> = listOf("fdfe:dcba:9876::1/126"),
    val dnsHijack: List<String> = listOf("any:53", "tcp://any:53"),
    val autoRoute: Boolean = true,
    val strictRoute: Boolean = true,
    val autoRedirect: Boolean = true,
    val includeUid: List<Int> = emptyList(),
    val excludeUid: List<Int> = emptyList(),
    val includeAndroidUser: List<Int> = listOf(0, 10),
    val routeAddress: List<String> = emptyList(),
    val routeExcludeAddress: List<String> = emptyList(),
    val dnsMode: RootTunDnsMode = RootTunDnsMode.RedirHost,
    val fakeIpRange: String? = "198.18.0.1/16",
    val fakeIpRange6: String? = "fc00::/18",
    val allowIpv6: Boolean = true,
    val debugLogPath: String? = null,
)

@Serializable
enum class RootTunDnsMode {
    @SerialName("redir-host") RedirHost,
    @SerialName("fake-ip") FakeIp,
}
