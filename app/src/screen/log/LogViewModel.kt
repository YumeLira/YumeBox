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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.screen.log

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.store.LogStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogViewModel(
    private val repository: LogStore,
) : ViewModel() {

    private val _isRecording = MutableStateFlow(repository.isRecording())
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _tempLogEntries = MutableStateFlow<List<LogStore.LogEntry>>(emptyList())
    val tempLogEntries: StateFlow<List<LogStore.LogEntry>> = _tempLogEntries.asStateFlow()

    fun startRecording() {
        repository.startRecording()
        _isRecording.value = true
        _tempLogEntries.value = emptyList()
    }

    fun stopRecording() {
        repository.stopRecording()
        _isRecording.value = false
    }

    fun refreshTempLogEntries() {
        if (!_isRecording.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _tempLogEntries.value = repository.readTempLogEntries()
        }
    }

    fun clearTempLog() {
        _tempLogEntries.value = emptyList()
    }

    suspend fun saveTempLog(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val entries = _tempLogEntries.value
        if (entries.isEmpty()) return@withContext false
        try {
            repository.writeLogEntries(targetUri, entries)
            true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            false
        }
    }
}
