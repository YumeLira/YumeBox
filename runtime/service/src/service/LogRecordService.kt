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

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.common.constants.Components
import com.github.yumelira.yumebox.service.remote.ILogObserver
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class LogRecordService : Service() {

    companion object {
        private const val TAG = "LogRecordService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "log_record_channel"
        private const val CHANNEL_NAME = "日志记录"

        private const val ACTION_START = "com.github.yumelira.yumebox.LOG_START"
        private const val ACTION_STOP = "com.github.yumelira.yumebox.LOG_STOP"

        const val LOG_DIR = "logs"
        const val LOG_PREFIX = ""
        const val LOG_SUFFIX = ".log"

        @Volatile
        var isRecording: Boolean = false
            private set

        @Volatile
        var currentLogFileName: String? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, LogRecordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LogRecordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun getLogDir(context: Context): File {
            return File(context.filesDir, LOG_DIR).apply { mkdirs() }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var logWriter: BufferedWriter? = null
    private var logCollectJob: Job? = null
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        closeLogWriter()
        serviceScope.cancel()
        isRecording = false
        currentLogFileName = null
        super.onDestroy()
    }

    private fun startRecording() {
        if (isRecording) return

        runCatching {
            val logDir = getLogDir(applicationContext)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "$LOG_PREFIX$timestamp$LOG_SUFFIX"
            logFile = File(logDir, fileName)
            logWriter = BufferedWriter(FileWriter(logFile, true))

            currentLogFileName = fileName
            isRecording = true

            startForeground(NOTIFICATION_ID, createNotification())

            logCollectJob = serviceScope.launch {
                try {
                    val observer = object : ILogObserver {
                        override fun newItem(log: LogMessage) {
                            if (isRecording) {
                                runCatching {
                                    val line = "[${dateFormat.format(log.time)}] [${log.level.name}] ${log.message}\n"
                                    logWriter?.write(line)
                                    logWriter?.flush()
                                }.onFailure { error ->
                                    Timber.tag(TAG).e(error, "Log write failed")
                                }
                            }
                        }
                    }
                    val clash = ClashManager(applicationContext)
                    clash.setLogObserver(observer)

                    try {
                        awaitCancellation()
                    } finally {
                        runCatching { clash.setLogObserver(null) }
                    }
                } catch (error: Exception) {
                    Timber.e(error, "Log observer setup failed")
                }
            }
        }.onFailure { error ->
            Timber.tag(TAG).e(error, "Log recording start failed")
            isRecording = false
            currentLogFileName = null
            stopSelf()
        }
    }

    private fun stopRecording() {
        logCollectJob?.cancel()
        logCollectJob = null
        closeLogWriter()

        isRecording = false
        currentLogFileName = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeLogWriter() {
        runCatching {
            logWriter?.flush()
            logWriter?.close()
            logWriter = null
            logFile = null
        }.onFailure { error ->
            Timber.tag(TAG).e(error, "Log writer close failed")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "日志记录服务通知"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent().setComponent(Components.MAIN_ACTIVITY)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LogRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在记录日志")
            .setContentText(currentLogFileName ?: "日志记录中...")
            .setSmallIcon(R.drawable.ic_logo_service)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "停止",
                stopPendingIntent
            )
            .build()
    }
}
