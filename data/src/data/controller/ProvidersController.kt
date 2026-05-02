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



package com.github.yumelira.yumebox.data.controller

import android.content.Context
import android.net.Uri
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProvidersController(
    private val context: Context,
    private val queryProvidersAction: suspend () -> List<Provider>,
) {

    suspend fun queryProviders(): Result<List<Provider>> {
        return try {
            Result.success(queryProvidersAction())
        } catch (error: Exception) {
            Result.failure(error)
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
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }

    private suspend fun updateProviderInternal(type: Provider.Type, name: String): Result<Unit> {
        return try {
            Clash.updateProvider(type, name).await()
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun buildTargetFile(provider: Provider): File {
        if (provider.path.isBlank()) {
            throw IllegalStateException("Provider path is empty")
        }
        val targetFile = File(provider.path).canonicalFile
        val importedRoot = context.filesDir.resolve("imported").canonicalFile
        val inImportedProviders = targetFile.toPath().startsWith(importedRoot.toPath()) &&
            targetFile.toRelativeString(importedRoot).replace('\\', '/')
                .matches(Regex("""^[^/]+/providers/(rules|proxies)/.+"""))
        if (!inImportedProviders) {
            throw IllegalStateException("Provider path must live under profile provider directories: ${provider.path}")
        }
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
