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

package com.github.yumelira.yumebox.feature.editor.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.feature.editor.editor.CodeEditor
import com.github.yumelira.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Atom
import com.github.yumelira.yumebox.presentation.icon.yume.Save
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun FullscreenEditorScreen(
    navigator: DestinationsNavigator,
    title: String = "编辑配置",
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
    onSave: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val showDiscardDialog = remember { mutableStateOf(false) }

    val editorState =
        rememberConfiguredCodeEditorState(
            initialContent = initialContent,
            language = language,
            readOnly = false,
        )
    val scrollBehavior = MiuixScrollBehavior()

    fun handleBack() {
        if (editorState.isModified) {
            showDiscardDialog.value = true
        } else {
            navigator.navigateUp()
        }
    }

    BackHandler { handleBack() }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = UiDp.dp12),
                        onClick = {
                            if (editorState.format()) {
                                context.toast("格式化成功")
                            } else {
                                context.toast("格式化失败或无需格式化")
                            }
                        },
                    ) {
                        Icon(imageVector = Yume.Atom, contentDescription = "Format")
                    }

                    IconButton(
                        onClick = {
                            if (editorState.validate()) {
                                onSave(editorState.content)
                                editorState.resetModified()
                                navigator.navigateUp()
                            } else {
                                context.toast("语法错误，请检查内容")
                            }
                        }
                    ) {
                        Icon(imageVector = Yume.Save, contentDescription = "Save")
                    }
                },
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.fillMaxSize(),
                onTextChange = { newContent -> editorState.syncContentFromEditor() },
            )
        }
    }

    AppDialog(
        show = showDiscardDialog.value,
        title = "未保存的修改",
        summary = "当前有未保存的修改，确定要放弃吗？",
        onDismissRequest = { showDiscardDialog.value = false },
    ) {
        DialogButtonRow(
            onCancel = { showDiscardDialog.value = false },
            onConfirm = {
                showDiscardDialog.value = false
                navigator.navigateUp()
            },
            cancelText = "取消",
            confirmText = "放弃",
        )
    }
}
