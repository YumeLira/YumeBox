package com.github.yumelira.yumebox.presentation.theme


import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object NavigationTransitions {

    private const val DURATION_ENTER = 400
    private const val DURATION_EXIT = 400
    private const val DURATION_POP_ENTER = 400
    private const val DURATION_POP_EXIT = 400

    private const val SCALE_BACK_FACTOR = 0.92f

    private val emphasizedEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    val defaultStyle = object : NavHostAnimatedDestinationStyle() {

        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(DURATION_ENTER, easing = emphasizedEasing)
                )
            }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                scaleOut(
                    targetScale = SCALE_BACK_FACTOR,
                    animationSpec = tween(DURATION_EXIT, easing = emphasizedEasing)
                ) + fadeOut(
                    targetAlpha = 0.7f,
                    animationSpec = tween(DURATION_EXIT, easing = emphasizedEasing)
                )
            }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                scaleIn(
                    initialScale = SCALE_BACK_FACTOR,
                    animationSpec = tween(DURATION_POP_ENTER, easing = emphasizedEasing)
                ) + fadeIn(
                    initialAlpha = 0.7f,
                    animationSpec = tween(DURATION_POP_ENTER, easing = emphasizedEasing)
                )
            }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(DURATION_POP_EXIT, easing = emphasizedEasing)
                )
            }
    }
}