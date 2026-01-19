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

import android.content.Context
import com.github.yumelira.yumebox.substore.SubStorePaths
import java.io.File

object WebViewUtils {

    fun checkLocalResources(): Boolean {
        val distDir = SubStorePaths.frontendDir
        val indexFile = File(distDir, "index.html")
        return distDir.exists() && indexFile.exists()
    }


    fun getLocalFileUrl(path: String): String {
        val file = File(SubStorePaths.frontendDir, path)
        return if (file.exists()) {
            "file://${file.absolutePath}"
        } else {
            ""
        }
    }

    fun getLocalBaseUrl(): String {
        return "file://${SubStorePaths.frontendDir.absolutePath}/"
    }

    fun getLocalBaseUrl(context: Context): String = getLocalBaseUrl()


    fun getPanelUrl(context: Context, panelType: Int): String {
        val panelNames = listOf("zashboard", "metacubexd")
        if (panelType < 0 || panelType >= panelNames.size) {
            return ""
        }

        val panelName = panelNames[panelType]
        val filesDir = context.filesDir.absolutePath
        val panelDir = File("$filesDir/panel/$panelName")

        if (!panelDir.exists()) {
            return ""
        }


        val entryFile = findPanelEntryFile(panelDir)
        return if (entryFile != null) {
            "file://${entryFile.absolutePath}"
        } else {
            ""
        }
    }

    private fun findPanelEntryFile(panelDir: File): File? {

        val entryFiles = listOf("index.html", "main.html", "app.html")

        for (entryFile in entryFiles) {
            val file = File(panelDir, entryFile)
            if (file.exists()) {
                return file
            }
        }

        val distDir = File(panelDir, "dist")
        if (distDir.exists()) {
            for (entryFile in entryFiles) {
                val file = File(distDir, entryFile)
                if (file.exists()) {
                    return file
                }
            }
        }
        return null
    }
}
