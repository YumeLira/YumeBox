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


package com.github.yumelira.yumebox.feature.editor.screen
import com.github.yumelira.yumebox.presentation.theme.UiDp
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.feature.editor.editor.CodeEditor
import com.github.yumelira.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumelira.yumebox.feature.editor.format.CodeFormatter
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import com.github.yumelira.yumebox.presentation.component.SmallTopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.ArrowLeft
import com.github.yumelira.yumebox.presentation.icon.yume.ArrowRight
import com.github.yumelira.yumebox.presentation.icon.yume.ListCollapse
import com.github.yumelira.yumebox.presentation.icon.yume.Save
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*

@Composable
fun ConfigPreviewScreen(
    navigator: DestinationsNavigator,
    title: String = "配置预览",
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
    onSave: (suspend (String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    val formattedContent = remember(initialContent, language) {
        if (language == LanguageScope.Json) {
            CodeFormatter.format(initialContent, language) ?: initialContent
        } else {
            initialContent
        }
    }

    val editorState = rememberConfiguredCodeEditorState(
        initialContent = formattedContent,
        language = language,
        readOnly = false,
    )
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            SmallTopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp12)) {
                        IconButton(
                            onClick = { editorState.undo() },
                            enabled = editorState.canUndo()
                        ) { Icon(Yume.ArrowLeft, null) }
                        IconButton(
                            onClick = { editorState.redo() },
                            enabled = editorState.canRedo()
                        ) { Icon(Yume.ArrowRight, null) }
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = UiDp.dp12),
                        onClick = { editorState.format() }
                    ) {
                        Icon(Yume.ListCollapse, contentDescription = "Format")
                    }
                    IconButton(
                        onClick = {
                            if (isSaving || onSave == null) return@IconButton
                            coroutineScope.launch {
                                isSaving = true
                                runCatching {
                                    onSave(editorState.content)
                                }.onSuccess {
                                    editorState.resetModified()
                                    navigator.navigateUp()
                                }.onFailure {
                                    context.toast(it.message ?: "保存失败")
                                }
                                isSaving = false
                            }
                        },
                        enabled = onSave != null && editorState.isModified && !isSaving
                    ) {
                        Icon(Yume.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
