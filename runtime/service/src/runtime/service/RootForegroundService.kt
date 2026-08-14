/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.runtime.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.api.initializeServiceGlobal
import com.github.yumeyucca.yumebox.runtime.service.notification.ServiceNotificationManager
import com.github.yumeyucca.yumebox.runtime.service.util.cancelAndJoinBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground notification host for the detached root Tun daemon. The core itself remains a
 * root-owned process so it can survive an app process restart; this service only owns Android's
 * user-visible lifecycle and traffic notification.
 */
class RootForegroundService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val notificationManager by lazy {
        ServiceNotificationManager(this, ServiceNotificationManager.rootConfig)
    }
    private var notificationJob: Job? = null
    override fun onCreate() {
        super.onCreate()
        initializeServiceGlobal(appContextOrSelf)
        notificationManager.createChannel()
        startForeground(
            ServiceNotificationManager.rootConfig.notificationId,
            notificationManager.createInitialNotification(),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (notificationJob?.isActive != true) {
            notificationJob = notificationManager.startTrafficUpdate(this)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        notificationJob?.cancel()
        notificationJob = null
        notificationManager.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
        cancelAndJoinBlocking()
        cancel()
    }

    companion object {
        fun start(context: Context) {
            val appContext = context.appContextOrSelf
            appContext.startForegroundService(Intent(appContext, RootForegroundService::class.java))
        }

        fun stop(context: Context) {
            val appContext = context.appContextOrSelf
            appContext.stopService(Intent(appContext, RootForegroundService::class.java))
        }
    }
}
