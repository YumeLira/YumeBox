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

package com.github.yumelira.yumebox.service.root

internal object RootTunConstants {
    const val IF_NAME = "Yume"
    const val MTU = 1500

    const val INET4 = "172.19.0.1/30"
    const val INET6 = "fdfe:dcba:9876::1/126"

    const val FAKE_IP_RANGE = "198.18.0.1/16"
    const val FAKE_IP_RANGE6 = "fc00::/18"

    val DEFAULT_DNS_HIJACK = listOf("any:53", "tcp://any:53")
}
