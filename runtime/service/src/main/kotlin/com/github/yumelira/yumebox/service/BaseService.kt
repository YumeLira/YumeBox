package com.github.yumelira.yumebox.service

import android.app.Service
import com.github.yumelira.yumebox.service.common.util.initializeServiceGlobal
import com.github.yumelira.yumebox.service.runtime.util.cancelAndJoinBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    override fun onCreate() {
        super.onCreate()
        // Ensure service-side globals are initialized for constants/broadcasts.
        val app = applicationContext as android.app.Application
        initializeServiceGlobal(app)
    }

    override fun onDestroy() {
        super.onDestroy()

        cancelAndJoinBlocking()
    }
}
