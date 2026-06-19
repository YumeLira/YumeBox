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

package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.core.model.RootTunDnsMode
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.tencent.mmkv.MMKV

class NetworkSettingsStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val proxyMode by enumFlow(ProxyMode.Tun)

    val bypassPrivateNetwork by boolFlow(true)
    val dnsHijack by boolFlow(true)
    val allowBypass by boolFlow(true)
    val enableIPv6 by boolFlow(false)
    val systemProxy by boolFlow(true)

    val tunStack by enumFlow(TunStack.System)
    val tunRouteExcludeAddress by stringListFlow(emptyList())
    val rootTunIfName by strFlow("Yume")
    val rootTunMtu by intFlow(1500)
    val rootTunAutoRoute by boolFlow(true)
    val rootTunStrictRoute by boolFlow(true)
    val rootTunAutoRedirect by boolFlow(true)
    val rootTunIncludeAndroidUser by intListFlow(listOf(0, 10))
    val rootTunRouteExcludeAddress by stringListFlow(emptyList())
    val rootTunDnsMode by enumFlow(RootTunDnsMode.RedirHost)
    val rootTunFakeIpRange by strFlow("198.18.0.1/16")
    val rootTunFakeIpRange6 by strFlow("fc00::/18")
    val accessControlMode by enumFlow(AccessControlMode.ALLOW_ALL)
    val accessControlPackages by stringSetFlow(emptySet())
}
