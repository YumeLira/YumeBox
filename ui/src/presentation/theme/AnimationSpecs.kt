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

import androidx.compose.animation.core.*

object AnimationSpecs {

    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Legacy = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val StandardEasing = FastOutSlowInEasing
    val EnterEasing = LinearOutSlowInEasing
    val ExitEasing = FastOutLinearInEasing

    const val DURATION_INSTANT = 120
    const val DURATION_FAST = 280

    val ButtonPress: AnimationSpec<Float> = tween(DURATION_FAST, easing = StandardEasing)
    val ButtonPressSpring: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 400f)
    val IconTransition: AnimationSpec<Float> = tween(320, easing = Legacy)

    object Proxy {

        const val VisibilityDuration = 180
        const val VisibilityFadeDuration = 140
        const val VisibilityInitialScale = 0.8f
        const val VisibilityTargetScale = 0.8f

        const val FabDuration = VisibilityDuration
        const val FabFadeDuration = VisibilityFadeDuration

        const val SheetSlideInDuration = 340
        const val SheetSlideOutDuration = 300
        const val SheetFadeInDuration = 140
        const val SheetFadeOutDuration = 140

        const val RefreshIndicatorDuration = 200
        const val RefreshIndicatorFadeDuration = 150
    }
}
