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



package com.github.yumelira.yumebox.service.runtime.session

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.service.ServiceNetworkObserver
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.records.SelectionRestoreExecutor
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.serializer
import timber.log.Timber
import java.io.File
import java.util.TimeZone
import java.util.UUID
import kotlin.math.min
import java.security.MessageDigest

class SessionRuntime(
    private val host: RuntimeHost,
    private val transport: RuntimeTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val compiledConfigPipeline = CompiledConfigPipeline(host.context.appContextOrSelf)
    private val lock = Any()
    @Volatile
    private var interruptReason: String? = null
    private var currentSpec: RuntimeSpec? = null
    private var currentSnapshot: RuntimeSnapshot = RuntimeSnapshot(targetMode = host.mode)
    private var networkObserver: ServiceNetworkObserver? = null
    private val queryCache = SessionRuntimeQueryCache()
    private val telemetry = SessionRuntimeTelemetry(
        host = host,
        scope = scope,
    ) { ready ->
        publishSnapshot(currentSnapshot.copy(logReady = ready))
    }

    fun start(spec: RuntimeSpec): RuntimeOperationResult {
        return synchronized(lock) {
            clearInterruptRequest()
            runCatching {
                stopInternal(reason = null, notifyHost = false)
                startupLog(spec, "session: start begin")
                startInternal(spec)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                if (error is RuntimeInterruptedException) {
                    startupLog(spec, "session: start interrupted reason=${error.message}")
                    RuntimeOperationResult(success = true)
                } else {
                    rollback(spec, error.message ?: "start runtime failed")
                    RuntimeOperationResult(success = false, error = error.message ?: "start runtime failed")
                }
            }
        }
    }

    fun reload(spec: RuntimeSpec): RuntimeOperationResult {
        return synchronized(lock) {
            clearInterruptRequest()
            runCatching {
                startupLog(spec, "session: reload begin")
                reloadInternal(spec)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                if (error is RuntimeInterruptedException) {
                    startupLog(spec, "session: reload interrupted reason=${error.message}")
                    RuntimeOperationResult(success = true)
                } else {
                    startupLog(spec, "failed=${error.message ?: "reload runtime failed"}")
                    RuntimeOperationResult(success = false, error = error.message ?: "reload runtime failed")
                }
            }
        }
    }

    fun restart(spec: RuntimeSpec): RuntimeOperationResult {
        return synchronized(lock) {
            clearInterruptRequest()
            runCatching {
                stopInternal(reason = null, notifyHost = false)
                startupLog(spec, "session: restart begin")
                startInternal(spec)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                if (error is RuntimeInterruptedException) {
                    startupLog(spec, "session: restart interrupted reason=${error.message}")
                    RuntimeOperationResult(success = true)
                } else {
                    rollback(spec, error.message ?: "restart runtime failed")
                    RuntimeOperationResult(success = false, error = error.message ?: "restart runtime failed")
                }
            }
        }
    }

    fun stop(reason: String? = null): RuntimeOperationResult {
        requestStop(reason)
        return synchronized(lock) {
            runCatching {
                stopInternal(reason = reason, notifyHost = true)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                RuntimeOperationResult(success = false, error = error.message ?: "stop runtime failed")
            }
        }
    }

    fun requestStop(reason: String? = null) {
        interruptReason = reason ?: "runtime stop requested"
    }

    fun destroy() {
        requestStop("runtime destroyed")
        synchronized(lock) {
            runCatching { stopInternal(reason = "runtime destroyed", notifyHost = false) }
        }
        scope.cancel()
    }

    fun snapshot(): RuntimeSnapshot = currentSnapshot

    fun queryTunnelState(): TunnelState {
        return if (currentSnapshot.phase == RuntimePhase.Running) Clash.queryTunnelState() else TunnelState(TunnelState.Mode.Rule)
    }

    fun queryTrafficNow(): Long {
        return if (currentSnapshot.phase == RuntimePhase.Running) {
            Clash.queryTrafficNow().also {
                queryCache.updateTrafficNow(it)
                publishSnapshot(currentSnapshot.copy(trafficReady = true))
            }
        } else {
            0L
        }
    }

    fun queryTrafficTotal(): Long {
        return if (currentSnapshot.phase == RuntimePhase.Running) {
            Clash.queryTrafficTotal().also {
                queryCache.updateTrafficTotal(it)
                publishSnapshot(currentSnapshot.copy(trafficReady = true))
            }
        } else {
            0L
        }
    }

    fun queryConnections(): ConnectionSnapshot {
        if (currentSnapshot.phase != RuntimePhase.Running) return ConnectionSnapshot()
        return Clash.queryConnections()
    }

    fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        if (currentSnapshot.phase != RuntimePhase.Running) return emptyList()
        val groups = runCatching {
            Clash.queryGroupNames(excludeNotSelectable).map { Clash.queryGroup(it, ProxySort.Default) }
        }.getOrElse {
            if (excludeNotSelectable) {
                val selectable = Clash.queryGroupNames(true).toSet()
                ensureRuntimeSnapshot().proxyGroups.filter { selectable.contains(it.name) }
            } else {
                ensureRuntimeSnapshot().proxyGroups
            }
        }
        queryCache.replaceProxyGroups(groups)
        publishSnapshot(currentSnapshot.copy(groupsReady = groups.isNotEmpty()))
        return groups
    }

    fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        if (currentSnapshot.phase != RuntimePhase.Running) return emptyList()
        return runCatching { Clash.queryGroupNames(excludeNotSelectable) }
            .getOrElse { queryAllProxyGroups(excludeNotSelectable).map { it.name } }
    }

    fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup {
        if (currentSnapshot.phase != RuntimePhase.Running) {
            error("runtime not running")
        }
        val group = Clash.queryGroup(name, proxySort)
        if (proxySort == ProxySort.Default && group.name.isNotBlank()) {
            queryCache.upsertProxyGroup(name, group)
        }
        return group
    }

    fun queryConfiguration(): UiConfiguration {
        if (currentSnapshot.phase != RuntimePhase.Running) return UiConfiguration()
        return ensureRuntimeSnapshot().configuration
    }

    fun queryProviders(): List<Provider> {
        if (currentSnapshot.phase != RuntimePhase.Running) return emptyList()
        return ensureRuntimeSnapshot().providers
    }

    fun patchSelector(group: String, name: String): Boolean {
        val profileUuid = currentSnapshot.profileUuid?.let(UUID::fromString)
        return Clash.patchSelector(group, name).also { patched ->
            if (!patched) {
                profileUuid?.let { SelectionDao.remove(it, group) }
                return@also
            }

            if (currentSnapshot.phase == RuntimePhase.Running || currentSnapshot.phase == RuntimePhase.Starting) {
                val refreshedGroup = refreshRuntimeProxyGroup(group)
                if (refreshedGroup?.type == Proxy.Type.Selector) {
                    profileUuid?.let { SelectionDao.upsertManualSelection(it, group, name) }
                } else {
                    profileUuid?.let { SelectionDao.remove(it, group) }
                }
            }
        }
    }

    fun closeConnection(id: String): Boolean {
        if (currentSnapshot.phase != RuntimePhase.Running) return false
        return Clash.closeConnection(id)
    }

    fun closeAllConnections() {
        if (currentSnapshot.phase != RuntimePhase.Running) return
        Clash.closeAllConnections()
    }

    suspend fun healthCheck(group: String): String? {
        Timber.d("SessionRuntime healthCheck: group=%s phase=%s owner=%s", group, currentSnapshot.phase, currentSnapshot.owner)
        return runCatching {
            Clash.healthCheck(group).await()
            refreshRuntimeProxyGroup(group)
            null
        }.getOrElse { it.message ?: "health check failed" }
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): String {
        Timber.d(
            "SessionRuntime healthCheckProxy: group=%s proxy=%s phase=%s owner=%s",
            group,
            proxyName,
            currentSnapshot.phase,
            currentSnapshot.owner,
        )
        return runCatching {
            Clash.healthCheckProxy(proxyName).await().also {
                refreshRuntimeProxyGroup(group)
            }
        }.getOrElse {
            """{"delay":-1,"error":${com.github.yumelira.yumebox.service.root.RootTunJson.Default.encodeToString(String.serializer(), it.message ?: "health check proxy failed")}}"""
        }
    }

    suspend fun updateProvider(type: String, name: String): String? {
        val providerType = runCatching { Provider.Type.valueOf(type) }.getOrElse {
            return "invalid provider type: $type"
        }
        return runCatching {
            Clash.updateProvider(providerType, name).await()
            refreshRuntimeSnapshot()
            null
        }.getOrElse { it.message ?: "update provider failed" }
    }

    fun setLogObserver(observer: ((LogMessage) -> Unit)?) {
        telemetry.setLogObserver(observer)
    }

    fun queryRecentLogsJson(sinceSeq: Long): RuntimeLogChunk {
        return telemetry.queryRecentLogsJson(sinceSeq)
    }

    private fun startInternal(spec: RuntimeSpec) {
        val startedAt = System.currentTimeMillis()
        currentSpec = spec
        publishSnapshot(
            RuntimeSnapshot(
                owner = spec.owner,
                phase = RuntimePhase.Starting,
                targetMode = host.mode,
                profileUuid = spec.profileUuid,
                profileName = spec.profileName,
                profileReady = true,
                startedAt = startedAt,
                effectiveFingerprint = spec.effectiveFingerprint,
            ),
        )
        host.onStarting(spec)
        ensureNotInterrupted(spec)

        teardownCore()
        compileAndLoad(spec)
        ensureNotInterrupted(spec)
        startObservers()
        notifyCurrentTimeZone()
        startConnectionTracking()
        ensureNotInterrupted(spec)

        transport.prepare(spec)
        transport.start(spec)
        awaitProxyGroupsReady(spec)
        ensureNotInterrupted(spec)
        restoreSelections(spec)
        startLogStream()
        startupLog(spec, "snapshot refresh: begin")
        refreshRuntimeSnapshot()
        startupLog(spec, "snapshot refresh: done")

        publishSnapshot(
            currentSnapshot.copy(
                phase = RuntimePhase.Running,
                profileReady = true,
                groupsReady = queryCache.snapshot().proxyGroups.isNotEmpty(),
                trafficReady = true,
                configReady = true,
                transportReady = true,
                logReady = telemetry.isLogStreaming(),
                startedAt = startedAt,
                effectiveFingerprint = spec.effectiveFingerprint,
            ),
        )
        host.onProfileLoaded(spec.profileUuid)
        host.onStarted(spec)
        startupLog(spec, "started")
    }

    private fun reloadInternal(spec: RuntimeSpec) {
        check(currentSpec != null) { "runtime not started" }
        publishSnapshot(
            currentSnapshot.copy(
                phase = RuntimePhase.Starting,
                profileUuid = spec.profileUuid,
                profileName = spec.profileName,
                effectiveFingerprint = spec.effectiveFingerprint,
                groupsReady = false,
                trafficReady = false,
            ),
        )

        compileAndLoad(spec)
        awaitProxyGroupsReady(spec)
        ensureNotInterrupted(spec)
        restoreSelections(spec)
        currentSpec = spec
        startupLog(spec, "snapshot refresh: begin")
        refreshRuntimeSnapshot()
        startupLog(spec, "snapshot refresh: done")
        publishSnapshot(
            currentSnapshot.copy(
                phase = RuntimePhase.Running,
                profileReady = true,
                groupsReady = queryCache.snapshot().proxyGroups.isNotEmpty(),
                trafficReady = true,
                configReady = true,
                transportReady = true,
                logReady = telemetry.isLogStreaming(),
                effectiveFingerprint = spec.effectiveFingerprint,
                lastError = null,
            ),
        )
        host.onProfileLoaded(spec.profileUuid)
        startupLog(spec, "reload done")
    }

    private fun stopInternal(reason: String?, notifyHost: Boolean) {
        if (currentSnapshot.phase == RuntimePhase.Idle && currentSpec == null) {
            clearInterruptRequest()
            return
        }

        publishSnapshot(
            currentSnapshot.copy(
                phase = if (currentSnapshot.phase == RuntimePhase.Idle) RuntimePhase.Idle else RuntimePhase.Stopping,
                transportReady = false,
                groupsReady = false,
                trafficReady = false,
                configReady = false,
                logReady = false,
                lastError = reason,
            ),
        )
        stopLogStream()
        stopConnectionTracking()
        stopObservers()
        runCatching { transport.stop() }
        teardownCore()
        currentSpec = null
        queryCache.clear()
        publishSnapshot(
            RuntimeSnapshot(
                owner = RuntimeOwner.None,
                phase = if (reason.isNullOrBlank()) RuntimePhase.Idle else RuntimePhase.Failed,
                targetMode = host.mode,
                lastError = reason,
            ),
        )
        if (notifyHost) {
            host.onStopped(reason)
        }
        clearInterruptRequest()
    }

    private fun rollback(spec: RuntimeSpec, reason: String) {
        stopLogStream()
        stopObservers()
        runCatching { transport.stop() }
        teardownCore()
        currentSpec = null
        queryCache.clear()
        publishSnapshot(
            RuntimeSnapshot(
                owner = spec.owner,
                phase = RuntimePhase.Failed,
                targetMode = host.mode,
                profileUuid = spec.profileUuid,
                profileName = spec.profileName,
                profileReady = false,
                lastError = reason,
                effectiveFingerprint = spec.effectiveFingerprint,
            ),
        )
        startupLog(spec, "failed=$reason")
        host.reportFailure(reason)
    }

    private fun compileAndLoad(spec: RuntimeSpec) {
        ensureNotInterrupted(spec)
        startupLog(spec, "runtime prepare: begin path=${spec.runtimeConfigPath}")
        runBlocking {
            compiledConfigPipeline.applyOverrideToRuntimeFile(spec) { message ->
                startupLog(spec, message)
            }
        }
        startupLog(spec, "runtime override: done ${describeFile(File(spec.runtimeConfigPath))}")
        ensureNotInterrupted(spec)
        startupLog(spec, "runtime load: loadCompiledConfig(${spec.runtimeConfigPath}) begin")
        runBlocking { Clash.loadCompiledConfig(File(spec.runtimeConfigPath)).await() }
        startupLog(spec, "runtime load: loadCompiledConfig done")
        ensureNotInterrupted(spec)
    }

    private fun awaitProxyGroupsReady(spec: RuntimeSpec) {
        ensureNotInterrupted(spec)
        val expectedGroups = readExpectedGroupNames(spec)
        startupLog(
            spec,
            "runtime verify: expectedGroups=${expectedGroups.size}" +
                expectedGroups.takeIf { it.isNotEmpty() }
                    ?.let { " sample=${it.take(5)}" }
                    .orEmpty(),
        )
        if (expectedGroups.isEmpty()) {
            return
        }

        repeat(PROXY_GROUP_READY_RETRY_COUNT) { attempt ->
            ensureNotInterrupted(spec)
            val names = runCatching { Clash.queryGroupNames(false) }.getOrDefault(emptyList())
            if (names.isNotEmpty()) {
                startupLog(spec, "runtime verify: actualGroups=${names.size} sample=${names.take(5)}")
                return
            }
            if (attempt < PROXY_GROUP_READY_RETRY_COUNT - 1) {
                startupLog(spec, "runtime verify: actualGroups=0 retry=${attempt + 1}")
                runBlocking {
                    PollingTimers.awaitTick(
                        PollingTimerSpecs.dynamic(
                            name = "runtime_group_ready_retry",
                            intervalMillis = PROXY_GROUP_READY_RETRY_DELAY_MS,
                            initialDelayMillis = PROXY_GROUP_READY_RETRY_DELAY_MS,
                        ),
                    )
                }
            }
        }

        ensureNotInterrupted(spec)
        error(
            "runtime loaded but exposed 0 proxy groups; expected=${expectedGroups.size} " +
                "sample=${expectedGroups.take(min(5, expectedGroups.size))}",
        )
    }

    private fun readExpectedGroupNames(spec: RuntimeSpec): List<String> {
        val runtimeFile = File(spec.runtimeConfigPath)
        if (!runtimeFile.exists()) {
            startupLog(spec, "runtime verify: runtime.yaml missing path=${runtimeFile.absolutePath}")
            return emptyList()
        }
        val yamlText = runtimeFile.readText()
        if (yamlText.isBlank()) {
            startupLog(spec, "runtime verify: runtime.yaml empty")
            return emptyList()
        }
        return runCatching {
            Clash.inspectCompiledGroups(yamlText, File(spec.profileDir), excludeNotSelectable = false)
                .map { it.name }
                .filter { it.isNotBlank() }
        }.getOrElse { error ->
            startupLog(spec, "runtime verify: inspect failed=${error.message}")
            emptyList()
        }
    }

    private fun restoreSelections(spec: RuntimeSpec) {
        val profileUuid = UUID.fromString(spec.profileUuid)
        val restoreSelections = SelectionDao.queryRestorableSelections(profileUuid)
        if (restoreSelections.isEmpty()) {
            return
        }
        val runtimeGroups = runCatching {
            Clash.queryGroupNames(false).map { Clash.queryGroup(it, ProxySort.Default) }
        }.getOrDefault(emptyList())
        SelectionRestoreExecutor.restore(
            profileUuid = profileUuid,
            selections = restoreSelections,
            runtimeGroups = runtimeGroups,
            tag = spec.owner.name,
        )
    }

    private fun startObservers() {
        if (networkObserver == null) {
            networkObserver = ServiceNetworkObserver(host.context.appContextOrSelf) {
                transport.onNetworkChanged()
            }.also { it.start() }
        }
    }

    private fun stopObservers() {
        runCatching { networkObserver?.stop() }
        networkObserver = null
    }

    private fun notifyCurrentTimeZone() {
        val timeZone = TimeZone.getDefault()
        Clash.notifyTimeZoneChanged(timeZone.id, timeZone.rawOffset / 1000)
    }

    private fun teardownCore() {
        runCatching { Clash.stopRootTun() }
        runCatching { Clash.stopTun() }
        runCatching { Clash.stopHttp() }
        runCatching { Clash.reset() }
    }

    private fun refreshRuntimeSnapshot() {
        if (currentSnapshot.phase != RuntimePhase.Running && currentSnapshot.phase != RuntimePhase.Starting) {
            queryCache.clear()
            return
        }

        val configuration = runCatching { Clash.queryConfiguration() }.getOrDefault(UiConfiguration())
        val providers = runCatching { Clash.queryProviders() }.getOrDefault(emptyList())
        val proxyGroups = runCatching {
            Clash.queryGroupNames(false).map { Clash.queryGroup(it, ProxySort.Default) }
        }.getOrDefault(emptyList())
        val trafficNow = runCatching { Clash.queryTrafficNow() }.getOrDefault(0L)
        val trafficTotal = runCatching { Clash.queryTrafficTotal() }.getOrDefault(0L)
        queryCache.replace(
            configuration = configuration,
            providers = providers,
            proxyGroups = proxyGroups,
            trafficNow = trafficNow,
            trafficTotal = trafficTotal,
        )
    }

    private fun refreshRuntimeProxyGroup(name: String, proxySort: ProxySort = ProxySort.Default): ProxyGroup? {
        if (currentSnapshot.phase != RuntimePhase.Running && currentSnapshot.phase != RuntimePhase.Starting) {
            return null
        }

        val group = runCatching { Clash.queryGroup(name, proxySort) }.getOrNull() ?: return null
        queryCache.upsertProxyGroup(name, group)
        return group
    }

    private fun ensureRuntimeSnapshot(): SessionRuntimeQuerySnapshot {
        val snapshot = queryCache.snapshot()
        if (snapshot.proxyGroups.isNotEmpty()) {
            return snapshot
        }
        refreshRuntimeSnapshot()
        return queryCache.snapshot()
    }

    private fun startLogStream() {
        telemetry.startLogStream(Clash::subscribeLogcat)
    }

    private fun stopLogStream() {
        telemetry.stopLogStream()
    }

    private fun startConnectionTracking() {
        telemetry.startConnectionTracking()
    }

    private fun stopConnectionTracking() {
        telemetry.stopConnectionTracking()
    }

    private fun publishSnapshot(snapshot: RuntimeSnapshot) {
        currentSnapshot = snapshot.copy(running = snapshot.phase.running)
        host.onSnapshotChanged(currentSnapshot)
    }

    private fun startupLog(spec: RuntimeSpec, message: String) {
        val scope = when (spec.owner) {
            RuntimeOwner.LocalTun -> RuntimeStartupLogStore.Scope.LOCAL_TUN
            RuntimeOwner.LocalHttp -> RuntimeStartupLogStore.Scope.LOCAL_HTTP
            RuntimeOwner.RootTun -> RuntimeStartupLogStore.Scope.ROOT_TUN
            RuntimeOwner.None -> return
        }
        RuntimeStartupLogStore(host.context.appContextOrSelf, scope)
            .append("${scope.tag} session: $message")
    }

    private fun ensureNotInterrupted(spec: RuntimeSpec) {
        val reason = interruptReason ?: return
        startupLog(spec, "session: interrupted reason=$reason")
        throw RuntimeInterruptedException(reason)
    }

    private fun clearInterruptRequest() {
        interruptReason = null
    }

    private fun describeFile(file: File): String {
        if (!file.exists()) {
            return "path=${file.absolutePath} exists=false"
        }
        val content = file.readText()
        return buildString {
            append("path=")
            append(file.absolutePath)
            append(" exists=true size=")
            append(content.length)
            append(" sha=")
            append(content.sha256Short())
            content.lineSequence()
                .map(String::trim)
                .firstOrNull { it.isNotEmpty() }
                ?.let {
                    append(" firstLine=")
                    append(it.take(160))
                }
        }
    }

    private fun String.sha256Short(): String {
        if (isBlank()) return "empty"
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private const val PROXY_GROUP_READY_RETRY_COUNT = 10
        private const val PROXY_GROUP_READY_RETRY_DELAY_MS = 200L
    }

    private class RuntimeInterruptedException(message: String) : CancellationException(message)
}
