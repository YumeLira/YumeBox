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

package com.github.yumelira.yumebox.service.runtime.config

import com.github.yumelira.yumebox.core.model.RootTunDnsMode
import com.tencent.mmkv.MMKV
import java.util.*
import kotlinx.serialization.json.Json

class ServiceStore {
    private val store = Store(MMKV.mmkvWithID("service", MMKV.MULTI_PROCESS_MODE).asStoreProvider())
    private val networkSettings = MMKV.mmkvWithID("network_settings", MMKV.MULTI_PROCESS_MODE)

    private fun readBoolean(newKey: String, legacyKey: String, defaultValue: Boolean): Boolean {
        return when {
            networkSettings.containsKey(newKey) -> networkSettings.decodeBool(newKey, defaultValue)
            else -> store.provider.getBoolean(legacyKey, defaultValue)
        }
    }

    private fun readString(newKey: String, legacyKey: String, defaultValue: String): String {
        return when {
            networkSettings.containsKey(newKey) ->
                networkSettings.decodeString(newKey, defaultValue) ?: defaultValue
            else -> store.provider.getString(legacyKey, defaultValue)
        }
    }

    private fun readInt(newKey: String, legacyKey: String, defaultValue: Int): Int {
        return when {
            networkSettings.containsKey(newKey) -> networkSettings.decodeInt(newKey, defaultValue)
            else -> store.provider.getInt(legacyKey, defaultValue)
        }
    }

    private fun readStringSet(
        newKey: String,
        legacyKey: String,
        defaultValue: Set<String>,
    ): Set<String> {
        return when {
            networkSettings.containsKey(newKey) ->
                networkSettings.decodeStringSet(newKey, defaultValue) ?: defaultValue
            else -> store.provider.getStringSet(legacyKey, defaultValue)
        }
    }

    private fun readStringList(newKey: String, defaultValue: List<String>): List<String> {
        if (!networkSettings.containsKey(newKey)) return defaultValue
        return runCatching {
                networkSettings.decodeString(newKey)?.let {
                    json.decodeFromString<List<String>>(it)
                } ?: defaultValue
            }
            .getOrDefault(defaultValue)
    }

    private fun readIntList(newKey: String, defaultValue: List<Int>): List<Int> {
        if (!networkSettings.containsKey(newKey)) return defaultValue
        return runCatching {
                networkSettings.decodeString(newKey)?.let { json.decodeFromString<List<Int>>(it) }
                    ?: defaultValue
            }
            .getOrDefault(defaultValue)
    }

    private inline fun <reified T : Enum<T>> readEnum(newKey: String, defaultValue: T): T {
        if (!networkSettings.containsKey(newKey)) return defaultValue
        val raw = networkSettings.decodeString(newKey, defaultValue.name) ?: defaultValue.name
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(defaultValue)
    }

    var activeProfile: UUID? by
        store.typedString(
            key = "active_profile",
            from = { if (it.isBlank()) null else UUID.fromString(it) },
            to = { it?.toString() ?: "" },
        )

    var bypassPrivateNetwork: Boolean
        get() =
            readBoolean(
                newKey = "bypassPrivateNetwork",
                legacyKey = "bypass_private_network",
                defaultValue = true,
            )
        set(value) {
            networkSettings.encode("bypassPrivateNetwork", value)
            store.provider.setBoolean("bypass_private_network", value)
        }

    private var accessControlModeRaw: String
        get() =
            readString(
                newKey = "accessControlMode",
                legacyKey = "access_control_mode",
                defaultValue = AccessControlMode.AcceptAll.name,
            )
        set(value) {
            networkSettings.encode("accessControlMode", value)
            store.provider.setString("access_control_mode", value)
        }

    var accessControlMode: AccessControlMode
        get() =
            when (accessControlModeRaw) {
                AccessControlMode.AcceptAll.name,
                "ALLOW_ALL" -> AccessControlMode.AcceptAll

                AccessControlMode.AcceptSelected.name,
                "ALLOW_SPECIFIC" -> AccessControlMode.AcceptSelected

                AccessControlMode.RejectAll.name,
                "DENY_ALL" -> AccessControlMode.RejectAll

                AccessControlMode.RejectSelected.name,
                "DENY_SPECIFIC" -> AccessControlMode.RejectSelected

                else -> AccessControlMode.AcceptAll
            }
        set(value) {
            accessControlModeRaw = value.name
        }

    var accessControlPackages: Set<String>
        get() =
            readStringSet(
                newKey = "accessControlPackages",
                legacyKey = "access_control_packages",
                defaultValue = emptySet(),
            )
        set(value) {
            networkSettings.encode("accessControlPackages", value)
            store.provider.setStringSet("access_control_packages", value)
        }

    var dnsHijacking: Boolean
        get() = readBoolean(newKey = "dnsHijack", legacyKey = "dns_hijacking", defaultValue = true)
        set(value) {
            networkSettings.encode("dnsHijack", value)
            store.provider.setBoolean("dns_hijacking", value)
        }

    var systemProxy: Boolean
        get() = readBoolean(newKey = "systemProxy", legacyKey = "system_proxy", defaultValue = true)
        set(value) {
            networkSettings.encode("systemProxy", value)
            store.provider.setBoolean("system_proxy", value)
        }

    var allowBypass: Boolean
        get() = readBoolean(newKey = "allowBypass", legacyKey = "allow_bypass", defaultValue = true)
        set(value) {
            networkSettings.encode("allowBypass", value)
            store.provider.setBoolean("allow_bypass", value)
        }

    var allowIpv6: Boolean
        get() =
            when {
                networkSettings.containsKey("enableIPv6") ->
                    networkSettings.decodeBool("enableIPv6", false)
                else -> store.provider.getBoolean("allow_ipv6", false)
            }
        set(value) {
            networkSettings.encode("enableIPv6", value)
            store.provider.setBoolean("allow_ipv6", value)
        }

    var tunStackMode: String
        get() {
            if (networkSettings.containsKey("tunStack")) {
                return when (networkSettings.decodeString("tunStack", "System")) {
                    "System",
                    "system" -> "system"
                    "GVisor",
                    "gvisor" -> "gvisor"
                    "Mixed",
                    "mixed" -> "mixed"
                    else -> "system"
                }
            }
            return store.provider.getString("tun_stack_mode", "system")
        }
        set(value) {
            val normalized = value.lowercase()
            store.provider.setString("tun_stack_mode", normalized)
            networkSettings.encode(
                "tunStack",
                when (normalized) {
                    "system" -> "System"
                    "gvisor" -> "GVisor"
                    "mixed" -> "Mixed"
                    else -> "System"
                },
            )
        }

    var tunRouteExcludeAddress: List<String>
        get() = readStringList("tunRouteExcludeAddress", emptyList())
        set(value) {
            networkSettings.encode("tunRouteExcludeAddress", json.encodeToString(value))
        }

    var rootTunIfName: String
        get() = readString("rootTunIfName", "root_tun_if_name", "Yume")
        set(value) {
            networkSettings.encode("rootTunIfName", value)
            store.provider.setString("root_tun_if_name", value)
        }

    var rootTunMtu: Int
        get() = readInt("rootTunMtu", "root_tun_mtu", 1500)
        set(value) {
            networkSettings.encode("rootTunMtu", value)
            store.provider.setInt("root_tun_mtu", value)
        }

    var rootTunAutoRoute: Boolean
        get() = readBoolean("rootTunAutoRoute", "root_tun_auto_route", true)
        set(value) {
            networkSettings.encode("rootTunAutoRoute", value)
            store.provider.setBoolean("root_tun_auto_route", value)
        }

    var rootTunStrictRoute: Boolean
        get() = readBoolean("rootTunStrictRoute", "root_tun_strict_route", true)
        set(value) {
            networkSettings.encode("rootTunStrictRoute", value)
            store.provider.setBoolean("root_tun_strict_route", value)
        }

    var rootTunAutoRedirect: Boolean
        get() = readBoolean("rootTunAutoRedirect", "root_tun_auto_redirect", true)
        set(value) {
            networkSettings.encode("rootTunAutoRedirect", value)
            store.provider.setBoolean("root_tun_auto_redirect", value)
        }

    var rootTunIncludeAndroidUser: List<Int>
        get() = readIntList("rootTunIncludeAndroidUser", listOf(0, 10))
        set(value) {
            networkSettings.encode("rootTunIncludeAndroidUser", json.encodeToString(value))
        }

    var rootTunRouteExcludeAddress: List<String>
        get() = readStringList("rootTunRouteExcludeAddress", emptyList())
        set(value) {
            networkSettings.encode("rootTunRouteExcludeAddress", json.encodeToString(value))
        }

    var rootTunDnsMode: RootTunDnsMode
        get() = readEnum("rootTunDnsMode", RootTunDnsMode.RedirHost)
        set(value) {
            networkSettings.encode("rootTunDnsMode", value.name)
        }

    var rootTunFakeIpRange: String
        get() = readString("rootTunFakeIpRange", "root_tun_fake_ip_range", "198.18.0.1/16")
        set(value) {
            networkSettings.encode("rootTunFakeIpRange", value)
            store.provider.setString("root_tun_fake_ip_range", value)
        }

    var rootTunFakeIpRange6: String
        get() = readString("rootTunFakeIpRange6", "root_tun_fake_ip_range6", "fc00::/18")
        set(value) {
            networkSettings.encode("rootTunFakeIpRange6", value)
            store.provider.setString("root_tun_fake_ip_range6", value)
        }

    var showTrafficNotification by
        store.boolean(key = "show_traffic_notification", defaultValue = true)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
