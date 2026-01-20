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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal object ProxyCardDefaults {
    val CornerRadius = 12.dp
    val PaddingHorizontal = 12.dp
    val PaddingVertical = 16.dp
    val TextSpacing = 8.dp
}

private data class DelayPillStyle(
    val text: String,
    val color: Color,
    val backgroundColor: Color,
)

@Composable
internal fun DelayPill(
    delay: Int,
    onClick: (() -> Unit)?,
    isLoading: Boolean = false,
    enableLoadingAnimation: Boolean = true,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MiuixTheme.textStyles.footnote1,
) {
    val style = remember(delay) {
        when {
            delay < 0 -> DelayPillStyle("TIMEOUT", Color(0xFF9E9E9E), Color(0xFF9E9E9E).copy(alpha = 0.14f))
            delay == 0 -> DelayPillStyle("N/A", Color(0xFFBDBDBD), Color(0xFFBDBDBD).copy(alpha = 0.14f))
            delay in 1..800 -> DelayPillStyle("${delay}", Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.14f))
            delay in 801..5000 -> DelayPillStyle("${delay}", Color(0xFFFFA726), Color(0xFFFFA726).copy(alpha = 0.14f))
            else -> null
        }
    } ?: return
    val displayText = if (isLoading && !enableLoadingAnimation) "..." else style.text
    val interactionSource = if (onClick != null) {
        remember { MutableInteractionSource() }
    } else {
        null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(style.backgroundColor)
            .let { base ->
                if (onClick != null) {
                    base.clickable(
                        interactionSource = interactionSource!!,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    base
                }
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading && enableLoadingAnimation) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = style.text,
                    style = textStyle,
                    color = style.color,
                    maxLines = 1,
                    modifier = Modifier.alpha(0f),
                )
                LoadingDots(color = style.color)
            }
        } else {
            Text(text = displayText, style = textStyle, color = style.color, maxLines = 1)
        }
    }
}

@Composable
private fun LoadingDots(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "DelayPillDots")
    val shift1 by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shift1",
    )
    val shift2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shift2",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val amplitude = 3.dp
        Box(
            Modifier
                .size(4.dp)
                .graphicsLayer { translationY = shift1 * amplitude.toPx() }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            Modifier
                .size(4.dp)
                .graphicsLayer { translationY = shift2 * amplitude.toPx() }
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
internal fun ProxySelectableCard(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MiuixTheme.colorScheme.background
    }

    val interactionSource = remember { MutableInteractionSource() }

    Card(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(ProxyCardDefaults.CornerRadius)
                )
                .clip(RoundedCornerShape(ProxyCardDefaults.CornerRadius))
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
                    horizontal = ProxyCardDefaults.PaddingHorizontal,
                    vertical = ProxyCardDefaults.PaddingVertical
                ),
            content = content
        )
    }
}

@Composable
fun ProxyNodeCard(
    name: String,
    typeName: String,
    delay: Int,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isSingleColumn: Boolean = false,
    showDetail: Boolean = false,
    onDelayClick: (() -> Unit)? = null,
    isDelayTesting: Boolean = false,
    enableLoadingAnimation: Boolean = true,
) {
    val textColor = if (isSelected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    ProxySelectableCard(
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
    ) {
            if (isSingleColumn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MiuixTheme.textStyles.body1,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showDetail) {
                            Spacer(modifier = Modifier.height(ProxyCardDefaults.TextSpacing))
                            Text(
                                text = typeName,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                    DelayPill(
                        delay = delay,
                        onClick = onDelayClick,
                        isLoading = isDelayTesting,
                        enableLoadingAnimation = enableLoadingAnimation,
                        textStyle = MiuixTheme.textStyles.body2,
                    )
                }
            } else if (showDetail) {
                Column {
                    Text(
                        text = name,
                        style = MiuixTheme.textStyles.body2,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(ProxyCardDefaults.TextSpacing))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = typeName,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        DelayPill(
                            delay = delay,
                            onClick = onDelayClick,
                            isLoading = isDelayTesting,
                            enableLoadingAnimation = enableLoadingAnimation,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MiuixTheme.textStyles.body2,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    DelayPill(
                        delay = delay,
                        onClick = onDelayClick,
                        isLoading = isDelayTesting,
                        enableLoadingAnimation = enableLoadingAnimation,
                    )
                }
            }
    }
}
