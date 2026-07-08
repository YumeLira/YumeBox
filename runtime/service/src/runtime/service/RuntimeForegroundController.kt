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

package com.github.yumelira.yumebox.runtime.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.runtime.api.Intents
import com.github.yumelira.yumebox.runtime.api.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.service.notification.ServiceNotificationManager
import com.github.yumelira.yumebox.runtime.service.session.RuntimeHost
import com.github.yumelira.yumebox.runtime.service.session.RuntimeSpec
import com.github.yumelira.yumebox.runtime.service.session.RuntimeStartupLogStore
import com.github.yumelira.yumebox.runtime.service.session.RuntimeTransport
import com.github.yumelira.yumebox.runtime.service.session.SessionRuntime
import com.github.yumelira.yumebox.runtime.service.util.CoreRuntimeConfig
import com.github.yumelira.yumebox.runtime.service.util.sendClashStarted
import com.github.yumelira.yumebox.runtime.service.util.sendClashStopped
import com.github.yumelira.yumebox.runtime.service.util.sendProfileLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Shared lifecycle logic for the foreground runtime services ([ClashService] and [TunService]).
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
    private val mode: ProxyMode,
    private val label: String,
    private val notificationConfig: ServiceNotificationManager.Config,
    private val logScope: RuntimeStartupLogStore.Scope,
    private val createTransport: () -> RuntimeTransport,
    private val createSpec: () -> RuntimeSpec,
) {
    private val tag = logScope.tag

    private var reason: String? = null
    private val notificationManager by lazy {
        ServiceNotificationManager(service, notificationConfig)
    }
    private val startupLogStore by lazy {
        RuntimeStartupLogStore(service, logScope)
    }
    private var notificationJob: Job? = null
    private var runtime: SessionRuntime? = null
    private var reloadJob: Job? = null
    private var stopRequested = false

    private val runtimeEventsReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action ?: return) {
                    Intents.ACTION_PROFILE_CHANGED,
                    Intents.ACTION_OVERRIDE_CHANGED -> scheduleReload()

                    Intents.ACTION_CLASH_REQUEST_STOP -> {
                        if (stopRequested) return
                        reason = intent.getStringExtra(Intents.EXTRA_STOP_REASON)
                        stopRequested = true
                        reloadJob?.cancel()
                        reloadJob = null
                        StatusProvider.markRuntimeStopping(mode)
                        notificationJob?.cancel()
                        notificationJob = null
                        thread(name = "session-runtime-stop") {
                            val stopResult = runtime?.stop(reason)
                            if (stopResult?.success == false) {
                                val error = stopResult.error ?: "${label.lowercase()} runtime stop failed"
                                this@RuntimeForegroundController.reason = error
                                StatusProvider.markRuntimeFailed(mode)
                                service.sendClashStopped(error)
                                Timber.e("$label runtime stop failed: $error")
                            }
                            stopForegroundService()
                            service.stopSelf()
                        }
                    }
                }
            }
        }

    fun onCreate() {
        runCatching {
                startupLogStore.append("$tag service: onCreate begin")

                notificationManager.createChannel()
                service.startForeground(
                    notificationConfig.notificationId,
                    notificationManager.createInitialNotification(),
                )
                startupLogStore.append("$tag service: startForeground done")

                StatusProvider.clearLegacyStateFiles()
                StatusProvider.markRuntimeStarting(mode)
                CoreRuntimeConfig.applyCustomUserAgentIfPresent(service)

                runtime =
                    SessionRuntime(
                        host =
                            object : RuntimeHost {
                                override val context = service
                                override val mode: ProxyMode = this@RuntimeForegroundController.mode

                                override fun onStarting(spec: RuntimeSpec) = Unit

                                override fun onStarted(spec: RuntimeSpec) {
                                    StatusProvider.markRuntimeRunning(this@RuntimeForegroundController.mode)
                                    service.sendClashStarted()
                                }

                                override fun onStopped(reason: String?) {
                                    this@RuntimeForegroundController.reason = reason
                                    StatusProvider.markRuntimeIdle(this@RuntimeForegroundController.mode)
                                    service.sendClashStopped(reason)
                                }

                                override fun onProfileLoaded(profileUuid: String) {
                                    service.sendProfileLoaded(UUID.fromString(profileUuid))
                                }

                                override fun onSnapshotChanged(snapshot: RuntimeSnapshot) = Unit

                                override fun onLogReady(ready: Boolean) = Unit

                                override fun onLogItem(log: LogMessage) = Unit

                                override fun reportFailure(error: String) {
                                    reason = error
                                    startupLogStore.append("$tag failed=$error")
                                    StatusProvider.markRuntimeFailed(this@RuntimeForegroundController.mode)
                                    service.sendClashStopped(error)
                                    Timber.e("$label runtime failed: $error")
                                    service.stopSelf()
                                }
                            },
                        transport = createTransport(),
                        scope = scope,
                    )

                registerRuntimeReceiver()
                startupLogStore.append("$tag service: receiver registered")
                scope.launch {
                    runCatching {
                            startupLogStore.append("$tag spec: create begin")
                            val spec = createSpec()
                            startupLogStore.append(
                                "$tag spec: create done profile=${spec.profileUuid} overrides=${spec.overrideSpecs.size}"
                            )
                            val result = runtime!!.start(spec)
                            check(result.success) { result.error ?: "${label.lowercase()} runtime start failed" }
                        }
                        .onFailure { error ->
                            reason = error.message ?: "${label.lowercase()} runtime start failed"
                            startupLogStore.append("$tag failed=$reason")
                            StatusProvider.markRuntimeFailed(mode)
                            service.sendClashStopped(reason)
                            service.stopSelf()
                        }
                }
            }
            .onFailure { error ->
                reason = error.message ?: "${label.lowercase()} runtime start failed"
                startupLogStore.append("$tag failed=$reason")
                StatusProvider.markRuntimeFailed(mode)
                service.sendClashStopped(reason)
                service.stopSelf()
            }
    }

    fun onStartCommand(): Int {
        if (notificationJob?.isActive != true) {
            notificationJob = notificationManager.startTrafficUpdate(scope)
        }
        // The launcher marks Starting unconditionally before every start request; on a
        // re-entrant command against an already-running session this is the only place
        // that can flip the persisted phase back, otherwise it is stuck at Starting.
        if (runtime?.snapshot()?.running == true) {
            StatusProvider.markRuntimeRunning(mode)
        }
        return Service.START_STICKY
    }

    fun onDestroy() {
        runCatching { service.unregisterReceiver(runtimeEventsReceiver) }
        reloadJob?.cancel()
        reloadJob = null
        notificationJob?.cancel()
        notificationJob = null
        stopForegroundService()

        runtime?.let { rt ->
            if (stopRequested) {
                rt.requestStop(reason)
            } else {
                rt.requestStop(reason)
                // Teardown contends with an in-flight native compile on the session lock;
                // waiting for it here would block the main thread past the ANR window.
                thread(name = "session-runtime-destroy") { rt.destroy() }
            }
        }

        // Guarded: the launcher may already have marked the phase Starting for the
        // replacement runtime; the phase store holds a single mode slot.
        if (StatusProvider.queryRuntimePhase(mode).isNotIdle) {
            StatusProvider.markRuntimeIdle(mode)
        }
        service.sendClashStopped(reason)
        startupLogStore.append("$tag destroy")
        Timber.i("${service.javaClass.simpleName} destroyed: ${reason ?: "successfully"}")
    }

    private fun stopForegroundService() {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    fun onTrimMemory() {
        com.github.yumelira.yumebox.core.Clash.forceGc()
    }

    private fun registerRuntimeReceiver() {
        val filter =
            IntentFilter().apply {
                addAction(Intents.ACTION_PROFILE_CHANGED)
                addAction(Intents.ACTION_OVERRIDE_CHANGED)
                addAction(Intents.ACTION_CLASH_REQUEST_STOP)
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
            startupLogStore.append("$tag spec: reload create begin")
            val spec =
                runCatching { createSpec() }
                    .getOrElse { error ->
                        reason = error.message
                        startupLogStore.append(
                            "$tag failed=${error.message ?: "${label.lowercase()} runtime spec refresh failed"}"
                        )
                        Timber.w("$label runtime spec refresh failed: ${error.message}")
                        return@launch
                    }
            startupLogStore.append(
                "$tag spec: reload create done profile=${spec.profileUuid} overrides=${spec.overrideSpecs.size}"
            )

            val result = runtime!!.reload(spec)
            if (!result.success) {
                reason = result.error
                startupLogStore.append(
                    "$tag failed=${result.error ?: "${label.lowercase()} runtime reload failed"}"
                )
                Timber.w("$label runtime reload failed: ${result.error}")
            }
        }
    }
}
