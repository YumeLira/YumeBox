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

package com.github.yumelira.yumebox.runtime.client.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.service.RootTunService
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.root.*
import com.topjohnwu.superuser.ipc.RootService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

object RootTunController {
    private val mutex = Mutex()

    @Volatile private var binder: IRootTunService? = null

    @Volatile private var connection: ServiceConnection? = null

    private suspend fun <T> remoteCall(
        context: Context,
        onBinderFailure: (() -> T)? = null,
        block: (IRootTunService) -> T,
    ): T {
        val appContext = context.appContextOrSelf
        return withContext(Dispatchers.IO) {
            try {
                block(bind(appContext))
            } catch (error: Throwable) {
                if (RootTunRuntimeRecovery.isBinderConnectionFailure(error)) {
                    invalidateConnection(
                        appContext,
                        RootTunRuntimeRecovery.binderFailureReason(error),
                    )
                    onBinderFailure?.let {
                        return@withContext it()
                    }
                }
                throw error
            }
        }
    }

    suspend fun start(context: Context): RootTunOperationResult {
        val appContext = context.appContextOrSelf
        val startupLogStore = RootTunStartupLogStore(appContext)
        val stateStore = RootTunStateStore(appContext)
        val startAt = System.currentTimeMillis()

        val request = RootTunStartRequest(source = "controller.start")
        startupLogStore.append(
            "ROOT_TUN controller: prepare=${System.currentTimeMillis() - startAt}ms"
        )
        val foregroundStartAt = System.currentTimeMillis()
        val current = stateStore.snapshot()
        stateStore.updateStatus(
            current.copy(
                state = RootTunState.Starting,
                running = true,
                runtimeReady = false,
                controllerReady = true,
                startedAt = startAt,
                lastError = null,
            )
        )
        RootTunService.start(appContext)
        startupLogStore.append(
            "ROOT_TUN controller: fgService=${System.currentTimeMillis() - foregroundStartAt}ms"
        )
        return start(appContext, request, stateStore, startupLogStore, startAt)
    }

    private suspend fun start(
        context: Context,
        request: RootTunStartRequest,
        stateStore: RootTunStateStore,
        startupLogStore: RootTunStartupLogStore,
        startedAt: Long,
    ): RootTunOperationResult {
        val appContext = context.appContextOrSelf
        val remoteStartAt = System.currentTimeMillis()
        val result =
            runCatching {
                    remoteCall(context) { service ->
                        val resultJson =
                            service.startRootTun(
                                RootTunJson.Default.encodeToString(
                                    RootTunStartRequest.serializer(),
                                    request,
                                )
                            )
                        RootTunJson.Default.decodeFromString(
                            RootTunOperationResult.serializer(),
                            resultJson,
                        )
                    }
                }
                .getOrElse { error ->
                    rollbackFailedStart(
                        context = appContext,
                        stateStore = stateStore,
                        error = error.message ?: "RootTun start failed",
                    )
                    startupLogStore.append(
                        "ROOT_TUN controller: total=${System.currentTimeMillis() - startedAt}ms failed=${error.message}"
                    )
                    return RootTunOperationResult(
                        success = false,
                        error = error.message ?: "RootTun start failed",
                    )
                }
        startupLogStore.append(
            "ROOT_TUN controller: remoteStart=${System.currentTimeMillis() - remoteStartAt}ms"
        )
        if (!result.success) {
            rollbackFailedStart(
                context = appContext,
                stateStore = stateStore,
                error = result.error ?: "RootTun start failed",
            )
            startupLogStore.append(
                "ROOT_TUN controller: total=${System.currentTimeMillis() - startedAt}ms"
            )
            return result
        }

        return try {
            runCatching { stateStore.updateStatus(queryStatus(context)) }
            startupLogStore.append(
                "ROOT_TUN controller: total=${System.currentTimeMillis() - startedAt}ms"
            )
            result
        } catch (error: Throwable) {
            val rollbackResult =
                withContext(Dispatchers.IO) {
                    runCatching {
                            val resultJson = bind(context).stopRootTun()
                            RootTunJson.Default.decodeFromString(
                                RootTunOperationResult.serializer(),
                                resultJson,
                            )
                        }
                        .getOrNull()
                }
            val appContext = context.appContextOrSelf
            runCatching { RootTunService.stop(appContext) }
            runCatching { RootService.stop(createIntent(appContext)) }
            disconnect()

            val message = buildString {
                append(error.message ?: "failed to start RootTun foreground service")
                rollbackResult
                    ?.error
                    ?.takeIf { it.isNotBlank() }
                    ?.let { append(" | rollback: ").append(it) }
            }
            startupLogStore.append(
                "ROOT_TUN controller: total=${System.currentTimeMillis() - startedAt}ms failed=$message"
            )
            stateStore.markIdle(message)
            RootTunOperationResult(success = false, error = message)
        }
    }

    suspend fun reload(context: Context): RootTunOperationResult {
        if (!isRuntimeActive(context)) {
            return RootTunOperationResult(success = true)
        }

        val appContext = context.appContextOrSelf
        val startupLogStore = RootTunStartupLogStore(appContext)
        val stateStore = RootTunStateStore(appContext)
        val currentStatus = stateStore.snapshot()
        val request = RootTunStartRequest(source = "controller.reload")
        startupLogStore.append(
            "ROOT_TUN controller: reload request currentTransport=${currentStatus.transportFingerprint}"
        )

        val result =
            runCatching {
                    remoteCall(appContext) { service ->
                        startupLogStore.append("ROOT_TUN controller: reload branch=service")
                        val resultJson =
                            service.reloadActiveProfile(
                                RootTunJson.Default.encodeToString(
                                    RootTunStartRequest.serializer(),
                                    request,
                                )
                            )
                        RootTunJson.Default.decodeFromString(
                            RootTunOperationResult.serializer(),
                            resultJson,
                        )
                    }
                }
                .getOrElse { error ->
                    return RootTunOperationResult(
                        success = false,
                        error = error.message ?: "RootTun reload failed",
                    )
                }
        if (result.success) {
            runCatching { stateStore.updateStatus(queryStatus(appContext)) }
        }
        return result
    }

    suspend fun stop(context: Context): RootTunOperationResult {
        if (!isRuntimeActive(context)) {
            return RootTunOperationResult(success = true)
        }

        val result =
            remoteCall(
                context = context,
                onBinderFailure = { RootTunOperationResult(success = true) },
            ) { service ->
                val resultJson = service.stopRootTun()
                RootTunJson.Default.decodeFromString(
                    RootTunOperationResult.serializer(),
                    resultJson,
                )
            }
        disconnect()
        return result
    }

    suspend fun requestStop(context: Context) {
        if (!isRuntimeActive(context)) return
        remoteCall(context = context, onBinderFailure = {}) { service -> service.requestStop() }
        disconnect()
    }

    suspend fun queryStatus(context: Context): RootTunStatus {
        return remoteCall(context) { service ->
            val statusJson = service.queryStatus()
            RootTunJson.Default.decodeFromString(RootTunStatus.serializer(), statusJson)
        }
    }

    suspend fun queryTunnelState(context: Context): TunnelState {
        return remoteCall(context) { service ->
            RootTunJson.Default.decodeFromString(
                TunnelState.serializer(),
                service.queryTunnelStateJson(),
            )
        }
    }

    suspend fun queryTrafficNow(context: Context): Long {
        return remoteCall(context) { service -> service.queryTrafficNow() }
    }

    suspend fun queryTrafficTotal(context: Context): Long {
        return remoteCall(context) { service -> service.queryTrafficTotal() }
    }

    suspend fun queryConnections(context: Context): ConnectionSnapshot {
        return remoteCall(context) { service ->
            RootTunJson.Default.decodeFromString(
                ConnectionSnapshot.serializer(),
                service.queryConnectionsJson(),
            )
        }
    }

    suspend fun queryProxyGroupNames(
        context: Context,
        excludeNotSelectable: Boolean,
    ): List<String> {
        return remoteCall(context) { service ->
            RootTunJson.Default.decodeFromString(
                ListSerializer(String.serializer()),
                service.queryProxyGroupNamesJson(excludeNotSelectable),
            )
        }
    }

    suspend fun queryAllProxyGroups(
        context: Context,
        excludeNotSelectable: Boolean,
    ): List<ProxyGroup> {
        return remoteCall(context) { service ->
            RootTunJson.Default.decodeFromString(
                ListSerializer(ProxyGroup.serializer()),
                service.queryAllProxyGroupsJson(excludeNotSelectable),
            )
        }
    }

    suspend fun queryProxyGroup(context: Context, name: String, sort: ProxySort): ProxyGroup {
        return remoteCall(context) { service ->
            val raw =
                service.queryProxyGroupJson(name, sort.name)
                    ?: error("proxy group not found: $name")
            RootTunJson.Default.decodeFromString(ProxyGroup.serializer(), raw)
        }
    }

    suspend fun queryConfiguration(context: Context): UiConfiguration {
        return remoteCall(context) { service ->
            RootTunJson.Default.decodeFromString(
                UiConfiguration.serializer(),
                service.queryConfigurationJson(),
            )
        }
    }

    suspend fun queryProviders(context: Context): List<Provider> {
        return remoteCall(context) { service ->
            RootTunJson.Default.decodeFromString(
                ListSerializer(Provider.serializer()),
                service.queryProvidersJson(),
            )
        }
    }

    suspend fun patchSelector(context: Context, group: String, name: String): Boolean {
        return remoteCall(context) { service -> service.patchSelector(group, name) }
    }

    suspend fun closeConnection(context: Context, id: String): Boolean {
        return remoteCall(context) { service -> service.closeConnection(id) }
    }

    suspend fun closeAllConnections(context: Context) {
        remoteCall(context) { service -> service.closeAllConnections() }
    }

    suspend fun healthCheck(context: Context, group: String) {
        val error = remoteCall(context) { service -> service.healthCheck(group) }
        if (!error.isNullOrBlank()) {
            error(error)
        }
    }

    suspend fun healthCheckProxy(context: Context, group: String, proxyName: String): String {
        return remoteCall(context) { service -> service.healthCheckProxy(group, proxyName) }
    }

    suspend fun updateProvider(context: Context, type: Provider.Type, name: String) {
        val error = remoteCall(context) { service -> service.updateProvider(type.name, name) }
        if (!error.isNullOrBlank()) {
            error(error)
        }
    }

    suspend fun queryRecentLogs(context: Context, sinceSeq: Long): RootTunLogChunk {
        return remoteCall(context) { service ->
            val raw = service.queryRecentLogsJson(sinceSeq)
            RootTunJson.Default.decodeFromString(RootTunLogChunk.serializer(), raw)
        }
    }

    private suspend fun disconnect() {
        mutex.withLock {
            val current = connection ?: return

            withContext(Dispatchers.Main) { runCatching { RootService.unbind(current) } }
            connection = null
            binder = null
        }
    }

    private suspend fun bind(context: Context): IRootTunService {
        cachedBinder(context)?.let {
            return it
        }

        return bindInternal(context)
    }

    private suspend fun bindInternal(context: Context): IRootTunService {
        return mutex.withLock {
            cachedBinder(context)?.let {
                return it
            }

            suspendCancellableCoroutine { continuation ->
                val appContext = context.appContextOrSelf
                val intent = createIntent(appContext)
                val mainHandler = Handler(Looper.getMainLooper())

                val newConnection =
                    object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            val remote = IRootTunService.Stub.asInterface(service)
                            if (remote == null) {
                                invalidateConnection(appContext, "root tun binder is null")
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        IllegalStateException("root tun binder is null")
                                    )
                                }
                                return
                            }

                            binder = remote
                            connection = this
                            if (continuation.isActive) {
                                continuation.resume(remote)
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            invalidateConnection(appContext, null)
                        }

                        override fun onNullBinding(name: ComponentName?) {
                            invalidateConnection(
                                appContext,
                                "root tun service returned null binding",
                            )
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IllegalStateException("root tun service returned null binding")
                                )
                            }
                        }

                        override fun onBindingDied(name: ComponentName?) {
                            invalidateConnection(appContext, "RootTun binding died")
                        }
                    }

                connection = newConnection
                continuation.invokeOnCancellation {
                    mainHandler.post { runCatching { RootService.unbind(newConnection) } }
                    if (connection === newConnection) {
                        connection = null
                    }
                    if (binder != null && connection == null) {
                        binder = null
                    }
                }

                mainHandler.post {
                    runCatching { RootService.bind(intent, newConnection) }
                        .onFailure { error ->
                            connection = null
                            binder = null
                            if (continuation.isActive) {
                                continuation.resumeWithException(error)
                            }
                        }
                }
            }
        }
    }

    private fun isRuntimeActive(context: Context): Boolean {
        cachedBinder(context)?.let {
            return true
        }
        val status = RootTunStateStore(context.appContextOrSelf).snapshot()
        return status.state.isActive || status.runtimeReady
    }

    private fun cachedBinder(context: Context): IRootTunService? {
        val current = binder ?: return null
        if (RootTunRuntimeRecovery.isBinderAlive(current)) {
            return current
        }
        invalidateConnection(context.appContextOrSelf, "RootTun binder cache is dead")
        return null
    }

    private fun invalidateConnection(context: Context, reason: String?) {
        binder = null
        connection = null
        RootTunRuntimeRecovery.handleBinderGone(context, reason)
    }

    private fun createIntent(context: Context): Intent {
        return Intent(context, RootTunRootService::class.java)
    }

    private suspend fun rollbackFailedStart(
        context: Context,
        stateStore: RootTunStateStore,
        error: String,
    ) {
        stateStore.markIdle(error)
        runCatching { RootTunService.stop(context) }
        runCatching { RootService.stop(createIntent(context)) }
        disconnect()
    }
}
