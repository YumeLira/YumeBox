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

package com.github.yumelira.yumebox.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.common.constants.Components
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.tencent.mmkv.MMKV
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class ServiceNotificationManager(private val service: Service, private val config: Config) {
    data class Config(val notificationId: Int, val channelId: String, val channelName: String)

    private val serviceStore by lazy { ServiceStore() }
    private val settingsStore by lazy { MMKV.mmkvWithID("settings", MMKV.MULTI_PROCESS_MODE) }
    private val notificationManager by lazy { NotificationManagerCompat.from(service) }
    private var lastNotificationFingerprint: String? = null

    fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                    config.channelId,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                )
                .setName(config.channelName)
                .build()
        )
    }

    fun createInitialNotification(): Notification {
        return buildRunningNotification()
    }

    fun startTrafficUpdate(scope: CoroutineScope): Job {
        return scope.launch(Dispatchers.Default) {
            PollingTimers.ticks(PollingTimerSpecs.ServiceTrafficNotification).collect {
                val notification = buildRunningNotification()
                val fingerprint =
                    "${notification.extras.getCharSequence(Notification.EXTRA_TITLE)}|" +
                        "${notification.extras.getCharSequence(Notification.EXTRA_TEXT)}"
                if (fingerprint != lastNotificationFingerprint) {
                    lastNotificationFingerprint = fingerprint
                    notificationManager.notify(config.notificationId, notification)
                }
            }
        }
    }

    private fun buildRunningNotification(): Notification {
        val profileName = resolveProfileName()
        if (!shouldShowTrafficNotification()) {
            return buildNotification(
                NotificationPresentationFactory.createStatus(
                    profileName = profileName,
                    status = MLang.Service.Notification.Running,
                )
            )
        }

        val now = runCatching { Clash.queryTrafficNow() }.getOrDefault(0L)
        val total = runCatching { Clash.queryTrafficTotal() }.getOrDefault(0L)
        return buildNotification(
            NotificationPresentationFactory.createRunning(
                profileName = profileName,
                trafficNow = now,
                trafficTotal = total,
            )
        )
    }

    private fun buildNotification(presentation: NotificationPresentation): Notification {
        val contentIntent =
            PendingIntent.getActivity(
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
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(presentation.title)
            .setContentText(presentation.content)
            .setSubText(presentation.subText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(presentation.expandedText)
                    .setSummaryText(presentation.subText)
            )
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
        return ImportedDao.queryByUUID(active)?.name?.takeIf { it.isNotBlank() }
            ?: MLang.Service.Notification.UnknownProfile
    }

    private fun shouldShowTrafficNotification(): Boolean {
        val settings = settingsStore
        if (settings.containsKey("showTrafficNotification")) {
            return settings.decodeBool("showTrafficNotification", true)
        }
        return serviceStore.showTrafficNotification
    }

    companion object {
        val VPN_CONFIG =
            Config(
                notificationId = 1001,
                channelId = "clash_vpn_service",
                channelName = "Clash VPN Service",
            )

        val HTTP_CONFIG =
            Config(
                notificationId = 1002,
                channelId = "clash_http_service",
                channelName = "Clash HTTP Service",
            )
    }
}
