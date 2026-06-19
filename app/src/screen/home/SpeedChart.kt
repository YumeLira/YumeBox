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

package com.github.yumelira.yumebox.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.TrafficChartConfig
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val SPEED_CHART_SAMPLE_LIMIT = 24
private const val SPEED_CHART_IDLE_SCROLL_DURATION_MS = 900
private const val SPEED_CHART_IDLE_WAVE_AMPLITUDE = 0.022f
private const val SPEED_CHART_IDLE_WAVE_SPAN = 4f

@Composable
fun SpeedChart(
    speedHistory: List<Long>,
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity
    val chartColor = MiuixTheme.colorScheme.primary
    val fractions = remember(speedHistory) { buildSpeedChartFractions(speedHistory = speedHistory) }
    val idleTransition = rememberInfiniteTransition(label = "speed_chart_idle")
    val idlePhase by
        idleTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = SPEED_CHART_IDLE_SCROLL_DURATION_MS,
                            easing = LinearEasing,
                        ),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "speed_chart_idle_phase",
        )

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(AppConstants.UI.SPEED_CHART_HEIGHT)
                .clip(RoundedCornerShape(AppConstants.UI.CARD_CORNER_RADIUS))
                .clickable(onClick = onClick)
    ) {
        val barGapPx = componentSizes.speedChartBarGap.toPx()
        val barCornerRadiusPx = componentSizes.speedChartBarCornerRadius.toPx()
        val chartBarCount = SPEED_CHART_SAMPLE_LIMIT

        val totalGapWidth = barGapPx * (chartBarCount - 1)
        val barWidthPx = ((size.width - totalGapWidth) / chartBarCount).coerceAtLeast(0f)
        if (barWidthPx <= 0f) {
            return@Canvas
        }

        val barColor = chartColor.copy(alpha = opacity.mediumStrong)

        if (!isRunning) {
            drawIdleBars(
                fractions = fractions,
                barWidthPx = barWidthPx,
                barGapPx = barGapPx,
                barCornerRadiusPx = barCornerRadiusPx,
                barColor = barColor,
                wavePhase = idlePhase * chartBarCount,
            )
        } else {
            drawStaticBars(
                fractions = fractions,
                barWidthPx = barWidthPx,
                barGapPx = barGapPx,
                barCornerRadiusPx = barCornerRadiusPx,
                barColor = barColor,
            )
        }
    }
}

internal fun buildSpeedChartFractions(
    speedHistory: List<Long>,
    sampleLimit: Int = SPEED_CHART_SAMPLE_LIMIT,
): FloatArray {
    require(sampleLimit > 0) { "sampleLimit must be greater than 0" }

    val fractions = FloatArray(sampleLimit) { TrafficChartConfig.MIN_VISIBLE_HEIGHT }
    val recentHistory = speedHistory.takeLast(sampleLimit)
    val offset = (sampleLimit - recentHistory.size).coerceAtLeast(0)
    recentHistory.forEachIndexed { index, sample ->
        fractions[offset + index] = TrafficChartConfig.calculateBarFraction(sample)
    }
    return fractions
}

private fun DrawScope.drawStaticBars(
    fractions: FloatArray,
    barWidthPx: Float,
    barGapPx: Float,
    barCornerRadiusPx: Float,
    barColor: Color,
) {
    val barCornerRadius =
        createBarCornerRadius(barWidthPx = barWidthPx, barCornerRadiusPx = barCornerRadiusPx)
    for (index in fractions.indices) {
        val barLeftPx = index * (barWidthPx + barGapPx)
        if (barLeftPx >= size.width || barLeftPx + barWidthPx <= 0f) {
            continue
        }
        drawChartBar(
            leftPx = barLeftPx,
            fraction = fractions[index],
            barWidthPx = barWidthPx,
            barColor = barColor,
            barCornerRadius = barCornerRadius,
        )
    }
}

private fun DrawScope.drawIdleBars(
    fractions: FloatArray,
    barWidthPx: Float,
    barGapPx: Float,
    barCornerRadiusPx: Float,
    barColor: Color,
    wavePhase: Float,
) {
    val barCornerRadius =
        createBarCornerRadius(barWidthPx = barWidthPx, barCornerRadiusPx = barCornerRadiusPx)
    for (index in fractions.indices) {
        val barLeftPx = index * (barWidthPx + barGapPx)
        if (barLeftPx >= size.width || barLeftPx + barWidthPx <= 0f) {
            continue
        }
        drawChartBar(
            leftPx = barLeftPx,
            fraction = applyIdleWave(fraction = fractions[index], index = index, phase = wavePhase),
            barWidthPx = barWidthPx,
            barColor = barColor,
            barCornerRadius = barCornerRadius,
        )
    }
}

private fun DrawScope.createBarCornerRadius(
    barWidthPx: Float,
    barCornerRadiusPx: Float,
): CornerRadius {
    return CornerRadius(
        x = barCornerRadiusPx.coerceAtMost(barWidthPx / 2f),
        y = barCornerRadiusPx.coerceAtMost(size.height / 2f),
    )
}

private fun DrawScope.drawChartBar(
    leftPx: Float,
    fraction: Float,
    barWidthPx: Float,
    barColor: Color,
    barCornerRadius: CornerRadius,
) {
    val clampedFraction = fraction.coerceIn(TrafficChartConfig.MIN_VISIBLE_HEIGHT, 1f)
    val barHeightPx = size.height * clampedFraction
    drawRoundRect(
        color = barColor,
        topLeft = Offset(x = leftPx, y = size.height - barHeightPx),
        size = Size(width = barWidthPx, height = barHeightPx),
        cornerRadius = barCornerRadius,
    )
}

private fun applyIdleWave(fraction: Float, index: Int, phase: Float): Float {
    val distance = kotlin.math.abs(index - phase)
    val wrappedDistance =
        minOf(
            distance,
            distance + SPEED_CHART_SAMPLE_LIMIT,
            kotlin.math.abs(index + SPEED_CHART_SAMPLE_LIMIT - phase),
        )
    val normalized = (1f - wrappedDistance / SPEED_CHART_IDLE_WAVE_SPAN).coerceIn(0f, 1f)
    val wave = normalized * normalized
    return fraction + wave * SPEED_CHART_IDLE_WAVE_AMPLITUDE
}
