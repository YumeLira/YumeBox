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

package com.github.yumeyucca.yumebox.runtime.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.runtime.api.Components
import com.github.yumeyucca.yumebox.runtime.service.R
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.profile.ImportedDao
import com.github.yumeyucca.yumebox.runtime.service.util.ServiceLogoIcons
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tf.gal.yumebox.locale.YumeTxt

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
    // Once released, the traffic updater must never notify() again — otherwise a tick that was mid
    // queryTrafficNow() (IPC to the core) when the service stopped can re-post the ongoing
    // notification AFTER stopForeground(REMOVE), leaving it stuck on screen.
    @Volatile private var released = false

    fun createChannel() {
        legacyChannelIds.forEach(notificationManager::deleteNotificationChannel)
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                    config.channelId,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                )
                .setName(config.channelName)
                .build()
        )
    }

    // The initial notification backs startForeground() inside onCreate: any throw before that
    // call crashes the app with a foreground-service contract violation, so this path must
    // stay free of MMKV/DAO reads. The enriched content follows via the traffic updater.
    fun createInitialNotification(): Notification =
        buildNotification(
            NotificationPresentationFactory.createStatus(
                profileName = service.applicationInfo.loadLabel(service.packageManager).toString(),
                status = YumeTxt.Service.Notification.Running,
            )
        )

    fun startTrafficUpdate(scope: CoroutineScope): Job =
        scope.launch(Dispatchers.Default) {
            PollingTimers.ticks(PollingTimerSpecs.ServiceTrafficNotification).collect {
                refreshRunningNotification()
            }
        }

    /**
     * Stop updating and clear the notification. After this the traffic updater never notifies
     * again.
     */
    fun release() {
        released = true
        runCatching { notificationManager.cancel(config.notificationId) }
    }

    private fun buildRunningNotification(): Notification {
        val profileName = resolveProfileName()
        if (!shouldShowTrafficNotification()) {
            return buildNotification(
                NotificationPresentationFactory.createStatus(
                    profileName = profileName,
                    status = YumeTxt.Service.Notification.Running,
                )
            )
        }

        val core = com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess.controller(service)
        val now = runCatching { core.queryTrafficNow() }.getOrDefault(0L)
        val total = runCatching { core.queryTrafficTotal() }.getOrDefault(0L)
        return buildNotification(
            NotificationPresentationFactory.createRunning(
                profileName = profileName,
                trafficNow = now,
                trafficTotal = total,
            )
        )
    }

    private fun refreshRunningNotification() {
        // This notification belongs to a running foreground service. Re-post it through
        // startForeground() instead of NotificationManager.notify(): Android may defer ordinary
        // notify() updates after the app leaves the foreground, while startForeground() remains
        // the service-owned update path even when POST_NOTIFICATIONS is denied.
        if (released) {
            return
        }
        val notification = buildRunningNotification()
        // Re-check after the (possibly slow) core query: the service may have stopped while we
        // were building the notification, and a notify() now would resurrect it.
        if (!released) {
            service.startForeground(config.notificationId, notification)
        }
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

        // Keep the small icon free of hard failures so startForeground() never trips before the
        // first frame; preference reads fall back to the default logo inside ServiceLogoIcons.
        val smallIcon = runCatching { ServiceLogoIcons.resId() }.getOrDefault(R.drawable.ic_logo_service)

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(presentation.title)
            .setContentText(presentation.content)
            .setSubText(presentation.subText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(presentation.expandedText)
                    .setSummaryText(presentation.subText)
            )
            .setSmallIcon(smallIcon)
            .setColor(service.getColor(R.color.color_yumebox))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun resolveProfileName(): String {
        val active =
            serviceStore.activeProfile ?: return YumeTxt.Service.Notification.UnknownProfile
        return ImportedDao.queryByUUID(active)?.name?.takeIf { it.isNotBlank() }
            ?: YumeTxt.Service.Notification.UnknownProfile
    }

    private fun shouldShowTrafficNotification(): Boolean {
        val settings = settingsStore
        if (settings.containsKey("showTrafficNotification")) {
            return settings.decodeBool("showTrafficNotification", true)
        }
        return serviceStore.showTrafficNotification
    }

    companion object {
        // Channel ids shipped before the YumeBox rebrand; deleted on channel creation so
        // upgraded installs don't keep orphaned "Clash ..." entries in notification settings.
        private val legacyChannelIds = listOf("clash_vpn_service", "clash_http_service")

        val vpnConfig =
            Config(
                notificationId = 1001,
                channelId = "yumebox_vpn_service",
                channelName = "YumeBox VPN Service",
            )

        val rootConfig =
            Config(
                notificationId = 1002,
                channelId = "yumebox_root_service",
                channelName = "YumeBox Root Service",
            )
    }
}
