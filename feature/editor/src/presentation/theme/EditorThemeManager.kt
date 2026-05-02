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



package com.github.yumelira.yumebox.feature.editor.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import com.github.yumelira.yumebox.feature.editor.language.TextMateInitializer
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import top.yukonga.miuix.kmp.theme.MiuixTheme

object EditorThemeManager {

    @Composable
    fun rememberEditorTheme(): EditorThemeState {
        val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f

        return remember(isDark) {
            EditorThemeState(
                isDark = isDark,
                themeName = if (isDark) "dark-high-contrast" else "light"
            )
        }
    }

    fun applyTheme(editor: CodeEditor) {
        try {
            val themeRegistry = ThemeRegistry.getInstance()
            editor.colorScheme = TextMateColorScheme.create(themeRegistry)
        } catch (error: Exception) {

            editor.colorScheme = EditorColorSynchronizer.createColorScheme(false)
        }
    }

    fun updateTheme(editor: CodeEditor, isDark: Boolean) {
        try {
            TextMateInitializer.setTheme(isDark)

            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        } catch (error: Exception) {

            editor.colorScheme = EditorColorSynchronizer.createColorScheme(isDark)
        }
    }
}

data class EditorThemeState(
    val isDark: Boolean,
    val themeName: String
)
