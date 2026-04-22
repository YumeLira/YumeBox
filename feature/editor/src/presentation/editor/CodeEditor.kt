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

import android.util.TypedValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.feature.editor.language.TextMateInitializer
import com.github.yumelira.yumebox.feature.editor.theme.EditorThemeManager
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import timber.log.Timber

@Composable
fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier = Modifier,
    onTextChange: ((String) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val editorRef = remember { mutableStateOf<CodeEditor?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Timber.d("CodeEditor: onPause")
                }
                Lifecycle.Event.ON_RESUME -> {
                    Timber.d("CodeEditor: onResume")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Timber.d("CodeEditor: Disposing...")
            lifecycleOwner.lifecycle.removeObserver(observer)
            editorRef.value?.let { editor ->
                try {
                    editor.release()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to release editor")
                }
            }
            state.editor = null
            editorRef.value = null
        }
    }

    val editorThemeState = EditorThemeManager.rememberEditorTheme()
    LaunchedEffect(editorThemeState.isDark) {
        editorRef.value?.let { editor ->
            EditorThemeManager.updateTheme(editor, editorThemeState.isDark)
        }
    }

    LaunchedEffect(state.content) {
        val editor = editorRef.value
        if (editor != null && editor.text.toString() != state.content) {
            editor.setText(state.content)
        }
    }

    AndroidView(
        factory = { ctx ->
            createCodeEditor(ctx, state, editorThemeState.isDark, onTextChange).also { editor ->
                state.editor = editor
                state.refreshHistoryState()
                editorRef.value = editor
            }
        },
        modifier = modifier,
        onRelease = { editor ->
            try {
                editor.release()
            } catch (e: Exception) {
                Timber.e(e, "Failed to release editor in onRelease")
            }
        }
    )
}

private fun createCodeEditor(
    context: android.content.Context,
    state: CodeEditorState,
    isDark: Boolean,
    onTextChange: ((String) -> Unit)?
): CodeEditor {

    TextMateInitializer.initialize(context)

    return CodeEditor(context).apply {

        isEditable = !state.readOnly

        val font = EditorFontManager.getEditorTypeface(context)
        typefaceText = font
        typefaceLineNumber = font

        setLineNumberMarginLeft(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                context.resources.displayMetrics,
            ),
        )

        setScrollBarEnabled(false)

        nonPrintablePaintingFlags = CodeEditor.FLAG_DRAW_LINE_SEPARATOR

        TextMateInitializer.setLanguage(this, state.language)

        EditorThemeManager.applyTheme(this)
        EditorThemeManager.updateTheme(this, isDark)

        setText(state.content)

        subscribeAlways<ContentChangeEvent> {

            state.syncContentFromEditor()

            state.updateDiagnostics()

            onTextChange?.invoke(state.content)
        }

        state.updateDiagnostics()

        Timber.d("CodeEditor created: language=${state.language}, readOnly=${state.readOnly}")
    }
}
