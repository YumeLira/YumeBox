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
import com.github.yumelira.yumebox.feature.editor.editor.CodeEditor
import com.github.yumelira.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import com.github.yumelira.yumebox.feature.editor.viewmodel.ConfigEditorViewModel
import com.github.yumelira.yumebox.feature.editor.viewmodel.ConfigType
import com.github.yumelira.yumebox.presentation.component.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun ConfigEditorScreen(
    navigator: DestinationsNavigator,
    configId: String,
    configType: ConfigType = ConfigType.Override,
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
) {
    val viewModel: ConfigEditorViewModel = koinViewModel()
    val session by viewModel.session.collectAsState()
    val editorState =
        rememberConfiguredCodeEditorState(
            initialContent = initialContent,
            language = language,
            readOnly = false,
        )
    val showDiscardDialog = remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(configId) {
        viewModel.loadConfig(
            configId = configId,
            configType = configType,
            initialContent = initialContent,
        )
    }

    LaunchedEffect(session.configId, session.savedContent) {
        if (session.configId == configId) {
            editorState.loadContent(session.draftContent)
        }
    }

    BackHandler {
        if (session.isDirty || editorState.isModified) {
            showDiscardDialog.value = true
        } else {
            navigator.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title =
                    when (configType) {
                        ConfigType.Override -> "编辑覆写配置"
                        ConfigType.Profile -> "编辑订阅配置"
                    },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.fillMaxSize(),
                onTextChange = { content -> viewModel.updateDraft(content) },
            )
        }

        AppDialog(
            show = showDiscardDialog.value,
            title = "放弃修改",
            summary = "当前有未保存的修改，确定要放弃吗？",
            onDismissRequest = { showDiscardDialog.value = false },
        ) {
            DialogButtonRow(
                onCancel = { showDiscardDialog.value = false },
                onConfirm = {
                    showDiscardDialog.value = false
                    viewModel.discardDraft()
                    navigator.navigateUp()
                },
                cancelText = "取消",
                confirmText = "放弃",
            )
        }
    }
}
