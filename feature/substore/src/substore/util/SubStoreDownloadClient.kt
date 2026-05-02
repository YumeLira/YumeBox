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

package com.github.yumelira.yumebox.substore.util

import android.app.Application
import com.github.yumelira.yumebox.common.util.ByteFormatter.formatSpeed
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val progress: Int,
    val currentSize: Long,
    val totalSize: Long,
    val speed: String,
)

data class SubscriptionInfo(
    val upload: Long = 0L,
    val download: Long = 0L,
    val total: Long = 0L,
    val expire: Long? = null,
    val title: String? = null,
    val filename: String? = null,
    val interval: Int = 24,
)

class SubStoreDownloadClient(
    private val application: Application,
    private val appSettings: AppSettingsStore,
) {
    companion object {
        private const val DEFAULT_USER_AGENT = "ClashMetaForAndroid"
        private const val UPDATE_INTERVAL_MS = 500L
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun download(
        url: String,
        targetFile: File,
        onProgress: ((DownloadProgress) -> Unit)? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        val (success, _) = downloadWithSubscriptionInfo(url, targetFile, onProgress)
        success
    }

    suspend fun downloadWithSubscriptionInfo(
        url: String,
        targetFile: File,
        onProgress: ((DownloadProgress) -> Unit)? = null,
    ): Pair<Boolean, SubscriptionInfo?> = withContext(Dispatchers.IO) {
        try {
            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) targetFile.delete()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", resolveUserAgent())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Pair(false, null)
                }

                val subscriptionInfo = parseSubscriptionInfo(response.headers)
                val body = response.body
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()

                var lastUpdateTime = 0L
                var lastBytesRead = 0L
                var totalBytesRead = 0L

                targetFile.sink().buffer().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
                            val timeDiff = (currentTime - lastUpdateTime) / 1000.0
                            val bytesDiff = totalBytesRead - lastBytesRead
                            val speed = if (timeDiff > 0) (bytesDiff / timeDiff).toLong() else 0L
                            val progress = if (contentLength > 0) {
                                ((totalBytesRead * 100) / contentLength).toInt()
                            } else {
                                0
                            }

                            onProgress?.invoke(
                                DownloadProgress(
                                    progress = progress,
                                    currentSize = totalBytesRead,
                                    totalSize = contentLength,
                                    speed = formatSpeed(speed),
                                ),
                            )

                            lastUpdateTime = currentTime
                            lastBytesRead = totalBytesRead
                        }
                    }
                    output.flush()
                }

                Pair(true, subscriptionInfo)
            }
        } catch (error: Exception) {
            Timber.e(error, "Download failed: %s", url)
            if (targetFile.exists()) targetFile.delete()
            Pair(false, null)
        }
    }

    suspend fun downloadAndExtract(
        url: String,
        targetDir: File,
        onProgress: ((DownloadProgress) -> Unit)? = null,
        flattenRootDir: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val fileExtension = when {
            url.endsWith(".zip", ignoreCase = true) -> ".zip"
            url.endsWith(".tar.gz", ignoreCase = true) -> ".tar.gz"
            url.endsWith(".tgz", ignoreCase = true) -> ".tgz"
            url.endsWith(".tar", ignoreCase = true) -> ".tar"
            else -> ".zip"
        }

        val tempFile = File(
            application.cacheDir,
            "temp_${System.currentTimeMillis()}$fileExtension",
        )
        val downloadSuccess = download(url, tempFile, onProgress)
        if (!downloadSuccess) {
            return@withContext false
        }

        val extractSuccess = when (fileExtension.lowercase()) {
            ".zip" -> ArchiveUtil.unzipZip(tempFile, targetDir)
            ".tar.gz", ".tgz" -> ArchiveUtil.untarGz(tempFile, targetDir)
            ".tar" -> ArchiveUtil.untar(tempFile, targetDir)
            else -> ArchiveUtil.unzipZip(tempFile, targetDir)
        }

        tempFile.delete()

        if (extractSuccess && flattenRootDir) {
            flattenRootDirectory(targetDir)
        }

        extractSuccess
    }

    private fun resolveUserAgent(): String {
        val customUA = appSettings.customUserAgent.value
        return customUA.ifEmpty { DEFAULT_USER_AGENT }
    }

    private fun parseSubscriptionInfo(headers: Headers): SubscriptionInfo {
        fun parseLikeJsParseInt(value: String): Long {
            val trimmed = value.trim()
            val integerPart = trimmed.takeWhile { it.isDigit() }
            if (integerPart.isNotEmpty()) return integerPart.toLongOrNull() ?: 0L
            return trimmed.substringBefore('.').toLongOrNull() ?: 0L
        }

        fun findHeaderBySuffix(suffix: String): String? {
            val target = suffix.lowercase(Locale.getDefault())
            val key = headers.names().firstOrNull {
                it.lowercase(Locale.getDefault()).endsWith(target)
            } ?: return null
            return headers[key]
        }

        fun parseExpireDate(value: String): Long? = runCatching {
            when {
                value.matches(Regex("\\d+")) -> value.toLong() * 1000L
                value.contains("-") -> {
                    val parts = value.split("-")
                    if (parts.size < 3) return@runCatching null

                    val year = parts[0].toIntOrNull() ?: return@runCatching null
                    val month = parts[1].toIntOrNull() ?: return@runCatching null
                    val day = parts[2].toIntOrNull() ?: return@runCatching null

                    val calendar = Calendar.getInstance()
                    calendar.set(year, month - 1, day, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }
                else -> null
            }
        }.getOrNull()

        val userInfo = headers["Subscription-Userinfo"] ?: findHeaderBySuffix("subscription-userinfo")
        var upload = 0L
        var download = 0L
        var total = 0L
        var expire = 0L

        if (!userInfo.isNullOrBlank()) {
            userInfo.split(";").forEach { flag ->
                val parts = flag.trim().split("=", limit = 2)
                if (parts.size < 2) return@forEach

                val key = parts[0].trim().lowercase(Locale.getDefault())
                val value = parts[1].trim()
                when {
                    key.contains("upload") -> upload = parseLikeJsParseInt(value)
                    key.contains("download") -> download = parseLikeJsParseInt(value)
                    key.contains("total") -> total = parseLikeJsParseInt(value)
                    key.contains("expire") -> expire = parseLikeJsParseInt(value) * 1000L
                }
            }
        }

        if (expire == 0L) {
            expire = (
                headers["Expires"]
                    ?: findHeaderBySuffix("expires")
                )?.let(::parseExpireDate) ?: 0L
        }

        val title = decodeSubscriptionTitle(
            headers["Profile-Title"]
                ?: headers["Subscription-Title"]
                ?: findHeaderBySuffix("profile-title")
                ?: findHeaderBySuffix("subscription-title"),
        )

        val filename = parseFilenameFromHeaders(headers, findHeaderBySuffix("content-disposition"))
        val interval = headers["Profile-Update-Interval"]?.toIntOrNull()
            ?: headers["Subscription-Update-Interval"]?.toIntOrNull()
            ?: findHeaderBySuffix("profile-update-interval")?.toIntOrNull()
            ?: findHeaderBySuffix("subscription-update-interval")?.toIntOrNull()
            ?: 24

        return SubscriptionInfo(
            upload = upload,
            download = download,
            total = total,
            expire = expire.takeIf { it > 0L },
            title = title,
            filename = filename,
            interval = interval,
        )
    }

    private fun decodeSubscriptionTitle(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null

        fun decodeBase64(encoded: String): String? {
            val candidate = encoded.trim().trim('"', '\'')
            if (candidate.isBlank()) return null
            if (!candidate.matches(Regex("^[A-Za-z0-9+/=]+$"))) return null
            return runCatching {
                String(Base64.getDecoder().decode(candidate), StandardCharsets.UTF_8).trim()
            }.getOrNull()
        }

        fun decodeRfc5987(candidate: String): String? {
            val match = Regex("""^([^']*)'[^']*'(.*)$""").find(candidate.trim()) ?: return null
            val charset = match.groupValues[1].ifBlank { "UTF-8" }
            val encoded = match.groupValues[2]

            return runCatching {
                URLDecoder.decode(encoded, charset).trim()
            }.getOrNull()
        }

        return runCatching {
            val normalized = value.trim().trim('"', '\'')
            when {
                normalized.startsWith("base64:", ignoreCase = true) -> {
                    decodeBase64(normalized.substringAfter(':')) ?: value
                }
                else -> {
                    decodeRfc5987(normalized)
                        ?: runCatching {
                            URLDecoder.decode(normalized, StandardCharsets.UTF_8.name()).trim()
                        }.getOrNull()
                        ?: decodeBase64(normalized)
                        ?: value
                }
            }
        }.getOrElse { value }.takeIf { it.isNotBlank() }
    }

    private fun parseFilenameFromHeaders(
        headers: Headers,
        fallbackContentDisposition: String?,
    ): String? {
        val contentDisposition = headers["Content-Disposition"] ?: fallbackContentDisposition ?: return null
        return runCatching {
            if (contentDisposition.contains("filename*=", ignoreCase = true)) {
                val regex = """filename\*=([^']*)'([^']*)'([^;]+)""".toRegex(RegexOption.IGNORE_CASE)
                regex.find(contentDisposition)?.let { match ->
                    val charset = match.groupValues[1].ifBlank { "UTF-8" }
                    val encodedFilename = match.groupValues[3].trim().trim('"', '\'')
                    val safeCharset = runCatching { Charset.forName(charset).name() }.getOrDefault("UTF-8")
                    URLDecoder.decode(encodedFilename, safeCharset).trim()
                }
            } else {
                val regex = """filename=([^;]+)""".toRegex(RegexOption.IGNORE_CASE)
                regex.find(contentDisposition)?.groupValues?.getOrNull(1)?.trim()?.trim('"', '\'')
            }?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun flattenRootDirectory(targetDir: File) {
        val subDirs = targetDir.listFiles { it.isDirectory } ?: return
        if (subDirs.size != 1) return

        val rootDir = subDirs[0]
        val commonRootNames = setOf("dist", "build", "public", "www", "static")
        if (rootDir.name !in commonRootNames) return

        rootDir.listFiles()?.forEach { file ->
            val newFile = File(targetDir, file.name)
            if (file.isDirectory) {
                moveDirectoryRecursively(file, newFile)
            } else {
                file.renameTo(newFile)
            }
        }
        rootDir.delete()
    }

    private fun moveDirectoryRecursively(source: File, destination: File) {
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()

        source.listFiles()?.forEach { file ->
            val destFile = File(destination, file.name)
            if (file.isDirectory) {
                moveDirectoryRecursively(file, destFile)
                file.delete()
            } else {
                file.renameTo(destFile)
            }
        }

        source.delete()
    }
}
