package com.github.yumelira.yumebox.service.common.log

import timber.log.Timber

object Log {
    private const val TAG = "YumeBox"

    fun d(message: String, throwable: Throwable? = null) = Timber.tag(TAG).d(throwable, message)
    fun i(message: String, throwable: Throwable? = null) = Timber.tag(TAG).i(throwable, message)
    fun w(message: String, throwable: Throwable? = null) = Timber.tag(TAG).w(throwable, message)
    fun e(message: String, throwable: Throwable? = null) = Timber.tag(TAG).e(throwable, message)
}
