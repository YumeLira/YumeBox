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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.viewmodel.LogViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun LogDetailScreen(
    navigator: DestinationsNavigator,
    fileName: String,
) {
    val viewModel = koinViewModel<LogViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isRecording by viewModel.isRecording.collectAsState()

    var logEntries by remember(fileName) { mutableStateOf<List<LogViewModel.LogEntry>>(emptyList()) }
    var isLoading by remember(fileName) { mutableStateOf(true) }
    var lastFileSize by remember(fileName) { mutableStateOf<Long?>(null) }

    val isCurrentFileRecording = isRecording && viewModel.isCurrentRecordingFile(fileName)

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val success = viewModel.exportLogFile(fileName, uri)
            if (!success) {
                launch(Dispatchers.Main) {
                    context.toast(MLang.Util.Error.UnknownError)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLogFiles()
    }

    LaunchedEffect(fileName, isCurrentFileRecording) {
        isLoading = true
        logEntries = viewModel.readLogContent(fileName).asReversed()
        lastFileSize = viewModel.getLogFileSize(fileName)
        isLoading = false

        if (isCurrentFileRecording) {
            while (isActive) {
                delay(1000)
                val latestFileSize = viewModel.getLogFileSize(fileName)
                if (latestFileSize != lastFileSize) {
                    logEntries = viewModel.readLogContent(fileName).asReversed()
                    lastFileSize = latestFileSize
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isCurrentFileRecording) MLang.Log.Detail.RealTimeLog else fileName,
                scrollBehavior = scrollBehavior,
                navigationIcon = { NavigationBackIcon(navigator = navigator) },
                actions = {
                    if (isCurrentFileRecording) {
                        IconButton(
                            onClick = { viewModel.stopRecording() },
                            modifier = Modifier.padding(end = 24.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Close,
                                contentDescription = MLang.Log.Action.Pause,
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { saveFileLauncher.launch(fileName) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Ok,
                                contentDescription = MLang.Log.Action.Save,
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteLogFile(fileName)
                                navigator.navigateUp()
                            },
                            modifier = Modifier.padding(end = 24.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                contentDescription = MLang.Log.Action.Delete,
                            )
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> {
                CenteredText(
                    firstLine = MLang.Log.Detail.Loading,
                    secondLine = "",
                )
            }

            logEntries.isEmpty() -> {
                CenteredText(
                    firstLine = if (isCurrentFileRecording) MLang.Log.Detail.WaitingLog else MLang.Log.Detail.LogEmpty,
                    secondLine = if (isCurrentFileRecording) {
                        MLang.Log.Detail.WillShowWhenGenerated
                    } else {
                        MLang.Log.Detail.NoLogContent
                    },
                )
            }

            else -> {
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    topPadding = 20.dp,
                ) {
                    logEntries.forEach { entry ->
                        item {
                            LogEntryRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogViewModel.LogEntry) {
    val levelColor = when (entry.level) {
        LogMessage.Level.Debug -> Color(0xFF9E9E9E)
        LogMessage.Level.Info -> MiuixTheme.colorScheme.primary
        LogMessage.Level.Warning -> Color(0xFFFF9800)
        LogMessage.Level.Error -> Color(0xFFF44336)
        LogMessage.Level.Silent -> Color(0xFF9E9E9E)
        LogMessage.Level.Unknown -> Color(0xFF9E9E9E)
    }

    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = entry.time,
                    style = MiuixTheme.textStyles.body2.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = entry.level.name.uppercase().take(1),
                    style = MiuixTheme.textStyles.body2.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = levelColor,
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = entry.message,
                style = MiuixTheme.textStyles.body2.copy(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
