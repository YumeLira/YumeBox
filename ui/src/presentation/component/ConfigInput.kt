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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

typealias OpenStringListModifiersEditor =
    (
        title: String,
        placeholder: String,
        replaceValue: List<String>?,
        startValue: List<String>?,
        endValue: List<String>?,
        onReplaceChange: (List<String>?) -> Unit,
        onStartChange: (List<String>?) -> Unit,
        onEndChange: (List<String>?) -> Unit,
    ) -> Unit

@Composable
fun PortInputContent(title: String, value: Int?, onValueChange: (Int?) -> Unit) {
    val showDialog = remember { mutableStateOf(false) }
    val textFieldValueState = remember { mutableStateOf(TextFieldValue()) }

    ArrowPreference(
        title = title,
        summary = if (value != null) "$value" else MLang.Component.Selector.NotModify,
        onClick = {
            val currentText = value?.toString().orEmpty()
            textFieldValueState.value =
                TextFieldValue(text = currentText, selection = TextRange(currentText.length))
            showDialog.value = true
        },
    )

    ConfigTextInputDialog(
        show = showDialog,
        title = title,
        textFieldValue = textFieldValueState,
        label = MLang.Component.ConfigInput.PortLabel,
        onClear = { onValueChange(null) },
        onConfirm = {
            val port = textFieldValueState.value.text.filter(Char::isDigit).toIntOrNull()
            if (port == null || (port in 1..65535)) {
                onValueChange(port)
            }
        },
    )
}

@Composable
fun StringInputContent(
    title: String,
    value: String?,
    placeholder: String = "",
    onValueChange: (String?) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    val textFieldValueState = remember { mutableStateOf(TextFieldValue()) }

    ArrowPreference(
        title = title,
        summary = value?.takeIf { it.isNotEmpty() } ?: MLang.Component.Selector.NotModify,
        onClick = {
            val currentText = value.orEmpty()
            textFieldValueState.value =
                TextFieldValue(text = currentText, selection = TextRange(currentText.length))
            showDialog.value = true
        },
    )

    ConfigTextInputDialog(
        show = showDialog,
        title = title,
        textFieldValue = textFieldValueState,
        label = placeholder,
        onClear = { onValueChange(null) },
        onConfirm = { onValueChange(textFieldValueState.value.text.takeIf { it.isNotEmpty() }) },
    )
}

@Composable
fun StringListInputContent(title: String, value: List<String>?, onClick: () -> Unit) {
    val itemCount = value?.size ?: 0
    val displayValue =
        if (itemCount > 0) {
            MLang.Component.ConfigInput.CountItems.format(itemCount)
        } else {
            MLang.Component.Selector.NotModify
        }

    ArrowPreference(title = title, summary = displayValue, onClick = onClick)
}

@Composable
fun StringMapInputContent(title: String, value: Map<String, String>?, onClick: () -> Unit) {
    val itemCount = value?.size ?: 0
    val displayValue =
        if (itemCount > 0) {
            MLang.Component.ConfigInput.CountItems.format(itemCount)
        } else {
            MLang.Component.Selector.NotModify
        }

    ArrowPreference(title = title, summary = displayValue, onClick = onClick)
}

@Composable
private fun ConfigTextInputDialog(
    show: MutableState<Boolean>,
    title: String,
    textFieldValue: MutableState<TextFieldValue>,
    label: String,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    if (!show.value) return
    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = {
            onDismiss?.invoke()
            show.value = false
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = UiDp.dp8),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            TextField(
                value = textFieldValue.value,
                onValueChange = { updatedTextFieldValue ->
                    textFieldValue.value = updatedTextFieldValue
                },
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
            DialogFilledButtonRow(
                onSecondary = {
                    onClear()
                    show.value = false
                },
                onPrimary = {
                    onConfirm()
                    show.value = false
                },
            )
        }
    }
}

@Composable
fun StringListWithModifiersInput(
    title: String,
    replaceValue: List<String>?,
    startValue: List<String>?,
    endValue: List<String>?,
    placeholder: String = "",
    onReplaceChange: (List<String>?) -> Unit,
    onStartChange: (List<String>?) -> Unit,
    onEndChange: (List<String>?) -> Unit,
    onEditListGroup: OpenStringListModifiersEditor,
) {
    val summary =
        remember(replaceValue, startValue, endValue) {
            buildList {
                    replaceValue?.takeIf { it.isNotEmpty() }?.let { add("Replace ${it.size}") }
                    startValue?.takeIf { it.isNotEmpty() }?.let { add("Prepend ${it.size}") }
                    endValue?.takeIf { it.isNotEmpty() }?.let { add("Append ${it.size}") }
                }
                .joinToString(" · ")
                .ifEmpty { MLang.Component.Selector.NotModify }
        }

    ArrowPreference(
        title = title,
        summary = summary,
        onClick = {
            onEditListGroup(
                title,
                placeholder,
                replaceValue,
                startValue,
                endValue,
                onReplaceChange,
                onStartChange,
                onEndChange,
            )
        },
    )
}

@Composable
fun StringMapWithModifiersInput(
    title: String,
    replaceValue: Map<String, String>?,
    mergeValue: Map<String, String>?,
    keyPlaceholder: String = "",
    valuePlaceholder: String = "",
    onReplaceChange: (Map<String, String>?) -> Unit,
    onMergeChange: (Map<String, String>?) -> Unit,
    onEditMap:
        (
            mode: MapMergeStrategy,
            title: String,
            keyPlaceholder: String,
            valuePlaceholder: String,
            value: Map<String, String>?,
            onValueChange: (Map<String, String>?) -> Unit,
        ) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val summary =
        remember(replaceValue, mergeValue) {
            buildList {
                    replaceValue?.takeIf { it.isNotEmpty() }?.let { add("Replace ${it.size}") }
                    mergeValue?.takeIf { it.isNotEmpty() }?.let { add("Merge ${it.size}") }
                }
                .joinToString(" · ")
                .ifEmpty { MLang.Component.Selector.NotModify }
        }

    Column {
        ArrowPreference(
            title = title,
            summary = summary,
            holdDownState = expanded,
            onClick = { expanded = !expanded },
        )

        AnimatedVisibility(
            visible = expanded,
            enter =
                expandVertically(
                    animationSpec = tween(durationMillis = 260),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(durationMillis = 180)),
            exit =
                shrinkVertically(
                    animationSpec = tween(durationMillis = 220),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(durationMillis = 160)),
            label = "map_modifiers_$title",
        ) {
            Column {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(UiDp.dp10))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                ) {
                    ModifierModeCard(
                        modifier = Modifier.weight(1f),
                        title = "Replace",
                        summary = buildMapModeSummary(replaceValue),
                        helperText = "Replace entire dictionary",
                        onEdit = {
                            onEditMap(
                                MapMergeStrategy.Replace,
                                "$title (Replace)",
                                keyPlaceholder,
                                valuePlaceholder,
                                replaceValue,
                                onReplaceChange,
                            )
                        },
                        onClear =
                            if (!replaceValue.isNullOrEmpty()) {
                                { onReplaceChange(null) }
                            } else {
                                null
                            },
                    )
                    ModifierModeCard(
                        modifier = Modifier.weight(1f),
                        title = "Merge",
                        summary = buildMapModeSummary(mergeValue),
                        helperText = "Overwrites values for matching keys",
                        onEdit = {
                            onEditMap(
                                MapMergeStrategy.Merge,
                                "$title (Merge)",
                                keyPlaceholder,
                                valuePlaceholder,
                                mergeValue,
                                onMergeChange,
                            )
                        },
                        onClear =
                            if (!mergeValue.isNullOrEmpty()) {
                                { onMergeChange(null) }
                            } else {
                                null
                            },
                    )
                }
                Spacer(modifier = Modifier.height(UiDp.dp10))
                Text(
                    text =
                        "Merge mode only modifies specified keys; unmodified keys remain unchanged.",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun ModifierModeCard(
    title: String,
    summary: String,
    helperText: String,
    onEdit: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, insideMargin = PaddingValues(UiDp.dp12)) {
        Text(text = title, color = MiuixTheme.colorScheme.onSurface)
        Text(
            text = summary,
            modifier = Modifier.padding(top = UiDp.dp6),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = helperText,
            modifier = Modifier.padding(top = UiDp.dp6),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(UiDp.dp10))
        Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp8)) {
            Button(modifier = Modifier.weight(1f), onClick = onEdit) { Text("Edit") }
            if (onClear != null) {
                Button(modifier = Modifier.weight(1f), onClick = onClear) { Text("Clear") }
            }
        }
    }
}

private fun buildListModeSummary(value: List<String>?): String {
    return when {
        value.isNullOrEmpty() -> "Not set"
        else -> "Total ${value.size} items · ${value.first()}"
    }
}

private fun buildMapModeSummary(value: Map<String, String>?): String {
    return when {
        value.isNullOrEmpty() -> "Not set"
        else -> "Total ${value.size} items · ${value.entries.first().key}"
    }
}
