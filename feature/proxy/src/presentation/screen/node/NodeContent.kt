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

package com.github.yumelira.yumebox.presentation.screen.node

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.data.model.normalizeProxySheetHeightFraction
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.isSelectable
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollHorizontal
import top.yukonga.miuix.kmp.utils.overScrollVertical

val NodeSheetContentPadding =
    PaddingValues(start = UiDp.dp0, end = UiDp.dp0, top = UiDp.dp8, bottom = UiDp.dp16)

private fun LazyListState.isScrolledFromTop(): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
}

private fun Modifier.nodeTabHaze(state: HazeState?, style: HazeStyle?): Modifier {
    if (state == null || style == null) return this
    return hazeEffect(state) {
        this.style = style
        blurRadius = UiDp.dp30
        noiseFactor = 0f
        progressive =
            HazeProgressive.verticalGradient(
                startIntensity = 1f,
                endIntensity = 0f,
                preferPerformance = true,
            )
    }
}

@Composable
internal fun NodeTabs(groups: List<ProxyGroupInfo>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val hazeState = LocalTopBarHazeState.current
    val hazeStyle = LocalTopBarHazeStyle.current
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex, groups.size) {
        if (groups.isEmpty()) return@LaunchedEffect
        val target = (selectedIndex - 1).coerceAtLeast(0).coerceAtMost(groups.lastIndex)
        if (target != listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(target)
        }
    }

    LazyRow(
        state = listState,
        modifier =
            Modifier.fillMaxWidth()
                .nodeTabHaze(hazeState, hazeStyle)
                .background(MiuixTheme.colorScheme.surface)
                .overScrollHorizontal(),
        contentPadding =
            PaddingValues(start = UiDp.dp14, end = UiDp.dp14, top = UiDp.dp10, bottom = UiDp.dp10),
        horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
        overscrollEffect = null,
    ) {
        itemsIndexed(groups, key = { _, group -> group.name }) { index, group ->
            val selected = index == selectedIndex
            val background =
                if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surface
                }
            val textColor =
                if (selected) {
                    MiuixTheme.colorScheme.onPrimary
                } else {
                    MiuixTheme.colorScheme.onSurface
                }

            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(UiDp.dp999))
                        .background(background)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = UiDp.dp11, vertical = UiDp.dp6)
            ) {
                Text(text = group.name, color = textColor, style = MiuixTheme.textStyles.footnote1)
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
internal fun rememberNodeSheetHeight(sheetHeightFraction: Float): Dp {
    val normalized = normalizeProxySheetHeightFraction(sheetHeightFraction)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    return remember(screenHeightDp, normalized) { screenHeightDp.dp * normalized }
}

@Composable
internal fun NodeGroupSheetContent(
    groups: List<ProxyGroupInfo>,
    testingGroupNames: Set<String>,
    sheetHeightFraction: Float,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)

    LaunchedEffect(testingGroupNames) {
        if (testingGroupNames.isNotEmpty() && listState.isScrolledFromTop()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(sheetHeight).overScrollVertical(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        contentPadding = NodeSheetContentPadding,
        overscrollEffect = null,
    ) {
        nodeGroupItems(
            groups = groups,
            onGroupClick = onGroupClick,
            testingGroupNames = testingGroupNames,
            itemVerticalPadding = UiDp.dp0,
        )
    }
}

@Composable
fun NodeSheetContent(
    group: ProxyGroupInfo,
    onSelectProxy: (String) -> Unit,
    isDelayTesting: Boolean,
    testingProxyNames: Set<String>,
    onTestDelay: () -> Unit,
    onTestProxyDelay: (String) -> Unit,
    sheetHeightFraction: Float,
    listState: LazyListState = rememberLazyListState(),
    singleNodeTestEnabled: Boolean = true,
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)

    LaunchedEffect(isDelayTesting) {
        if (isDelayTesting && listState.isScrolledFromTop()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(sheetHeight).overScrollVertical(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        contentPadding = NodeSheetContentPadding,
        overscrollEffect = null,
    ) {
        item(key = "__refresh_indicator__") {
            AnimatedVisibility(
                visible = isDelayTesting,
                enter =
                    expandVertically(
                        animationSpec = tween(durationMillis = 200),
                        expandFrom = Alignment.Top,
                    ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                exit =
                    shrinkVertically(
                        animationSpec = tween(durationMillis = 200),
                        shrinkTowards = Alignment.Top,
                    ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = UiDp.dp12),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiDp.dp6),
                ) {
                    InfiniteProgressIndicator(modifier = Modifier.size(UiDp.dp24))
                    Text(
                        text = MLang.Proxy.Testing.InProgress,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        nodeGridItems(
            proxies = group.proxies,
            selectedProxyName = group.now,
            onProxyClick = { proxyName ->
                if (group.isSelectable) {
                    onSelectProxy(proxyName)
                } else {
                    onTestDelay()
                }
            },
            isDelayTesting = isDelayTesting,
            testingProxyNames = testingProxyNames,
            onSingleNodeTestClick = onTestProxyDelay,
            singleNodeTestEnabled = singleNodeTestEnabled,
        )
    }
}
