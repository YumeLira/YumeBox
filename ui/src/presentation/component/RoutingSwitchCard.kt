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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.github.panpf.sketch.AsyncImage as SketchAsyncImage
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.state.IntColorDrawableStateImage
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun <T> RoutingSwitchCard(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    iconUrl: (T) -> String?,
    itemTitle: (T) -> String,
    isChecked: (T) -> Boolean,
    onCheckedChange: (T, Boolean) -> Unit,
    iconSize: Dp = UiDp.dp40,
    contentPadding: PaddingValues = PaddingValues(horizontal = UiDp.dp16, vertical = UiDp.dp12),
    applyHorizontalPadding: Boolean = true,
    titleContent: @Composable (String) -> Unit = { Title(it) },
) {
    val spacing = AppTheme.spacing

    val rowsPerCard =
        when {
            items.size > 32 -> 12
            items.size > 20 -> 16
            else -> items.size.coerceAtLeast(1)
        }
    val itemChunks = remember(items, rowsPerCard) { items.chunked(rowsPerCard) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space6),
    ) {
        titleContent(title)
        itemChunks.forEach { chunk ->
            Card(applyHorizontalPadding = applyHorizontalPadding) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    chunk.forEach { item ->
                        key(item) {
                            RoutingSwitchRow(
                                iconUrl = iconUrl(item),
                                title = itemTitle(item),
                                checked = isChecked(item),
                                iconSize = iconSize,
                                contentPadding = contentPadding,
                                onCheckedChange = { onCheckedChange(item, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutingSwitchRow(
    iconUrl: String?,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconSize: Dp,
    contentPadding: PaddingValues,
) {
    val spacing = AppTheme.spacing

    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(contentPadding)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCheckedChange(!checked) },
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space16),
    ) {
        if (!iconUrl.isNullOrBlank()) {
            RoutingIcon(iconUrl = iconUrl, size = iconSize)
        }
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RoutingIcon(iconUrl: String?, size: Dp) {
    val opacity = AppTheme.opacity
    val spacing = AppTheme.spacing

    val context = LocalContext.current
    val placeholderColorInt = MiuixTheme.colorScheme.onSurface.copy(alpha = opacity.subtle).toArgb()
    val request =
        remember(iconUrl, placeholderColorInt, context) {
            iconUrl?.takeIf(String::isNotBlank)?.let { url ->
                ImageRequest(context, url) {
                    placeholder(IntColorDrawableStateImage(placeholderColorInt))
                    error(IntColorDrawableStateImage(placeholderColorInt))
                    crossfade(true)
                }
            }
        }

    if (request == null) return

    SketchAsyncImage(
        request = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(RoundedCornerShape(spacing.space14)),
    )
}
