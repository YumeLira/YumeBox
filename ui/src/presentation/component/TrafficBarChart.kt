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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BarChartItem(val label: String, val value: Long, val isHighlighted: Boolean = false)

@Composable
fun TrafficBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    maxDisplayValue: Long? = null,
    onItemClick: ((Int) -> Unit)? = null,
    selectedIndex: Int = -1,
    barColor: Color = MiuixTheme.colorScheme.primary.copy(alpha = AppTheme.opacity.medium),
    highlightColor: Color = MiuixTheme.colorScheme.primary,
    chartHeight: Dp = AppTheme.sizes.trafficBarChartHeight,
    barWidth: Dp = AppTheme.sizes.trafficBarWidth,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val componentSizes = AppTheme.sizes

    val computedMaxValue = maxDisplayValue ?: items.maxOfOrNull { it.value } ?: 1L
    val safeMaxValue = if (computedMaxValue <= 0L) 1L else computedMaxValue

    val animatedMaxValue by
        animateFloatAsState(
            targetValue = safeMaxValue.toFloat(),
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
            label = "maxValue",
        )

    val displayItems =
        remember(items) {
            if (items.size <= 7) {
                items + List(7 - items.size) { BarChartItem("", 0L) }
            } else {
                items.take(7)
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(componentSizes.trafficBarLabelHeight),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatBytes(animatedMaxValue.toLong()),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(spacing.space4))

        Row(
            modifier = Modifier.fillMaxWidth().height(chartHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            displayItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex || item.isHighlighted
                val isValidItem = item.label.isNotEmpty()

                val targetHeight =
                    if (animatedMaxValue > 0 && item.value > 0) {
                        (item.value.toFloat() / animatedMaxValue).coerceIn(0.04f, 1f)
                    } else {
                        0.04f
                    }

                val animatedHeight by
                    animateFloatAsState(
                        targetValue = if (isValidItem) targetHeight else 0f,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
                        label = "barHeight_$index",
                    )

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (isValidItem && animatedHeight > 0f) {
                        Spacer(
                            modifier =
                                Modifier.width(barWidth)
                                    .fillMaxHeight(animatedHeight)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = radii.radius4,
                                            topEnd = radii.radius4,
                                        )
                                    )
                                    .background(if (isSelected) highlightColor else barColor)
                                    .then(
                                        if (onItemClick != null) {
                                            Modifier.clickable { onItemClick(index) }
                                        } else {
                                            Modifier
                                        }
                                    )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.space8))

        Row(
            modifier = Modifier.fillMaxWidth().height(componentSizes.trafficBarLabelHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            displayItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex || item.isHighlighted
                Text(
                    modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally),
                    text = item.label,
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 9.sp),
                    color =
                        if (isSelected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                    maxLines = 1,
                )
            }
        }
    }
}
