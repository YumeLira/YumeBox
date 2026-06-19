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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.settings

import android.content.Context
import android.net.Uri
import com.github.yumelira.yumebox.core.util.acgWallpaperFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies the ACG wallpaper from an external [sourceUri] (typically a `content://` URI from the photo
 * picker) into the app-private files dir, so rendering survives deletion/permission loss of the
 * original source.
 *
 * The copy is written to a temp file then renamed over the stable slot, so a half-written copy never
 * replaces a good one (storage-failure edge case).
 *
 * @return a `file://` URI pointing at the local copy on success, or `null` if the source could not
 *   be read (caller should fall back to the original source URI in that case).
 */
object AcgWallpaperImporter {

    suspend fun importToLocal(context: Context, sourceUri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                    val target = context.acgWallpaperFile()
                    val tmp = File(target.parentFile, "wallpaper.tmp")
                    context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                        requireNotNull(input) { "Unable to open ACG wallpaper source: $sourceUri" }
                        tmp.outputStream().use { input.copyTo(it) }
                    }
                    if (!tmp.renameTo(target)) {
                        target.delete()
                        check(tmp.renameTo(target)) { "Failed to swap ACG wallpaper temp file" }
                    }
                    "file://${target.absolutePath}"
                }
                .getOrNull()
        }
}
