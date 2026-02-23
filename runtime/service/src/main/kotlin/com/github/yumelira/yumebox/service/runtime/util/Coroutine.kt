package com.github.yumelira.yumebox.service.runtime.util

import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking

fun CoroutineScope.cancelAndJoinBlocking() {
    val scope = this
    val job = scope.coroutineContext.job

    job.cancel()

    // Single-process mode: service onDestroy runs on app main thread.
    // Blocking join on main thread can freeze the whole UI.
    if (Looper.myLooper() == Looper.getMainLooper()) {
        return
    }

    runBlocking {
        job.join()
    }
}
