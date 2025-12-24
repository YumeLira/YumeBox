package com.github.yumelira.yumebox.clash.importer

import com.github.yumelira.yumebox.clash.exception.*
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.bridge.ClashException
import com.github.yumelira.yumebox.core.model.FetchStatus
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ImportService(private val workDir: File) {

    companion object {
        private const val IMPORTED_DIR = "imported"
        private const val CONFIG_FILE = "config.yaml"
    }

    private val activeImports = ConcurrentHashMap<String, Job>()

    init {
        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
        if (importedDir != null) {
            if (!importedDir.exists()) {
                val created = importedDir.mkdirs()
                if (!created && !importedDir.exists()) {
                    Timber.e("无法创建导入目录: ${importedDir.absolutePath}")
                }
            }
        } else {
            Timber.e("无法确定导入目录路径: workDir=${workDir.absolutePath}")
        }
    }

    suspend fun importProfile(
        profile: Profile,
        force: Boolean = false,
        onProgress: ((ImportState) -> Unit)? = null
    ): Result<String> = coroutineScope {
        try {
            if (activeImports.containsKey(profile.id)) {
                throw IllegalStateException("配置正在导入中: ${profile.name}")
            }

            activeImports[profile.id] = coroutineContext.job

            onProgress?.invoke(ImportState.Preparing("准备导入配置..."))

            val configPath = when (profile.type) {
                ProfileType.URL -> importFromUrl(profile, force, onProgress)
                ProfileType.FILE -> importFromFile(profile, onProgress)
            }

            onProgress?.invoke(ImportState.Success(configPath))
            Result.success(configPath)

        } catch (e: CancellationException) {
            onProgress?.invoke(ImportState.Cancelled())
            cleanupFailedImport(profile.id)
            throw e
        } catch (e: Exception) {
            val importException = e.toConfigImportException()

            val errorMessage = importException.userFriendlyMessage

            Timber.e(e, "配置导入失败: ${profile.name} - $errorMessage")
            onProgress?.invoke(ImportState.Failed(errorMessage, e))
            cleanupFailedImport(profile.id)
            Result.failure(importException)
        } finally {
            activeImports.remove(profile.id)
        }
    }

    fun cancelImport(profileId: String): Boolean {
        val job = activeImports[profileId]
        return if (job != null) {
            job.cancel()
            true
        } else {
            false
        }
    }

    private suspend fun importFromUrl(
        profile: Profile,
        force: Boolean,
        onProgress: ((ImportState) -> Unit)?
    ): String = withContext(Dispatchers.IO) {
        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
            ?: throw FileAccessException("无法确定导入目录", FileAccessException.Reason.INVALID_PATH)

        if (!importedDir.exists()) {
            val created = importedDir.mkdirs()
            if (!created && !importedDir.exists()) {
                throw FileAccessException(
                    "无法创建导入目录: ${importedDir.absolutePath}",
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        val targetDir = File(importedDir, profile.id)

        if (!targetDir.exists()) {
            val created = targetDir.mkdirs()
            if (!created && !targetDir.exists()) {
                throw FileAccessException(
                    "无法创建配置目录: ${targetDir.absolutePath}",
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        val url = profile.remoteUrl ?: profile.config
        if (url.isBlank()) {
            throw NetworkException("URL 地址为空")
        }

        onProgress?.invoke(ImportState.Downloading(0, 0L, 0L, "正在下载配置..."))

        try {
            Clash.fetchAndValid(
                path = targetDir,
                url = url,
                force = force,
                reportStatus = { status ->
                    ensureActive()

                    val progress = if (status.max > 0) {
                        (status.progress * 100 / status.max).coerceIn(0, 100)
                    } else 0

                    val state = when (status.action) {
                        FetchStatus.Action.FetchConfiguration -> {
                            ImportState.Downloading(progress, 0L, 0L, "下载配置文件...")
                        }

                        FetchStatus.Action.FetchProviders -> {
                            ImportState.LoadingProviders(
                                progress = progress,
                                currentProvider = status.args.firstOrNull(),
                                totalProviders = status.max,
                                completedProviders = status.progress,
                                message = "下载外部资源..."
                            )
                        }

                        FetchStatus.Action.Verifying -> {
                            ImportState.Validating(progress, "验证配置...")
                        }
                    }
                    onProgress?.invoke(state)
                }
            ).await()

            File(targetDir, CONFIG_FILE).absolutePath

        } catch (e: ClashException) {
            targetDir.deleteRecursively()
            throw ConfigValidationException(
                e.message ?: "配置验证失败",
                e
            )
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            throw NetworkException("下载失败: ${e.message}", e)
        }
    }

    private suspend fun importFromFile(
        profile: Profile,
        onProgress: ((ImportState) -> Unit)?
    ): String = withContext(Dispatchers.IO) {
        val sourcePath = profile.config
        if (sourcePath.isBlank()) {
            throw FileAccessException("配置文件路径为空", FileAccessException.Reason.INVALID_PATH)
        }

        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            throw FileAccessException("配置文件不存在: $sourcePath", FileAccessException.Reason.NOT_FOUND)
        }

        if (!sourceFile.canRead()) {
            throw FileAccessException(
                "无法读取配置文件: $sourcePath",
                FileAccessException.Reason.NO_READ_PERMISSION
            )
        }

        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
            ?: throw FileAccessException("无法确定导入目录", FileAccessException.Reason.INVALID_PATH)

        if (!importedDir.exists()) {
            val created = importedDir.mkdirs()
            if (!created && !importedDir.exists()) {
                throw FileAccessException(
                    "无法创建导入目录: ${importedDir.absolutePath}",
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        val targetDir = File(importedDir, profile.id)

        if (!targetDir.exists()) {
            val created = targetDir.mkdirs()
            if (!created && !targetDir.exists()) {
                throw FileAccessException(
                    "无法创建配置目录: ${targetDir.absolutePath}",
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        onProgress?.invoke(ImportState.Copying(0, "复制配置文件..."))

        val targetFile = File(targetDir, CONFIG_FILE)

        try {
            val isSameFile = runCatching {
                sourceFile.canonicalPath == targetFile.canonicalPath
            }.getOrElse { e ->
                Timber.w(e, "无法比较文件路径，使用绝对路径比较")
                sourceFile.absolutePath == targetFile.absolutePath
            }

            if (!isSameFile) {
                sourceFile.copyTo(targetFile, overwrite = true)
                onProgress?.invoke(ImportState.Copying(50, "复制完成"))
            } else {
                onProgress?.invoke(ImportState.Copying(50, "文件已就位"))
            }

            onProgress?.invoke(ImportState.Validating(50, "验证配置..."))

            Clash.fetchAndValid(
                path = targetDir,
                url = "",
                force = false,
                reportStatus = { status ->
                    ensureActive()

                    val progress = if (status.max > 0) {
                        50 + (status.progress * 50 / status.max).coerceIn(0, 50)
                    } else 50

                    val state = when (status.action) {
                        FetchStatus.Action.FetchProviders -> {
                            ImportState.LoadingProviders(
                                progress = progress,
                                currentProvider = status.args.firstOrNull(),
                                totalProviders = status.max,
                                completedProviders = status.progress,
                                message = "下载外部资源..."
                            )
                        }

                        FetchStatus.Action.Verifying -> {
                            ImportState.Validating(progress, "验证配置...")
                        }

                        else -> {
                            null
                        }
                    }
                    state?.let { onProgress?.invoke(it) }
                }
            ).await()

            targetFile.absolutePath

        } catch (e: ClashException) {
            targetDir.deleteRecursively()
            throw ConfigValidationException(
                e.message ?: "配置验证失败",
                e
            )
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            throw FileAccessException("导入失败: ${e.message}", FileAccessException.Reason.UNKNOWN, e)
        }
    }

    private fun cleanupFailedImport(profileId: String) {
        runCatching {
            val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR) ?: return
            val profileDir = File(importedDir, profileId)
            if (profileDir.exists()) {
                profileDir.deleteRecursively()
            }
        }.onFailure { e ->
            Timber.e(e, "清理失败的导入出错: $profileId")
        }
    }

    fun cleanupOrphanedConfigs(validProfileIds: Set<String>) {
        runCatching {
            val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
            if (importedDir == null || !importedDir.exists()) {
                return
            }

            var cleanedCount = 0
            importedDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.name !in validProfileIds) {
                    dir.deleteRecursively()
                    cleanedCount++
                }
            }

            if (cleanedCount > 0) {
                Timber.i("已清理 $cleanedCount 个孤儿配置")
            }
        }.onFailure { e ->
            Timber.e(e, "清理孤儿配置出错")
        }
    }
}
