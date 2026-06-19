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

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

@Stable
class BottomBarScrollBehavior {
    var isBottomBarVisible by mutableStateOf(true)
        private set

    var isAutoHideEnabled by mutableStateOf(true)

    private val scrollThreshold = 12f
    private val flingThreshold = 120f

    private var lastToggleTime = 0L
    private val toggleDelay = 150L

    private var accumulatedScroll = 0f

    val nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isAutoHideEnabled) return Offset.Zero
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                updateVisibilityFromDelta(available.y)
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!isAutoHideEnabled) return Offset.Zero
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                val delta =
                    when {
                        abs(consumed.y) >= 0.5f -> consumed.y
                        abs(available.y) >= 0.5f -> available.y
                        else -> 0f
                    }
                if (delta == 0f) return Offset.Zero

                updateVisibilityFromDelta(delta)
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!isAutoHideEnabled) return Velocity.Zero

                accumulatedScroll = 0f
                val velocityY =
                    when {
                        abs(consumed.y) >= flingThreshold -> consumed.y
                        abs(available.y) >= flingThreshold -> available.y
                        else -> 0f
                    }

                when {
                    velocityY < 0f -> hideBottomBar()
                    velocityY > 0f -> showBottomBar()
                }
                return Velocity.Zero
            }
        }

    private fun updateVisibilityFromDelta(deltaY: Float) {
        if (abs(deltaY) < 0.5f) return

        if ((accumulatedScroll > 0f && deltaY < 0f) || (accumulatedScroll < 0f && deltaY > 0f)) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += deltaY

        if (abs(accumulatedScroll) >= scrollThreshold) {
            if (accumulatedScroll < 0f) hideBottomBar() else showBottomBar()
            accumulatedScroll = 0f
        }
    }

    fun showBottomBar() {
        val currentTime = System.currentTimeMillis()
        if (!isBottomBarVisible && currentTime - lastToggleTime >= toggleDelay) {
            isBottomBarVisible = true
            lastToggleTime = currentTime
        }
    }

    fun hideBottomBar() {
        val currentTime = System.currentTimeMillis()
        if (isBottomBarVisible && currentTime - lastToggleTime >= toggleDelay) {
            isBottomBarVisible = false
            lastToggleTime = currentTime
        }
    }
}

@Composable
fun rememberBottomBarScrollBehavior(autoHideEnabled: Boolean = true): BottomBarScrollBehavior {
    return remember(autoHideEnabled) {
        BottomBarScrollBehavior().apply { isAutoHideEnabled = autoHideEnabled }
    }
}

@Composable
fun BottomBarScrollBehavior.withLazyListState(listState: LazyListState): BottomBarScrollBehavior {
    val isAtTop by
        remember(listState) {
            derivedStateOf {
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            }
        }

    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            showBottomBar()
        }
    }

    return this
}

val LocalBottomBarScrollBehavior = compositionLocalOf<BottomBarScrollBehavior?> { null }
