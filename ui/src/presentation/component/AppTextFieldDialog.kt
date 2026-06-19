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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumelira.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.TextField

@Composable
fun AppTextFieldDialog(
    show: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = 1,
    renderInRootScaffold: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    var localTextFieldValue by
        remember(show) {
            mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
        }
    var pendingExternalSyncText by remember(show) { mutableStateOf<String?>(null) }

    LaunchedEffect(show) {
        if (show) {
            localTextFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
            pendingExternalSyncText = null
        }
    }

    LaunchedEffect(value, show) {
        if (!show) return@LaunchedEffect
        when {
            value == localTextFieldValue.text -> {
                pendingExternalSyncText = null
            }

            pendingExternalSyncText != null -> {
                localTextFieldValue =
                    TextFieldValue(text = value, selection = TextRange(value.length))
                pendingExternalSyncText = null
            }
        }
    }

    AppTextFieldDialog(
        show = show,
        title = title,
        textFieldValue = localTextFieldValue,
        onTextFieldValueChange = { updatedTextFieldValue ->
            localTextFieldValue = updatedTextFieldValue
            pendingExternalSyncText = updatedTextFieldValue.text
            onValueChange(updatedTextFieldValue.text)
        },
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        modifier = modifier,
        summary = summary,
        label = label,
        useLabelAsPlaceholder = useLabelAsPlaceholder,
        singleLine = singleLine,
        maxLines = maxLines,
        renderInRootScaffold = renderInRootScaffold,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        supportingContent = supportingContent,
    )
}

@Composable
fun AppTextFieldDialog(
    show: Boolean,
    title: String,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = 1,
    renderInRootScaffold: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    AppDialog(
        show = show,
        modifier = modifier,
        title = title,
        summary = summary,
        onDismissRequest = onDismissRequest,
        renderInRootScaffold = renderInRootScaffold,
    ) {
        AppDialogColumn {
            val textFieldModifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = if (supportingContent == null) UiDp.dp0 else UiDp.dp8)
            if (label.isBlank()) {
                TextField(
                    modifier = textFieldModifier,
                    value = textFieldValue,
                    onValueChange = onTextFieldValueChange,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    trailingIcon = trailingIcon,
                )
            } else {
                TextField(
                    modifier = textFieldModifier,
                    value = textFieldValue,
                    onValueChange = onTextFieldValueChange,
                    label = label,
                    useLabelAsPlaceholder = useLabelAsPlaceholder,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    trailingIcon = trailingIcon,
                )
            }
            supportingContent?.invoke()
            DialogButtonRow(onCancel = onDismissRequest, onConfirm = onConfirm)
        }
    }
}
