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


object RouteConfig {

    const val TUN_GATEWAY6 = "fdfe:dcba:9876::1"
    const val TUN_SUBNET_PREFIX6 = 126
    const val TUN_PORTAL6 = "fdfe:dcba:9876::2"
    const val TUN_DNS6 = "fdfe:dcba:9876::2"

    val BYPASS_PRIVATE_ROUTES = listOf(
        "1.0.0.0/8", "2.0.0.0/7", "4.0.0.0/6", "8.0.0.0/5",
        "16.0.0.0/4", "32.0.0.0/3", "64.0.0.0/2", "128.0.0.0/3",
        "160.0.0.0/5", "168.0.0.0/6", "172.0.0.0/12", "172.32.0.0/11",
        "172.64.0.0/10", "172.128.0.0/9", "173.0.0.0/8", "174.0.0.0/7",
        "176.0.0.0/4", "192.0.0.0/9", "192.128.0.0/11", "192.160.0.0/13",
        "192.169.0.0/16", "192.170.0.0/15", "192.172.0.0/14", "192.176.0.0/12",
        "192.192.0.0/10", "193.0.0.0/8", "194.0.0.0/7", "196.0.0.0/6",
        "200.0.0.0/5", "208.0.0.0/4", "224.0.0.0/3"
    )


    val BYPASS_PRIVATE_ROUTES_V6 = listOf(
        "::/1", "8000::/2", "c000::/3", "e000::/4",
        "f000::/5", "f800::/6", "fe00::/9", "fec0::/10"
    )


    val HTTP_PROXY_LOCAL_LIST = listOf(
        "localhost", "*.local", "127.*", "10.*",
        "172.16.*", "172.17.*", "172.18.*", "172.19.*",
        "172.2*", "172.30.*", "172.31.*", "192.168.*"
    )


    val HTTP_PROXY_BLACK_LIST = listOf(
        "*zhihu.com", "*zhimg.com", "*jd.com",
        "100ime-iat-api.xfyun.cn", "*360buyimg.com"
    )


    fun parseCidr(cidr: String): Pair<String, Int> {
        val parts = cidr.split('/')
        return Pair(parts[0], parts[1].toInt())
    }
}
