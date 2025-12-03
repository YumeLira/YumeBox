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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ScreenLazyColumn(
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    topPadding: Dp = 0.dp,
    enableBottomBarAutoHide: Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()

    if (enableBottomBarAutoHide) {
        var previousScrollOffset by remember { mutableIntStateOf(0) }
        var previousFirstVisibleIndex by remember { mutableIntStateOf(0) }
        
        val isScrollingDown by remember {
            derivedStateOf {
                val currentFirstVisibleIndex = listState.firstVisibleItemIndex
                val currentScrollOffset = listState.firstVisibleItemScrollOffset
                
                val scrollingDown = when {
                    currentFirstVisibleIndex > previousFirstVisibleIndex -> true
                    currentFirstVisibleIndex < previousFirstVisibleIndex -> false
                    else -> currentScrollOffset > previousScrollOffset
                }
                
                previousFirstVisibleIndex = currentFirstVisibleIndex
                previousScrollOffset = currentScrollOffset
                
                scrollingDown
            }
        }
        
        LaunchedEffect(isScrollingDown) {
            BottomBarVisibility.toggle(!isScrollingDown)
        }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        state = listState,
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + topPadding,
            bottom = innerPadding.calculateBottomPadding() + bottomPadding,
        ),
        overscrollEffect = null,
        content = content,
    )
}

@Composable
fun combinePaddingValues(
    localPadding: PaddingValues,
    mainPadding: PaddingValues,
): PaddingValues {
    return PaddingValues(
        top = localPadding.calculateTopPadding(),
        bottom = localPadding.calculateBottomPadding() + mainPadding.calculateBottomPadding() + LocalSpacing.current.md,
        start = localPadding.calculateStartPadding(LayoutDirection.Ltr),
        end = localPadding.calculateEndPadding(LayoutDirection.Ltr),
    )
}
