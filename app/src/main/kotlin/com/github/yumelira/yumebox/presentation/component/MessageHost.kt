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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.WindowDialog 

enum class MessageType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class Message(
    val title: String,
    val content: String,
    val type: MessageType = MessageType.INFO,
    val autoClose: Boolean = true,
    val autoCloseDelay: Long = 2000L,
)

@Composable
fun MessageHost(
    message: Message?,
    onDismiss: () -> Unit,
) {

    val showDialog = remember { mutableStateOf(false) }


    LaunchedEffect(message) {
        showDialog.value = (message != null)
    }

    if (message != null) {
        WindowDialog (
            title = getTitle(message.type, message.title),
            summary = message.content,
            show = showDialog,
            onDismissRequest = {
                showDialog.value = false
                onDismiss()
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                SuperArrow(
                    title = MLang.Component.Message.Confirm,
                    onClick = {
                        showDialog.value = false
                        onDismiss()
                    },
                )
            }
        }


        if (message.autoClose) {
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(message.autoCloseDelay)
                showDialog.value = false
                onDismiss()
            }
        }
    }
}

private fun getTitle(type: MessageType, title: String): String {
    val prefix = when (type) {
        MessageType.SUCCESS -> "✓ "
        MessageType.ERROR -> "✗ "
        MessageType.WARNING -> "⚠ "
        MessageType.INFO -> ""
    }
    return prefix + title
}

@Composable
fun SimpleMessage(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message != null) {
        MessageHost(
            message = Message(MLang.Component.Message.Hint, message),
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun ErrorMessage(
    error: String?,
    onDismiss: () -> Unit,
) {
    if (error != null) {
        MessageHost(
            message = Message(MLang.Component.Message.Error, error, MessageType.ERROR, autoClose = false),
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun SuccessMessage(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message != null) {
        MessageHost(
            message = Message(MLang.Component.Message.Success, message, MessageType.SUCCESS),
            onDismiss = onDismiss,
        )
    }
}

