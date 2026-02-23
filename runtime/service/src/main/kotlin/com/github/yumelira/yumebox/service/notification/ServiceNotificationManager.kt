package com.github.yumelira.yumebox.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.common.util.formatSpeed
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.common.constants.Components
import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.github.yumelira.yumebox.service.runtime.records.PendingDao
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.tencent.mmkv.MMKV
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServiceNotificationManager(
    private val service: Service,
    private val config: Config,
) {
    data class Config(
        val notificationId: Int,
        val channelId: String,
        val channelName: String,
    )

    private val serviceStore by lazy { ServiceStore() }
    private val settingsStore by lazy { MMKV.mmkvWithID("settings", MMKV.MULTI_PROCESS_MODE) }
    private val notificationManager by lazy { NotificationManagerCompat.from(service) }

    fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                config.channelId,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).setName(config.channelName).build()
        )
    }

    fun createInitialNotification(): Notification {
        return buildRunningNotification()
    }

    fun startTrafficUpdate(scope: CoroutineScope): Job {
        return scope.launch(Dispatchers.Default) {
            while (isActive) {
                notificationManager.notify(config.notificationId, buildRunningNotification())
                delay(1000L)
            }
        }
    }

    private fun buildRunningNotification(): Notification {
        val profileName = resolveProfileName()
        if (!shouldShowTrafficNotification()) {
            return buildNotification(profileName, MLang.Service.Notification.Running)
        }

        val now = runCatching { Clash.queryTrafficNow() }.getOrDefault(0L)
        val total = runCatching { Clash.queryTrafficTotal() }.getOrDefault(0L)

        val upNow = decodeTrafficHalf(now ushr 32)
        val downNow = decodeTrafficHalf(now and 0xFFFFFFFFL)
        val upTotal = decodeTrafficHalf(total ushr 32)
        val downTotal = decodeTrafficHalf(total and 0xFFFFFFFFL)

        val speedStr = "↓ ${formatSpeed(downNow)} ↑ ${formatSpeed(upNow)}"
        val totalStr = MLang.Service.Notification.TrafficFormat.format(formatBytes(upTotal + downTotal))
        return buildNotification(profileName, "$speedStr | $totalStr")
    }

    private fun buildNotification(title: CharSequence, content: CharSequence): Notification {
        val contentIntent = PendingIntent.getActivity(
            service,
            0,
            Intent().apply {
                component = Components.PROXY_SHEET_ACTIVITY
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_logo_service)
            .setColor(service.getColor(R.color.color_clash))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun resolveProfileName(): String {
        val active = serviceStore.activeProfile ?: return MLang.Service.Notification.UnknownProfile
        return (PendingDao.queryByUUID(active)?.name ?: ImportedDao.queryByUUID(active)?.name)
            ?.takeIf { it.isNotBlank() }
            ?: MLang.Service.Notification.UnknownProfile
    }

    private fun shouldShowTrafficNotification(): Boolean {
        val settings = settingsStore
        if (settings.containsKey("showTrafficNotification")) {
            return settings.decodeBool("showTrafficNotification", true)
        }
        return serviceStore.showTrafficNotification
    }

    private fun decodeTrafficHalf(encoded: Long): Long {
        val type = (encoded ushr 30) and 0x3L
        val data = encoded and 0x3FFFFFFFL
        return when (type.toInt()) {
            0 -> data
            1 -> (data * 1024L) / 100L
            2 -> (data * 1024L * 1024L) / 100L
            3 -> (data * 1024L * 1024L * 1024L) / 100L
            else -> 0L
        }
    }

    companion object {
        val VPN_CONFIG = Config(
            notificationId = 1001,
            channelId = "clash_vpn_service",
            channelName = "Clash VPN Service",
        )

        val HTTP_CONFIG = Config(
            notificationId = 1002,
            channelId = "clash_http_service",
            channelName = "Clash HTTP Service",
        )
    }
}
