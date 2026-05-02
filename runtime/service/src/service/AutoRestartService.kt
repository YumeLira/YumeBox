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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.github.yumelira.yumebox.core.util.AutoStartSessionGate
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.common.util.AutoStartExecutionGate
import com.github.yumelira.yumebox.service.common.util.AutoStartUpdatePolicy
import com.github.yumelira.yumebox.service.root.RootTunServiceBridge
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.runtime.session.RuntimeServiceLauncher
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class AutoRestartService : Service() {

    companion object {
        private const val TAG = "AutoRestartService"
        private const val NOTIFICATION_ID = 1101
        private const val CHANNEL_ID = "auto_restart_channel"
        const val EXTRA_REASON = "auto_restart_reason"
        const val REASON_BOOT_COMPLETED = "boot_completed"
        const val REASON_PACKAGE_REPLACED = "package_replaced"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mmkvProvider by lazy { MMKVProvider() }
    private val appSettingsStorage by lazy { AppSettingsStore(mmkvProvider.getMMKV("settings")) }
    private val featureStore by lazy { FeatureStore(mmkvProvider.getMMKV("substore")) }
    private val networkSettingsStorage by lazy { NetworkSettingsStore(mmkvProvider.getMMKV("network_settings")) }
    private val serviceCache by lazy { mmkvProvider.getMMKV("service_cache") }
    private val profileManager by lazy { ProfileManager(applicationContext) }
    private val foregroundStarted = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundStarted()
        AutoStartExecutionGate.markStarted(serviceCache)

        serviceScope.launch {
            val reason = intent?.getStringExtra(EXTRA_REASON).orEmpty().ifBlank { "unknown" }
            try {
                runCatching {
                    checkAndAutoStart(reason)
                }.onFailure { error ->
                    Timber.tag(TAG).e(error, "Auto start failed: ${error.message}")
                }
            } finally {
                AutoStartExecutionGate.clear(serviceCache)
                ServiceCompat.stopForeground(this@AutoRestartService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun ensureForegroundStarted() {
        if (!foregroundStarted.compareAndSet(false, true)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = createNotification()
            val foregroundFlags = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                else -> 0
            }
            startForeground(NOTIFICATION_ID, notification, foregroundFlags)
        }
    }

    private suspend fun checkAndAutoStart(reason: String) {
        if (!appSettingsStorage.automaticRestart.value) return
        if (AutoStartSessionGate.shouldSkipAutoStart()) {
            Timber.tag(TAG).i("Skip auto start: manual pause gate is active in current session")
            return
        }
        StartupTaskCoordinator.awaitRuntimeWarmup()
        val skipUpdateOnPostUpdateColdStart = featureStore.consumePostUpdateColdStartPending()

        val activeProfile = profileManager.queryActive()
        if (activeProfile == null) {
            Timber.tag(TAG).w("No active profile for auto start")
            return
        }

        tryUpdateActiveProfileOnStart(
            activeProfile = activeProfile,
            reason = reason,
            skipForPostUpdateColdStart = skipUpdateOnPostUpdateColdStart,
        )

        val startupSource = when (reason) {
            REASON_BOOT_COMPLETED -> RuntimeServiceLauncher.SOURCE_AUTO_RESTART_BOOT
            REASON_PACKAGE_REPLACED -> RuntimeServiceLauncher.SOURCE_AUTO_RESTART_REPLACED
            else -> RuntimeServiceLauncher.SOURCE_AUTO_RESTART
        }
        when (networkSettingsStorage.proxyMode.value) {
            ProxyMode.Tun -> {
                if (VpnService.prepare(this) != null) {
                    Timber.tag(TAG).i("Skip auto start: VPN permission is missing for Tun mode")
                    return
                }
                RuntimeServiceLauncher.start(this, ProxyMode.Tun, startupSource)
            }
            ProxyMode.RootTun -> {
                val result = RootTunServiceBridge.start(this)
                if (!result.success) {
                    error(result.error ?: "RootTun auto start failed")
                }
            }
            ProxyMode.Http -> {
                RuntimeServiceLauncher.start(this, ProxyMode.Http, startupSource)
            }
        }

        Timber.tag(TAG).i("Auto start triggered: reason=$reason profile=${activeProfile.name}, mode=${networkSettingsStorage.proxyMode.value}")
    }

    private suspend fun tryUpdateActiveProfileOnStart(
        activeProfile: Profile,
        reason: String,
        skipForPostUpdateColdStart: Boolean,
    ) {
        when (
            AutoStartUpdatePolicy.decide(
                autoUpdateEnabled = appSettingsStorage.autoUpdateCurrentProfileOnStart.value,
                activeProfile = activeProfile,
                skipForPostUpdateColdStart = skipForPostUpdateColdStart,
                startupReason = reason,
                coldStartReasons = setOf(REASON_BOOT_COMPLETED, REASON_PACKAGE_REPLACED),
            )
        ) {
            AutoStartUpdatePolicy.Decision.Proceed -> Unit
            AutoStartUpdatePolicy.Decision.AutoUpdateDisabled -> return
            AutoStartUpdatePolicy.Decision.SkipPostUpdateColdStart -> {
                Timber.tag(TAG).d("Skip auto update: post-update cold-start marker consumed")
                return
            }
            AutoStartUpdatePolicy.Decision.SkipColdStartReason -> {
                Timber.tag(TAG).d("Skip auto update on cold-start reason=$reason")
                return
            }
            AutoStartUpdatePolicy.Decision.UnsupportedProfileType -> {
                Timber.tag(TAG).d("Skip boot update: unsupported profile type=${activeProfile.type}")
                return
            }
            AutoStartUpdatePolicy.Decision.NoActiveProfile -> return
        }

        try {
            profileManager.update(activeProfile.uuid, null)
            Timber.tag(TAG).i("Boot update ok: ${activeProfile.uuid}")
        } catch (error: Exception) {
            Timber.tag(TAG).w(error, "Boot update failed")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Restart Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used to restart proxy service automatically"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YumeBox")
            .setContentText("Checking auto-start...")
            .setSmallIcon(R.drawable.ic_logo_service)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        AutoStartExecutionGate.clear(serviceCache)
        serviceScope.cancel()
        super.onDestroy()
    }
}
