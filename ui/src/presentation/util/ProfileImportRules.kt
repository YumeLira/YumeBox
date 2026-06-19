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

package com.github.yumelira.yumebox.presentation.util

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import java.io.File

const val PROFILE_IMPORT_TYPE_URL = 0
const val PROFILE_IMPORT_TYPE_FILE = 1
const val PROFILE_IMPORT_TYPE_QR = 2

private val SUBSCRIPTION_URL_PATTERN =
    Regex(pattern = "^https?://\\S+$", options = setOf(RegexOption.IGNORE_CASE))

fun isSubscriptionUrl(value: String): Boolean {
    return SUBSCRIPTION_URL_PATTERN.matches(value.trim())
}

fun readClipboardSubscriptionUrl(context: Context): String? {
    return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip ?: return null
            if (clipData.itemCount <= 0) return null
            clipData.getItemAt(0)?.text?.toString()?.trim()
        }
        .getOrNull()
        ?.takeIf { it.isNotBlank() && isSubscriptionUrl(it) }
}

fun isYamlConfigFileName(fileName: String): Boolean {
    return when (fileName.substringAfterLast(".", "").lowercase()) {
        "yaml",
        "yml" -> true
        else -> false
    }
}

fun profileNameFromConfigFileName(fileName: String, fallback: String): String {
    return fileName.substringBeforeLast(".").ifBlank { fallback }
}

fun readDisplayName(context: Context, uri: Uri, fallback: String): String {
    return context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex < 0 || !cursor.moveToFirst()) {
                fallback
            } else {
                cursor.getString(nameIndex)
            }
        } ?: fallback
}

fun importTypeIndexFor(profileType: Profile.Type): Int {
    return when (profileType) {
        Profile.Type.Url -> PROFILE_IMPORT_TYPE_URL
        Profile.Type.File,
        Profile.Type.External -> PROFILE_IMPORT_TYPE_FILE
    }
}

fun sourceFileName(source: String): String {
    return source.takeIf(String::isNotEmpty)?.let { File(it).name }.orEmpty()
}
