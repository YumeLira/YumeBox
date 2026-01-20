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

package com.github.yumelira.yumebox.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.theme.MiuixTheme

object EditorDataHolder {
    var listEditorTitle: String = ""
    var listEditorPlaceholder: String = ""
    var listEditorItems: MutableList<String> = mutableListOf()
    var listEditorCallback: ((List<String>?) -> Unit)? = null

    var mapEditorTitle: String = ""
    var mapEditorKeyPlaceholder: String = ""
    var mapEditorValuePlaceholder: String = ""
    var mapEditorItems: MutableMap<String, String> = mutableMapOf()
    var mapEditorCallback: ((Map<String, String>?) -> Unit)? = null

    fun setupListEditor(
        title: String,
        placeholder: String,
        items: List<String>?,
        callback: (List<String>?) -> Unit,
    ) {
        listEditorTitle = title
        listEditorPlaceholder = placeholder
        listEditorItems = items?.toMutableList() ?: mutableListOf()
        listEditorCallback = callback
    }

    fun setupMapEditor(
        title: String,
        keyPlaceholder: String,
        valuePlaceholder: String,
        items: Map<String, String>?,
        callback: (Map<String, String>?) -> Unit,
    ) {
        mapEditorTitle = title
        mapEditorKeyPlaceholder = keyPlaceholder
        mapEditorValuePlaceholder = valuePlaceholder
        mapEditorItems = items?.toMutableMap() ?: mutableMapOf()
        mapEditorCallback = callback
    }

    fun clearListEditor() {
        listEditorTitle = ""
        listEditorPlaceholder = ""
        listEditorItems = mutableListOf()
        listEditorCallback = null
    }

    fun clearMapEditor() {
        mapEditorTitle = ""
        mapEditorKeyPlaceholder = ""
        mapEditorValuePlaceholder = ""
        mapEditorItems = mutableMapOf()
        mapEditorCallback = null
    }
}

@SuppressLint("MutableCollectionMutableState")
@Destination<RootGraph>
@Composable
fun StringListEditorScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var editableItems by remember { mutableStateOf(EditorDataHolder.listEditorItems.toMutableList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    val title = EditorDataHolder.listEditorTitle
    val placeholder = EditorDataHolder.listEditorPlaceholder

    DisposableEffect(Unit) {
        onDispose {
            EditorDataHolder.listEditorCallback?.invoke(
                editableItems.takeIf { it.isNotEmpty() })
            EditorDataHolder.clearListEditor()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = { NavigationBackIcon(navigator) },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Reset,
                            contentDescription = MLang.Component.Editor.Action.Reset,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showAddDialog = true }, modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.AddCircle,
                            contentDescription = MLang.Component.Editor.Action.Add,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (editableItems.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SmallTitle(MLang.Component.Editor.CountItems.format(editableItems.size))
                }

                itemsIndexed(editableItems) { index, item ->
                    ListItem(
                        index = index + 1,
                        text = item,
                        onClick = {
                            editingIndex = index
                            showEditDialog = true
                        },
                        onDelete = {
                            editableItems = editableItems.toMutableList().also {
                                it.removeAt(index)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        InputDialog(
            title = MLang.Component.Editor.Dialog.AddTitle,
            placeholder = placeholder,
            onConfirm = { value ->
                editableItems = editableItems.toMutableList().also { it.add(value) }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showEditDialog && editingIndex >= 0 && editingIndex < editableItems.size) {
        InputDialog(
            title = MLang.Component.Editor.Dialog.EditTitle,
            initialValue = editableItems[editingIndex],
            placeholder = placeholder,
            onConfirm = { value ->
                editableItems = editableItems.toMutableList().also {
                    it[editingIndex] = value
                }
                editingIndex = -1
                showEditDialog = false
            },
            onDismiss = {
                editingIndex = -1
                showEditDialog = false
            },
        )
    }

    if (showResetDialog) {
        ConfirmDialogSimple(
            title = MLang.Component.Editor.Dialog.ResetTitle,
            message = MLang.Component.Editor.Dialog.ResetMessage,
            onConfirm = {
                showResetDialog = false
                EditorDataHolder.listEditorCallback?.invoke(null)
                EditorDataHolder.clearListEditor()
                navigator.popBackStack()
            },
            onDismiss = { showResetDialog = false },
        )
    }
}

@Destination<RootGraph>
@Composable
fun KeyValueEditorScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var editableItems by remember { mutableStateOf(EditorDataHolder.mapEditorItems.toMutableMap()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<String?>(null) }

    val title = EditorDataHolder.mapEditorTitle
    val keyPlaceholder = EditorDataHolder.mapEditorKeyPlaceholder
    val valuePlaceholder = EditorDataHolder.mapEditorValuePlaceholder

    DisposableEffect(Unit) {
        onDispose {
            EditorDataHolder.mapEditorCallback?.invoke(
                editableItems.takeIf { it.isNotEmpty() })
            EditorDataHolder.clearMapEditor()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = { NavigationBackIcon(navigator) },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Reset,
                            contentDescription = MLang.Component.Editor.Action.Reset,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showAddDialog = true }, modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.AddCircle,
                            contentDescription = MLang.Component.Editor.Action.Add,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (editableItems.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SmallTitle(MLang.Component.Editor.CountItems.format(editableItems.size))
                }

                val itemsList = editableItems.toList()
                itemsIndexed(itemsList) { index, (key, value) ->
                    KeyValueItem(
                        index = index + 1,
                        key = key,
                        value = value,
                        onClick = {
                            editingKey = key
                            showEditDialog = true
                        },
                        onDelete = {
                            editableItems = editableItems.toMutableMap().also {
                                it.remove(key)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        KeyValueInputDialog(
            title = MLang.Component.Editor.Dialog.AddTitle,
            keyPlaceholder = keyPlaceholder,
            valuePlaceholder = valuePlaceholder,
            existingKeys = editableItems.keys,
            onConfirm = { key, value ->
                editableItems = editableItems.toMutableMap().also {
                    it[key] = value
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showEditDialog && editingKey != null) {
        val currentKey = editingKey!!
        KeyValueInputDialog(
            title = MLang.Component.Editor.Dialog.EditTitle,
            initialKey = currentKey,
            initialValue = editableItems[currentKey] ?: "",
            keyPlaceholder = keyPlaceholder,
            valuePlaceholder = valuePlaceholder,
            existingKeys = editableItems.keys,
            currentEditingKey = currentKey,
            onConfirm = { key, value ->
                editableItems = editableItems.toMutableMap().also {
                    it.remove(currentKey)
                    it[key] = value
                }
                editingKey = null
                showEditDialog = false
            },
            onDismiss = {
                editingKey = null
                showEditDialog = false
            },
        )
    }

    if (showResetDialog) {
        ConfirmDialogSimple(
            title = MLang.Component.Editor.Dialog.ResetTitle,
            message = MLang.Component.Editor.Dialog.ResetMessage,
            onConfirm = {
                showResetDialog = false
                EditorDataHolder.mapEditorCallback?.invoke(null)
                EditorDataHolder.clearMapEditor()
                navigator.popBackStack()
            },
            onDismiss = { showResetDialog = false },
        )
    }
}


@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    CenteredText(
        firstLine = MLang.Component.Editor.Empty.Title,
        secondLine = MLang.Component.Editor.Empty.Hint,
        modifier = modifier
    )
}

@Composable
private fun ListItem(
    index: Int,
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$index.",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.width(40.dp),
            )
            Text(
                text = text,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = MLang.Component.Editor.Action.Delete,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun KeyValueItem(
    index: Int,
    key: String,
    value: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$index.",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.width(40.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                Text(text = key, style = MiuixTheme.textStyles.body1)
                Text(
                    text = value,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = MLang.Component.Editor.Action.Delete,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    initialValue: String = "",
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }

    WindowBottomSheet(
        show = remember { mutableStateOf(true) },
        title = title,
        insideMargin = DpSize(24.dp, 16.dp),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = value,
                onValueChange = { value = it },
                label = placeholder,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = {
                    val trimmedValue = value.trim()
                    if (trimmedValue.isNotBlank()) {
                        onConfirm(trimmedValue)
                    }
                },
            )
        }
    }
}

@Composable
private fun KeyValueInputDialog(
    title: String,
    initialKey: String = "",
    initialValue: String = "",
    keyPlaceholder: String,
    valuePlaceholder: String,
    existingKeys: Set<String>,
    currentEditingKey: String? = null,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf(initialKey) }
    var value by remember { mutableStateOf(initialValue) }
    var keyError by remember { mutableStateOf<String?>(null) }

    WindowBottomSheet(
        show = remember { mutableStateOf(true) },
        title = title,
        insideMargin = DpSize(24.dp, 16.dp),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = key,
                onValueChange = {
                    key = it
                    keyError = null
                },
                label = keyPlaceholder,
                modifier = Modifier.fillMaxWidth(),
            )
            if (keyError != null) {
                Text(
                    text = keyError!!,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = value,
                onValueChange = { value = it },
                label = valuePlaceholder,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = {
                    val trimmedKey = key.trim()
                    val trimmedValue = value.trim()

                    when {
                        trimmedKey.isBlank() -> keyError = MLang.Component.Editor.Error.KeyEmpty
                        trimmedKey != currentEditingKey && existingKeys.contains(trimmedKey) -> keyError =
                            MLang.Component.Editor.Error.KeyExists

                        else -> onConfirm(trimmedKey, trimmedValue)
                    }
                },
            )
        }
    }
}
