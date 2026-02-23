package com.github.yumelira.yumebox.service

import android.app.Application
import com.github.yumelira.yumebox.data.repository.LogRecordGateway
import java.io.File

class LogRecordServiceGateway : LogRecordGateway {
    override val isRecording: Boolean
        get() = LogRecordService.isRecording

    override val currentLogFileName: String?
        get() = LogRecordService.currentLogFileName

    override val logPrefix: String
        get() = LogRecordService.LOG_PREFIX

    override val logSuffix: String
        get() = LogRecordService.LOG_SUFFIX

    override fun start(application: Application) {
        LogRecordService.start(application)
    }

    override fun stop(application: Application) {
        LogRecordService.stop(application)
    }

    override fun getLogDir(application: Application): File {
        return LogRecordService.getLogDir(application)
    }
}
