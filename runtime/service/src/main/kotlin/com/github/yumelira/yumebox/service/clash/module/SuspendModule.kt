package com.github.yumelira.yumebox.service.clash.module

import android.app.Service
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.common.util.ticker
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class SuspendModule(service: Service) : Module<Unit>(service) {
    private var suspended: Boolean? = null

    private fun applySuspend(shouldSuspend: Boolean, reason: String) {
        if (suspended == shouldSuspend) return

        Clash.suspendCore(shouldSuspend)
        suspended = shouldSuspend

        if (shouldSuspend) {
            Log.d("Clash suspended ($reason)")
        } else {
            Log.d("Clash resumed ($reason)")
        }
    }

    override suspend fun run() {
        val power = service.getSystemService<PowerManager>()
        val interactive = power?.isInteractive ?: true

        applySuspend(!interactive, "initial")

        val screenToggle = receiveBroadcast(Channel.CONFLATED) {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        val safetyTicker = ticker(5_000L)

        try {
            while (true) {
                select<Unit> {
                    screenToggle.onReceive {
                        when (it.action) {
                            Intent.ACTION_SCREEN_OFF -> applySuspend(true, "screen_off")
                            Intent.ACTION_SCREEN_ON,
                            Intent.ACTION_USER_PRESENT -> applySuspend(false, "screen_on")
                            else -> Unit
                        }
                    }
                    safetyTicker.onReceive {
                        val shouldSuspend = !(power?.isInteractive ?: true)
                        applySuspend(shouldSuspend, "poll")
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                Clash.suspendCore(false)
                suspended = false
            }
        }
    }
}
