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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class AutoRestartService : Service() {

    companion object {
        private const val TAG = "AutoRestartService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_restart_channel"
    }

    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettingsStorage: NetworkSettingsStorage by inject()
    private val profilesStore: ProfilesStore by inject()
    private val clashManager: ClashManager by inject()
    private val proxyConnectionService: ProxyConnectionService by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 对于 Android 8.0+，必须在5秒内调用 startForeground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        serviceScope.launch {
            runCatching {
                ProxyAutoStartHelper.checkAndAutoStart(
                    proxyConnectionService = proxyConnectionService,
                    appSettingsStorage = appSettingsStorage,
                    networkSettingsStorage = networkSettingsStorage,
                    profilesStore = profilesStore,
                    clashManager = clashManager,
                    isBootCompleted = true
                )
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "自动启动失败: ${e.message}")
            }
            // 完成后立即停止前台服务
            ServiceCompat.stopForeground(this@AutoRestartService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "自动重启服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于自动重启代理服务"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YumeBox")
            .setContentText("正在检查自动启动...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
