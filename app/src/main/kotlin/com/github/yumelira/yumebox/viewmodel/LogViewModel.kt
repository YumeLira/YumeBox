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

package com.github.yumelira.yumebox.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LogViewModel(
    private val repository: LogRepository,
) : ViewModel() {
    private companion object {
        const val MAX_LOG_ENTRIES = 2000
    }

    private val _isRecording = MutableStateFlow(repository.isRecording())
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _logFiles = MutableStateFlow<List<LogFileInfo>>(emptyList())
    val logFiles: StateFlow<List<LogFileInfo>> = _logFiles.asStateFlow()

    init {
        refreshLogFiles()
    }

    fun startRecording() {
        repository.startRecording()
        _isRecording.value = true
        viewModelScope.launch {
            delay(300)
            refreshLogFiles()
        }
    }

    fun stopRecording() {
        repository.stopRecording()
        _isRecording.value = false
        viewModelScope.launch {
            delay(300)
            refreshLogFiles()
        }
    }

    fun refreshLogFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = repository.listLogFiles()
            _isRecording.value = repository.isRecording()
            _logFiles.value = files.map {
                LogFileInfo(
                    name = it.name,
                    createdAt = it.createdAt,
                    size = it.size,
                    isRecording = it.isRecording,
                )
            }
        }
    }

    fun isCurrentRecordingFile(fileName: String): Boolean {
        return repository.isCurrentRecordingFile(fileName)
    }

    fun deleteLogFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLogFile(fileName)
            _isRecording.value = repository.isRecording()
            _logFiles.update { files -> files.filterNot { it.name == fileName } }
            refreshLogFiles()
        }
    }

    fun deleteAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllLogs()
            _isRecording.value = repository.isRecording()
            _logFiles.value = emptyList()
            refreshLogFiles()
        }
    }

    suspend fun getLogFileSize(fileName: String): Long? {
        return repository.getLogFileSize(fileName)
    }

    suspend fun readLogContent(fileName: String): List<LogEntry> {
        return repository.readLogEntries(fileName, MAX_LOG_ENTRIES).map {
            LogEntry(
                time = it.time,
                level = it.level,
                message = it.message,
            )
        }
    }

    suspend fun exportLogFile(fileName: String, targetUri: Uri): Boolean {
        return repository.exportLogFile(fileName, targetUri)
    }

    data class LogFileInfo(
        val name: String,
        val createdAt: Long,
        val size: Long,
        val isRecording: Boolean,
    )

    data class LogEntry(
        val time: String,
        val level: LogMessage.Level,
        val message: String,
    )
}
