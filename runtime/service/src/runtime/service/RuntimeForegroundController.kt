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

package com.github.yumeyucca.yumebox.runtime.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.github.yumeyucca.yumebox.core.model.LogMessage
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.runtime.api.Intents
import com.github.yumeyucca.yumebox.runtime.api.RuntimeSnapshot
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import com.github.yumeyucca.yumebox.runtime.service.notification.ServiceNotificationManager
import com.github.yumeyucca.yumebox.runtime.service.session.RuntimeHost
import com.github.yumeyucca.yumebox.runtime.service.session.RuntimeSpec
import com.github.yumeyucca.yumebox.runtime.service.session.RuntimeTransport
import com.github.yumeyucca.yumebox.runtime.service.session.SessionRuntime
import com.github.yumeyucca.yumebox.runtime.service.util.sendProfileLoaded
import com.github.yumeyucca.yumebox.runtime.service.util.sendRuntimeStarted
import com.github.yumeyucca.yumebox.runtime.service.util.sendRuntimeStopped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.concurrent.thread

/**
 * Shared lifecycle logic for the foreground runtime services ([TunService] and root host).
 *
 * The two services cannot share a superclass — the TUN path is mandated by Android to extend
 * [android.net.VpnService] while the HTTP path extends a plain [Service] — so the identical
 * onCreate/onStartCommand/onDestroy/receiver/reload behavior is delegated to this controller
 * instead. The hosting service keeps only its required parent class, transport, notification
 * config, startup scope, and any service-specific global init/teardown.
 */
class RuntimeForegroundController(
    private val service: Service,
    private val scope: CoroutineScope,
    private val mode: RunMode,
    private val label: String,
    private val notificationConfig: ServiceNotificationManager.Config,
    private val logSource: RuntimeLog.Source,
    private val createTransport: () -> RuntimeTransport,
    private val createSpec: () -> RuntimeSpec,
) {
    private var reason: String? = null
    private val notificationManager by lazy {
        ServiceNotificationManager(service, notificationConfig)
    }
    private val runtimeLog by lazy { RuntimeLog.writer(service, logSource) }
    private var notificationJob: Job? = null
    private var runtime: SessionRuntime? = null
    private var reloadJob: Job? = null
    @Volatile private var stopRequested = false

    /** Write token for the persisted phase slot; stale writers are dropped by StatusProvider. */
    private var sessionToken: String = ""

    /** Most recent startId delivered to onStartCommand; pins stopSelf to what we've seen. */
    @Volatile private var lastStartId = -1

    /** startId observed when the stop was requested; a later command means a raced start. */
    @Volatile private var stopCommandStartId = -1

    /** A stale session asks this instance to recreate itself only after onDestroy releases it. */
    @Volatile private var restartAfterStop = false

    /** Once destroyed, async stop work must not touch the (possibly replacement) service. */
    @Volatile private var destroyed = false

    private val runtimeEventsReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action ?: return) {
                    Intents.ACTION_PROFILE_CHANGED -> {
                        if (intent.getBooleanExtra(Intents.EXTRA_AFFECTS_RUNTIME, true)) {
                            scheduleReload()
                        }
                    }

                    Intents.ACTION_OVERRIDE_CHANGED -> scheduleReload()

                    Intents.ACTION_RUNTIME_REQUEST_STOP -> {
                        val targetMode = intent.getStringExtra(Intents.EXTRA_RUNTIME_MODE)
                        if (targetMode == null || targetMode == mode.name) {
                            requestStop(
                                stopReason = intent.getStringExtra(Intents.EXTRA_STOP_REASON),
                                restart = intent.getBooleanExtra(Intents.EXTRA_RESTART, false),
                            )
                        }
                    }
                }
            }
        }

    fun onCreate() {
        // Mark this runtime service alive for StatusProvider's in-process liveness check (both
        // services share this process). Set first so it holds even if the start below fails.
        StatusProvider.setServiceAlive(mode, true)
        runCatching {
            runtimeLog.i(RuntimeLog.Type.Service, "onCreate begin mode=${mode.name}")

            notificationManager.createChannel()
            service.startForeground(
                notificationConfig.notificationId,
                notificationManager.createInitialNotification(),
            )
            runtimeLog.i(RuntimeLog.Type.Service, "startForeground done")

            StatusProvider.clearLegacyStateFiles()
            sessionToken = StatusProvider.adoptOrBeginRuntimeSession(mode)
            runtime =
                SessionRuntime(
                    host =
                        object : RuntimeHost {
                            override val context = service
                            override val mode: RunMode = this@RuntimeForegroundController.mode

                            override fun onStarting(spec: RuntimeSpec) = Unit

                            override fun onStarted(spec: RuntimeSpec) {
                                StatusProvider.markRuntimeRunning(
                                    this@RuntimeForegroundController.mode,
                                    sessionToken,
                                )
                                service.sendRuntimeStarted()
                            }

                            override fun onStopped(reason: String?) {
                                this@RuntimeForegroundController.reason = reason
                                StatusProvider.markRuntimeIdle(
                                    this@RuntimeForegroundController.mode,
                                    sessionToken,
                                )
                                service.sendRuntimeStopped(reason)
                            }

                            override fun onProfileLoaded(profileUuid: String) {
                                service.sendProfileLoaded(UUID.fromString(profileUuid))
                            }

                            override fun restoreActiveProfile(
                                profileUuid: String,
                                profileName: String,
                            ) {
                                ServiceStore().activeProfile = UUID.fromString(profileUuid)
                                StatusProvider.currentProfile = profileName
                            }

                            override fun onSnapshotChanged(snapshot: RuntimeSnapshot) = Unit

                            override fun onLogReady(ready: Boolean) = Unit

                            override fun onLogItem(log: LogMessage) = Unit

                            override fun reportFailure(error: String) {
                                reason = error
                                runtimeLog.e(
                                    RuntimeLog.Type.Session,
                                    "runtime reported failure: $error",
                                )
                                markFailed(error)
                                service.sendRuntimeStopped(error)
                                Timber.e("$label runtime failed: $error")
                                service.stopSelf()
                            }
                        },
                    transport = createTransport(),
                    scope = scope,
                )

            registerRuntimeReceiver()
            runtimeLog.i(RuntimeLog.Type.Service, "receiver registered")
            scope.launch {
                runCatching {
                    runtimeLog.i(RuntimeLog.Type.Spec, "create begin")
                    val spec = createSpec()
                    runtimeLog.i(
                        RuntimeLog.Type.Spec,
                        "create done profile=${spec.profileUuid} " +
                            "overrides=${spec.overrideSpecs.size}",
                    )
                    val result = runtime!!.start(spec)
                    check(result.success) {
                        result.error ?: "${label.lowercase()} runtime start failed"
                    }
                }
                    .onFailure { error -> failStartup(startFailureMessage(error), error) }
            }
        }
            .onFailure { error -> failStartup(startFailureMessage(error), error) }
    }

    private fun startFailureMessage(error: Throwable): String =
        error.message?.takeIf(String::isNotBlank) ?: "${label.lowercase()} runtime start failed"

    /** [error] carries the cause chain the persisted status message alone would lose. */
    private fun failStartup(message: String, error: Throwable? = null) {
        reason = message
        runtimeLog.e(RuntimeLog.Type.Service, "startup failed: $message", error)
        markFailed(message)
        service.sendRuntimeStopped(message)
        service.stopSelf()
    }

    private fun markFailed(message: String) {
        // The token is empty only when onCreate failed before the session was claimed; the
        // slot then still holds the launcher's Starting record, which the force write clears
        // (a lingering Starting would read as "killed while starting" and confuse recovery).
        if (sessionToken.isNotEmpty()) {
            StatusProvider.markRuntimeFailed(mode, sessionToken, message)
        } else {
            StatusProvider.markRuntimeFailed(mode, message)
        }
    }

    fun onStartCommand(startId: Int): Int {
        lastStartId = startId
        if (notificationJob?.isActive != true) {
            notificationJob = notificationManager.startTrafficUpdate(scope)
        }
        // The launcher marks Starting unconditionally before every start request; on a
        // re-entrant command against an already-running session this is the only place
        // that can flip the persisted phase back, otherwise it is stuck at Starting.
        if (runtime?.snapshot()?.running == true) {
            StatusProvider.markRuntimeRunning(mode, sessionToken)
        }
        // Let Android recreate a killed started service with its original command instead of
        // relying on an app-managed polling alarm.
        return Service.START_REDELIVER_INTENT
    }

    fun onVpnRevoked() {
        requestStop("VPN connection revoked by system")
    }

    fun onDestroy() {
        destroyed = true
        StatusProvider.setServiceAlive(mode, false)
        runCatching { service.unregisterReceiver(runtimeEventsReceiver) }
        reloadJob?.cancel()
        reloadJob = null
        notificationJob?.cancel()
        notificationJob = null
        stopForegroundService()

        runtime?.let { rt ->
            rt.requestStop(reason)
            if (!stopRequested) {
                // Teardown contends with an in-flight native compile on the session lock;
                // waiting for it here would block the main thread past the ANR window.
                thread(name = "session-runtime-destroy") { rt.destroy() }
            }
        }

        // Token-guarded: a no-op when the launcher already claimed the slot for a
        // replacement session, and when this session recorded a Failed phase (the failure
        // must stay readable after the service is gone).
        StatusProvider.markRuntimeIdle(mode, sessionToken)
        service.sendRuntimeStopped(reason)
        runtimeLog.i(RuntimeLog.Type.Service, "destroyed reason=${reason ?: "normal stop"}")
        Timber.i("${service.javaClass.simpleName} destroyed: ${reason ?: "successfully"}")

        // A start command can land on a dying instance, and a stale session may explicitly
        // request a handoff. Both cases must wait until this instance releases its token.
        if (restartAfterStop || (stopRequested && lastStartId != stopCommandStartId)) {
            runtimeLog.i(RuntimeLog.Type.Service, "relaunching after stop")
            runCatching { service.startForegroundService(Intent(service, service.javaClass)) }
                .onFailure { error ->
                    runtimeLog.e(RuntimeLog.Type.Service, "relaunch failed", error)
                }
        }
    }

    private fun stopForegroundService() {
        // Silence the traffic updater first so a late notify() can't re-post the ongoing
        // notification
        // after we remove it, then drop the foreground notification.
        notificationManager.release()
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    fun onTrimMemory() {
        // The core owns its heap in its own process now; nothing to GC in-process.
    }

    private fun requestStop(stopReason: String?, restart: Boolean = false) {
        if (stopRequested) return
        stopRequested = true
        restartAfterStop = restart
        stopCommandStartId = lastStartId
        reason = stopReason
        reloadJob?.cancel()
        reloadJob = null
        StatusProvider.markRuntimeStopping(mode, sessionToken)
        notificationJob?.cancel()
        notificationJob = null
        // The session thread below performs the data-plane stop. It sends the VPN child SIGTERM
        // first so mihomo can persist selector state; AndroidRuntimeLauncher retains a timeout
        // based SIGKILL fallback if the service becomes stuck.
        stopForegroundService()
        val initialStopStartId = lastStartId
        service.stopSelf(initialStopStartId)
        thread(name = "session-runtime-stop") {
            val stopResult = runtime?.stop(reason)
            if (stopResult?.success == false) {
                val error = stopResult.error ?: "${label.lowercase()} runtime stop failed"
                this@RuntimeForegroundController.reason = error
                markFailed(error)
                service.sendRuntimeStopped(error)
                Timber.e("$label runtime stop failed: $error")
            }
            // Once destroyed, stopSelf/stopForeground resolve through the service token and
            // can put down a relaunched replacement instance instead of this dead one.
            // stopSelf is pinned to a startId (never the unconditional overload) so AMS
            // refuses the stop when a start command raced past the pinned id; retrying with
            // the newer id still stops this instance, and onDestroy then relaunches a fresh
            // one for the raced command instead of silently swallowing it.
            if (!destroyed) {
                var pinnedStartId = initialStopStartId
                while (!destroyed && pinnedStartId != lastStartId) {
                    pinnedStartId = lastStartId
                    service.stopSelf(pinnedStartId)
                }
            }
        }
    }

    private fun registerRuntimeReceiver() {
        val filter =
            IntentFilter().apply {
                addAction(Intents.ACTION_PROFILE_CHANGED)
                addAction(Intents.ACTION_OVERRIDE_CHANGED)
                addAction(Intents.ACTION_RUNTIME_REQUEST_STOP)
            }
        ContextCompat.registerReceiver(
            service,
            runtimeEventsReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = scope.launch {
            runtimeLog.i(RuntimeLog.Type.Reload, "spec create begin")
            val spec = runCatching {
                createSpec()
            }
                .getOrElse { error ->
                    reason = error.message
                    runtimeLog.e(RuntimeLog.Type.Reload, "spec refresh failed", error)
                    Timber.w("$label runtime spec refresh failed: ${error.message}")
                    return@launch
                }
            runtimeLog.i(
                RuntimeLog.Type.Reload,
                "spec create done profile=${spec.profileUuid} " +
                    "overrides=${spec.overrideSpecs.size}",
            )

            val result = runtime!!.reload(spec)
            if (result.success) {
                runtimeLog.i(RuntimeLog.Type.Reload, "success profile=${spec.profileUuid}")
            } else {
                reason = result.error
                runtimeLog.e(
                    RuntimeLog.Type.Reload,
                    "failed: ${result.error ?: "${label.lowercase()} runtime reload failed"}",
                )
                Timber.w("$label runtime reload failed: ${result.error}")
            }
        }
    }
}
