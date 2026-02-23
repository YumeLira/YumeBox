package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.feature.update.R
import com.github.yumelira.yumebox.update.*
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class UpdateViewState {
    DETAIL,
    DOWNLOADING,
}

@Composable
fun EmasUpdateDialogHost(
    currentVersionName: String,
) {
    val event by EmasUpdateDialogBridge.event.collectAsState()
    var eventSnapshot by remember { mutableStateOf<EmasUpdateDialogEvent?>(null) }

    LaunchedEffect(event) {
        if (event != null) {
            eventSnapshot = event
        }
    }

    eventSnapshot?.let { eventData ->
        EmasUpdateSheet(
            event = eventData,
            currentVersionName = currentVersionName,
        )
    }
}

@Composable
private fun EmasUpdateSheet(
    event: EmasUpdateDialogEvent,
    currentVersionName: String,
) {
    val show = remember(event) { mutableStateOf(true) }
    val latestEvent = rememberUpdatedState(event)
    val scope = rememberCoroutineScope()
    val progress by EmasUpdateDialogBridge.progress.collectAsState()
    var viewState by remember(event) { mutableStateOf(UpdateViewState.DETAIL) }
    var updateConfirmed by remember(event) { mutableStateOf(false) }
    var dismissing by remember(event) { mutableStateOf(false) }

    val dismissSheet: (Boolean) -> Unit = { shouldCancel ->
        if (!dismissing) {
            dismissing = true
            show.value = false
            if (shouldCancel) latestEvent.value.onCancel()
            scope.launch { EmasUpdateDialogBridge.dismiss() }
        }
    }

    val dismissWithCancel = {
        val shouldCancel = !(event.type == EmasUpdateDialogType.UPDATE_AVAILABLE && updateConfirmed)
        dismissSheet(shouldCancel)
    }

    WindowBottomSheet(
        title = event.title,
        show = show,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = dismissWithCancel
    ) {
        AnimatedContent(
            targetState = viewState,
            transitionSpec = {
                if (targetState == UpdateViewState.DOWNLOADING) {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                } else {
                    slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                }
            },
            label = "EmasUpdateSheetTransition"
        ) { state ->
            when (state) {
                UpdateViewState.DETAIL -> {
                    UpdateDetailContent(
                        type = event.type,
                        message = event.message,
                        remoteVersion = event.remoteVersion,
                        currentVersionName = currentVersionName,
                        cancelText = event.cancelText,
                        confirmText = event.confirmText,
                        onCancel = dismissWithCancel,
                        onConfirm = {
                            latestEvent.value.onConfirm()
                            if (event.type == EmasUpdateDialogType.UPDATE_AVAILABLE) {
                                updateConfirmed = true
                                viewState = UpdateViewState.DOWNLOADING
                            } else {
                                dismissSheet(false)
                            }
                        }
                    )
                }

                UpdateViewState.DOWNLOADING -> {
                    DownloadingContent(
                        progress = progress,
                        onClose = {
                            dismissSheet(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateDetailContent(
    type: EmasUpdateDialogType,
    message: String,
    remoteVersion: String,
    currentVersionName: String,
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column {
        if (type == EmasUpdateDialogType.UPDATE_AVAILABLE) {
            UpdateCover()
            Spacer(modifier = Modifier.height(16.dp))
            VersionCompareCard(
                remoteVersion = remoteVersion,
                currentVersionName = currentVersionName,
            )
        } else {
            Text(
                text = message.ifBlank { MLang.Component.Update.Message.Available },
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ActionButtons(
            cancelText = cancelText,
            confirmText = confirmText,
            onCancel = onCancel,
            onConfirm = onConfirm,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun UpdateCover() {
    Image(
        painter = painterResource(id = R.drawable.update),
        contentDescription = MLang.Component.Update.Message.CoverDesc,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun VersionCompareCard(
    remoteVersion: String,
    currentVersionName: String,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = MLang.Component.Update.Message.CurrentVersion,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(text = currentVersionName, style = MiuixTheme.textStyles.body1)
            }
            Text(
                text = "→",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MLang.Component.Update.Message.RemoteVersion,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    text = remoteVersion.ifBlank { "-" },
                    style = MiuixTheme.textStyles.body1
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        ) {
            Text(cancelText)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(confirmText, color = MiuixTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun DownloadingContent(
    progress: EmasDownloadProgress,
    onClose: () -> Unit,
) {
    val progressValue = (progress.progress.coerceIn(0, 100)) / 100f
    val statusText = when (progress.stage) {
        EmasDownloadStage.IDLE -> MLang.Component.Update.Message.Waiting
        EmasDownloadStage.PREPARING -> progress.message.ifBlank { MLang.Component.Update.Message.Preparing }
        EmasDownloadStage.DOWNLOADING -> progress.message.ifBlank { MLang.Component.Update.Message.Downloading }
        EmasDownloadStage.VERIFYING -> progress.message.ifBlank { MLang.Component.Update.Message.Verifying }
        EmasDownloadStage.FINISHED -> progress.message.ifBlank { MLang.Component.Update.Message.Finished }
        EmasDownloadStage.ERROR -> progress.message.ifBlank { MLang.Component.Update.Message.Error }
    }

    Column {
        Text(
            text = MLang.Component.Update.Message.Updating,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.height(12.dp))
        ProgressBar(progressValue = progressValue)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(MLang.Component.Update.Message.Close, color = MiuixTheme.colorScheme.onPrimary)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProgressBar(progressValue: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressValue)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MiuixTheme.colorScheme.primary)
        )
    }
}
