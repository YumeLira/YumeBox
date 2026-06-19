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

import android.content.Intent
import android.os.IBinder
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.service.common.util.initializeServiceGlobal
import com.github.yumelira.yumebox.service.runtime.session.RootTunTransport
import com.github.yumelira.yumebox.service.runtime.session.RuntimeSpec
import com.github.yumelira.yumebox.service.runtime.session.SessionRuntime
import com.github.yumelira.yumebox.service.runtime.session.SessionRuntimeSpecFactory
import com.tencent.mmkv.MMKV
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

class RootTunRootService : RootService() {
    private lateinit var runtime: SessionRuntime
    private lateinit var stateStore: RootTunStateStore
    private lateinit var startupLogStore: RootTunStartupLogStore
    private lateinit var runtimeSpecFactory: SessionRuntimeSpecFactory
    private lateinit var runtimeHost: RootTunRuntimeHost

    private val binder =
        object : IRootTunService.Stub() {
            override fun startRootTun(requestJson: String): String {
                val request = decodeRequest(requestJson)
                startupLogStore.append(
                    "ROOT_TUN root-service: binder branch=start source=${request.source}"
                )
                val spec = createSpec("start")
                startupLogStore.append(
                    "ROOT_TUN root-service: binder branch=start transport=${spec.transportFingerprint}"
                )
                stateStore.updateStatus(
                    stateStore
                        .snapshot()
                        .copy(
                            state = RootTunState.Starting,
                            running = true,
                            profileUuid = spec.profileUuid,
                            profileName = spec.profileName,
                            runtimeReady = false,
                            controllerReady = true,
                            startedAt = System.currentTimeMillis(),
                            staticPlanFingerprint = spec.staticPlanFingerprint,
                            transportFingerprint = spec.transportFingerprint,
                            overrideFingerprint = spec.effectiveFingerprint,
                            profileFingerprint = spec.profileFingerprint,
                            lastError = null,
                        )
                )
                return encodeResult(runtime.start(spec))
            }

            override fun restartRootTun(requestJson: String): String {
                val request = decodeRequest(requestJson)
                startupLogStore.append(
                    "ROOT_TUN root-service: binder branch=restart source=${request.source}"
                )
                return encodeResult(runtime.restart(createSpec("restart")))
            }

            override fun reloadActiveProfile(requestJson: String): String {
                val request = decodeRequest(requestJson)
                startupLogStore.append(
                    "ROOT_TUN root-service: binder branch=reload source=${request.source}"
                )
                val spec = createSpec("reload")
                val currentTransport = stateStore.snapshot().transportFingerprint
                return if (
                    currentTransport != null && currentTransport != spec.transportFingerprint
                ) {
                    startupLogStore.append(
                        "ROOT_TUN root-service: binder branch=reload action=restart currentTransport=$currentTransport nextTransport=${spec.transportFingerprint}"
                    )
                    encodeResult(runtime.restart(spec))
                } else {
                    startupLogStore.append(
                        "ROOT_TUN root-service: binder branch=reload action=reload currentTransport=$currentTransport nextTransport=${spec.transportFingerprint}"
                    )
                    encodeResult(runtime.reload(spec))
                }
            }

            override fun stopRootTun(): String {
                val result = runtime.stop()
                if (result.success) {
                    stopSelf()
                }
                return encodeResult(result)
            }

            override fun queryStatus(): String {
                return RootTunJson.Default.encodeToString(
                    RootTunStatus.serializer(),
                    stateStore.snapshot(),
                )
            }

            override fun queryTunnelStateJson(): String {
                return RootTunJson.Default.encodeToString(
                    com.github.yumelira.yumebox.core.model.TunnelState.serializer(),
                    runtime.queryTunnelState(),
                )
            }

            override fun queryTrafficNow(): Long = runtime.queryTrafficNow()

            override fun queryTrafficTotal(): Long = runtime.queryTrafficTotal()

            override fun queryConnectionsJson(): String {
                return RootTunJson.Default.encodeToString(
                    com.github.yumelira.yumebox.core.model.ConnectionSnapshot.serializer(),
                    runtime.queryConnections(),
                )
            }

            override fun queryAllProxyGroupsJson(excludeNotSelectable: Boolean): String {
                return RootTunJson.Default.encodeToString(
                    ListSerializer(com.github.yumelira.yumebox.core.model.ProxyGroup.serializer()),
                    runtime.queryAllProxyGroups(excludeNotSelectable),
                )
            }

            override fun queryProxyGroupNamesJson(excludeNotSelectable: Boolean): String {
                return RootTunJson.Default.encodeToString(
                    ListSerializer(String.serializer()),
                    runtime.queryProxyGroupNames(excludeNotSelectable),
                )
            }

            override fun queryProxyGroupJson(name: String, sort: String): String {
                return RootTunJson.Default.encodeToString(
                    com.github.yumelira.yumebox.core.model.ProxyGroup.serializer(),
                    runtime.queryProxyGroup(
                        name,
                        com.github.yumelira.yumebox.core.model.ProxySort.valueOf(sort),
                    ),
                )
            }

            override fun queryConfigurationJson(): String =
                RootTunJson.Default.encodeToString(
                    com.github.yumelira.yumebox.core.model.UiConfiguration.serializer(),
                    runtime.queryConfiguration(),
                )

            override fun queryProvidersJson(): String =
                RootTunJson.Default.encodeToString(
                    ListSerializer(com.github.yumelira.yumebox.core.model.Provider.serializer()),
                    runtime.queryProviders(),
                )

            override fun patchSelector(group: String, name: String): Boolean {
                return runtime.patchSelector(group, name)
            }

            override fun closeConnection(id: String): Boolean {
                return runtime.closeConnection(id)
            }

            override fun closeAllConnections() {
                runtime.closeAllConnections()
            }

            override fun healthCheck(group: String): String? = runBlocking {
                runtime.healthCheck(group)
            }

            override fun healthCheckProxy(group: String, proxyName: String): String = runBlocking {
                runtime.healthCheckProxy(group, proxyName)
            }

            override fun updateProvider(type: String, name: String): String? = runBlocking {
                runtime.updateProvider(type, name)
            }

            override fun requestStop() {
                runtime.requestStop()
                runtime.stop()
                stopSelf()
            }

            override fun queryRecentLogsJson(sinceSeq: Long): String {
                return RootTunJson.Default.encodeToString(
                    RootTunLogChunk.serializer(),
                    RootTunLogChunk.from(runtime.queryRecentLogsJson(sinceSeq)),
                )
            }
        }

    override fun onCreate() {
        super.onCreate()
        Global.init(this)
        initializeServiceGlobal(this)
        MMKV.initialize(this)
        stateStore = RootTunStateStore(this)
        startupLogStore = RootTunStartupLogStore(this)
        runtimeSpecFactory = SessionRuntimeSpecFactory(this)
        runtimeHost = RootTunRuntimeHost(this, stateStore)
        startupLogStore.append("ROOT_TUN root-service: onCreate")
        runtime = SessionRuntime(host = runtimeHost, transport = RootTunTransport())
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return false
    }

    override fun onDestroy() {
        if (this::runtime.isInitialized && runtime.snapshot().phase.running) {
            runtime.requestStop("runtime destroyed")
            runtime.destroy()
        }
        super.onDestroy()
    }

    private fun decodeRequest(requestJson: String): RootTunStartRequest {
        return RootTunJson.Default.decodeFromString(RootTunStartRequest.serializer(), requestJson)
    }

    private fun createSpec(action: String): RuntimeSpec {
        startupLogStore.append("ROOT_TUN root-service: spec create begin action=$action")
        val spec = runtimeSpecFactory.createRootTunSpec()
        startupLogStore.append(
            "ROOT_TUN root-service: spec create done action=$action profile=${spec.profileUuid} transport=${spec.transportFingerprint}"
        )
        return spec
    }

    private fun encodeResult(
        result: com.github.yumelira.yumebox.service.runtime.session.RuntimeOperationResult
    ): String {
        return RootTunJson.Default.encodeToString(
            RootTunOperationResult.serializer(),
            RootTunOperationResult(success = result.success, error = result.error),
        )
    }
}
