package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp

@Composable
fun LoadingDotsWave(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.dp,
    dotSpacing: Dp = 3.dp,
    amplitude: Dp = 2.dp,
) {
    val transition = rememberInfiniteTransition(label = "LoadingDotsWave")

    val p1 by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "p1",
    )
    val p2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
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
private fun Dot(
    color: Color,
    dotSize: Dp,
    shift: Float,
    amplitude: Dp,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(dotSize)
            .graphicsLayer { translationY = shift * amplitude.toPx() }
            .background(color, CircleShape),
    )
}
