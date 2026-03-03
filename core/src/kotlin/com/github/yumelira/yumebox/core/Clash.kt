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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.core

import com.github.yumelira.yumebox.core.bridge.*
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.util.parseInetSocketAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.InetSocketAddress

object Clash {
    enum class OverrideSlot {
        Persist,
        Session
    }

    private val ConfigurationOverrideJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun reset() {
        Bridge.nativeReset()
    }

    fun forceGc() {
        Bridge.nativeForceGc()
    }

    fun suspendCore(suspended: Boolean) {
        Bridge.nativeSuspend(suspended)
    }

    fun queryTunnelState(): TunnelState {
        val json = Bridge.nativeQueryTunnelState()
        return Json.decodeFromString(TunnelState.serializer(), json)
    }

    fun queryTrafficNow(): Traffic {
        return Bridge.nativeQueryTrafficNow()
    }

    fun queryTrafficTotal(): Traffic {
        return Bridge.nativeQueryTrafficTotal()
    }

    fun notifyDnsChanged(dns: List<String>) {
        Bridge.nativeNotifyDnsChanged(dns.toSet().joinToString(separator = ","))
    }

    fun notifyTimeZoneChanged(name: String, offset: Int) {
        Bridge.nativeNotifyTimeZoneChanged(name, offset)
    }

    fun notifyInstalledAppsChanged(uids: List<Pair<Int, String>>) {
        val uidList = uids.joinToString(separator = ",") { "${it.first}:${it.second}" }
        Bridge.nativeNotifyInstalledAppChanged(uidList)
    }

    fun startTun(
        fd: Int,
        stack: String,
        gateway: String,
        portal: String,
        dns: String,
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int,
    ) {
        Bridge.nativeStartTun(
            fd, stack, gateway, portal, dns,
            object : TunInterface {
                override fun markSocket(fd: Int) {
                    markSocket(fd)
                }

                override fun querySocketUid(protocol: Int, source: String, target: String): Int {
                    return querySocketUid(
                        protocol,
                        parseInetSocketAddress(source),
                        parseInetSocketAddress(target),
                    )
                }
            },
        )
    }

    fun stopTun() {
        Bridge.nativeStopTun()
    }

    fun startHttp(listenAt: String): String? {
        return Bridge.nativeStartHttp(listenAt)
    }

    fun stopHttp() {
        Bridge.nativeStopHttp()
    }

    fun queryGroupNames(excludeNotSelectable: Boolean): List<String> {
        val names = Json.decodeFromString(
            JsonArray.serializer(),
            Bridge.nativeQueryGroupNames(excludeNotSelectable),
        )

        return names.map {
            require(it.jsonPrimitive.isString)
            it.jsonPrimitive.content
        }
    }

    fun queryProfileGroupNames(path: File, excludeNotSelectable: Boolean): List<String> {
        val namesJson = Bridge.nativeQueryProfileGroupNames(path.absolutePath, excludeNotSelectable)
            ?: return emptyList()
        val names = runCatching {
            Json.decodeFromString(JsonArray.serializer(), namesJson)
        }.getOrElse {
            return emptyList()
        }
        return names.map {
            require(it.jsonPrimitive.isString)
            it.jsonPrimitive.content
        }
    }

    fun queryProfileGroups(path: File, excludeNotSelectable: Boolean): List<ProxyGroup> {
        val groupsJson = Bridge.nativeQueryProfileGroups(path.absolutePath, excludeNotSelectable)
            ?: return emptyList()
        val groups = runCatching {
            Json.decodeFromString(JsonArray.serializer(), groupsJson)
        }.getOrElse {
            return emptyList()
        }
        return List(groups.size) {
            runCatching {
                Json.decodeFromJsonElement(ProxyGroup.serializer(), groups[it])
            }.getOrDefault(ProxyGroup(type = Proxy.Type.Unknown, proxies = emptyList(), now = ""))
        }
    }

    fun queryGroup(name: String, sort: ProxySort): ProxyGroup {
        return Bridge.nativeQueryGroup(name, sort.name)
            ?.let { Json.decodeFromString(ProxyGroup.serializer(), it) }
            ?: ProxyGroup(name = name, type = Proxy.Type.Unknown, proxies = emptyList(), now = "")
    }

    fun healthCheck(name: String): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            Bridge.nativeHealthCheck(this, name)
        }
    }

    fun healthCheckAll() {
        Bridge.nativeHealthCheckAll()
    }

    fun patchSelector(selector: String, name: String): Boolean {
        return Bridge.nativePatchSelector(selector, name)
    }

    fun fetchAndValid(
        path: File,
        url: String,
        force: Boolean,
        reportStatus: (FetchStatus) -> Unit,
    ): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            Bridge.nativeFetchAndValid(
                object : FetchCallback {
                    override fun report(statusJson: String) {
                        reportStatus(
                            Json.decodeFromString(
                                FetchStatus.serializer(),
                                statusJson,
                            ),
                        )
                    }

                    override fun complete(error: String?) {
                        if (error != null)
                            completeExceptionally(ClashException(error))
                        else
                            complete(Unit)
                    }
                },
                path.absolutePath,
                url,
                force,
            )
        }
    }

    fun load(path: File): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            Bridge.nativeLoad(this, path.absolutePath)
        }
    }

    fun queryProviders(): List<Provider> {
        val providers =
            Json.decodeFromString(JsonArray.serializer(), Bridge.nativeQueryProviders())

        return List(providers.size) {
            Json.decodeFromJsonElement(Provider.serializer(), providers[it])
        }
    }

    fun updateProvider(type: Provider.Type, name: String): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            Bridge.nativeUpdateProvider(this, type.toString(), name)
        }
    }

    fun queryOverride(slot: OverrideSlot): ConfigurationOverride {
        return try {
            ConfigurationOverrideJson.decodeFromString(
                ConfigurationOverride.serializer(),
                Bridge.nativeReadOverride(slot.ordinal),
            )
        } catch (e: Exception) {
            ConfigurationOverride()
        }
    }

    fun patchOverride(slot: OverrideSlot, configuration: ConfigurationOverride) {
        Bridge.nativeWriteOverride(
            slot.ordinal,
            ConfigurationOverrideJson.encodeToString(
                ConfigurationOverride.serializer(),
                configuration,
            ),
        )
    }


    fun clearOverride(slot: OverrideSlot) {
        Bridge.nativeClearOverride(slot.ordinal)
    }

    fun queryConfiguration(): UiConfiguration {
        return Json.decodeFromString(
            UiConfiguration.serializer(),
            Bridge.nativeQueryConfiguration(),
        )
    }

    fun subscribeLogcat(): ReceiveChannel<LogMessage> {
        return Channel<LogMessage>(32).apply {
            Bridge.nativeSubscribeLogcat(
                object : LogcatInterface {
                    override fun received(jsonPayload: String) {
                        trySend(Json.decodeFromString(LogMessage.serializer(), jsonPayload))
                    }
                },
            )
        }
    }

    fun setCustomUserAgent(userAgent: String) {
        Bridge.nativeSetCustomUserAgent(userAgent)
    }
}
