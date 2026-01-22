package com.github.yumelira.yumebox.data.repository

import android.content.Context
import android.net.Uri
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProvidersRepository {

    suspend fun queryProviders(): Result<List<Provider>> {
        return try {
            Result.success(Clash.queryProviders())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProvider(provider: Provider): Result<Unit> {
        return updateProviderInternal(provider.type, provider.name)
    }

    suspend fun updateAllProviders(providers: List<Provider>): Result<UpdateProvidersResult> {
        if (providers.isEmpty()) return Result.success(UpdateProvidersResult(emptyList()))

        val failed = mutableListOf<String>()
        providers.forEach { provider ->
            val result = updateProviderInternal(provider.type, provider.name)
            if (result.isFailure) {
                failed.add(provider.name)
            }
        }
        return Result.success(UpdateProvidersResult(failed))
    }

    suspend fun uploadProviderFile(
        context: Context,
        provider: Provider,
        uri: Uri,
        maxBytes: Long = MAX_UPLOAD_SIZE_BYTES
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val targetFile = buildTargetFile(provider)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IllegalStateException("无法读取文件: $uri"))

                inputStream.use { input ->
                    val size = input.available().toLong()
                    if (size > maxBytes) {
                        return@withContext Result.failure(IllegalStateException("文件超过 ${maxBytes / (1024 * 1024)}MB 限制"))
                    }

                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun updateProviderInternal(type: Provider.Type, name: String): Result<Unit> {
        return try {
            Clash.updateProvider(type, name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildTargetFile(provider: Provider): File {
        if (provider.path.isBlank()) {
            throw IllegalStateException("Provider path is empty")
        }
        val targetFile = File(provider.path)
        targetFile.parentFile?.mkdirs()
        return targetFile
    }

    data class UpdateProvidersResult(
        val failedProviders: List<String>
    )

    companion object {
        private const val MAX_UPLOAD_SIZE_BYTES = 50L * 1024 * 1024
    }
}
