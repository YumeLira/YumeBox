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

package com.github.yumelira.yumebox.screen.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumelira.yumebox.presentation.component.AppConfirmDialog
import com.github.yumelira.yumebox.presentation.component.AppFormDialog
import com.github.yumelira.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumelira.yumebox.presentation.component.EditorAction
import com.github.yumelira.yumebox.presentation.component.EditorEmptyState
import com.github.yumelira.yumebox.presentation.component.EditorListItem
import com.github.yumelira.yumebox.presentation.component.EditorScaffold
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.PreferenceValueItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Badge-plus`
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.hazeSource
import dev.oom_wg.purejoy.mlang.MLang
import java.util.UUID
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

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

private data class TextDraftItem(val id: String, val value: String)

private data class KeyValueDraftItem(val id: String, val key: String, val value: String)

private sealed interface StringListDialogState {
    data object None : StringListDialogState

    data object Add : StringListDialogState

    data class Edit(val itemId: String) : StringListDialogState

    data object Reset : StringListDialogState

    data object AddRule : StringListDialogState
}

private sealed interface KeyValueDialogState {
    data object None : KeyValueDialogState

    data object Add : KeyValueDialogState

    data class Edit(val itemId: String) : KeyValueDialogState

    data object Reset : KeyValueDialogState
}

@Destination<RootGraph>
@Composable
fun StringListEditorScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val topBarHazeState = LocalTopBarHazeState.current
    val items = remember { mutableStateListOf<TextDraftItem>() }
    val title = EditorDataHolder.listEditorTitle
    val placeholder = EditorDataHolder.listEditorPlaceholder
    val isOverrideRuleEditor = title == MLang.Override.Label.RulesReplace
    var dialogState by remember {
        mutableStateOf<StringListDialogState>(StringListDialogState.None)
    }

    LaunchedEffect(title, placeholder) {
        items.clear()
        items.addAll(
            EditorDataHolder.listEditorItems.map { value ->
                TextDraftItem(id = UUID.randomUUID().toString(), value = value)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            EditorDataHolder.listEditorCallback?.invoke(
                items.map(TextDraftItem::value).ifEmpty { null }
            )
            EditorDataHolder.clearListEditor()
        }
    }

    val listState = rememberLazyListState()
    EditorScaffold(
        title = title,
        scrollBehavior = scrollBehavior,
        actions =
            listOf(
                EditorAction(
                    icon = MiuixIcons.Reset,
                    contentDescription = "Reset",
                    onClick = { dialogState = StringListDialogState.Reset },
                ),
                EditorAction(
                    icon = Yume.`Badge-plus`,
                    contentDescription = "Add",
                    onClick = {
                        dialogState =
                            if (isOverrideRuleEditor) {
                                StringListDialogState.AddRule
                            } else {
                                StringListDialogState.Add
                            }
                    },
                ),
            ),
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        val combinedInnerPadding = combinePaddingValues(innerPadding, mainLikePadding)
        if (items.isEmpty()) {
            EditorEmptyState(
                title = MLang.Component.Editor.Empty.Title,
                hint = MLang.Component.Editor.Empty.Hint,
                modifier =
                    Modifier.fillMaxSize()
                        .let { mod ->
                            if (topBarHazeState != null) mod.hazeSource(topBarHazeState) else mod
                        }
                        .padding(combinedInnerPadding),
            )
        } else {
            ScreenLazyColumn(
                lazyListState = listState,
                scrollBehavior = scrollBehavior,
                innerPadding = combinedInnerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Title(MLang.Component.Editor.CountItems.format(items.size)) }
                items(items = items, key = { it.id }) { item ->
                    val index =
                        remember(items, item.id) { items.indexOfFirst { it.id == item.id } + 1 }
                    EditorListItem(
                        index = index,
                        title = item.value,
                        onClick = { dialogState = StringListDialogState.Edit(item.id) },
                        onDelete = { items.removeAll { it.id == item.id } },
                        deleteIcon = MiuixIcons.Delete,
                        deleteContentDescription = "Delete",
                    )
                }
            }
        }
    }

    when (val state = dialogState) {
        StringListDialogState.None -> Unit
        StringListDialogState.Add -> {
            SimpleTextEditorDialog(
                title = MLang.Component.Editor.Dialog.AddTitle,
                placeholder = placeholder,
                initialValue = "",
                onDismiss = { dialogState = StringListDialogState.None },
                onConfirm = { value ->
                    items.add(TextDraftItem(UUID.randomUUID().toString(), value))
                    dialogState = StringListDialogState.None
                },
            )
        }

        is StringListDialogState.Edit -> {
            val currentItem = items.firstOrNull { it.id == state.itemId }
            if (currentItem != null) {
                SimpleTextEditorDialog(
                    title = MLang.Component.Editor.Dialog.EditTitle,
                    placeholder = placeholder,
                    initialValue = currentItem.value,
                    onDismiss = { dialogState = StringListDialogState.None },
                    onConfirm = { value ->
                        val index = items.indexOfFirst { it.id == state.itemId }
                        if (index >= 0) {
                            items[index] = items[index].copy(value = value)
                        }
                        dialogState = StringListDialogState.None
                    },
                )
            } else {
                dialogState = StringListDialogState.None
            }
        }

        StringListDialogState.Reset -> {
            AppConfirmDialog(
                show = true,
                title = MLang.Component.Editor.Dialog.ResetTitle,
                message = MLang.Component.Editor.Dialog.ResetMessage,
                onDismissRequest = { dialogState = StringListDialogState.None },
                onConfirm = {
                    dialogState = StringListDialogState.None
                    EditorDataHolder.listEditorCallback?.invoke(null)
                    EditorDataHolder.clearListEditor()
                    navigator.popBackStack()
                },
            )
        }

        StringListDialogState.AddRule -> {
            RuleEditorDialog(
                title = MLang.Component.Editor.Dialog.AddTitle,
                onDismiss = { dialogState = StringListDialogState.None },
                onConfirm = { value ->
                    items.add(TextDraftItem(UUID.randomUUID().toString(), value))
                    dialogState = StringListDialogState.None
                },
            )
        }
    }
}

@Destination<RootGraph>
@Composable
fun KeyValueEditorScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val topBarHazeState = LocalTopBarHazeState.current
    val items = remember { mutableStateListOf<KeyValueDraftItem>() }
    val title = EditorDataHolder.mapEditorTitle
    val keyPlaceholder = EditorDataHolder.mapEditorKeyPlaceholder
    val valuePlaceholder = EditorDataHolder.mapEditorValuePlaceholder
    var dialogState by remember { mutableStateOf<KeyValueDialogState>(KeyValueDialogState.None) }

    LaunchedEffect(title, keyPlaceholder, valuePlaceholder) {
        items.clear()
        items.addAll(
            EditorDataHolder.mapEditorItems.map { (key, value) ->
                KeyValueDraftItem(id = UUID.randomUUID().toString(), key = key, value = value)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            EditorDataHolder.mapEditorCallback?.invoke(
                items.associate { it.key to it.value }.ifEmpty { null }
            )
            EditorDataHolder.clearMapEditor()
        }
    }

    val listState = rememberLazyListState()
    EditorScaffold(
        title = title,
        scrollBehavior = scrollBehavior,
        actions =
            listOf(
                EditorAction(
                    icon = MiuixIcons.Reset,
                    contentDescription = "Reset",
                    onClick = { dialogState = KeyValueDialogState.Reset },
                ),
                EditorAction(
                    icon = MiuixIcons.AddCircle,
                    contentDescription = "Add",
                    onClick = { dialogState = KeyValueDialogState.Add },
                ),
            ),
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        val combinedInnerPadding = combinePaddingValues(innerPadding, mainLikePadding)
        if (items.isEmpty()) {
            EditorEmptyState(
                title = MLang.Component.Editor.Empty.Title,
                hint = MLang.Component.Editor.Empty.Hint,
                modifier =
                    Modifier.fillMaxSize()
                        .let { mod ->
                            if (topBarHazeState != null) mod.hazeSource(topBarHazeState) else mod
                        }
                        .padding(combinedInnerPadding),
            )
        } else {
            ScreenLazyColumn(
                lazyListState = listState,
                scrollBehavior = scrollBehavior,
                innerPadding = combinedInnerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Title(MLang.Component.Editor.CountItems.format(items.size)) }
                items(items = items, key = { it.id }) { item ->
                    val index =
                        remember(items, item.id) { items.indexOfFirst { it.id == item.id } + 1 }
                    EditorListItem(
                        index = index,
                        title = item.key,
                        summary = item.value,
                        onClick = { dialogState = KeyValueDialogState.Edit(item.id) },
                        onDelete = { items.removeAll { it.id == item.id } },
                        deleteIcon = MiuixIcons.Delete,
                        deleteContentDescription = "Delete",
                    )
                }
            }
        }
    }

    when (val state = dialogState) {
        KeyValueDialogState.None -> Unit
        KeyValueDialogState.Add -> {
            KeyValueFormDialog(
                title = MLang.Component.Editor.Dialog.AddTitle,
                keyPlaceholder = keyPlaceholder,
                valuePlaceholder = valuePlaceholder,
                existingKeys = items.map(KeyValueDraftItem::key).toSet(),
                initialKey = "",
                initialValue = "",
                onDismiss = { dialogState = KeyValueDialogState.None },
                onConfirm = { key, value ->
                    items.add(KeyValueDraftItem(UUID.randomUUID().toString(), key, value))
                    dialogState = KeyValueDialogState.None
                },
            )
        }

        is KeyValueDialogState.Edit -> {
            val currentItem = items.firstOrNull { it.id == state.itemId }
            if (currentItem != null) {
                KeyValueFormDialog(
                    title = MLang.Component.Editor.Dialog.EditTitle,
                    keyPlaceholder = keyPlaceholder,
                    valuePlaceholder = valuePlaceholder,
                    existingKeys = items.map(KeyValueDraftItem::key).toSet(),
                    currentEditingKey = currentItem.key,
                    initialKey = currentItem.key,
                    initialValue = currentItem.value,
                    onDismiss = { dialogState = KeyValueDialogState.None },
                    onConfirm = { key, value ->
                        val index = items.indexOfFirst { it.id == state.itemId }
                        if (index >= 0) {
                            items[index] = items[index].copy(key = key, value = value)
                        }
                        dialogState = KeyValueDialogState.None
                    },
                )
            } else {
                dialogState = KeyValueDialogState.None
            }
        }

        KeyValueDialogState.Reset -> {
            AppConfirmDialog(
                show = true,
                title = MLang.Component.Editor.Dialog.ResetTitle,
                message = MLang.Component.Editor.Dialog.ResetMessage,
                onDismissRequest = { dialogState = KeyValueDialogState.None },
                onConfirm = {
                    dialogState = KeyValueDialogState.None
                    EditorDataHolder.mapEditorCallback?.invoke(null)
                    EditorDataHolder.clearMapEditor()
                    navigator.popBackStack()
                },
            )
        }
    }
}

private val RULE_TYPE_PRESETS =
    listOf(
        "DOMAIN",
        "DOMAIN-SUFFIX",
        "DOMAIN-KEYWORD",
        "DOMAIN-WILDCARD",
        "DOMAIN-REGEX",
        "GEOSITE",
        "IP-CIDR",
        "IP-CIDR6",
        "IP-SUFFIX",
        "IP-ASN",
        "GEOIP",
        "SRC-GEOIP",
        "SRC-IP-ASN",
        "SRC-IP-CIDR",
        "SRC-IP-SUFFIX",
        "DST-PORT",
        "SRC-PORT",
        "IN-PORT",
        "IN-TYPE",
        "IN-USER",
        "IN-NAME",
        "PROCESS-PATH",
        "PROCESS-PATH-WILDCARD",
        "PROCESS-PATH-REGEX",
        "PROCESS-NAME",
        "PROCESS-NAME-WILDCARD",
        "PROCESS-NAME-REGEX",
        "UID",
        "NETWORK",
        "DSCP",
        "RULE-SET",
        "AND",
        "OR",
        "NOT",
        "SUB-RULE",
        "MATCH",
    )

private val RULE_EXTRA_SUPPORTED_TYPES =
    setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP")

private fun supportsRuleExtra(ruleType: String): Boolean =
    ruleType.uppercase() in RULE_EXTRA_SUPPORTED_TYPES

@Composable
private fun SimpleTextEditorDialog(
    title: String,
    placeholder: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var textFieldValue by
        remember(initialValue) {
            mutableStateOf(
                TextFieldValue(text = initialValue, selection = TextRange(initialValue.length))
            )
        }
    AppTextFieldDialog(
        show = true,
        title = title,
        textFieldValue = textFieldValue,
        onTextFieldValueChange = { updatedTextFieldValue ->
            textFieldValue = updatedTextFieldValue
        },
        onDismissRequest = onDismiss,
        onConfirm = {
            val normalizedValue = textFieldValue.text.trim()
            if (normalizedValue.isNotBlank()) {
                onConfirm(normalizedValue)
            }
        },
        label = placeholder,
    )
}

@Composable
private fun KeyValueFormDialog(
    title: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    initialKey: String,
    initialValue: String,
    currentEditingKey: String? = null,
) {
    var keyTextFieldValue by
        remember(initialKey) {
            mutableStateOf(
                TextFieldValue(text = initialKey, selection = TextRange(initialKey.length))
            )
        }
    var valueTextFieldValue by
        remember(initialValue) {
            mutableStateOf(
                TextFieldValue(text = initialValue, selection = TextRange(initialValue.length))
            )
        }
    var error by remember { mutableStateOf<String?>(null) }

    AppFormDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        onConfirm = {
            val normalizedKey = keyTextFieldValue.text.trim()
            val normalizedValue = valueTextFieldValue.text.trim()
            error =
                when {
                    normalizedKey.isBlank() -> MLang.Component.Editor.Error.KeyEmpty
                    normalizedKey != currentEditingKey && normalizedKey in existingKeys ->
                        MLang.Component.Editor.Error.KeyExists
                    else -> null
                }
            if (error == null) {
                onConfirm(normalizedKey, normalizedValue)
            }
        },
        error = error,
    ) {
        TextField(
            value = keyTextFieldValue,
            onValueChange = { updatedTextFieldValue ->
                keyTextFieldValue = updatedTextFieldValue
                error = null
            },
            label = keyPlaceholder,
            modifier = Modifier.fillMaxWidth(),
        )
        TextField(
            value = valueTextFieldValue,
            onValueChange = { updatedTextFieldValue ->
                valueTextFieldValue = updatedTextFieldValue
            },
            label = valuePlaceholder,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RuleEditorDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var ruleType by remember { mutableStateOf("DOMAIN-SUFFIX") }
    var payloadTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var target by remember { mutableStateOf(MLang.Component.Editor.Rule.TargetReject) }
    var useSrc by remember { mutableStateOf(false) }
    var useNoResolve by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val targetItems = remember {
        listOf(
            MLang.Component.Editor.Rule.TargetReject,
            MLang.Component.Editor.Rule.TargetDirect,
            MLang.Component.Editor.Rule.TargetMatch,
        )
    }
    val selectedRuleTypeIndex =
        remember(ruleType) {
            RULE_TYPE_PRESETS.indexOfFirst { it.equals(ruleType, ignoreCase = true) }
                .coerceAtLeast(0)
        }
    val selectedTargetIndex = remember(target) { targetItems.indexOf(target).coerceAtLeast(0) }

    AppFormDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        onConfirm = {
            val normalizedType = ruleType.trim().uppercase()
            val normalizedPayload = payloadTextFieldValue.text.trim()

            if (target != MLang.Component.Editor.Rule.TargetMatch && normalizedPayload.isBlank()) {
                error = MLang.Component.Editor.Rule.ErrorContentRequired
                return@AppFormDialog
            }

            val result =
                if (target == MLang.Component.Editor.Rule.TargetMatch) {
                    "MATCH"
                } else {
                    buildList {
                            add(normalizedType)
                            add(normalizedPayload)
                            add(target)
                            if (supportsRuleExtra(normalizedType)) {
                                if (useSrc) add("src")
                                if (useNoResolve) add("no-resolve")
                            }
                        }
                        .joinToString(",")
                }
            onConfirm(result)
        },
        error = error,
    ) {
        WindowDropdownPreference(
            title = MLang.Component.Editor.Rule.Type,
            items = RULE_TYPE_PRESETS,
            selectedIndex = selectedRuleTypeIndex,
            onSelectedIndexChange = { index ->
                ruleType = RULE_TYPE_PRESETS.getOrElse(index) { ruleType }
                error = null
            },
        )
        WindowDropdownPreference(
            title = MLang.Component.Editor.Rule.Target,
            items = targetItems,
            selectedIndex = selectedTargetIndex,
            onSelectedIndexChange = { index ->
                target = targetItems.getOrElse(index) { target }
                error = null
            },
        )
        TextField(
            value = payloadTextFieldValue,
            onValueChange = { updatedTextFieldValue ->
                payloadTextFieldValue = updatedTextFieldValue
                error = null
            },
            label = MLang.Component.Editor.Rule.Content,
            modifier = Modifier.fillMaxWidth(),
        )
        if (supportsRuleExtra(ruleType)) {
            PreferenceValueItem(
                title = MLang.Component.Editor.Rule.Src,
                summary = null,
                onClick = { useSrc = !useSrc },
                endActions = {
                    Checkbox(
                        state = if (useSrc) ToggleableState.On else ToggleableState.Off,
                        onClick = { useSrc = !useSrc },
                    )
                },
            )
            PreferenceValueItem(
                title = MLang.Component.Editor.Rule.NoResolve,
                summary = null,
                onClick = { useNoResolve = !useNoResolve },
                endActions = {
                    Switch(
                        checked = useNoResolve,
                        onCheckedChange = { checked -> useNoResolve = checked },
                    )
                },
            )
        }
    }
}
