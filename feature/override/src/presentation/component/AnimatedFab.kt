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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Stable
class OverrideFabController internal constructor() {
    var isHiddenByScroll by mutableStateOf(false)
        private set

    fun onScrollDirectionChanged(hidden: Boolean) {
        isHiddenByScroll = hidden
    }
}

@Composable
fun rememberOverrideFabController(): OverrideFabController {
    return remember { OverrideFabController() }
}

@Composable
fun OverrideAnimatedFab(
    controller: OverrideFabController,
    visible: Boolean,
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val fabVisibilityState = remember { MutableTransitionState(false) }
    val hiddenByScroll = controller.isHiddenByScroll
    val actualVisible = visible && !hiddenByScroll
    fabVisibilityState.targetState = actualVisible

    AnimatedVisibility(
        visibleState = fabVisibilityState,
        enter =
            slideInVertically(
                animationSpec =
                    tween(
                        durationMillis = AnimationSpecs.Proxy.VisibilityDuration,
                        easing = AnimationSpecs.EmphasizedDecelerate,
                    ),
                initialOffsetY = { it / 2 },
            ) +
                scaleIn(
                    initialScale = AnimationSpecs.Proxy.VisibilityInitialScale,
                    animationSpec =
                        tween(
                            durationMillis = AnimationSpecs.Proxy.VisibilityDuration,
                            easing = LinearEasing,
                        ),
                ) +
                fadeIn(
                    animationSpec =
                        tween(
                            durationMillis = AnimationSpecs.Proxy.VisibilityFadeDuration,
                            easing = AnimationSpecs.EnterEasing,
                        )
                ),
        exit =
            slideOutVertically(
                animationSpec =
                    tween(
                        durationMillis = AnimationSpecs.Proxy.VisibilityDuration,
                        easing = AnimationSpecs.EmphasizedAccelerate,
                    ),
                targetOffsetY = { it / 2 },
            ) +
                scaleOut(
                    targetScale = AnimationSpecs.Proxy.VisibilityTargetScale,
                    animationSpec =
                        tween(
                            durationMillis = AnimationSpecs.Proxy.VisibilityDuration,
                            easing = LinearEasing,
                        ),
                ) +
                fadeOut(
                    animationSpec =
                        tween(
                            durationMillis = AnimationSpecs.Proxy.VisibilityFadeDuration,
                            easing = AnimationSpecs.ExitEasing,
                        )
                ),
        label = "override_shared_fab_visibility",
    ) {
        FloatingActionButton(
            modifier = Modifier.padding(end = UiDp.dp20, bottom = UiDp.dp85),
            onClick = onClick,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MiuixTheme.colorScheme.onPrimary,
            )
        }
    }
}
