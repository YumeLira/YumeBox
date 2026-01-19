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

package com.github.yumelira.yumebox.clash.config

object Configuration {
    object Defaults {
        const val MIXED_PORT = 7890
        const val HTTP_ADDRESS = "127.0.0.1"
        const val TUN_MTU = 9000
        const val TUN_GATEWAY = "172.19.0.1"
        const val TUN_PORTAL = "172.19.0.2"
        const val TUN_DNS = "172.19.0.2"
        const val TUN_STACK = "gvisor"
    }

    sealed class ProxyMode {
        object Tun : ProxyMode()
    }

    data class TunConfig(
        val mtu: Int = Defaults.TUN_MTU,
        val gateway: String = Defaults.TUN_GATEWAY,
        val portal: String = Defaults.TUN_PORTAL,
        val dns: String = Defaults.TUN_DNS,
        val stack: String = Defaults.TUN_STACK,
        val dnsHijacking: Boolean = true,
        val autoRoute: Boolean = true
    )

    data class HttpConfig(
        val host: String = Defaults.HTTP_ADDRESS, val port: Int = Defaults.MIXED_PORT
    ) {
        val address: String get() = "$host:$port"
        val listenAddress: String get() = ":$port"
    }
}
