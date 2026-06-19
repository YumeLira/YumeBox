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

package com.github.yumelira.yumebox.feature.meta.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TabRowWithContour(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return

    val shape = RoundedCornerShape(100.dp)
    val backgroundColor = MiuixTheme.colorScheme.surfaceVariant
    val selectedColor = MiuixTheme.colorScheme.primary
    val indicatorInset = 4.dp
    val indicatorGap = 4.dp
    val indicatorAnimationSpec = remember {
        tween<Dp>(durationMillis = 280, easing = FastOutSlowInEasing)
    }
    val textAnimationSpec = remember {
        tween<Color>(durationMillis = 220, easing = FastOutSlowInEasing)
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(indicatorInset)
    ) {
        val clampedSelectedTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
        val tabCount = tabs.size
        val tabWidth =
            remember(maxWidth, tabCount) {
                ((maxWidth - indicatorGap * (tabCount - 1)) / tabCount).coerceAtLeast(0.dp)
            }
        val targetOffset =
            remember(tabWidth, clampedSelectedTabIndex) {
                (tabWidth + indicatorGap) * clampedSelectedTabIndex
            }
        val indicatorOffset by
            animateDpAsState(
                targetValue = targetOffset,
                animationSpec = indicatorAnimationSpec,
                label = "tab_row_indicator_offset",
            )

        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier =
                    Modifier.offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(selectedColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(indicatorGap),
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = clampedSelectedTabIndex == index
                val textColor by
                    animateColorAsState(
                        targetValue =
                            if (isSelected) {
                                MiuixTheme.colorScheme.onPrimary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                        animationSpec = textAnimationSpec,
                        label = "tab_row_text_color_$index",
                    )

                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clip(shape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(index) },
                            )
                            .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = tab, style = MiuixTheme.textStyles.body2, color = textColor)
                }
            }
        }
    }
}
