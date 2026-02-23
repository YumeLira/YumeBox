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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalTopBarHazeState = compositionLocalOf<HazeState?> { null }
val LocalTopBarHazeStyle = compositionLocalOf<HazeStyle?> { null }

private fun Modifier.topBarHazeEffect(
    state: HazeState?,
    style: HazeStyle?,
): Modifier {
    if (state == null || style == null) return this

    return hazeEffect(state) {
        this.style = style
        blurRadius = 30.dp
        noiseFactor = 0f
        progressive = HazeProgressive.verticalGradient(
            startIntensity = 1f,
            endIntensity = 0f,
            preferPerformance = true,
        )
    }
}

@Composable
fun TopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val hazeState = LocalTopBarHazeState.current
    val hazeStyle = LocalTopBarHazeStyle.current
    val hazeEnabled = hazeState != null && hazeStyle != null

    TopAppBar(
        title = title,
        modifier = modifier.topBarHazeEffect(hazeState, hazeStyle),
        color = if (hazeEnabled) Color.Transparent else MiuixTheme.colorScheme.surface,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun SmallTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val hazeState = LocalTopBarHazeState.current
    val hazeStyle = LocalTopBarHazeStyle.current
    val hazeEnabled = hazeState != null && hazeStyle != null

    SmallTopAppBar(
        title = title,
        modifier = modifier.topBarHazeEffect(hazeState, hazeStyle),
        color = if (hazeEnabled) Color.Transparent else MiuixTheme.colorScheme.surface,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}
