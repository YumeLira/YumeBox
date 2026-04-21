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



package com.github.yumelira.yumebox.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.common.util.CoreRuntimeConfig
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.common.util.initializeServiceGlobal
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import com.github.yumelira.yumebox.service.runtime.session.*
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import com.github.yumelira.yumebox.service.runtime.util.cancelAndJoinBlocking
import com.github.yumelira.yumebox.service.runtime.util.sendClashStarted
import com.github.yumelira.yumebox.service.runtime.util.sendClashStopped
import com.github.yumelira.yumebox.service.runtime.util.sendProfileLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class TunService : VpnService(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private var reason: String? = null
    private val notificationManager by lazy {
        ServiceNotificationManager(this, ServiceNotificationManager.VPN_CONFIG)
    }
    private val startupLogStore by lazy {
        RuntimeStartupLogStore(this, RuntimeStartupLogStore.Scope.LOCAL_TUN)
    }
    private var notificationJob: Job? = null
    private lateinit var runtime: SessionRuntime
    private var reloadJob: Job? = null

    private val runtimeEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action ?: return) {
                Intents.ACTION_PROFILE_CHANGED,
                Intents.ACTION_OVERRIDE_CHANGED -> scheduleReload()
                Intents.ACTION_CLASH_REQUEST_STOP -> {
                    reason = intent.getStringExtra(Intents.EXTRA_STOP_REASON)
                    reloadJob?.cancel()
                    reloadJob = null
                    StatusProvider.markRuntimeStopping(ProxyMode.Tun)
                    if (this@TunService::runtime.isInitialized) {
                        runtime.requestStop(reason)
                    }
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            initializeServiceGlobal(appContextOrSelf)
            startupLogStore.append("LOCAL_TUN service: onCreate begin")

            notificationManager.createChannel()
            startForeground(
                ServiceNotificationManager.VPN_CONFIG.notificationId,
                notificationManager.createInitialNotification(),
            )
            startupLogStore.append("LOCAL_TUN service: startForeground done")

            StatusProvider.clearLegacyStateFiles()
            StatusProvider.markRuntimeStarting(ProxyMode.Tun)
            CoreRuntimeConfig.applyCustomUserAgentIfPresent(this)

            runtime = SessionRuntime(
                host = object : RuntimeHost {
                    override val context = this@TunService
                    override val mode: ProxyMode = ProxyMode.Tun

                    override fun onStarting(spec: RuntimeSpec) = Unit

                    override fun onStarted(spec: RuntimeSpec) {
                        StatusProvider.markRuntimeRunning(ProxyMode.Tun)
                        sendClashStarted()
                    }

                    override fun onStopped(reason: String?) {
                        this@TunService.reason = reason
                        StatusProvider.markRuntimeIdle(ProxyMode.Tun)
                        sendClashStopped(reason)
                    }

                    override fun onProfileLoaded(profileUuid: String) {
                        sendProfileLoaded(UUID.fromString(profileUuid))
                    }

                    override fun onSnapshotChanged(snapshot: RuntimeSnapshot) = Unit

                    override fun onLogReady(ready: Boolean) = Unit

                    override fun onLogItem(log: LogMessage) = Unit

                    override fun reportFailure(error: String) {
                        reason = error
                        startupLogStore.append("LOCAL_TUN failed=$error")
                        StatusProvider.markRuntimeFailed(ProxyMode.Tun)
                        sendClashStopped(error)
                        Log.e("Tun runtime failed: $error")
                        stopSelf()
                    }
                },
                transport = VpnTunTransport(this),
                scope = this,
            )

            registerRuntimeReceiver()
            startupLogStore.append("LOCAL_TUN service: receiver registered")
            launch {
                runCatching {
                    startupLogStore.append("LOCAL_TUN spec: create begin")
                    val spec = SessionRuntimeSpecFactory(appContextOrSelf).createTunSpec()
                    startupLogStore.append("LOCAL_TUN spec: create done profile=${spec.profileUuid} overrides=${spec.overrideSpecs.size}")
                    val result = runtime.start(spec)
                    check(result.success) { result.error ?: "tun runtime start failed" }
                }.onFailure { error ->
                    reason = error.message ?: "tun runtime start failed"
                    startupLogStore.append("LOCAL_TUN failed=$reason")
                    StatusProvider.markRuntimeFailed(ProxyMode.Tun)
                    sendClashStopped(reason)
                    stopSelf()
                }
            }
        }.onFailure { error ->
            reason = error.message ?: "tun runtime start failed"
            startupLogStore.append("LOCAL_TUN failed=$reason")
            StatusProvider.markRuntimeFailed(ProxyMode.Tun)
            sendClashStopped(reason)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (notificationJob?.isActive != true) {
            notificationJob = notificationManager.startTrafficUpdate(this)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(runtimeEventsReceiver) }
        reloadJob?.cancel()
        reloadJob = null
        notificationJob?.cancel()
        notificationJob = null

        if (this::runtime.isInitialized) {
            runtime.requestStop(reason)
            runtime.destroy()
        }

        StatusProvider.markRuntimeIdle(ProxyMode.Tun)
        sendClashStopped(reason)
        startupLogStore.append("LOCAL_TUN destroy")
        Log.i("TunService destroyed: ${reason ?: "successfully"}")

        super.onDestroy()
        cancelAndJoinBlocking()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.github.yumelira.yumebox.core.Clash.forceGc()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerRuntimeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intents.ACTION_PROFILE_CHANGED)
            addAction(Intents.ACTION_OVERRIDE_CHANGED)
            addAction(Intents.ACTION_CLASH_REQUEST_STOP)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(runtimeEventsReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(runtimeEventsReceiver, filter)
        }
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = launch {
            startupLogStore.append("LOCAL_TUN spec: reload create begin")
            val spec = runCatching {
                SessionRuntimeSpecFactory(appContextOrSelf).createTunSpec()
            }.getOrElse { error ->
                reason = error.message
                startupLogStore.append("LOCAL_TUN failed=${error.message ?: "tun runtime spec refresh failed"}")
                Log.w("Tun runtime spec refresh failed: ${error.message}")
                return@launch
            }
            startupLogStore.append("LOCAL_TUN spec: reload create done profile=${spec.profileUuid} overrides=${spec.overrideSpecs.size}")

            val result = runtime.reload(spec)
            if (!result.success) {
                reason = result.error
                startupLogStore.append("LOCAL_TUN failed=${result.error ?: "tun runtime reload failed"}")
                Log.w("Tun runtime reload failed: ${result.error}")
            }
        }
    }
}
