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

package com.github.yumelira.yumebox.presentation.screen

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.OverrideConfig
import com.github.yumelira.yumebox.data.model.OverrideContentType
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumelira.yumebox.presentation.component.AppDialog
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.OverrideAnimatedFab
import com.github.yumelira.yumebox.presentation.component.OverrideCardActionIconButton
import com.github.yumelira.yumebox.presentation.component.OverrideStatusBadge
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberOverrideFabController
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Badge-plus`
import com.github.yumelira.yumebox.presentation.icon.yume.Copy
import com.github.yumelira.yumebox.presentation.icon.yume.Delete
import com.github.yumelira.yumebox.presentation.icon.yume.Edit
import com.github.yumelira.yumebox.presentation.icon.yume.Share
import com.github.yumelira.yumebox.presentation.icon.yume.ShieldCheck
import com.github.yumelira.yumebox.presentation.icon.yume.ShieldMinus
import com.github.yumelira.yumebox.presentation.theme.Spacing
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideConfigViewModel
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private val overrideConfigItemGap = Spacing().space12

@Composable
fun OverrideListScreen(
    onOpenCodeEditor: (OverrideConfig) -> Unit,
) {
    val viewModel: OverrideConfigViewModel = koinViewModel()
    val userConfigs by viewModel.userConfigs.collectAsState()
    val usageCountMap by viewModel.usageCountMap.collectAsState()
    val pendingRevealConfigId by viewModel.pendingRevealConfigId.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = MiuixScrollBehavior()

    val showCreateDialog = remember { mutableStateOf(false) }
    var createDialogMode by remember { mutableStateOf(OverrideConfigInputMode.CreateNew) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val deleteTargetConfig = remember { mutableStateOf<OverrideConfig?>(null) }
    val exportTargetConfig = remember { mutableStateOf<OverrideConfig?>(null) }

    val listState = rememberLazyListState()
    val createFabController = rememberOverrideFabController()
    val configItems = remember(userConfigs, usageCountMap) {
        userConfigs.map { config ->
            OverrideConfigListItem(
                config = config,
                isInUse = (usageCountMap[config.id] ?: 0) > 0,
            )
        }
    }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.reorderUserConfigs(from.index, to.index)
    }

    val exportConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val targetConfig = exportTargetConfig.value
        if (uri == null || targetConfig == null) {
            exportTargetConfig.value = null
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(targetConfig.content.toByteArray())
                output.flush()
            } ?: error(MLang.Override.Export.Failed.format(targetConfig.name))
        }.onSuccess {
            context.toast(MLang.Override.Export.Success.format(targetConfig.name))
        }.onFailure { error ->
            context.toast(MLang.Override.Export.Failed.format(error.message))
        }

        exportTargetConfig.value = null
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(configItems, pendingRevealConfigId) {
        val targetId = pendingRevealConfigId ?: return@LaunchedEffect
        val targetIndex = configItems.indexOfFirst { it.config.id == targetId }
        if (targetIndex < 0) return@LaunchedEffect
        listState.animateScrollToItem((targetIndex - 1).coerceAtLeast(0))
        viewModel.consumePendingRevealConfig(targetId)
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = createFabController,
                visible = !showCreateDialog.value,
                imageVector = Yume.`Badge-plus`,
                contentDescription = MLang.Override.Action.Create,
                onClick = {
                    createDialogMode = OverrideConfigInputMode.CreateNew
                    showCreateDialog.value = true
                },
            )
        },
        topBar = {
            TopBar(
                title = MLang.Override.Title,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainLikePadding),
            lazyListState = listState,
            onScrollDirectionChanged = createFabController::onScrollDirectionChanged,
        ) {
            item(key = "override-docs-tip", contentType = "override-docs-tip") {
                OverrideDocsTipCard(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://wiki.metacubex.one/".toUri(),
                                ),
                            )
                        }.onFailure { error ->
                            context.toast(error.message ?: "无法打开 Mihomo Docs")
                        }
                    },
                )
            }

            when {
                userConfigs.isEmpty() -> {
                    item(key = "override-empty", contentType = "override-empty") {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = UiDp.dp24, vertical = UiDp.dp80)
                                .wrapContentSize(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
                        ) {
                            CenteredText(
                                firstLine = MLang.Override.Empty.Title,
                                secondLine = MLang.Override.Empty.Hint,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp12)) {
                                Button(
                                    onClick = {
                                        createDialogMode = OverrideConfigInputMode.CreateNew
                                        showCreateDialog.value = true
                                    },
                                ) {
                                    Text(MLang.Override.Action.New)
                                }
                                Button(
                                    onClick = {
                                        createDialogMode = OverrideConfigInputMode.LocalFile
                                        showCreateDialog.value = true
                                    },
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                ) {
                                    Text(
                                        text = MLang.ProfilesPage.Type.LocalFile,
                                        color = colorScheme.background,
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    items(
                        items = configItems,
                        key = { it.config.id },
                        contentType = { "override-config-card" },
                    ) { item ->
                        val config = item.config
                        ReorderableItem(
                            state = reorderState,
                            key = config.id,
                        ) { isDragging ->
                            OverrideConfigCard(
                                config = config,
                                isDragging = isDragging,
                                isInUse = item.isInUse,
                                onCopy = { viewModel.duplicateConfig(config.id) },
                                onExport = {
                                    exportTargetConfig.value = config
                                    exportConfigLauncher.launch("${config.name}.${config.contentType.extension}")
                                },
                                onEdit = { onOpenCodeEditor(config) },
                                onDelete = {
                                    deleteTargetConfig.value = config
                                    showDeleteDialog.value = true
                                },
                            )
                        }
                    }
                }
            }
        }

        CreateConfigDialog(
            show = showCreateDialog,
            initialMode = createDialogMode,
            onConfirmCreate = { name, contentType ->
                viewModel.createConfig(
                    name = name,
                    contentType = contentType,
                )
                showCreateDialog.value = false
            },
            onConfirmImport = { content, sourceName ->
                viewModel.importConfig(content, sourceName).onSuccess {
                    context.toast(MLang.Override.Import.Success.format(sourceName.ifBlank { it.name }, 1))
                    showCreateDialog.value = false
                }.onFailure { error ->
                    context.toast(error.message ?: MLang.Override.Import.ReadError)
                }
            },
            onDismiss = { showCreateDialog.value = false },
        )

        DeleteConfirmDialog(
            show = showDeleteDialog,
            config = deleteTargetConfig.value,
            viewModel = viewModel,
            onConfirm = {
                deleteTargetConfig.value?.id?.let(viewModel::deleteConfig)
                deleteTargetConfig.value = null
                showDeleteDialog.value = false
            },
            onDismiss = {
                deleteTargetConfig.value = null
                showDeleteDialog.value = false
            },
        )
    }
}

@Composable
private fun OverrideDocsTipCard(
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.padding(top = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    append("如果你不知道什么是覆写. 请阅读 ")
                    pushStyle(SpanStyle(color = colorScheme.primary))
                    append("Mihomo Docs")
                    pop()
                },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.OverrideConfigCard(
    config: OverrideConfig,
    isDragging: Boolean,
    isInUse: Boolean,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accentTintColor = colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = overrideConfigItemGap / 2)
            .longPressDraggableHandle()
            .alpha(if (isDragging) 0.92f else 1f),
        insideMargin = PaddingValues(UiDp.dp16),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
                ) {
                    Text(
                        text = config.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = config.contentType.label,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OverrideConfigStateIndicator(inUse = isInUse)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = UiDp.dp12),
                thickness = UiDp.dp0_5,
                color = colorScheme.outline.copy(alpha = 0.5f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp8)) {
                    OverrideCardActionIconButton(
                        imageVector = Yume.Copy,
                        contentDescription = MLang.Override.Card.Copy,
                        onClick = onCopy,
                    )
                    OverrideCardActionIconButton(
                        imageVector = Yume.Share,
                        contentDescription = MLang.Override.Card.Export,
                        onClick = onExport,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    modifier = Modifier.padding(end = UiDp.dp8),
                    backgroundColor = colorScheme.primary.copy(alpha = 0.1f),
                    minHeight = UiDp.dp35,
                    minWidth = UiDp.dp35,
                    onClick = onEdit,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UiDp.dp10),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiDp.dp2),
                    ) {
                        Icon(
                            modifier = Modifier.size(UiDp.dp20),
                            imageVector = Yume.Edit,
                            tint = accentTintColor,
                            contentDescription = MLang.Override.Card.Edit,
                        )
                        Text(
                            modifier = Modifier.padding(end = UiDp.dp3),
                            text = MLang.Override.Card.EditButton,
                            color = accentTintColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }

                IconButton(
                    backgroundColor = colorScheme.secondaryContainer.copy(alpha = 0.78f),
                    minHeight = UiDp.dp35,
                    minWidth = UiDp.dp35,
                    onClick = onDelete,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UiDp.dp10),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.size(UiDp.dp20),
                            imageVector = Yume.Delete,
                            tint = colorScheme.onSurface.copy(alpha = 0.85f),
                            contentDescription = MLang.Override.Card.Delete,
                        )
                        Text(
                            modifier = Modifier.padding(start = UiDp.dp4, end = UiDp.dp3),
                            text = MLang.Override.Card.DeleteButton,
                            color = colorScheme.onSurface.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverrideConfigStateIndicator(inUse: Boolean) {
    val tint = if (inUse) colorScheme.primary else colorScheme.onSurfaceVariantSummary
    OverrideStatusBadge(
        imageVector = if (inUse) Yume.ShieldCheck else Yume.ShieldMinus,
        contentDescription = if (inUse) MLang.Override.Status.InUse else MLang.Override.Status.NotInUse,
        tint = tint,
        backgroundColor = if (inUse) {
            colorScheme.primary.copy(alpha = 0.1f)
        } else {
            colorScheme.secondaryContainer.copy(alpha = 0.78f)
        },
    )
}

@Composable
private fun CreateConfigDialog(
    show: MutableState<Boolean>,
    initialMode: OverrideConfigInputMode,
    onConfirmCreate: (String, OverrideContentType) -> Unit,
    onConfirmImport: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputMode by remember(show.value, initialMode) { mutableStateOf(initialMode) }
    var name by remember(show.value) { mutableStateOf("") }
    var contentType by remember(show.value) { mutableStateOf(OverrideContentType.Yaml) }
    var selectedImportUri by remember(show.value) { mutableStateOf<Uri?>(null) }
    var selectedImportFileName by remember(show.value) { mutableStateOf("") }
    var stableContentHeightPx by remember(show.value) { mutableStateOf(0) }
    val canConfirm = when (inputMode) {
        OverrideConfigInputMode.CreateNew -> name.isNotBlank()
        OverrideConfigInputMode.LocalFile -> selectedImportUri != null && selectedImportFileName.isNotBlank()
    }
    val stableContentHeight = remember(stableContentHeightPx, density) {
        if (stableContentHeightPx <= 0) UiDp.dp0 else with(density) { stableContentHeightPx.toDp() }
    }
    val importConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        selectedImportUri = uri
        selectedImportFileName = uri?.let { selectedUri ->
            context.contentResolver.query(
                selectedUri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && columnIndex >= 0) cursor.getString(columnIndex) else ""
            }.orEmpty().ifBlank {
                selectedUri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.substringAfterLast('\\')
                    .orEmpty()
            }
        }.orEmpty()
    }

    AppActionBottomSheet(
        show = show.value,
        title = MLang.Override.Dialog.Create.Title,
        startAction = { AppBottomSheetCloseAction(onClick = onDismiss) },
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = canConfirm,
                contentDescription = MLang.Override.Action.Create,
                onClick = {
                    if (!canConfirm) return@AppBottomSheetConfirmAction
                    keyboardController?.hide()
                    when (inputMode) {
                        OverrideConfigInputMode.CreateNew -> {
                            onConfirmCreate(name, contentType)
                        }

                        OverrideConfigInputMode.LocalFile -> {
                            val importUri = selectedImportUri ?: return@AppBottomSheetConfirmAction
                            runCatching {
                                context.contentResolver.openInputStream(importUri)
                                    ?.bufferedReader()
                                    ?.use { reader -> reader.readText() }
                                    ?: error(MLang.Override.Import.ReadError)
                            }.onSuccess { content ->
                                onConfirmImport(content, selectedImportFileName)
                            }.onFailure { error ->
                                context.toast(MLang.Override.Import.FileError.format(error.message))
                            }
                        }
                    }
                },
            )
        },
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            OverrideInputModeSelector(
                selectedMode = inputMode,
                onSelectedModeChange = { inputMode = it },
            )
            if (inputMode == OverrideConfigInputMode.CreateNew) {
                OverrideTypeSelector(
                    selectedType = contentType,
                    onSelectedTypeChange = { contentType = it },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = stableContentHeight),
            ) {
                Crossfade(
                    targetState = inputMode,
                    animationSpec = tween(200),
                    label = "OverrideInputModeContent",
                ) { currentInputMode ->
                    when (currentInputMode) {
                        OverrideConfigInputMode.CreateNew -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { stableContentHeightPx = maxOf(stableContentHeightPx, it.height) },
                                verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
                            ) {
                                TextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = MLang.Override.Dialog.Create.Name,
                                )
                            }
                        }

                        OverrideConfigInputMode.LocalFile -> {
                            ImportOverrideFileContent(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { stableContentHeightPx = maxOf(stableContentHeightPx, it.height) },
                                fileName = selectedImportFileName,
                                onPickFile = { importConfigLauncher.launch("*/*") },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverrideInputModeSelector(
    selectedMode: OverrideConfigInputMode,
    onSelectedModeChange: (OverrideConfigInputMode) -> Unit,
) {
    val inputModeOptions = remember {
        OverrideConfigInputMode.entries.toList()
    }
    val selectedModeIndex = inputModeOptions.indexOf(selectedMode).coerceAtLeast(0)

   top.yukonga.miuix.kmp.basic.Card {
        WindowSpinnerPreference(
            title = MLang.ProfilesPage.Type.Title,
            items = inputModeOptions.map { inputMode ->
                SpinnerEntry(title = inputMode.label)
            },
            selectedIndex = selectedModeIndex,
            onSelectedIndexChange = { index ->
                inputModeOptions.getOrNull(index)?.let(onSelectedModeChange)
            },
        )
    }
}

@Composable
private fun OverrideTypeSelector(
    selectedType: OverrideContentType,
    onSelectedTypeChange: (OverrideContentType) -> Unit,
) {
    val contentTypeOptions = remember {
        OverrideContentType.entries.toList()
    }
    val selectedTypeIndex = contentTypeOptions.indexOf(selectedType).coerceAtLeast(0)

    top.yukonga.miuix.kmp.basic.Card {
        WindowDropdownPreference(
            title = "创建类型",
            items = contentTypeOptions.map { it.label },
            selectedIndex = selectedTypeIndex,
            onSelectedIndexChange = { index ->
                contentTypeOptions.getOrNull(index)?.let(onSelectedTypeChange)
            },
        )
    }
}

@Composable
private fun ImportOverrideFileContent(
    modifier: Modifier = Modifier,
    fileName: String,
    onPickFile: () -> Unit,
) {
    Box(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onPickFile,
        ),
    ) {
        TextField(
            value = fileName,
            onValueChange = {},
            label = MLang.ProfilesPage.Input.SelectFile,
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    show: MutableState<Boolean>,
    config: OverrideConfig?,
    viewModel: OverrideConfigViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isInUse by remember { mutableStateOf(false) }

    LaunchedEffect(show.value, config?.id) {
        isInUse = if (show.value && config != null) viewModel.isConfigInUse(config.id) else false
    }

    val summary = when {
        config == null -> ""
        isInUse -> MLang.Override.Dialog.Delete.InUseMessage.format(config.name)
        else -> MLang.Override.Dialog.Delete.Message.format(config.name)
    }

    AppDialog(
        show = show.value,
        title = MLang.Override.Dialog.Delete.Title,
        summary = summary,
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp12)) {
            Button(modifier = Modifier.weight(1f), onClick = onDismiss) {
                Text(MLang.Override.Dialog.Button.Cancel)
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = MLang.Override.Dialog.Button.Delete,
                    color = colorScheme.onPrimary,
                )
            }
        }
    }
}

private data class OverrideConfigListItem(
    val config: OverrideConfig,
    val isInUse: Boolean,
)

private enum class OverrideConfigInputMode {
    CreateNew,
    LocalFile,
}

private val OverrideContentType.label: String
    get() = when (this) {
        OverrideContentType.Yaml -> "YAML"
        OverrideContentType.JavaScript -> "JavaScript"
    }

private val OverrideConfigInputMode.label: String
    get() = when (this) {
        OverrideConfigInputMode.CreateNew -> MLang.Override.Action.New
        OverrideConfigInputMode.LocalFile -> MLang.ProfilesPage.Type.LocalFile
    }
