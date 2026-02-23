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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PulseRippleLoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val ripple1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )

    val ripple2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Canvas(modifier = modifier.size(180.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2

        listOf(ripple1, ripple2).forEach { progress ->
            val radius = maxRadius * progress
            val alpha = (1f - progress) * 0.5f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val gradient = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = breathe * 0.6f),
                color.copy(alpha = breathe * 0.2f),
                color.copy(alpha = 0f)
            ),
            center = Offset(centerX, centerY),
            radius = 35.dp.toPx()
        )
        drawCircle(
            brush = gradient,
            radius = 35.dp.toPx(),
            center = Offset(centerX, centerY)
        )

        drawCircle(
            color = color.copy(alpha = 0.9f),
            radius = 10.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun StartupLoadingOverlay(
    isVisible: Boolean,
    loadingText: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EnterEasing)
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.StandardEasing)
        ),
        exit = fadeOut(
            animationSpec = tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.ExitEasing)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulseRippleLoadingAnimation()

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = loadingText ?: MLang.Component.Loading.Starting,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(AnimationSpecs.DURATION_INSTANT, easing = AnimationSpecs.EnterEasing)
                    ) togetherWith
                            fadeOut(
                                animationSpec = tween(AnimationSpecs.DURATION_INSTANT, easing = AnimationSpecs.ExitEasing)
                            )
                },
                label = "loadingText"
            ) { text ->
                Text(
                    text = text,
                    style = MiuixTheme.textStyles.body1.copy(
                        fontSize = 16.sp,
                        letterSpacing = 2.sp
                    ),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}
