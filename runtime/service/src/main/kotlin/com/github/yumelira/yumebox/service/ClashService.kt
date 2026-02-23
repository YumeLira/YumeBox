package com.github.yumelira.yumebox.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.clash.clashRuntime
import com.github.yumelira.yumebox.service.clash.module.*
import com.github.yumelira.yumebox.service.common.util.CoreRuntimeConfig
import com.github.yumelira.yumebox.service.runtime.util.sendClashStarted
import com.github.yumelira.yumebox.service.runtime.util.sendClashStopped
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class ClashService : BaseService() {
    private val self: ClashService
        get() = this

    private var reason: String? = null
    private val notificationManager by lazy {
        ServiceNotificationManager(this, ServiceNotificationManager.HTTP_CONFIG)
    }
    private var notificationJob: Job? = null
    private var periodicGcJob: Job? = null

    private val runtime = clashRuntime {
        val close = install(CloseModule(self))
        val config = install(ConfigurationModule(self))
        val network = install(NetworkObserveModule(self))

        install(AppListCacheModule(self))
        install(TimeZoneModule(self))
        install(SuspendModule(self))

        try {
            while (isActive) {
                val quit = select<Boolean> {
                    close.onEvent {
                        true
                    }
                    config.onEvent {
                        reason = it.message

                        true
                    }
                    network.onEvent {
                        false
                    }
                }

                if (quit) break
            }
        } catch (e: Exception) {
            Log.e("Create clash runtime: ${e.message}", e)

            reason = e.message
        } finally {
            withContext(NonCancellable) {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (StatusProvider.serviceRunning)
            return stopSelf()

        StatusProvider.serviceRunning = true

        Log.i("ClashService created in pid=${android.os.Process.myPid()}")

        CoreRuntimeConfig.applyCustomUserAgentIfPresent(this)

        notificationManager.createChannel()
        startForeground(
            ServiceNotificationManager.HTTP_CONFIG.notificationId,
            notificationManager.createInitialNotification()
        )

        runtime.launch()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendClashStarted()
        if (notificationJob?.isActive != true) {
            notificationJob = notificationManager.startTrafficUpdate(this)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        notificationJob = null
        periodicGcJob?.cancel()
        periodicGcJob = null

        StatusProvider.serviceRunning = false

        sendClashStopped(reason)

        Log.i("ClashService destroyed: ${reason ?: "successfully"}")

        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        runtime.requestGc()
    }
}
