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

package com.github.yumelira.yumebox.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Type-safe Dialog state management utility.
 *
 * Provides a consistent pattern for managing dialog visibility and payload across the app,
 * reducing boilerplate show/hide/dismiss logic.
 *
 * Usage example:
 * ```kotlin
 * // Simple boolean dialog
 * val deleteDialog = rememberDialogVisibilityState()
 *
 * Button(onClick = { deleteDialog.show() }) {
 *     Text("Delete")
 * }
 *
 * if (deleteDialog.isShown) {
 *     ConfirmDialog(
 *         onDismiss = { deleteDialog.dismiss() },
 *         onConfirm = {
 *             performDelete()
 *             deleteDialog.dismiss()
 *         }
 *     )
 * }
 *
 * // Dialog with payload
 * val editDialog = rememberDialogState<User>()
 *
 * Button(onClick = { editDialog.show(currentUser) }) {
 *     Text("Edit")
 * }
 *
 * editDialog.payload?.let { user ->
 *     EditDialog(
 *         user = user,
 *         onDismiss = { editDialog.dismiss() },
 *         onSave = { updatedUser ->
 *             saveUser(updatedUser)
 *             editDialog.dismiss()
 *         }
 *     )
 * }
 * ```
 */
class DialogState<T>(
    private val isShownState: MutableState<Boolean> = mutableStateOf(false),
    private val payloadState: MutableState<T?> = mutableStateOf(null),
) {
    /**
     * Whether the dialog is currently shown.
     */
    val isShown: Boolean
        get() = isShownState.value

    /**
     * The payload associated with this dialog, if any.
     */
    val payload: T?
        get() = payloadState.value

    /**
     * Shows the dialog without payload.
     */
    fun show() {
        isShownState.value = true
    }

    /**
     * Shows the dialog with associated payload.
     */
    fun show(payload: T) {
        payloadState.value = payload
        isShownState.value = true
    }

    /**
     * Dismisses the dialog and clears payload.
     */
    fun dismiss() {
        isShownState.value = false
        payloadState.value = null
    }

    /**
     * Updates the dialog payload without changing visibility.
     */
    fun updatePayload(payload: T?) {
        payloadState.value = payload
    }
}

/**
 * Creates and remembers a [DialogState] for managing dialog visibility.
 *
 * @return A remembered DialogState instance
 */
@Composable
fun rememberDialogVisibilityState(): DialogState<Unit> {
    return remember { DialogState() }
}

/**
 * Creates and remembers a [DialogState] with associated payload type.
 *
 * @param T The type of payload associated with the dialog
 * @return A remembered DialogState instance
 */
@Composable
fun <T> rememberDialogState(): DialogState<T> {
    return remember { DialogState() }
}

/**
 * Specialized dialog state for common confirm/delete scenarios.
 *
 * Usage example:
 * ```kotlin
 * val confirmDelete = rememberConfirmDialogState<File>()
 *
 * Button(onClick = { confirmDelete.show(file, "Delete this file?") }) {
 *     Text("Delete")
 * }
 *
 * if (confirmDelete.isShown) {
 *     ConfirmDialog(
 *         title = "Confirm",
 *         message = confirmDelete.message,
 *         onDismiss = { confirmDelete.dismiss() },
 *         onConfirm = {
 *             confirmDelete.payload?.let { deleteFile(it) }
 *             confirmDelete.dismiss()
 *         }
 *     )
 * }
 * ```
 */
class ConfirmDialogState<T>(
    private val isShownState: MutableState<Boolean> = mutableStateOf(false),
    private val payloadState: MutableState<T?> = mutableStateOf(null),
    private val messageState: MutableState<String> = mutableStateOf(""),
) {
    val isShown: Boolean get() = isShownState.value
    val payload: T? get() = payloadState.value
    val message: String get() = messageState.value

    fun show(payload: T, message: String) {
        payloadState.value = payload
        messageState.value = message
        isShownState.value = true
    }

    fun dismiss() {
        isShownState.value = false
        payloadState.value = null
        messageState.value = ""
    }
}

/**
 * Creates and remembers a [ConfirmDialogState] for confirm dialogs.
 */
@Composable
fun <T> rememberConfirmDialogState(): ConfirmDialogState<T> {
    return remember { ConfirmDialogState() }
}
