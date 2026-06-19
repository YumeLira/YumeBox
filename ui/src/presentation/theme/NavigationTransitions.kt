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

package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object NavigationTransitions {

    private const val DURATION = 340
    private const val FADE_DURATION = 140

    private val enterEasing = CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.0f)
    private val exitEasing = CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.0f)

    val defaultStyle =
        object : NavHostAnimatedDestinationStyle() {

            override val enterTransition:
                AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = DURATION, easing = enterEasing),
                    ) +
                        fadeIn(
                            animationSpec =
                                tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                            initialAlpha = 0f,
                        )
                }

            override val exitTransition:
                AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = DURATION, easing = exitEasing),
                    ) +
                        fadeOut(
                            animationSpec =
                                tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                            targetAlpha = 0f,
                        )
                }

            override val popEnterTransition:
                AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(durationMillis = DURATION, easing = enterEasing),
                    ) +
                        fadeIn(
                            animationSpec =
                                tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                            initialAlpha = 0f,
                        )
                }

            override val popExitTransition:
                AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = DURATION, easing = exitEasing),
                    ) +
                        fadeOut(
                            animationSpec =
                                tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                            targetAlpha = 0f,
                        )
                }
        }
}
