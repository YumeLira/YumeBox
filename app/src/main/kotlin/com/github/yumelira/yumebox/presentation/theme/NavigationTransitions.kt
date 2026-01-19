package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object NavigationTransitions {

    private const val DURATION_ENTER = 420
    private const val DURATION_EXIT = 360
    private const val DURATION_POP_ENTER = 360
    private const val DURATION_POP_EXIT = 420

    val defaultStyle = object : NavHostAnimatedDestinationStyle() {

        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(DURATION_ENTER, easing = AnimationSpecs.EmphasizedDecelerate)
                ) + fadeIn(
                    animationSpec = tween(DURATION_ENTER - 80, easing = AnimationSpecs.EnterEasing),
                    initialAlpha = 0.5f
                )
            }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                slideOutHorizontally(
                    targetOffsetX = { -it * 2 / 5 },
                    animationSpec = tween(DURATION_EXIT, easing = AnimationSpecs.EmphasizedAccelerate)
                ) + fadeOut(
                    animationSpec = tween(DURATION_EXIT - 60, easing = AnimationSpecs.ExitEasing)
                )
            }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                slideInHorizontally(
                    initialOffsetX = { -it * 2 / 5 },
                    animationSpec = tween(DURATION_POP_ENTER, easing = AnimationSpecs.EmphasizedDecelerate)
                ) + fadeIn(
                    animationSpec = tween(DURATION_POP_ENTER - 60, easing = AnimationSpecs.EnterEasing),
                    initialAlpha = 0.7f
                )
            }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(DURATION_POP_EXIT, easing = AnimationSpecs.EmphasizedAccelerate)
                ) + fadeOut(
                    animationSpec = tween(DURATION_POP_EXIT - 80, easing = AnimationSpecs.ExitEasing)
                )
            }
    }
}
