package com.github.yumelira.yumebox.clash

import com.github.yumelira.yumebox.clash.exception.ConfigImportException
import com.github.yumelira.yumebox.clash.exception.UnknownException
import com.github.yumelira.yumebox.clash.importer.ImportService
import com.github.yumelira.yumebox.clash.importer.ImportState
import com.github.yumelira.yumebox.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

suspend fun downloadProfile(
    profile: Profile,
    workDir: File,
    force: Boolean = true,
    onProgress: ((String, Int) -> Unit)? = null
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val importService = ImportService(workDir)

        val result = importService.importProfile(
            profile = profile,
            force = force,
            onProgress = { state ->
                val (message, progress) = mapStateToProgress(state)
                onProgress?.invoke(message, progress)
            }
        )

        if (result.isSuccess) {
            val configPath = result.getOrThrow()
            Result.success(configPath)
        } else {
            val error = result.exceptionOrNull()
            Timber.e(error, "配置下载失败: ${profile.name}")

            val friendlyError = when (error) {
                is ConfigImportException -> error.userFriendlyMessage
                else -> error?.message ?: "未知错误"
            }

            Result.failure(UnknownException(friendlyError, error))
        }
    }.getOrElse { e ->
        Timber.e(e, "下载配置异常: ${profile.name}")
        Result.failure(e)
    }
}

private fun mapStateToProgress(state: ImportState): Pair<String, Int> {
    return when (state) {
        is ImportState.Idle -> "空闲" to 0
        is ImportState.Preparing -> state.message to 5
        is ImportState.Downloading -> state.message to (10 + (state.progressPercent * 0.4).toInt())
        is ImportState.Copying -> state.message to (10 + (state.progress * 0.4).toInt())
        is ImportState.Validating -> state.message to (50 + (state.progress * 0.2).toInt())
        is ImportState.LoadingProviders -> state.message to (70 + (state.progress * 0.2).toInt())
        is ImportState.Retrying -> state.retryMessage to 5
        is ImportState.Cleaning -> state.message to 95
        is ImportState.Success -> state.message to 100
        is ImportState.Failed -> state.shortError to 0
        is ImportState.Cancelled -> state.message to 0
    }
}

fun cleanupOrphanedConfigs(
    workDir: File,
    validProfiles: List<Profile>
) {
    runCatching {
        val importService = ImportService(workDir)
        val validIds = validProfiles.map { it.id }.toSet()
        importService.cleanupOrphanedConfigs(validIds)
    }.onFailure { e ->
        Timber.e(e, "孤儿配置清理失败")
    }
}

fun Profile.isConfigSaved(workDir: File): Boolean {
    val importedDir = workDir.parentFile?.resolve("imported") ?: return false
    val configFile = File(importedDir, "$id/config.yaml")
    return configFile.exists() && configFile.length() > 0
}
