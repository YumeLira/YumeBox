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
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.WindowBottomSheet


@Composable
fun PortInputContent(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value?.toString() ?: "") }

    SuperArrow(
        title = title,
        summary = if (value != null) "$value" else MLang.Component.Selector.NotModify,
        onClick = {
            textValue = value?.toString() ?: ""
            showDialog.value = true
        },
    )

    ConfigTextInputDialog(
        show = showDialog,
        title = title,
        textValue = textValue,
        label = MLang.Component.ConfigInput.PortLabel,
        onTextValueChange = { input ->
            textValue = input.filter(Char::isDigit)
        },
        onClear = { onValueChange(null) },
        onConfirm = {
            val port = textValue.toIntOrNull()
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
    var textValue by remember { mutableStateOf(value ?: "") }

    SuperArrow(
        title = title,
        summary = value?.takeIf { it.isNotEmpty() } ?: MLang.Component.Selector.NotModify,
        onClick = {
            textValue = value ?: ""
            showDialog.value = true
        },
    )

    ConfigTextInputDialog(
        show = showDialog,
        title = title,
        textValue = textValue,
        label = placeholder,
        onTextValueChange = { textValue = it },
        onClear = { onValueChange(null) },
        onConfirm = { onValueChange(textValue.takeIf { it.isNotEmpty() }) },
    )
}

@Composable
fun StringListInputContent(
    title: String,
    value: List<String>?,
    onClick: () -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) {
        MLang.Component.ConfigInput.CountItems.format(itemCount)
    } else {
        MLang.Component.Selector.NotModify
    }

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = onClick,
    )
}

@Composable
fun StringMapInputContent(
    title: String,
    value: Map<String, String>?,
    onClick: () -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) {
        MLang.Component.ConfigInput.CountItems.format(itemCount)
    } else {
        MLang.Component.Selector.NotModify
    }

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = onClick,
    )
}

@Composable
private fun ConfigTextInputDialog(
    show: MutableState<Boolean>,
    title: String,
    textValue: String,
    label: String,
    onTextValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!show.value) return
    WindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = textValue,
                onValueChange = onTextValueChange,
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            DialogButtonRow(
                onCancel = {
                    onClear()
                    show.value = false
                },
                onConfirm = {
                    onConfirm()
                    show.value = false
                },
                cancelText = MLang.Component.Button.Clear,
            )
        }
    }
}
