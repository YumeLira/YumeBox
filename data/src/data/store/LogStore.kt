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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.data.store

import android.app.Application
import android.net.Uri
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.gateway.LogRecordGateway
import java.io.File
import java.io.IOException
import kotlin.enums.enumEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogStore(
    private val application: Application,
    private val logRecordGateway: LogRecordGateway,
) {
    companion object {
        private const val DEFAULT_MAX_ENTRIES = 2000
        private val LOG_LINE_REGEX = """\[(.+?)] \[(.+?)] (.+)""".toRegex()
        private val LOG_LEVELS = enumEntries<LogMessage.Level>().associateBy { it.name }
    }

    private val logDir: File
        get() = logRecordGateway.getLogDir(application)

    fun startRecording() {
        logRecordGateway.start(application)
    }

    fun stopRecording() {
        logRecordGateway.stop(application)
    }

    fun isRecording(): Boolean {
        return logRecordGateway.isRecording
    }

    fun isCurrentRecordingFile(fileName: String): Boolean {
        return isRecording() && logRecordGateway.currentLogFileName == fileName
    }

    fun listLogFiles(): List<LogFileInfo> {
        val currentlyRecording = isRecording()
        val currentFileName = logRecordGateway.currentLogFileName
        val files =
            logDir.listFiles(::isManagedLogFile)?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        return files.map { file ->
            LogFileInfo(
                name = file.name,
                createdAt = file.lastModified(),
                size = file.length(),
                isRecording = currentlyRecording && file.name == currentFileName,
            )
        }
    }

    suspend fun getLogFileSize(fileName: String): Long? =
        withContext(Dispatchers.IO) { resolveLogFile(fileName)?.length() }

    suspend fun readLogEntries(
        fileName: String,
        maxEntries: Int = DEFAULT_MAX_ENTRIES,
    ): List<LogEntry> =
        withContext(Dispatchers.IO) {
            val file = resolveLogFile(fileName) ?: return@withContext emptyList()
            if (maxEntries <= 0) return@withContext emptyList()
            try {
                readTailLogEntries(file, maxEntries)
            } catch (_: IOException) {
                emptyList()
            } catch (_: SecurityException) {
                emptyList()
            }
        }

    suspend fun exportLogFile(fileName: String, targetUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val source = resolveLogFile(fileName) ?: return@withContext false
            try {
                application.contentResolver.openOutputStream(targetUri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: return@withContext false
                true
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }

    suspend fun readTempLogEntries(maxEntries: Int = 2000): List<LogEntry> =
        withContext(Dispatchers.IO) {
            val currentlyRecording = isRecording()
            val currentFileName = logRecordGateway.currentLogFileName
            if (!currentlyRecording || currentFileName == null) {
                return@withContext emptyList()
            }
            readLogEntries(currentFileName, maxEntries)
        }

    suspend fun writeLogEntries(targetUri: Uri, entries: List<LogEntry>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                application.contentResolver.openOutputStream(targetUri)?.use { output ->
                    val sb = StringBuilder()
                    entries.forEach { entry ->
                        sb.append("[${entry.time}] [${entry.level.name}] ${entry.message}\n")
                    }
                    output.write(sb.toString().toByteArray())
                }
                true
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }

    suspend fun deleteLogFile(fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = resolveLogFile(fileName) ?: return@withContext false
            stopRecordingIfNeeded(file.name)
            file.delete()
        }

    suspend fun deleteAllLogs() =
        withContext(Dispatchers.IO) {
            stopRecordingIfNeeded()
            val files = logDir.listFiles(::isManagedLogFile) ?: return@withContext
            files.forEach { it.delete() }
        }

    private suspend fun stopRecordingIfNeeded(fileName: String? = null) {
        if (!isRecording()) return
        if (fileName != null && !isCurrentRecordingFile(fileName)) return
        stopRecording()
        PollingTimers.awaitTick(logRecordGateway.stopWaitSpec)
    }

    private fun readTailLogEntries(file: File, maxEntries: Int): List<LogEntry> {
        return file.useLines { lines ->
            val ring = ArrayDeque<LogEntry>(maxEntries)
            lines.forEach { line ->
                val entry = parseLogLine(line) ?: return@forEach
                if (ring.size == maxEntries) {
                    ring.removeFirst()
                }
                ring.addLast(entry)
            }
            ring.toList()
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        if (line.isBlank()) return null
        val match = LOG_LINE_REGEX.find(line) ?: return null
        val (timeStr, levelStr, message) = match.destructured
        val level = LOG_LEVELS[levelStr] ?: LogMessage.Level.Unknown
        return LogEntry(time = timeStr, level = level, message = message)
    }

    private fun resolveLogFile(fileName: String): File? {
        if (!isSafeLogFileName(fileName)) return null
        val file = File(logDir, fileName)
        return file.takeIf(::isManagedLogFile)
    }

    private fun isManagedLogFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        if (!file.name.endsWith(logRecordGateway.logSuffix)) return false
        val prefix = logRecordGateway.logPrefix
        return prefix.isBlank() || file.name.startsWith(prefix)
    }

    private fun isSafeLogFileName(fileName: String): Boolean {
        if (fileName.isBlank()) return false
        if (fileName.contains('/') || fileName.contains('\\') || fileName.contains(".."))
            return false
        if (!fileName.endsWith(logRecordGateway.logSuffix)) return false
        val prefix = logRecordGateway.logPrefix
        return prefix.isBlank() || fileName.startsWith(prefix)
    }

    data class LogFileInfo(
        val name: String,
        val createdAt: Long,
        val size: Long,
        val isRecording: Boolean,
    )

    data class LogEntry(val time: String, val level: LogMessage.Level, val message: String)
}
