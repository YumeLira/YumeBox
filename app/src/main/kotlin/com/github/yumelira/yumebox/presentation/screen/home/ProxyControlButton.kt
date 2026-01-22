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

package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Play
import com.github.yumelira.yumebox.presentation.icon.yume.Square
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProxyControlButton(
    isRunning: Boolean,
    isEnabled: Boolean,
    hasEnabledProfile: Boolean,
    hasProfiles: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val cornerRadius = AppConstants.UI.BUTTON_CORNER_RADIUS
    val buttonWidthFraction = 0.3f

    MiuixTheme.colorScheme.surface
    MiuixTheme.colorScheme.onSurface

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasProfiles) {
            HintText(MLang.Home.Control.HintAddProfile)
        } else if (!hasEnabledProfile) {
            HintText(MLang.Home.Control.HintEnableProfile)
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    scaleAnim.animateTo(
                        targetValue = 0.90f,
                        animationSpec = tween(AnimationSpecs.DURATION_INSTANT, easing = AnimationSpecs.EmphasizedAccelerate)
                    )
                    scaleAnim.animateTo(
                        targetValue = 1.02f,
                        animationSpec = AnimationSpecs.ButtonPressSpring
                    )
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 1f,
                            stiffness = 500f
                        )
                    )
                }
                onClick()
            },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth(buttonWidthFraction)
                .scale(scaleAnim.value)
                .shadow(
                    elevation = 1.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius),
                    clip = false
                )
                .border(
                    width = 0.2.dp,
                    color = MiuixTheme.colorScheme.outline,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
                ),
            colors = ButtonDefaults.buttonColors(MiuixTheme.colorScheme.background),
            cornerRadius = cornerRadius,
            minHeight = 36.dp
        ) {
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    val enterTransition = slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = tween(AnimationSpecs.DURATION_INSTANT + 40, easing = AnimationSpecs.EnterEasing)
                    ) + fadeIn(
                        animationSpec = tween(AnimationSpecs.DURATION_INSTANT + 40, easing = AnimationSpecs.EnterEasing)
                    ) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = AnimationSpecs.IconTransition as FiniteAnimationSpec<Float>
                    )

                    val exitTransition = slideOutVertically(
                        targetOffsetY = { -it / 5 },
                        animationSpec = tween(AnimationSpecs.DURATION_INSTANT + 20, easing = AnimationSpecs.ExitEasing)
                    ) + fadeOut(
                        animationSpec = tween(AnimationSpecs.DURATION_INSTANT + 20, easing = AnimationSpecs.ExitEasing)
                    ) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = AnimationSpecs.IconTransition
                    )

                    enterTransition.togetherWith(exitTransition)
                },
                label = "IconTransition"
            ) { running ->
                Icon(
                    imageVector = if (running) Yume.Square else Yume.Play,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
}
