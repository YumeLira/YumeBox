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

package com.github.yumelira.yumebox.common.util

import com.github.yumelira.yumebox.App
import com.github.yumelira.yumebox.substore.SubStorePaths
import java.io.File

object AppUtil {
    fun initFirstOpen() {
        SubStorePaths.ensureStructure()
        createRootJson()
        extractBackendFile()
        extractFrontendDist()
    }

    private fun createRootJson() {
        runCatching {
            val rootJsonFile = File(SubStorePaths.dataDir, "root.json")
            rootJsonFile.parentFile?.mkdirs()
            if (!rootJsonFile.exists()) rootJsonFile.writeText("{}")
        }.onFailure { e -> timber.log.Timber.e(e, "创建root.json失败") }
    }

    private fun extractBackendFile() {
        runCatching {
            val assetManager = App.instance.assets
            SubStorePaths.backendDir.mkdirs()
            assetManager.open("backend/sub-store.bundle.js").use { inputStream ->
                SubStorePaths.backendBundle.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }.onFailure { e -> timber.log.Timber.e(e, "提取后端文件失败") }
    }

    private fun extractFrontendDist() {
        runCatching {
            val assetManager = App.instance.assets
            val cacheDir = App.instance.cacheDir

            val zipPath = File(cacheDir, "substore_frontend.zip")
            assetManager.open("frontend/dist.zip").use { inputStream ->
                zipPath.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val stagingDir = File(cacheDir, "substore_frontend_stage").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            val unzipSuccess = ArchiveUtil.unzipZip(zipPath, stagingDir)
            if (!unzipSuccess) {
                throw IllegalStateException("Sub-Store 前端资源解压失败")
            }

            val extractedRoot = File(stagingDir, "dist").takeIf { it.exists() } ?: stagingDir
            val targetDir = SubStorePaths.frontendDir
            targetDir.parentFile?.mkdirs()
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            extractedRoot.copyRecursively(targetDir, overwrite = true)

            stagingDir.deleteRecursively()
            zipPath.delete()
        }.onFailure { e -> timber.log.Timber.e(e, "提取前端资源失败") }
    }
}
