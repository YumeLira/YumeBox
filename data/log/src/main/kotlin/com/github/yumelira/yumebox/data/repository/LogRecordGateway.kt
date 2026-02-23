package com.github.yumelira.yumebox.data.repository

import android.app.Application
import java.io.File

interface LogRecordGateway {
    val isRecording: Boolean
    val currentLogFileName: String?
    val logPrefix: String
    val logSuffix: String

    fun start(application: Application)
    fun stop(application: Application)
    fun getLogDir(application: Application): File
}
