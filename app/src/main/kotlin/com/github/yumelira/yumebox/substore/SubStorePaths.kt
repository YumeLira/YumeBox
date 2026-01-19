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

package com.github.yumelira.yumebox.substore

import com.github.yumelira.yumebox.App
import java.io.File

object SubStorePaths {

    private const val BASE_DIR = "Sub-Store"
    private const val FRONTEND_DIR = "frontend"
    private const val BACKEND_DIR = "backend"
    private const val DATA_DIR = "data"
    private const val BACKEND_BUNDLE = "sub-store.bundle.js"

    private val baseDir: File
        get() = File(App.instance.filesDir, BASE_DIR)

    val frontendDir: File
        get() = File(baseDir, FRONTEND_DIR)

    val backendDir: File
        get() = File(baseDir, BACKEND_DIR)

    val dataDir: File
        get() = File(baseDir, DATA_DIR)

    val backendBundle: File
        get() = File(backendDir, BACKEND_BUNDLE)

    val workingDir: File
        get() = baseDir

    fun ensureStructure() {
        listOf(baseDir, frontendDir, backendDir, dataDir).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    fun isBackendReady(): Boolean = backendBundle.exists()

    fun isFrontendReady(): Boolean {
        return frontendDir.exists() &&
                frontendDir.isDirectory &&
                (frontendDir.listFiles()?.isNotEmpty() == true)
    }

    fun isResourcesReady(): Boolean = isBackendReady() && isFrontendReady()
}
