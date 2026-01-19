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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Arrow-down-up`
import com.github.yumelira.yumebox.presentation.icon.yume.Bolt
import com.github.yumelira.yumebox.presentation.icon.yume.House
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }

@Composable
fun BottomBar(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    isVisible: Boolean = true,
) {
    LocalContext.current
    val pagerState = LocalPagerState.current
    val page by remember(pagerState) {
        derivedStateOf { if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage }
    }
    val handlePageChange = LocalHandlePageChange.current
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val bottomBarFloating by appSettingsViewModel.bottomBarFloating.state.collectAsState()
    val showDivider by appSettingsViewModel.showDivider.state.collectAsState()

    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = destination.label,
            icon = destination.icon,
        )
    }

    val onItemClick: (Int) -> Unit = { index ->
        handlePageChange(index)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(200, easing = AnimationSpecs.EnterEasing),
        ) + slideInVertically(
            initialOffsetY = { (it / 6).toInt() },
            animationSpec = tween(240, easing = AnimationSpecs.EmphasizedDecelerate),
        ) + scaleIn(
            initialScale = 0.97f,
            animationSpec = tween(240, easing = AnimationSpecs.EmphasizedDecelerate),
        ),
        exit = fadeOut(
            animationSpec = tween(160, easing = AnimationSpecs.ExitEasing),
        ) + slideOutVertically(
            targetOffsetY = { (it / 8).toInt() },
            animationSpec = tween(180, easing = AnimationSpecs.EmphasizedAccelerate),
        ) + scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(180, easing = AnimationSpecs.ExitEasing),
        ),
        label = "BottomBarVisibility"
    ) {
        val modifier = Modifier.hazeEffect(hazeState) {
            style = hazeStyle
            blurRadius = 30.dp
            noiseFactor = 0f
        }

        if (bottomBarFloating) {
            FloatingNavigationBar(
                modifier = modifier,
                color = Color.Transparent,
                items = items,
                selected = page,
                onClick = onItemClick,
                showDivider = showDivider,
            )
        } else {
            NavigationBar(
                modifier = modifier,
                color = Color.Transparent,
                items = items,
                selected = page,
                onClick = onItemClick,
                showDivider = showDivider,
            )
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
