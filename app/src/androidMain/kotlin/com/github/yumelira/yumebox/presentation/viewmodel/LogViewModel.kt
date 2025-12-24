/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.service.LogRecordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LogViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _logFiles = MutableStateFlow<List<LogFileInfo>>(emptyList())
    val logFiles: StateFlow<List<LogFileInfo>> = _logFiles.asStateFlow()

    private val logDir: File
        get() = LogRecordService.getLogDir(getApplication())

    init {
        refreshLogFiles()
    }

    fun startRecording() {
        LogRecordService.start(getApplication())
        _isRecording.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            refreshLogFiles()
        }
    }

    fun stopRecording() {
        LogRecordService.stop(getApplication())
        _isRecording.value = false
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            refreshLogFiles()
        }
    }

    fun refreshLogFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val isCurrentlyRecording = LogRecordService.isRecording
            val currentFileName = LogRecordService.currentLogFileName

            _isRecording.value = isCurrentlyRecording

            val files = logDir.listFiles { file ->
                file.isFile && file.name.startsWith(LogRecordService.LOG_PREFIX) && file.name.endsWith(LogRecordService.LOG_SUFFIX)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            val fileInfos = files.map { file ->
                LogFileInfo(
                    file = file,
                    name = file.name,
                    createdAt = file.lastModified(),
                    size = file.length(),
                    isRecording = isCurrentlyRecording && file.name == currentFileName
                )
            }
            _logFiles.value = fileInfos
        }
    }

    fun deleteLogFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFileName = LogRecordService.currentLogFileName
            if (LogRecordService.isRecording && file.name == currentFileName) {
                LogRecordService.stop(getApplication())
                _isRecording.value = false
                kotlinx.coroutines.delay(500)
            }

            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    timber.log.Timber.e("删除文件失败: ${file.absolutePath}")
                }
            }
            refreshLogFiles()
        }
    }

    fun deleteAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            if (LogRecordService.isRecording) {
                LogRecordService.stop(getApplication())
                _isRecording.value = false
                kotlinx.coroutines.delay(500)
            }

            logDir.listFiles()?.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            refreshLogFiles()
        }
    }

    suspend fun readLogContent(file: File): List<LogEntry> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext emptyList()
            file.readLines().mapNotNull { line ->
                parseLogLine(line)
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "读取日志文件失败")
            emptyList()
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        if (line.isBlank()) return null
        val regex = """\[(.+?)] \[(.+?)] (.+)""".toRegex()
        val match = regex.find(line) ?: return null
        val (timeStr, levelStr, message) = match.destructured

        val level = try {
            LogMessage.Level.valueOf(levelStr)
        } catch (_: Exception) {
            LogMessage.Level.Unknown
        }

        return LogEntry(time = timeStr, level = level, message = message)
    }

    data class LogFileInfo(
        val file: File, val name: String, val createdAt: Long, val size: Long, val isRecording: Boolean
    )

    data class LogEntry(
        val time: String, val level: LogMessage.Level, val message: String
    )
}
