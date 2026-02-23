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
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import timber.log.Timber

class AutoRestartService : Service() {

    companion object {
        private const val TAG = "AutoRestartService"
        private const val NOTIFICATION_ID = 1101
    private const val CHANNEL_ID = "auto_restart_channel"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mmkvProvider by lazy { MMKVProvider() }
    private val appSettingsStorage by lazy { AppSettingsStorage(mmkvProvider.getMMKV("settings")) }
    private val networkSettingsStorage by lazy { NetworkSettingsStorage(mmkvProvider.getMMKV("network_settings")) }
    private val profileManager by lazy { ProfileManager(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        serviceScope.launch {
            runCatching {
                checkAndAutoStart(isBootCompleted = true)
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "自动启动失败: ${e.message}")
            }
            ServiceCompat.stopForeground(this@AutoRestartService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun checkAndAutoStart(isBootCompleted: Boolean) {
        if (!appSettingsStorage.automaticRestart.value) return
        if (!StatusProvider.shouldStartClashOnBoot) return

        val activeProfile = profileManager.queryActive()
        if (activeProfile == null) {
            Timber.tag(TAG).w("没有可用的配置文件，无法自动启动")
            return
        }

        if (isBootCompleted) delay(3000)

        val useTun = networkSettingsStorage.proxyMode.value == ProxyMode.Tun
        val serviceIntent = Intent(
            this,
            if (useTun) TunService::class.java else ClashService::class.java,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Timber.tag(TAG).i("自动启动代理已触发: profile=${activeProfile.name}, useTun=$useTun")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                MLang.Service.AutoRestart.ChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = MLang.Service.AutoRestart.ChannelDescription
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(MLang.Home.Title)
            .setContentText(MLang.Service.AutoRestart.Checking)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

