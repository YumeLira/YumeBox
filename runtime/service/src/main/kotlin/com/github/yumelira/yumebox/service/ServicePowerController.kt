package com.github.yumelira.yumebox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServicePowerController(private val context: Context) {
    private val powerManager = context.getSystemService(PowerManager::class.java)

    private val _screenOn = MutableStateFlow(isScreenOn())
    val screenOn: StateFlow<Boolean> = _screenOn.asStateFlow()

    private val _deviceIdle = MutableStateFlow(isDeviceIdle())
    val deviceIdle: StateFlow<Boolean> = _deviceIdle.asStateFlow()

    private var started = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateState()
        }
    }

    fun start() {
        if (started) return
        started = true
        register()
        updateState()
    }

    fun stop() {
        if (!started) return
        started = false
        unregister()
    }

    private fun updateState() {
        _screenOn.value = isScreenOn()
        _deviceIdle.value = isDeviceIdle()
    }

    private fun isScreenOn(): Boolean {
        return powerManager?.isInteractive != false
    }

    private fun isDeviceIdle(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isDeviceIdleMode == true
        } else {
            false
        }
    }

    private fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
    }

    private fun unregister() {
        runCatching { context.unregisterReceiver(receiver) }
    }
}
