package com.github.yumelira.yumebox.data.repository

import android.app.Application
import android.net.Uri
import com.github.yumelira.yumebox.core.model.LogMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.enums.enumEntries

class LogRepository(
    private val application: Application,
    private val logRecordGateway: LogRecordGateway,
) {
    companion object {
        private const val STOP_WAIT_MS = 300L
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
        val files = logDir.listFiles(::isManagedLogFile)?.sortedByDescending { it.lastModified() } ?: emptyList()
        return files.map { file ->
            LogFileInfo(
                name = file.name,
                createdAt = file.lastModified(),
                size = file.length(),
                isRecording = currentlyRecording && file.name == currentFileName,
            )
        }
    }

    suspend fun getLogFileSize(fileName: String): Long? = withContext(Dispatchers.IO) {
        resolveLogFile(fileName)?.length()
    }

    suspend fun readLogEntries(fileName: String, maxEntries: Int = DEFAULT_MAX_ENTRIES): List<LogEntry> = withContext(Dispatchers.IO) {
        val file = resolveLogFile(fileName) ?: return@withContext emptyList()
        if (maxEntries <= 0) return@withContext emptyList()
        try {
            file.useLines { lines ->
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
        } catch (_: IOException) {
            emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    suspend fun exportLogFile(fileName: String, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val source = resolveLogFile(fileName) ?: return@withContext false
        try {
            application.contentResolver.openOutputStream(targetUri)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext false
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun deleteLogFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveLogFile(fileName) ?: return@withContext false
        if (isCurrentRecordingFile(file.name)) {
            stopRecording()
            delay(STOP_WAIT_MS)
        }
        file.delete()
    }

    suspend fun deleteAllLogs() = withContext(Dispatchers.IO) {
        if (isRecording()) {
            stopRecording()
            delay(STOP_WAIT_MS)
        }
        val files = logDir.listFiles(::isManagedLogFile) ?: return@withContext
        files.forEach { it.delete() }
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
        if (fileName.contains('/') || fileName.contains('\\') || fileName.contains("..")) return false
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

    data class LogEntry(
        val time: String,
        val level: LogMessage.Level,
        val message: String,
    )
}
