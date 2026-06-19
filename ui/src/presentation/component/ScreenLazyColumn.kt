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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ScreenLazyColumn(
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    bottomPadding: Dp = UiDp.dp0,
    topPadding: Dp = UiDp.dp0,
    enableGlobalScroll: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState(),
    onScrollDirectionChanged: ((Boolean) -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val bottomBarScrollBehavior =
        if (enableGlobalScroll) {
            LocalBottomBarScrollBehavior.current?.withLazyListState(lazyListState)
        } else {
            null
        }
    val topBarHazeState = LocalTopBarHazeState.current
    val latestScrollDirectionCallback by rememberUpdatedState(onScrollDirectionChanged)
    var lastHiddenState by remember(lazyListState) { mutableStateOf(false) }
    val resolvedContentPadding =
        remember(contentPadding, innerPadding, topPadding, bottomPadding) {
            contentPadding
                ?: PaddingValues(
                    top = innerPadding.calculateTopPadding() + topPadding,
                    bottom = innerPadding.calculateBottomPadding() + bottomPadding,
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                )
        }
    val fabScrollObserver =
        remember(lazyListState) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val scrollDirectionCallback =
                        latestScrollDirectionCallback ?: return Offset.Zero
                    val hiddenState =
                        when {
                            available.y < -1f -> true
                            available.y > 1f -> false
                            else -> return Offset.Zero
                        }
                    if (hiddenState != lastHiddenState) {
                        lastHiddenState = hiddenState
                        scrollDirectionCallback(hiddenState)
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    if (consumed.y > 1f || available.y > 1f) {
                        latestScrollDirectionCallback?.invoke(false)
                        lastHiddenState = false
                    }
                    return Velocity.Zero
                }
            }
        }

    LaunchedEffect(lazyListState, latestScrollDirectionCallback) {
        val scrollDirectionCallback = onScrollDirectionChanged ?: return@LaunchedEffect
        scrollDirectionCallback(false)
        lastHiddenState = false
    }

    LazyColumn(
        state = lazyListState,
        modifier =
            modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .let { mod ->
                    if (topBarHazeState != null) mod.hazeSource(state = topBarHazeState) else mod
                }
                .nestedScroll(fabScrollObserver)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .let { mod ->
                    if (enableGlobalScroll && bottomBarScrollBehavior != null) {
                        mod.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
                    } else mod
                },
        contentPadding = resolvedContentPadding,
        overscrollEffect = null,
        content = content,
    )
}

@Composable
fun combinePaddingValues(localPadding: PaddingValues, mainPadding: PaddingValues): PaddingValues {
    return PaddingValues(
        top = localPadding.calculateTopPadding(),
        bottom =
            localPadding.calculateBottomPadding() +
                mainPadding.calculateBottomPadding() +
                LocalSpacing.current.space12,
        start = localPadding.calculateStartPadding(LayoutDirection.Ltr),
        end = localPadding.calculateEndPadding(LayoutDirection.Ltr),
    )
}

@Composable
fun rememberStandalonePageMainPadding(reservedBottomBarHeight: Dp? = null): PaddingValues {
    val resolvedReservedBottomBarHeight =
        reservedBottomBarHeight ?: rememberBottomBarReservedHeight()
    return remember(resolvedReservedBottomBarHeight) {
        PaddingValues(bottom = resolvedReservedBottomBarHeight)
    }
}
