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
import dev.oom_wg.purejoy.mlang.MLang

@Composable
fun AppConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = MLang.Component.Button.Confirm,
    cancelText: String = MLang.Component.Button.Cancel,
) {
    AppDialog(show = show, title = title, onDismissRequest = onDismissRequest) {
        AppConfirmDialogContent(
            message = message,
            onCancel = onDismissRequest,
            onConfirm = onConfirm,
            cancelText = cancelText,
            confirmText = confirmText,
        )
    }
}
