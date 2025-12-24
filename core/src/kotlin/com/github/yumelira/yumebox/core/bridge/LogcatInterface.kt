package com.github.yumelira.yumebox.core.bridge

import androidx.annotation.Keep

@Keep
interface LogcatInterface {
    fun received(jsonPayload: String)
}
