package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object NavigationTransitions {

    // Flat "push with fade": old slides away and fades out by halfway; new slides in and fades in early.
    private const val DURATION = 450
    private const val FADE_DURATION = DURATION / 2

    val defaultStyle = object : NavHostAnimatedDestinationStyle() {

        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                    initialAlpha = 0f
                )
            }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                    targetAlpha = 0f
                )
            }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
            {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                    initialAlpha = 0f
                )
            }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
            {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = DURATION, easing = AnimationSpecs.StandardEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = FADE_DURATION, easing = LinearEasing),
                    targetAlpha = 0f
                )
            }
    }
}
