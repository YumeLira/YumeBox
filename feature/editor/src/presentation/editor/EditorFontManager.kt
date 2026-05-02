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



package com.github.yumelira.yumebox.feature.editor.editor

import android.content.Context
import android.graphics.Typeface
import timber.log.Timber

object EditorFontManager {

    private const val FONT_PATH = "fonts/JetBrainsMono-Regular.ttf"
    private var cachedFont: Typeface? = null

    fun getEditorTypeface(context: Context): Typeface {
        return cachedFont ?: try {
            Typeface.createFromAsset(context.assets, FONT_PATH).also {
                cachedFont = it
                Timber.d("JetBrainsMono font loaded successfully")
            }
        } catch (error: Exception) {
            Timber.w(error, "Failed to load JetBrainsMono font, falling back to MONOSPACE")
            Typeface.MONOSPACE
        }
    }

    fun clearCache() {
        cachedFont = null
    }
}
