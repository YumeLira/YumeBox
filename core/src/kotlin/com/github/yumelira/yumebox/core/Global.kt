package com.github.yumelira.yumebox.core

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

object Global : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    val application: Application
        get() = _application

    private lateinit var _application: Application

    fun init(application: Application) {
        _application = application
    }

    fun destroy() {
        cancel()
    }
}

