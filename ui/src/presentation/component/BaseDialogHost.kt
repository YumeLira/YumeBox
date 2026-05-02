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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.github.yumelira.yumebox.presentation.util.DialogState

@Composable
fun BaseDialogVisibilityHost(
    dialogState: DialogState<Unit>,
    content: @Composable (visible: Boolean, dismiss: () -> Unit) -> Unit,
) {
    val dismiss: () -> Unit = { dialogState.dismiss() }
    content(dialogState.isShown, dismiss)
}

@Composable
fun <T> BaseDialogPayloadHost(
    dialogState: DialogState<T>,
    content: @Composable (payload: T, dismiss: () -> Unit) -> Unit,
) {
    val dismiss: () -> Unit = { dialogState.dismiss() }
    val payload = dialogState.payload
    if (dialogState.isShown && payload != null) {
        content(payload, dismiss)
    } else {
        // Ensure stale payload does not survive invisible state transitions.
        LaunchedEffect(dialogState.isShown) {
            if (!dialogState.isShown) dialogState.dismiss()
        }
    }
}
