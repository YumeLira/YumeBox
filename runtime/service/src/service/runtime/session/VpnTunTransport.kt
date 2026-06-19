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

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.net.IpPrefix
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import com.github.yumelira.yumebox.core.util.parseInetSocketAddress
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.common.compat.pendingIntentFlags
import com.github.yumelira.yumebox.service.common.constants.Components
import com.github.yumelira.yumebox.service.common.util.SocketOwnerResolver
import com.github.yumelira.yumebox.service.runtime.config.AccessControlMode
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.util.buildIncludedRoutesFromExcludedCidrs
import com.github.yumelira.yumebox.service.runtime.util.parseCIDR
import java.net.InetAddress
import java.security.SecureRandom

class VpnTunTransport(
    private val vpnService: VpnService,
    private val store: ServiceStore = ServiceStore(),
) : RuntimeTransport {
    private val random = SecureRandom()
    private val startupLogStore =
        RuntimeStartupLogStore(vpnService, RuntimeStartupLogStore.Scope.LOCAL_TUN)
    private val ownerResolver = SocketOwnerResolver(vpnService)

    override fun start(spec: RuntimeSpec) {
        startupLogStore.append("LOCAL_TUN transport start: begin")
        val device =
            with(vpnService.Builder()) {
                val explicitRouteExcludes =
                    store.tunRouteExcludeAddress.map(String::trim).filter(String::isNotEmpty)
                val hasExplicitRouteExcludes = explicitRouteExcludes.isNotEmpty()
                val canUseExcludeRoute = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                val includedRoutes =
                    if (hasExplicitRouteExcludes && !canUseExcludeRoute) {
                        buildIncludedRoutesFromExcludedCidrs(
                            cidrs = explicitRouteExcludes,
                            includeIpv6 = store.allowIpv6,
                        )
                    } else {
                        null
                    }

                addAddress(TUN_GATEWAY, TUN_SUBNET_PREFIX)
                if (store.allowIpv6) {
                    addAddress(TUN_GATEWAY6, TUN_SUBNET_PREFIX6)
                }

                if (hasExplicitRouteExcludes && canUseExcludeRoute) {
                    addRoute(NET_ANY, 0)
                    if (store.allowIpv6) {
                        addRoute(NET_ANY6, 0)
                    }
                    explicitRouteExcludes.map(::parseCIDR).forEach {
                        runCatching {
                            excludeRoute(IpPrefix(InetAddress.getByName(it.ip), it.prefix))
                        }
                    }
                    addRoute(TUN_DNS, 32)
                    if (store.allowIpv6) {
                        addRoute(TUN_DNS6, 128)
                    }
                } else if (includedRoutes != null) {
                    includedRoutes.ipv4.forEach { addRoute(it.ip, it.prefix) }
                    if (store.allowIpv6) {
                        includedRoutes.ipv6.forEach { addRoute(it.ip, it.prefix) }
                    }
                    addRoute(TUN_DNS, 32)
                    if (store.allowIpv6) {
                        addRoute(TUN_DNS6, 128)
                    }
                } else if (store.bypassPrivateNetwork) {
                    vpnService.resources
                        .getStringArray(R.array.bypass_private_route)
                        .map(::parseCIDR)
                        .forEach { addRoute(it.ip, it.prefix) }
                    if (store.allowIpv6) {
                        vpnService.resources
                            .getStringArray(R.array.bypass_private_route6)
                            .map(::parseCIDR)
                            .forEach { addRoute(it.ip, it.prefix) }
                    }
                    addRoute(TUN_DNS, 32)
                    if (store.allowIpv6) {
                        addRoute(TUN_DNS6, 128)
                    }
                } else {
                    addRoute(NET_ANY, 0)
                    if (store.allowIpv6) {
                        addRoute(NET_ANY6, 0)
                    }
                }

                when (store.accessControlMode) {
                    AccessControlMode.AcceptAll -> Unit
                    AccessControlMode.AcceptSelected -> {
                        (store.accessControlPackages + vpnService.packageName).forEach {
                            runCatching { addAllowedApplication(it) }
                        }
                    }
                    AccessControlMode.RejectAll -> Unit
                    AccessControlMode.RejectSelected -> {
                        (store.accessControlPackages - vpnService.packageName).forEach {
                            runCatching { addDisallowedApplication(it) }
                        }
                    }
                }

                setBlocking(false)
                setMtu(TUN_MTU)
                setSession("Clash")
                addDnsServer(TUN_DNS)
                if (store.allowIpv6) {
                    addDnsServer(TUN_DNS6)
                }
                setConfigureIntent(
                    PendingIntent.getActivity(
                        vpnService,
                        R.id.nf_vpn_status,
                        Intent()
                            .setComponent(Components.PROXY_SHEET_ACTIVITY)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            ),
                        pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT),
                    )
                )

                if (Build.VERSION.SDK_INT >= 29) {
                    setMetered(false)
                }

                if (Build.VERSION.SDK_INT >= 29 && store.systemProxy) {
                    listenHttp()?.let {
                        setHttpProxy(
                            ProxyInfo.buildDirectProxy(
                                it.address.hostAddress,
                                it.port,
                                HTTP_PROXY_BLACK_LIST +
                                    if (store.bypassPrivateNetwork || hasExplicitRouteExcludes) {
                                        HTTP_PROXY_LOCAL_LIST
                                    } else {
                                        emptyList()
                                    },
                            )
                        )
                    }
                }

                if (store.allowBypass) {
                    allowBypass()
                }

                TunDevice(
                    fd = establish()?.detachFd() ?: error("Establish VPN rejected by system"),
                    stack = store.tunStackMode,
                    gateway =
                        "$TUN_GATEWAY/$TUN_SUBNET_PREFIX" +
                            if (store.allowIpv6) ",$TUN_GATEWAY6/$TUN_SUBNET_PREFIX6" else "",
                    portal = TUN_PORTAL + if (store.allowIpv6) ",$TUN_PORTAL6" else "",
                    dns =
                        if (store.dnsHijacking) NET_ANY
                        else (TUN_DNS + if (store.allowIpv6) ",$TUN_DNS6" else ""),
                )
            }

        com.github.yumelira.yumebox.core.Clash.startTun(
            fd = device.fd,
            stack = device.stack,
            gateway = device.gateway,
            portal = device.portal,
            dns = device.dns,
            markSocket = vpnService::protect,
            querySocketOwner = ownerResolver::queryOwner,
        )
        startupLogStore.append("LOCAL_TUN transport start: done")
    }

    override fun stop() {
        com.github.yumelira.yumebox.core.Clash.stopHttp()
        com.github.yumelira.yumebox.core.Clash.stopTun()
    }

    override fun onNetworkChanged() {
        if (Build.VERSION.SDK_INT in 22..28) {
            @Suppress("DEPRECATION") @TargetApi(22) vpnService.setUnderlyingNetworks(null)
        }
    }

    private fun listenHttp(): java.net.InetSocketAddress? {
        val r = { 1 + random.nextInt(199) }
        val listenAt = "127.${r()}.${r()}.${r()}:0"
        val address = com.github.yumelira.yumebox.core.Clash.startHttp(listenAt)
        return address?.let(::parseInetSocketAddress)
    }

    private data class TunDevice(
        val fd: Int,
        val stack: String,
        val gateway: String,
        val portal: String,
        val dns: String,
    )

    private companion object {
        private const val TUN_MTU = 9000
        private const val TUN_SUBNET_PREFIX = 30
        private const val TUN_GATEWAY = "172.19.0.1"
        private const val TUN_SUBNET_PREFIX6 = 126
        private const val TUN_GATEWAY6 = "fdfe:dcba:9876::1"
        private const val TUN_PORTAL = "172.19.0.2"
        private const val TUN_PORTAL6 = "fdfe:dcba:9876::2"
        private const val TUN_DNS = TUN_PORTAL
        private const val TUN_DNS6 = TUN_PORTAL6
        private const val NET_ANY = "0.0.0.0"
        private const val NET_ANY6 = "::"

        private val HTTP_PROXY_LOCAL_LIST =
            listOf(
                "localhost",
                "*.local",
                "127.*",
                "10.*",
                "172.16.*",
                "172.17.*",
                "172.18.*",
                "172.19.*",
                "172.2*",
                "172.30.*",
                "172.31.*",
                "192.168.*",
            )
        private val HTTP_PROXY_BLACK_LIST =
            listOf(
                "*zhihu.com",
                "*zhimg.com",
                "*jd.com",
                "100ime-iat-api.xfyun.cn",
                "*360buyimg.com",
            )
    }
}
