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

package com.github.yumelira.yumebox.feature.meta.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.feature.editor.editor.CodeEditor
import com.github.yumelira.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import com.github.yumelira.yumebox.feature.meta.presentation.viewmodel.CustomRoutingViewModel
import com.github.yumelira.yumebox.presentation.component.SmallTopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.ArrowLeft
import com.github.yumelira.yumebox.presentation.icon.yume.ArrowRight
import com.github.yumelira.yumebox.presentation.icon.yume.ListCollapse
import com.github.yumelira.yumebox.presentation.icon.yume.Save
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun CustomRoutingScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: CustomRoutingViewModel = koinViewModel()
    val content by viewModel.content.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editorState = rememberConfiguredCodeEditorState(
        initialContent = content,
        language = LanguageScope.Yaml,
        readOnly = false,
    )
    var isSaving by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(content) {
        if (!editorState.isModified && editorState.content != content) {
            editorState.loadContent(content)
        }
    }

    Scaffold(
        topBar = {
            SmallTopBar(
                title = MLang.MetaFeature.CustomRouting.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Row {
                        IconButton(
                            onClick = { editorState.undo() },
                            enabled = editorState.canUndo(),
                        ) {
                            Icon(Yume.ArrowLeft, null)
                        }
                        IconButton(
                            onClick = { editorState.redo() },
                            enabled = editorState.canRedo(),
                        ) {
                            Icon(Yume.ArrowRight, null)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { editorState.format() }) {
                        Icon(Yume.ListCollapse, contentDescription = "Format")
                    }
                    IconButton(
                        onClick = {
                            if (isSaving) return@IconButton
                            scope.launch {
                                isSaving = true
                                val saved = viewModel.saveContent(editorState.content)
                                isSaving = false
                                if (saved) {
                                    editorState.resetModified()
                                    onNavigateBack()
                                } else {
                                    context.toast("保存自定义路由失败")
                                }
                            }
                        },
                        enabled = !isSaving,
                    ) {
                        Icon(Yume.Save, contentDescription = "Save")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.fillMaxSize(),
                onTextChange = {},
            )
        }
    }
}
