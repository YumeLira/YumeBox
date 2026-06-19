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

import android.content.Context
import com.github.yumelira.yumebox.core.model.RootTunConfig
import com.github.yumelira.yumebox.core.model.RootTunDnsMode
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.github.yumelira.yumebox.service.runtime.util.directoryLastModified
import com.github.yumelira.yumebox.service.runtime.util.importedDir
import java.io.File
import java.util.*

class RootTunConfigFactory(
    private val context: Context,
    private val store: ServiceStore = ServiceStore(),
) {
    private val packageResolver = RootTunPackageResolver(context, store)
    private val startupLogStore = RootTunStartupLogStore(context)

    data class StaticRootTunPlan(
        val fingerprint: String,
        val ifName: String,
        val mtu: Int,
        val dnsHijack: List<String>,
        val autoRoute: Boolean,
        val strictRoute: Boolean,
        val autoRedirect: Boolean,
        val includeUid: List<Int>,
        val excludeUid: List<Int>,
        val includeAndroidUser: List<Int>,
        val routeAddress: List<String>,
        val routeExcludeAddress: List<String>,
        val dnsMode: RootTunDnsMode,
        val fakeIpRange: String?,
        val fakeIpRange6: String?,
        val allowIpv6: Boolean,
        val missingPackages: Set<String>,
    )

    data class DynamicRootTunOverrides(
        val transportFingerprint: String,
        val profileFingerprint: String,
    )

    data class Result(
        val profileUuid: UUID,
        val profileName: String,
        val profileDir: File,
        val staticPlan: StaticRootTunPlan,
        val dynamicOverrides: DynamicRootTunOverrides,
        val config: RootTunConfig,
    )

    fun create(): Result {
        val startedAt = System.currentTimeMillis()
        startupLogStore.clear()
        startupLogStore.append(startupLogStore.formatProfilesStoreLine())

        startupLogStore.append("ROOT_TUN factory: resolve active profile")
        val activeProfile = store.activeProfile ?: error("No active profile selected")
        val imported =
            ImportedDao.queryByUUID(activeProfile)
                ?: error("Active profile metadata not found: $activeProfile")
        val profileDir = context.importedDir.resolve(imported.uuid.toString())
        startupLogStore.append(
            "ROOT_TUN factory: activeProfile=${imported.uuid} name=${imported.name}"
        )

        val staticPlanResolveAt = System.currentTimeMillis()
        startupLogStore.append("ROOT_TUN factory: resolve static transport plan")
        val staticPlan = resolveStaticPlan()
        val staticPlanResolveCost = System.currentTimeMillis() - staticPlanResolveAt
        startupLogStore.append(
            "ROOT_TUN factory: static transport plan done ${staticPlanResolveCost}ms"
        )

        val stack = firstNonBlank(store.tunStackMode) ?: "system"
        val allowIpv6 = staticPlan.allowIpv6
        val config =
            RootTunConfig(
                ifName = staticPlan.ifName,
                mtu = staticPlan.mtu,
                stack = stack,
                inet4Address = listOf(RootTunConstants.INET4),
                inet6Address = if (allowIpv6) listOf(RootTunConstants.INET6) else emptyList(),
                dnsHijack = staticPlan.dnsHijack,
                autoRoute = staticPlan.autoRoute,
                strictRoute = staticPlan.strictRoute,
                autoRedirect = staticPlan.autoRedirect,
                includeUid = staticPlan.includeUid,
                excludeUid = staticPlan.excludeUid,
                includeAndroidUser = staticPlan.includeAndroidUser,
                routeAddress = staticPlan.routeAddress,
                routeExcludeAddress = staticPlan.routeExcludeAddress,
                dnsMode = staticPlan.dnsMode,
                fakeIpRange = staticPlan.fakeIpRange,
                fakeIpRange6 = staticPlan.fakeIpRange6,
                allowIpv6 = allowIpv6,
                debugLogPath = startupLogStore.path(),
            )
        val transportFingerprint = buildTransportFingerprint(config)
        val dynamicOverrides =
            DynamicRootTunOverrides(
                transportFingerprint = transportFingerprint,
                profileFingerprint =
                    buildProfileFingerprint(imported.uuid, profileDir.directoryLastModified ?: -1L),
            )
        startupLogStore.append(
            "ROOT_TUN factory: derived RootTunConfig transportFingerprint=$transportFingerprint"
        )

        startupLogStore.append(
            "ROOT_TUN factory: timings staticPlan=${staticPlanResolveCost}ms total=${System.currentTimeMillis() - startedAt}ms"
        )

        val summary = buildString {
            append("ROOT_TUN factory: includeUid=")
            append(config.includeUid.size)
            append(", excludeUid=")
            append(config.excludeUid)
            append(", dnsHijack=")
            append(config.dnsHijack)
            append(", routeAddress=")
            append(config.routeAddress.size)
            if (staticPlan.missingPackages.isNotEmpty()) {
                append(", missingPackages=")
                append(staticPlan.missingPackages)
            }
        }
        startupLogStore.append(summary)
        startupLogStore.append(
            "ROOT_TUN factory: config=" +
                RootTunJson.Default.encodeToString(RootTunConfig.serializer(), config)
        )

        return Result(
            profileUuid = imported.uuid,
            profileName = imported.name,
            profileDir = profileDir,
            staticPlan = staticPlan,
            dynamicOverrides = dynamicOverrides,
            config = config,
        )
    }

    private fun resolveStaticPlan(): StaticRootTunPlan {
        val fingerprint = buildStaticPlanFingerprint()
        val cached = cachedStaticPlan
        if (cached != null && cached.fingerprint == fingerprint) {
            return cached
        }

        val uidPlan = packageResolver.resolve()
        val dnsMode = store.rootTunDnsMode
        val plan =
            StaticRootTunPlan(
                fingerprint = fingerprint,
                ifName = firstNonBlank(store.rootTunIfName) ?: RootTunConstants.IF_NAME,
                mtu = store.rootTunMtu.coerceAtLeast(1),
                dnsHijack = resolveDnsHijack(),
                autoRoute = store.rootTunAutoRoute,
                strictRoute = store.rootTunStrictRoute,
                autoRedirect = store.rootTunAutoRedirect,
                includeUid = uidPlan.includeUid,
                excludeUid = uidPlan.excludeUid,
                includeAndroidUser =
                    store.rootTunIncludeAndroidUser
                        .filter { it >= 0 }
                        .distinct()
                        .sorted()
                        .ifEmpty { listOf(0, 10) },
                routeAddress = resolveRouteAddress(store.allowIpv6),
                routeExcludeAddress =
                    store.rootTunRouteExcludeAddress.map(String::trim).filter(String::isNotEmpty),
                dnsMode = dnsMode,
                fakeIpRange =
                    resolveFakeIpRange(
                        dnsMode,
                        store.rootTunFakeIpRange,
                        RootTunConstants.FAKE_IP_RANGE,
                    ),
                fakeIpRange6 =
                    resolveFakeIpRange(
                        dnsMode,
                        store.rootTunFakeIpRange6,
                        RootTunConstants.FAKE_IP_RANGE6,
                    ),
                allowIpv6 = store.allowIpv6,
                missingPackages = uidPlan.missingPackages,
            )
        cachedStaticPlan = plan
        return plan
    }

    private fun buildStaticPlanFingerprint(): String {
        return buildString {
                append(store.accessControlMode.name)
                append('|')
                append(store.accessControlPackages.sorted().joinToString(","))
                append('|')
                append(store.allowIpv6)
                append('|')
                append(store.bypassPrivateNetwork)
                append('|')
                append(store.dnsHijacking)
                append('|')
                append(store.tunStackMode)
                append('|')
                append(store.rootTunIfName.trim())
                append('|')
                append(store.rootTunMtu)
                append('|')
                append(store.rootTunAutoRoute)
                append('|')
                append(store.rootTunStrictRoute)
                append('|')
                append(store.rootTunAutoRedirect)
                append('|')
                append(store.rootTunIncludeAndroidUser.joinToString(","))
                append('|')
                append(store.rootTunRouteExcludeAddress.joinToString(","))
                append('|')
                append(store.rootTunDnsMode.name)
                append('|')
                append(store.rootTunFakeIpRange.trim())
                append('|')
                append(store.rootTunFakeIpRange6.trim())
            }
            .hashCode()
            .toString()
    }

    private fun buildTransportFingerprint(config: RootTunConfig): String {
        return listOf(
                config.ifName,
                config.mtu.toString(),
                config.stack,
                config.dnsMode.name,
                config.fakeIpRange.orEmpty(),
                config.fakeIpRange6.orEmpty(),
                config.allowIpv6.toString(),
                config.dnsHijack.joinToString(","),
                config.routeAddress.joinToString(","),
                config.routeExcludeAddress.joinToString(","),
                config.includeUid.joinToString(","),
                config.excludeUid.joinToString(","),
                config.includeAndroidUser.joinToString(","),
                config.autoRoute.toString(),
                config.strictRoute.toString(),
                config.autoRedirect.toString(),
            )
            .joinToString("|")
            .hashCode()
            .toString()
    }

    private fun buildProfileFingerprint(profileUuid: UUID, updatedAt: Long): String {
        return "$profileUuid|$updatedAt"
    }

    private fun resolveDnsHijack(): List<String> {
        if (!store.dnsHijacking) return emptyList()
        return RootTunConstants.DEFAULT_DNS_HIJACK
    }

    private fun resolveRouteAddress(allowIpv6: Boolean): List<String> {
        if (!store.bypassPrivateNetwork) return emptyList()

        val values = buildList {
            addAll(
                context.resources.getStringArray(
                    com.github.yumelira.yumebox.runtime.service.R.array.bypass_private_route
                )
            )
            if (allowIpv6) {
                addAll(
                    context.resources.getStringArray(
                        com.github.yumelira.yumebox.runtime.service.R.array.bypass_private_route6
                    )
                )
            }
        }

        return values.map(String::trim).filter(String::isNotEmpty)
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun resolveFakeIpRange(
        dnsMode: RootTunDnsMode,
        value: String,
        fallback: String,
    ): String? {
        if (dnsMode != RootTunDnsMode.FakeIp) return null
        return firstNonBlank(value, fallback)
    }

    companion object {
        @Volatile private var cachedStaticPlan: StaticRootTunPlan? = null
    }
}
