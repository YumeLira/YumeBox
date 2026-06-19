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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class TrafficDonutSlice(val key: String, val label: String, val value: Long, val color: Color)

@Composable
fun TrafficDonutChart(
    total: Long,
    slices: List<TrafficDonutSlice>,
    selectedKey: String?,
    onSliceClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    animationKey: Any? = Unit,
    strokeWidth: Dp = UiDp.dp22,
    segmentGapAngle: Float = 3.6f,
    selectedExpansion: Dp = UiDp.dp8,
    centerContent: @Composable BoxScope.() -> Unit = {},
) {
    val semanticColors = AppTheme.colors
    val opacity = AppTheme.opacity

    val safeTotal = total.coerceAtLeast(0L)
    val interactiveSlices =
        remember(slices, safeTotal) {
            if (safeTotal <= 0L) emptyList()
            else buildInteractiveSlices(slices, safeTotal, segmentGapAngle)
        }
    val animationProgress = remember { Animatable(0f) }
    var lastAnimationKey by remember { mutableStateOf(animationKey) }
    var hasAnimatedForKey by remember { mutableStateOf(false) }

    LaunchedEffect(animationKey, interactiveSlices.isNotEmpty()) {
        if (lastAnimationKey != animationKey) {
            lastAnimationKey = animationKey
            hasAnimatedForKey = false
        }

        if (interactiveSlices.isEmpty()) {
            animationProgress.snapTo(0f)
            return@LaunchedEffect
        }

        if (!hasAnimatedForKey) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = EaseInOutQuart),
            )
            hasAnimatedForKey = true
        } else if (animationProgress.value != 1f) {
            animationProgress.snapTo(1f)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier =
                Modifier.fillMaxSize().pointerInput(interactiveSlices, selectedKey) {
                    detectTapGestures { offset ->
                        if (interactiveSlices.isEmpty()) {
                            onSliceClick(null)
                            return@detectTapGestures
                        }

                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val radius = min(size.width, size.height) / 2f
                        val innerRadius = (radius - strokeWidth.toPx()).coerceAtLeast(0f)
                        val distance = sqrt(dx.pow(2) + dy.pow(2))
                        if (distance !in innerRadius..radius) {
                            onSliceClick(null)
                            return@detectTapGestures
                        }

                        val angle = ((atan2(dy, dx) * 180f / PI.toFloat()) + 450f) % 360f
                        val tapped = interactiveSlices.firstOrNull {
                            angle >= it.startAngle && angle < it.endAngle
                        }
                        onSliceClick(tapped?.key)
                    }
                }
        ) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            val canvasSize = Size(size.width, size.height)
            val expansionPx = selectedExpansion.toPx()

            if (interactiveSlices.isEmpty()) {
                drawArc(
                    color = semanticColors.traffic.donutTrackForeground,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    size = canvasSize,
                    style = stroke,
                )
                return@Canvas
            }

            drawArc(
                color = semanticColors.traffic.donutTrackBackground,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                size = canvasSize,
                style = stroke,
            )

            interactiveSlices.forEach { slice ->
                val isSelected = selectedKey == slice.key
                val animatedSweep = slice.sweepAngle * animationProgress.value
                if (animatedSweep <= 0f) return@forEach

                val drawSize =
                    if (isSelected) {
                        Size(width = size.width + expansionPx, height = size.height + expansionPx)
                    } else {
                        canvasSize
                    }
                val drawTopLeft =
                    if (isSelected) {
                        Offset(-expansionPx / 2f, -expansionPx / 2f)
                    } else {
                        Offset.Zero
                    }

                drawArc(
                    color =
                        if (selectedKey == null || isSelected) {
                            slice.color
                        } else {
                            slice.color.copy(alpha = opacity.emphasizedTrack)
                        },
                    startAngle = slice.startAngle - 90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = drawTopLeft,
                    size = drawSize,
                    style = stroke,
                )
            }
        }

        centerContent()
    }
}

private data class InteractiveDonutSlice(
    val key: String,
    val startAngle: Float,
    val endAngle: Float,
    val sweepAngle: Float,
    val color: Color,
)

private fun buildInteractiveSlices(
    slices: List<TrafficDonutSlice>,
    total: Long,
    segmentGapAngle: Float,
): List<InteractiveDonutSlice> {
    if (total <= 0L) return emptyList()
    val visibleSlices = slices.filter { it.value > 0L }
    if (visibleSlices.isEmpty()) return emptyList()

    val normalizedTotal = visibleSlices.sumOf(TrafficDonutSlice::value).coerceAtLeast(1L)
    val rawSweeps = visibleSlices.map { slice ->
        ((slice.value.toDouble() / normalizedTotal.toDouble()) * 360.0).toFloat()
    }
    val sliceCount = visibleSlices.size
    val minRawSweep = rawSweeps.minOrNull() ?: 0f
    val uniformGap =
        when {
            sliceCount <= 1 -> 0f
            minRawSweep <= 0f -> 0f
            else -> min(segmentGapAngle.coerceAtLeast(0f), minRawSweep * 0.6f)
        }
    val totalGap = uniformGap * sliceCount
    val availableSweep = (360f - totalGap).coerceAtLeast(0f)

    var cursor = 0f
    var consumedSweep = 0f
    return visibleSlices.mapIndexed { index, slice ->
        val sweep =
            if (index == visibleSlices.lastIndex) {
                (availableSweep - consumedSweep).coerceAtLeast(0f)
            } else {
                val share = slice.value.toFloat() / normalizedTotal.toFloat()
                (availableSweep * share).coerceAtLeast(0f)
            }
        val start = cursor + uniformGap / 2f
        val end = (start + sweep).coerceAtMost(360f)
        cursor += sweep + uniformGap
        consumedSweep += sweep

        InteractiveDonutSlice(
            key = slice.key,
            startAngle = start,
            endAngle = end,
            sweepAngle = sweep,
            color = slice.color,
        )
    }
}

private val EaseInOutQuart = Easing { fraction ->
    if (fraction < 0.5f) {
        8f * fraction * fraction * fraction * fraction
    } else {
        val value = -2f * fraction + 2f
        1f - (value * value * value * value) / 2f
    }
}
