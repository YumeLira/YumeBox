package com.github.yumelira.yumebox.service.remote

import com.github.yumelira.yumebox.core.model.FetchStatus

fun interface IFetchObserver {
    fun updateStatus(status: FetchStatus)
}
