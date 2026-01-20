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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TextEditBottomSheet(
    show: MutableState<Boolean>,
    title: String,
    textFieldValue: MutableState<TextFieldValue>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit = { show.value = false },
) {
    WindowBottomSheet(
        show = show, title = title, insideMargin = DpSize(32.dp, 16.dp), onDismissRequest = onDismiss
    ) {
        Column {
            TextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss, modifier = Modifier.weight(1f)
                ) { Text(MLang.Component.Button.Cancel) }
                Button(
                    onClick = {
                        onConfirm(textFieldValue.value.text)
                        show.value = false
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()
                ) { Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WarningBottomSheet(
    show: MutableState<Boolean>,
    title: String,
    messages: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = { show.value = false },
) {
    WindowBottomSheet(
        show = show,
        title = title,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = onDismiss,
    ) {
        Column {
            messages.forEachIndexed { index, message ->
                Text(message)
                if (index < messages.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onConfirm()
                    show.value = false
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()
            ) { Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background) }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
