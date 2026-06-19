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

import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.presentation.util.DialogState
import com.github.yumelira.yumebox.presentation.util.rememberDialogVisibilityState
import dev.oom_wg.purejoy.mlang.MLang

@Composable
fun ConfirmDialog(
    show: DialogState<Unit>,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = { show.dismiss() },
    cancelText: String = MLang.Component.Button.Cancel,
    confirmText: String = MLang.Component.Button.Confirm,
) {
    AppActionBottomSheet(show = show.isShown, title = title, onDismissRequest = onDismiss) {
        ConfirmDialogContent(
            message = message,
            onCancel = onDismiss,
            onConfirm = onConfirm,
            cancelText = cancelText,
            confirmText = confirmText,
        )
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
    val show = rememberDialogVisibilityState()
    if (!show.isShown) {
        show.show()
    }
    AppActionBottomSheet(
        show = show.isShown,
        title = title,
        onDismissRequest = {
            show.dismiss()
            onDismiss()
        },
    ) {
        ConfirmDialogContent(
            message = message,
            onCancel = {
                show.dismiss()
                onDismiss()
            },
            onConfirm = {
                show.dismiss()
                onConfirm()
            },
            cancelText = cancelText,
            confirmText = confirmText,
        )
    }
}

@Composable
private fun ConfirmDialogContent(
    message: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String,
    confirmText: String,
) {
    AppConfirmDialogContent(
        message = message,
        onCancel = onCancel,
        onConfirm = onConfirm,
        cancelText = cancelText,
        confirmText = confirmText,
    )
}
