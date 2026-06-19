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

package com.github.yumelira.yumebox.screen.log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.store.LogStore
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Play
import com.github.yumelira.yumebox.presentation.icon.yume.PowerOff
import com.github.yumelira.yumebox.presentation.icon.yume.Share
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun LogScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<LogViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    val isRecording by viewModel.isRecording.collectAsState()
    val logEntries by viewModel.tempLogEntries.collectAsState()

    var fabHidden by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val saveFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val success = viewModel.saveTempLog(uri)
                if (!success) {
                    launch(Dispatchers.Main) { context.toast(MLang.Util.Error.UnknownError) }
                }
            }
        }

    LaunchedEffect(listState) {
        var previousFirstVisibleItemIndex = 0
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleItemIndex ->
                fabHidden = firstVisibleItemIndex > previousFirstVisibleItemIndex
                previousFirstVisibleItemIndex = firstVisibleItemIndex
            }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            PollingTimers.ticks(PollingTimerSpecs.LogScreenRefresh).collect {
                viewModel.refreshTempLogEntries()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Log.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (logEntries.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                saveFileLauncher.launch("log_${System.currentTimeMillis()}.txt")
                            }
                        ) {
                            Icon(imageVector = Yume.Share, contentDescription = "Save")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !fabHidden,
                enter =
                    scaleIn(
                        animationSpec =
                            tween(
                                durationMillis = AnimationSpecs.Proxy.FabDuration,
                                easing = AnimationSpecs.EmphasizedDecelerate,
                            ),
                        initialScale = AnimationSpecs.Proxy.VisibilityInitialScale,
                    ) +
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = AnimationSpecs.Proxy.FabFadeDuration,
                                    easing = AnimationSpecs.EmphasizedDecelerate,
                                )
                        ),
                exit =
                    scaleOut(
                        animationSpec =
                            tween(
                                durationMillis = AnimationSpecs.Proxy.FabDuration,
                                easing = AnimationSpecs.EmphasizedDecelerate,
                            ),
                        targetScale = AnimationSpecs.Proxy.VisibilityTargetScale,
                    ) +
                        fadeOut(
                            animationSpec =
                                tween(
                                    durationMillis = AnimationSpecs.Proxy.FabFadeDuration,
                                    easing = AnimationSpecs.EmphasizedDecelerate,
                                )
                        ),
                label = "log_record_fab_visibility",
            ) {
                FloatingActionButton(
                    modifier =
                        Modifier.padding(
                            end = spacing.space20,
                            bottom = componentSizes.floatingActionButtonBottomInset,
                        ),
                    onClick = {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (isRecording) Yume.PowerOff else Yume.Play,
                        contentDescription =
                            if (isRecording) "Stop recording" else "Start recording",
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { innerPadding ->
        if (logEntries.isEmpty() && isRecording.not()) {
            CenteredText(
                firstLine = MLang.Log.Empty.NoLogs,
                secondLine = MLang.Log.Empty.StartRecordingHint,
            )
            return@Scaffold
        }

        if (logEntries.isEmpty() && isRecording) {
            CenteredText(
                firstLine = MLang.Log.Detail.WaitingLog,
                secondLine = MLang.Log.Detail.WillShowWhenGenerated,
            )
            return@Scaffold
        }

        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
            lazyListState = listState,
        ) {
            val reversed = logEntries.asReversed()
            itemsIndexed(
                items = reversed,
                key = { index, item -> "${item.time}_${item.level}_${item.message}_$index" },
            ) { _, entry ->
                LogEntryRow(entry = entry)
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogStore.LogEntry) {
    val spacing = AppTheme.spacing
    val semanticColors = AppTheme.colors

    val levelColor =
        when (entry.level) {
            LogMessage.Level.Debug -> semanticColors.logLevel.debug
            LogMessage.Level.Info -> MiuixTheme.colorScheme.primary
            LogMessage.Level.Warning -> semanticColors.logLevel.warning
            LogMessage.Level.Error -> semanticColors.logLevel.error
            LogMessage.Level.Silent -> semanticColors.logLevel.neutral
            LogMessage.Level.Unknown -> semanticColors.logLevel.neutral
        }

    Card(modifier = Modifier.padding(vertical = spacing.space4)) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = spacing.space12, vertical = spacing.space10)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space8),
            ) {
                Text(
                    text = entry.time,
                    style =
                        MiuixTheme.textStyles.body2.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = entry.level.name.uppercase().take(1),
                    style =
                        MiuixTheme.textStyles.body2.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    color = levelColor,
                )
            }
            Spacer(modifier = Modifier.size(spacing.space6))
            Text(
                text = entry.message,
                style =
                    MiuixTheme.textStyles.body2.copy(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
