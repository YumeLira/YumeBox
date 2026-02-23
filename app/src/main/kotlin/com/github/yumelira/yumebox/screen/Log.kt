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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.viewmodel.LogViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.LogDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@Destination<RootGraph>
fun LogScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<LogViewModel>()
    val scrollBehavior = MiuixScrollBehavior()

    val isRecording by viewModel.isRecording.collectAsState()
    val logFiles by viewModel.logFiles.collectAsState()

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.refreshLogFiles()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Log.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = { NavigationBackIcon(navigator = navigator) },
                actions = {
                    IconButton(
                        onClick = {
                            if (isRecording) viewModel.stopRecording() else viewModel.startRecording()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) MiuixIcons.Close else MiuixIcons.Play,
                            contentDescription = if (isRecording) MLang.Log.Action.StopRecording else MLang.Log.Action.StartRecording,
                        )
                    }
                    IconButton(
                        onClick = viewModel::deleteAllLogs,
                        modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = MLang.Log.Action.ClearLogs,
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        if (logFiles.isEmpty()) {
            CenteredText(
                firstLine = MLang.Log.Empty.NoLogs,
                secondLine = MLang.Log.Empty.Hint,
            )
            return@Scaffold
        }

        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            topPadding = 20.dp,
        ) {
            item {
                Card {
                    logFiles.forEach { fileInfo ->
                        LogFileRow(
                            fileInfo = fileInfo,
                            onClick = {
                                navigator.navigate(LogDetailScreenDestination(fileName = fileInfo.name))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFileRow(
    fileInfo: LogViewModel.LogFileInfo,
    onClick: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val summary = "${dateFormat.format(Date(fileInfo.createdAt))}  ·  ${formatFileSize(fileInfo.size)}"

    SuperArrow(
        title = fileInfo.name,
        summary = summary,
        onClick = onClick,
        startAction = {
            if (fileInfo.isRecording) {
                Text(
                    text = MLang.Log.Status.Recording,
                    modifier = Modifier.padding(end = 16.dp),
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    )
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024.0))
    }
}
