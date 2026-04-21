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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.yumelira.yumebox.feature.editor.diagnostic.JsonDiagnosticsProvider
import com.github.yumelira.yumebox.feature.editor.format.CodeFormatter
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import io.github.rosemoe.sora.widget.CodeEditor

class CodeEditorState(
    initialContent: String = "",
    val language: LanguageScope = LanguageScope.Yaml,
    val readOnly: Boolean = false,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
) {

    var editor: CodeEditor? by mutableStateOf(null)
        internal set

    private var canUndoState: Boolean by mutableStateOf(false)
    private var canRedoState: Boolean by mutableStateOf(false)

    var content: String by mutableStateOf(initialContent)
        private set

    var isModified: Boolean by mutableStateOf(false)
        private set

    fun updateContent(newContent: String) {
        if (content != newContent) {
            content = newContent
            isModified = true
            editor?.setText(newContent)
        }
    }

    fun loadContent(newContent: String) {
        if (content != newContent) {
            content = newContent
            editor?.setText(newContent)
        }
        isModified = false
        refreshHistoryState()
    }

    fun syncContentFromEditor() {
        editor?.text?.toString()?.let { editorContent ->
            if (content != editorContent) {
                content = editorContent
                isModified = true
            }
        }
        refreshHistoryState()
    }

    fun resetModified() {
        isModified = false
    }

    fun undo() {
        editor?.undo()
        syncContentFromEditor()
    }

    fun redo() {
        editor?.redo()
        syncContentFromEditor()
    }

    fun canUndo(): Boolean = canUndoState

    fun canRedo(): Boolean = canRedoState

    internal fun refreshHistoryState() {
        canUndoState = editor?.canUndo() == true
        canRedoState = editor?.canRedo() == true
    }

    fun format(): Boolean {
        val formatted = CodeFormatter.format(content, language)
        return if (formatted != null && formatted != content) {
            content = formatted
            editor?.setText(formatted)
            isModified = true
            true
        } else {
            false
        }
    }

    fun validate(): Boolean {
        return CodeFormatter.validate(content, language)
    }

    fun updateDiagnostics() {
        val editor = editor ?: return

        when (language) {
            LanguageScope.Json -> {
                editor.diagnostics = JsonDiagnosticsProvider.analyze(content)
            }
            LanguageScope.Yaml,
            LanguageScope.JavaScript,
            LanguageScope.Text -> {

                editor.diagnostics = null
            }
        }
    }

    fun clearDiagnostics() {
        editor?.diagnostics = null
    }
}
