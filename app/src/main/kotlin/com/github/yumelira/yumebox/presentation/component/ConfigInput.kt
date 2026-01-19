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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.screen.EditorDataHolder
import com.ramcosta.composedestinations.generated.destinations.KeyValueEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StringListEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperBottomSheet


@Composable
fun PortInput(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value?.toString() ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    SuperArrow(
        title = title,
        summary = if (value != null) "$value" else "不修改",
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SuperBottomSheet(
            show = remember(showDialog) { mutableStateOf(true) },
            title = title,
            onDismissRequest = { showDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it.filter { c -> c.isDigit() } },
                    label = "端口号 (留空表示不修改)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogButtonRow(
                    onCancel = {
                        onValueChange(null)
                        showDialog = false
                    },
                    onConfirm = {
                        val port = textValue.toIntOrNull()
                        if (port == null || (port in 1..65535)) {
                            onValueChange(port)
                        }
                        showDialog = false
                    },
                    cancelText = "清除",
                )
            }
        }
    }
}

@Composable
fun StringInput(
    title: String,
    value: String?,
    placeholder: String = "",
    onValueChange: (String?) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    SuperArrow(
        title = title,
        summary = value?.takeIf { it.isNotEmpty() } ?: "不修改",
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SuperBottomSheet(
            show = remember(showDialog) { mutableStateOf(true) },
            title = title,
            onDismissRequest = { showDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = placeholder,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogButtonRow(
                    onCancel = {
                        onValueChange(null)
                        showDialog = false
                    },
                    onConfirm = {
                        onValueChange(textValue.takeIf { it.isNotEmpty() })
                        showDialog = false
                    },
                    cancelText = "清除",
                )
            }
        }
    }
}

@Composable
fun StringListInput(
    title: String,
    value: List<String>?,
    placeholder: String = "",
    navigator: DestinationsNavigator,
    onValueChange: (List<String>?) -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) "${itemCount} 项" else "不修改"

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = {
            EditorDataHolder.setupListEditor(
                title = title,
                placeholder = placeholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(StringListEditorScreenDestination)
        },
    )
}

@Composable
fun StringMapInput(
    title: String,
    value: Map<String, String>?,
    keyPlaceholder: String = "键",
    valuePlaceholder: String = "值",
    navigator: DestinationsNavigator,
    onValueChange: (Map<String, String>?) -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) "${itemCount} 项" else "不修改"

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = {
            EditorDataHolder.setupMapEditor(
                title = title,
                keyPlaceholder = keyPlaceholder,
                valuePlaceholder = valuePlaceholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(KeyValueEditorScreenDestination)
        },
    )
}
