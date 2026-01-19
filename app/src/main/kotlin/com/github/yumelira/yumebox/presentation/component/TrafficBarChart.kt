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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.formatBytes
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BarChartItem(
    val label: String,
    val value: Long,
    val isHighlighted: Boolean = false
)

@Composable
fun TrafficBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    maxDisplayValue: Long? = null,
    onItemClick: ((Int) -> Unit)? = null,
    selectedIndex: Int = -1,
    barColor: Color = MiuixTheme.colorScheme.primary.copy(alpha = 0.5f),
    highlightColor: Color = MiuixTheme.colorScheme.primary,
    chartHeight: Dp = 140.dp,
    barWidth: Dp = 20.dp
) {
    val computedMaxValue = maxDisplayValue ?: items.maxOfOrNull { it.value } ?: 1L
    val safeMaxValue = if (computedMaxValue <= 0L) 1L else computedMaxValue

    val animatedMaxValue by animateFloatAsState(
        targetValue = safeMaxValue.toFloat(),
        animationSpec = tween(durationMillis = 400),
        label = "maxValue"
    )

    val displayItems = remember(items) {
        if (items.size <= 7) {
            items + List(7 - items.size) { BarChartItem("", 0L) }
        } else {
            items.take(7)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = formatBytes(animatedMaxValue.toLong()),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            displayItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex || item.isHighlighted
                val isValidItem = item.label.isNotEmpty()

                val targetHeight = if (animatedMaxValue > 0 && item.value > 0) {
                    (item.value.toFloat() / animatedMaxValue).coerceIn(0.03f, 1f)
                } else {
                    0.03f
                }

                val animatedHeight by animateFloatAsState(
                    targetValue = if (isValidItem) targetHeight else 0f,
                    animationSpec = tween(durationMillis = 400),
                    label = "barHeight_$index"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (isValidItem && animatedHeight > 0f) {
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .fillMaxHeight(animatedHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex || item.isHighlighted
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 9.sp),
                        color = if (isSelected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}
