package com.github.yumelira.yumebox.service.remote

import com.github.yumelira.yumebox.core.model.LogMessage

interface ILogObserver {
    fun newItem(log: LogMessage)
}
