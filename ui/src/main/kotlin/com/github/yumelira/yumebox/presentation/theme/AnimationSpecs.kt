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

package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AnimationSpecs {

    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Legacy = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val StandardEasing = FastOutSlowInEasing
    val EnterEasing = LinearOutSlowInEasing
    val ExitEasing = FastOutLinearInEasing

    const val DURATION_INSTANT = 120
    const val DURATION_FAST = 280
    const val DURATION_STANDARD = 350
    const val DURATION_SLOW = 450
    const val DURATION_EXTRA_SLOW = 550

    val ButtonPress: AnimationSpec<Float> = tween(DURATION_FAST, easing = StandardEasing)
    val ButtonPressSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 400f
    )
    val IconTransition: AnimationSpec<Float> = tween(320, easing = Emphasized)
    val IconTransition3D: AnimationSpec<Float> = tween(360, easing = Legacy)
    val PageTransition: AnimationSpec<Float> = tween(DURATION_STANDARD, easing = EmphasizedDecelerate)
    val FadeContent: AnimationSpec<Float> = tween(DURATION_FAST, easing = EnterEasing)
    val ListItemEnter: AnimationSpec<Float> = tween(DURATION_FAST, easing = EmphasizedDecelerate)
    val ListItemEnterStagger: AnimationSpec<Float> = tween(280, easing = Emphasized)
    val NavigationBarVisibility: AnimationSpec<Float> = tween(300, easing = EmphasizedDecelerate)
    val NavigationBarIconSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 500f
    )
    val ChartDataUpdate: AnimationSpec<Float> = tween(DURATION_FAST, easing = StandardEasing)
    val PullToRefresh: AnimationSpec<Float> = tween(DURATION_STANDARD, easing = StandardEasing)
    val CardEnter: AnimationSpec<Float> = tween(380, easing = EmphasizedDecelerate)
    val TextReveal: AnimationSpec<Float> = tween(300, easing = Emphasized)

    const val STAGGER_DELAY_ITEM = 40
    const val STAGGER_DELAY_SECTION = 80
    const val STAGGER_DELAY_LAYER = 60
}
