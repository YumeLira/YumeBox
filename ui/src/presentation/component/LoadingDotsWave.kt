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

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.github.yumelira.yumebox.presentation.theme.UiDp

@Composable
fun LoadingDotsWave(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = UiDp.dp3,
    dotSpacing: Dp = UiDp.dp3,
    amplitude: Dp = UiDp.dp2,
) {
    val transition = rememberInfiniteTransition(label = "LoadingDotsWave")

    val p1 by
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 420, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "p1",
        )
    val p2 by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = -1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 420, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "p2",
        )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(color = color, dotSize = dotSize, shift = p1, amplitude = amplitude)
        Dot(color = color, dotSize = dotSize, shift = p2, amplitude = amplitude)
    }
}

@Composable
private fun Dot(color: Color, dotSize: Dp, shift: Float, amplitude: Dp) {
    androidx.compose.foundation.layout.Box(
        modifier =
            Modifier.size(dotSize)
                .graphicsLayer { translationY = shift * amplitude.toPx() }
                .background(color, CircleShape)
    )
}
