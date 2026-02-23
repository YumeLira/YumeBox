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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Arrow-down-up`
import com.github.yumelira.yumebox.presentation.icon.yume.Bolt
import com.github.yumelira.yumebox.presentation.icon.yume.House
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }

@Composable
fun BottomBarContent(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    bottomBarFloating: Boolean,
    showDivider: Boolean,
    iconWithSelectedLabel: Boolean,
    isVisible: Boolean = true,
) {
    val pagerState = LocalPagerState.current
    val page by remember(pagerState) {
        derivedStateOf { if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage }
    }
    val handlePageChange = LocalHandlePageChange.current
    val onItemClick: (Int) -> Unit = onItemClick@{ index ->
        if (index == pagerState.currentPage && !pagerState.isScrollInProgress) return@onItemClick
        handlePageChange(index)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 200, easing = AnimationSpecs.EnterEasing),
        ) + slideInVertically(
            initialOffsetY = { (it / 5f).toInt() },
            animationSpec = tween(durationMillis = 300, easing = AnimationSpecs.EmphasizedDecelerate),
        ) + scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(durationMillis = 300, easing = AnimationSpecs.EmphasizedDecelerate),
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 160, easing = AnimationSpecs.ExitEasing),
        ) + slideOutVertically(
            targetOffsetY = { (it / 6f).toInt() },
            animationSpec = tween(durationMillis = 240, easing = AnimationSpecs.EmphasizedAccelerate),
        ) + scaleOut(
            targetScale = 0.99f,
            animationSpec = tween(durationMillis = 240, easing = AnimationSpecs.EmphasizedAccelerate),
        ),
        label = "BottomBarVisibility"
    ) {
        val modifier = Modifier.hazeEffect(hazeState) {
            style = hazeStyle
            blurRadius = 30.dp
            noiseFactor = 0f
            progressive = HazeProgressive.verticalGradient(
                startIntensity = 0f,
                endIntensity = 1f,
                preferPerformance = true,
            )
        }
        val colorScheme = MiuixTheme.colorScheme
        val bottomBarColorScheme = colorScheme.copy(
            onSurfaceContainer = colorScheme.primary,
            onSurfaceContainerVariant = colorScheme.primary.copy(alpha = 0.3f),
        )

        MiuixTheme(colors = bottomBarColorScheme) {
            if (bottomBarFloating) {
                FloatingNavigationBar(
                    modifier = modifier,
                    color = Color.Transparent,
                    showDivider = showDivider,
                    mode = NavigationDisplayMode.IconOnly,
                ) {
                    BottomBarDestination.entries.forEachIndexed { index, destination ->
                        FloatingNavigationBarItem(
                            selected = page == index,
                            onClick = { onItemClick(index) },
                            icon = destination.icon,
                            label = destination.label,
                        )
                    }
                }
            } else {
                NavigationBar(
                    modifier = modifier,
                    color = Color.Transparent,
                    showDivider = showDivider,
                    mode = if (iconWithSelectedLabel) NavigationDisplayMode.IconWithSelectedLabel else NavigationDisplayMode.IconAndText,
                ) {
                    BottomBarDestination.entries.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = page == index,
                            onClick = { onItemClick(index) },
                            icon = destination.icon,
                            label = destination.label,
                        )
                    }
                }
            }
        }
    }
}

enum class BottomBarDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home(MLang.Component.BottomBar.Home, Yume.House),
    Proxy(MLang.Component.BottomBar.Proxy, Yume.`Arrow-down-up`),
    Config(MLang.Component.BottomBar.Config, Yume.`Package-check`),
    Setting(MLang.Component.BottomBar.Setting, Yume.Bolt),
}
