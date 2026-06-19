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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yumelira.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class EditorAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
fun EditorScaffold(
    title: String,
    scrollBehavior: ScrollBehavior,
    actions: List<EditorAction>,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                actions = {
                    actions.forEachIndexed { index, action ->
                        IconButton(onClick = action.onClick) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                            )
                        }
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
fun EditorEmptyState(title: String, hint: String, modifier: Modifier = Modifier) {
    CenteredText(firstLine = title, secondLine = hint, modifier = modifier)
}

@Composable
fun EditorListItem(
    index: Int,
    title: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailingText: String? = null,
    deleteIcon: ImageVector,
    deleteContentDescription: String,
) {
    Card(modifier = modifier.padding(vertical = UiDp.dp4)) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = UiDp.dp16, vertical = UiDp.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$index.",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.width(UiDp.dp40),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = UiDp.dp8),
                verticalArrangement = Arrangement.spacedBy(UiDp.dp4),
            ) {
                Text(text = title, style = MiuixTheme.textStyles.body1)
                summary
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = it,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
            }
            trailingText
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    Text(
                        text = it,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(end = UiDp.dp8),
                    )
                }
            IconButton(onClick = onDelete, modifier = Modifier) {
                Icon(
                    imageVector = deleteIcon,
                    contentDescription = deleteContentDescription,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
