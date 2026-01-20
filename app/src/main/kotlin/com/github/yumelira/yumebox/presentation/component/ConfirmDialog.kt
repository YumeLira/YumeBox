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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConfirmDialog(
    show: MutableState<Boolean>,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = { show.value = false },
    cancelText: String = MLang.Component.Button.Cancel,
    confirmText: String = MLang.Component.Button.Confirm,
) {
    WindowBottomSheet(
        show = show,
        title = title,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                cancelText = cancelText,
                confirmText = confirmText,
            )
        }
    }
}

@Composable
fun ConfirmDialogSimple(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelText: String = MLang.Component.Button.Cancel,
    confirmText: String = MLang.Component.Button.Confirm,
) {
    WindowBottomSheet(
        show = remember { mutableStateOf(true) },
        title = title,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, style = MiuixTheme.textStyles.body1)
            Spacer(modifier = Modifier.height(16.dp))
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                cancelText = cancelText,
                confirmText = confirmText,
            )
        }
    }
}
