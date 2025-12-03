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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private object ProxyCardConstants {
    val CARD_CORNER_RADIUS = 12.dp
    val CONTENT_PADDING_HORIZONTAL = 12.dp
    val CONTENT_PADDING_VERTICAL = 16.dp
    val TEXT_SPACING = 8.dp
}

@Composable
private fun DelayIndicator(
    delay: Int,
    textStyle: androidx.compose.ui.text.TextStyle = MiuixTheme.textStyles.footnote1
) {
    val (text, color) = when {
        delay < 0 -> "TIMEOUT" to Color(0xFF9E9E9E)
        delay == 0 -> "N/A" to Color(0xFFBDBDBD)
        delay in 1..500 -> "${delay}" to Color(0xFF4CAF50)
        delay in 501..1000 -> "${delay}" to Color(0xFFFFA726)
        else -> return
    }
    Text(text = text, style = textStyle, color = color)
}

@Composable
fun ProxyNodeCard(
    proxy: Proxy,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isSingleColumn: Boolean = false,
    showDetail: Boolean = false
) {
    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MiuixTheme.colorScheme.background
    }

    val textColor = if (isSelected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    val interactionSource = remember { MutableInteractionSource() }

    Card(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(ProxyCardConstants.CARD_CORNER_RADIUS)
                )
                .clip(RoundedCornerShape(ProxyCardConstants.CARD_CORNER_RADIUS))
                .let {
                    if (onClick != null) {
                        it.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        it
                    }
                }
                .padding(
                    horizontal = ProxyCardConstants.CONTENT_PADDING_HORIZONTAL,
                    vertical = ProxyCardConstants.CONTENT_PADDING_VERTICAL
                )
        ) {
            if (isSingleColumn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = proxy.name,
                            style = MiuixTheme.textStyles.body1,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showDetail) {
                            Spacer(modifier = Modifier.height(ProxyCardConstants.TEXT_SPACING))
                            Text(
                                text = proxy.type.name,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                    DelayIndicator(proxy.delay, MiuixTheme.textStyles.body2)
                }
            } else if (showDetail) {
                Column {
                    Text(
                        text = proxy.name,
                        style = MiuixTheme.textStyles.body2,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(ProxyCardConstants.TEXT_SPACING))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = proxy.type.name,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        DelayIndicator(proxy.delay)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = proxy.name,
                        style = MiuixTheme.textStyles.body2,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    DelayIndicator(proxy.delay)
                }
            }
        }
    }
}
