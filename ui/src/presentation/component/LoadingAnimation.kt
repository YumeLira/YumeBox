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
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PulseRippleLoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary,
) {
    val opacity = AppTheme.opacity
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val ripple1 by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "ripple1",
        )

    val ripple2 by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = LinearOutSlowInEasing, delayMillis = 1000),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "ripple2",
        )

    val breathe by
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "breathe",
        )

    Canvas(modifier = modifier.size(UiDp.dp180)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2

        listOf(ripple1, ripple2).forEach { progress ->
            val radius = maxRadius * progress
            val alpha = (1f - progress) * opacity.medium
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = UiDp.dp2_5.toPx(), cap = StrokeCap.Round),
            )
        }

        val gradient =
            Brush.radialGradient(
                colors =
                    listOf(
                        color.copy(alpha = breathe * opacity.secondaryText),
                        color.copy(alpha = breathe * opacity.mediumOverlay),
                        color.copy(alpha = opacity.none),
                    ),
                center = Offset(centerX, centerY),
                radius = UiDp.dp35.toPx(),
            )
        drawCircle(brush = gradient, radius = UiDp.dp35.toPx(), center = Offset(centerX, centerY))

        drawCircle(
            color = color.copy(alpha = opacity.prominentText),
            radius = UiDp.dp10.toPx(),
            center = Offset(centerX, centerY),
        )
    }
}

@Composable
fun StartupLoadingOverlay(isVisible: Boolean, loadingText: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isVisible,
        enter =
            fadeIn(
                animationSpec =
                    tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EnterEasing)
            ) +
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec =
                        tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.StandardEasing),
                ),
        exit =
            fadeOut(
                animationSpec =
                    tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.ExitEasing)
            ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = UiDp.dp60),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PulseRippleLoadingAnimation()

            Spacer(modifier = Modifier.height(UiDp.dp32))

            AnimatedContent(
                targetState = loadingText ?: MLang.Component.Loading.Starting,
                transitionSpec = {
                    fadeIn(
                        animationSpec =
                            tween(
                                AnimationSpecs.DURATION_INSTANT,
                                easing = AnimationSpecs.EnterEasing,
                            )
                    ) togetherWith
                        fadeOut(
                            animationSpec =
                                tween(
                                    AnimationSpecs.DURATION_INSTANT,
                                    easing = AnimationSpecs.ExitEasing,
                                )
                        )
                },
                label = "loadingText",
            ) { text ->
                Text(
                    text = text,
                    style =
                        MiuixTheme.textStyles.body1.copy(fontSize = 16.sp, letterSpacing = 2.sp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
