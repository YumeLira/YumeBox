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
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plus.yumeyuka.yumebox.common.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import plus.yumeyuka.yumebox.App
import plus.yumeyuka.yumebox.BuildConfig
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val progress: Int,
    val currentSize: Long,
    val totalSize: Long,
    val speed: String
)

data class SubscriptionInfo(
    val upload: Long = 0L,
    val download: Long = 0L,
    val total: Long = 0L,
    val expire: Long? = null,
    val title: String? = null,
    val filename: String? = null,
    val interval: Int = 24
)

object DownloadUtil {
    private const val USER_AGENT = "Clash.Meta"
    private const val UPDATE_INTERVAL_MS = 500L

    private fun parseFilenameFromContentDisposition(headers: Headers): String? {
        val contentDisposition = headers["Content-Disposition"] ?: return null

        return try {
            if (contentDisposition.contains("filename*=")) {
                val regex = """filename\*=([^']*)'([^']*)'([^;]+)""".toRegex(RegexOption.IGNORE_CASE)
                regex.find(contentDisposition)?.let { match ->
                    val (_, charset, _, encodedFilename) = match.groupValues
                    URLDecoder.decode(encodedFilename, charset.ifEmpty { "UTF-8" })
                }
            } else {
                val regex = """filename=([^;]+)""".toRegex(RegexOption.IGNORE_CASE)
                regex.find(contentDisposition)?.let { match ->
                    match.groupValues[1].trim('"', '\'')
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSubscriptionInfo(headers: Headers, remoteUrl: String? = null): SubscriptionInfo {
        fun parseTrafficToBytes(trafficStr: String): Long {
            val regex = """(\d+\.?\d*)\s*([KMGT]?B)""".toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(trafficStr) ?: return 0L

            val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
            val unit = match.groupValues[2].uppercase()

            return when (unit) {
                "KB" -> (value * 1024).toLong()
                "MB" -> (value * 1024 * 1024).toLong()
                "GB" -> (value * 1024 * 1024 * 1024).toLong()
                "TB" -> (value * 1024 * 1024 * 1024 * 1024).toLong()
                "B" -> value.toLong()
                else -> 0L
            }
        }

        fun parseExpireDate(expireStr: String): Long? {
            return try {
                when {
                    expireStr.matches(Regex("\\d+")) -> expireStr.toLong() * 1000
                    expireStr.contains("-") -> {

                        val parts = expireStr.split("-")
                        if (parts.size >= 3) {
                            val year = parts[0].toIntOrNull()
                            val month = parts[1].toIntOrNull()
                            val day = parts[2].toIntOrNull()

                            if (year != null && month != null && day != null) {
                                val calendar = java.util.Calendar.getInstance()
                                calendar.set(year, month - 1, day, 0, 0, 0)
                                calendar.set(java.util.Calendar.MILLISECOND, 0)
                                calendar.timeInMillis
                            } else null
                        } else null
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

        return SubscriptionInfo(
            upload = headers["Subscription-Userinfo"]?.let { userInfo ->
                val uploadMatch = Regex("upload=(\\d+)").find(userInfo)
                uploadMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            } ?: 0L,

            download = headers["Subscription-Userinfo"]?.let { userInfo ->
                val downloadMatch = Regex("download=(\\d+)").find(userInfo)
                downloadMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            } ?: 0L,

            total = headers["Subscription-Userinfo"]?.let { userInfo ->
                val totalMatch = Regex("total=(\\d+)").find(userInfo)
                totalMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            } ?: 0L,

            expire = headers["Subscription-Userinfo"]?.let { userInfo ->
                val expireMatch = Regex("expire=(\\d+)").find(userInfo)
                expireMatch?.groupValues?.get(1)?.toLongOrNull()?.let { it * 1000 }
            } ?: headers["Expires"]?.let { parseExpireDate(it) },

            title = headers["Profile-Title"] ?: headers["Subscription-Title"],

            filename = parseFilenameFromContentDisposition(headers),

            interval = headers["Profile-Update-Interval"]?.toIntOrNull() ?:
                      headers["Subscription-Update-Interval"]?.toIntOrNull() ?: 24
        )
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
        onProgress: ((DownloadProgress) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val (success, _) = downloadWithSubscriptionInfo(url, targetFile, onProgress)
        success
    }

    suspend fun downloadWithSubscriptionInfo(
        url: String,
        targetFile: File,
        onProgress: ((DownloadProgress) -> Unit)? = null
    ): Pair<Boolean, SubscriptionInfo?> = withContext(Dispatchers.IO) {
        var success = false
        var subscriptionInfo: SubscriptionInfo? = null

        try {
            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) targetFile.delete()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Pair(false, null)
            }


            subscriptionInfo = parseSubscriptionInfo(response.headers, url)

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
                        } else 0

                        onProgress?.invoke(
                            DownloadProgress(
                                progress = progress,
                                currentSize = totalBytesRead,
                                totalSize = contentLength,
                                speed = formatSpeed(speed)
                            )
                        )

                        lastUpdateTime = currentTime
                        lastBytesRead = totalBytesRead
                    }
                }
                output.flush()
            }

            success = true
        } catch (e: Exception) {
            timber.log.Timber.e(e, "下载失败: $url")
            if (targetFile.exists()) targetFile.delete()
        }

        Pair(success, subscriptionInfo)
    }

    suspend fun downloadAndExtract(
        url: String,
        targetDir: File,
        onProgress: ((DownloadProgress) -> Unit)? = null,
        flattenRootDir: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {

        val fileExtension = when {
            url.endsWith(".zip", ignoreCase = true) -> ".zip"
            url.endsWith(".tar.gz", ignoreCase = true) -> ".tar.gz"
            url.endsWith(".tgz", ignoreCase = true) -> ".tgz"
            url.endsWith(".tar", ignoreCase = true) -> ".tar"
            else -> ".zip"
        }

        val tempFile = File(App.instance.cacheDir, "temp_${System.currentTimeMillis()}$fileExtension")
        val downloadSuccess = download(url, tempFile, onProgress)

        if (downloadSuccess) {
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
        } else {
            false
        }
    }

    private fun flattenRootDirectory(targetDir: File) {
        val subDirs = targetDir.listFiles { it.isDirectory } ?: return


        if (subDirs.size == 1) {
            val rootDir = subDirs[0]
            val rootDirName = rootDir.name


            val commonRootNames = listOf("dist", "build", "public", "www", "static")
            if (commonRootNames.contains(rootDirName)) {

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
        }
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

    suspend fun downloadAndInstallApk(
        context: Context,
        url: String,
        onProgress: ((DownloadProgress) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val apkFile = File(context.cacheDir, "update_${System.currentTimeMillis()}.apk")
        val downloadSuccess = download(url, apkFile, onProgress)

        if (downloadSuccess) {
            installApk(context, apkFile)
        } else {
            false
        }
    }

    private suspend fun installApk(context: Context, apkFile: File): Boolean = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )

                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            timber.log.Timber.e(e, "安装APK失败")
            false
        }
    }
}