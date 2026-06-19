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

package com.github.yumelira.yumebox.feature.editor.theme

import androidx.compose.ui.graphics.toArgb
import com.github.yumelira.yumebox.presentation.theme.AppColors
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object EditorColorSynchronizer {

    fun createColorScheme(isDark: Boolean, appColors: AppColors = AppColors()): EditorColorScheme {
        val editorColors = appColors.editor
        return object : EditorColorScheme(isDark) {
            override fun applyDefault() {
                super.applyDefault()

                setColor(
                    WHOLE_BACKGROUND,
                    if (isDark) editorColors.darkBackground.toArgb()
                    else editorColors.lightBackground.toArgb(),
                )
                setColor(
                    TEXT_NORMAL,
                    if (isDark) editorColors.darkText.toArgb() else editorColors.lightText.toArgb(),
                )
                setColor(
                    LINE_NUMBER,
                    if (isDark) editorColors.darkLineNumber.toArgb()
                    else editorColors.lightLineNumber.toArgb(),
                )
                setColor(
                    LINE_NUMBER_BACKGROUND,
                    if (isDark) editorColors.darkLineNumberBackground.toArgb()
                    else editorColors.lightLineNumberBackground.toArgb(),
                )
                setColor(
                    CURRENT_LINE,
                    if (isDark) editorColors.darkCurrentLine.toArgb()
                    else editorColors.lightCurrentLine.toArgb(),
                )

                setColor(SELECTION_INSERT, editorColors.accent.toArgb())
                setColor(SELECTION_HANDLE, editorColors.accent.toArgb())
                setColor(
                    SELECTED_TEXT_BACKGROUND,
                    if (isDark) editorColors.darkSelectionBackground.toArgb()
                    else editorColors.lightSelectionBackground.toArgb(),
                )

                setColor(
                    TEXT_ACTION_WINDOW_BACKGROUND,
                    if (isDark) editorColors.darkTextActionBackground.toArgb()
                    else editorColors.lightTextActionBackground.toArgb(),
                )
                setColor(
                    TEXT_ACTION_WINDOW_ICON_COLOR,
                    if (isDark) editorColors.darkTextActionIcon.toArgb()
                    else editorColors.lightTextActionIcon.toArgb(),
                )

                setColor(
                    HIGHLIGHTED_DELIMITERS_FOREGROUND,
                    if (isDark) editorColors.delimiterDark.toArgb()
                    else editorColors.delimiterLight.toArgb(),
                )
            }
        }
    }

    fun updateColors(editor: CodeEditor, isDark: Boolean, appColors: AppColors = AppColors()) {
        val editorColors = appColors.editor
        val scheme = editor.colorScheme

        scheme.setColor(EditorColorScheme.SELECTION_INSERT, editorColors.accent.toArgb())
        scheme.setColor(EditorColorScheme.SELECTION_HANDLE, editorColors.accent.toArgb())
        scheme.setColor(
            EditorColorScheme.SELECTED_TEXT_BACKGROUND,
            if (isDark) editorColors.darkSelectionBackground.toArgb()
            else editorColors.lightSelectionBackground.toArgb(),
        )

        scheme.setColor(
            EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND,
            if (isDark) editorColors.darkTextActionBackground.toArgb()
            else editorColors.lightTextActionBackground.toArgb(),
        )
        scheme.setColor(
            EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR,
            if (isDark) editorColors.darkTextActionIcon.toArgb()
            else editorColors.lightTextActionIcon.toArgb(),
        )

        scheme.setColor(
            EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND,
            if (isDark) editorColors.delimiterDark.toArgb()
            else editorColors.delimiterLight.toArgb(),
        )
        scheme.setColor(
            EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND,
            editorColors.delimiterBackground.toArgb(),
        )
    }
}
