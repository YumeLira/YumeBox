package com.github.yumelira.yumebox.presentation.screen.node

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.normalizeProxySheetHeightFraction
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import dev.oom_wg.purejoy.mlang.MLang
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

val NodeSheetContentPadding = PaddingValues(
    start = 0.dp,
    end = 0.dp,
    top = 8.dp,
    bottom = 16.dp,
)

private fun Modifier.nodeTabHaze(state: HazeState?, style: HazeStyle?): Modifier {
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
internal fun NodeTabs(
    groups: List<ProxyGroupInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
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
        modifier = Modifier
            .fillMaxWidth()
            .nodeTabHaze(hazeState, hazeStyle)
            .background(MiuixTheme.colorScheme.surface),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(groups, key = { _, group -> group.name }) { index, group ->
            val selected = index == selectedIndex
            val background = if (selected) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.surface
            }
            val textColor = if (selected) {
                MiuixTheme.colorScheme.onPrimary
            } else {
                MiuixTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(background)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    text = group.name,
                    color = textColor,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
}

@Composable
internal fun rememberNodeSheetHeight(sheetHeightFraction: Float): Dp {
    val normalized = normalizeProxySheetHeightFraction(sheetHeightFraction)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    return remember(screenHeightDp, normalized) { screenHeightDp.dp * normalized }
}

@Composable
internal fun NodeGroupSheetContent(
    groups: List<ProxyGroupInfo>,
    displayMode: ProxyDisplayMode,
    testingGroupNames: Set<String>,
    sheetHeightFraction: Float,
    onRefreshAllGroups: () -> Unit,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupDelayClick: (ProxyGroupInfo) -> Unit,
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)
    val refreshTexts = remember {
        listOf(
            MLang.Proxy.PullToRefresh.PullToTestAllGroups,
            MLang.Proxy.PullToRefresh.ReleaseToTestAllGroups,
            MLang.Proxy.PullToRefresh.TestingAllGroups,
            MLang.Proxy.Testing.RequestSent,
        )
    }
    val pullToRefreshState = rememberPullToRefreshState()
    var pullRefreshing by remember { mutableStateOf(false) }
    var pullRefreshObservedTesting by remember { mutableStateOf(false) }
    LaunchedEffect(pullRefreshing, testingGroupNames) {
        if (!pullRefreshing) return@LaunchedEffect
        val isTesting = testingGroupNames.isNotEmpty()
        if (!pullRefreshObservedTesting) {
            if (isTesting) {
                pullRefreshObservedTesting = true
            }
            return@LaunchedEffect
        }
        if (!isTesting) {
            pullRefreshing = false
            pullRefreshObservedTesting = false
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight),
    ) {
        PullToRefresh(
            isRefreshing = pullRefreshing,
            onRefresh = {
                if (pullRefreshing) return@PullToRefresh
                pullRefreshObservedTesting = false
                pullRefreshing = true
                onRefreshAllGroups()
            },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = refreshTexts,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = NodeSheetContentPadding,
                overscrollEffect = null,
            ) {
                nodeGroupItems(
                    groups = groups,
                    displayMode = displayMode,
                    onGroupClick = onGroupClick,
                    onGroupDelayClick = onGroupDelayClick,
                    testingGroupNames = testingGroupNames,
                    onGroupBoundsChanged = null,
                    itemVerticalPadding = 0.dp,
                )
            }
        }
    }
}

@Composable
fun NodeSheetContent(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    onSelectProxy: (String) -> Unit,
    isDelayTesting: Boolean,
    onTestDelay: () -> Unit,
    sheetHeightFraction: Float,
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)
    val refreshTexts = remember {
        listOf(
            MLang.Proxy.PullToRefresh.PullToTestCurrentGroup,
            MLang.Proxy.PullToRefresh.ReleaseToTestCurrentGroup,
            MLang.Proxy.PullToRefresh.TestingCurrentGroup,
            MLang.Proxy.Testing.RequestSent,
        )
    }
    val pullToRefreshState = rememberPullToRefreshState()
    var pullRefreshing by remember { mutableStateOf(false) }
    var pullRefreshObservedTesting by remember { mutableStateOf(false) }
    var pullRefreshListStateVersion by remember(group.name) { mutableStateOf(0) }
    LaunchedEffect(pullRefreshing, isDelayTesting) {
        if (!pullRefreshing) return@LaunchedEffect
        if (!pullRefreshObservedTesting) {
            if (isDelayTesting) {
                pullRefreshObservedTesting = true
            }
            return@LaunchedEffect
        }
        if (!isDelayTesting) {
            pullRefreshListStateVersion += 1
            pullRefreshing = false
            pullRefreshObservedTesting = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight),
        contentAlignment = Alignment.Center,
    ) {
        PullToRefresh(
            isRefreshing = pullRefreshing,
            onRefresh = {
                if (pullRefreshing) return@PullToRefresh
                pullRefreshObservedTesting = false
                pullRefreshing = true
                onTestDelay()
            },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = refreshTexts,
        ) {
            NodeList(
                group = group,
                displayMode = displayMode,
                onProxyClick = { proxyName ->
                    if (group.type == Proxy.Type.Selector) {
                        onSelectProxy(proxyName)
                    } else {
                        onTestDelay()
                    }
                },
                isDelayTesting = isDelayTesting,
                onTestDelay = onTestDelay,
                listStateKeyPrefix = "node_sheet:$pullRefreshListStateVersion",
                contentPadding = NodeSheetContentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun NodeList(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    onProxyClick: (String) -> Unit,
    isDelayTesting: Boolean,
    onTestDelay: () -> Unit,
    listStateKeyPrefix: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val shouldShowLoading = remember(group.proxies.size) {
        group.proxies.size > 10
    }
    var showContent by remember(group.name, shouldShowLoading) {
        mutableStateOf(!shouldShowLoading)
    }

    LaunchedEffect(group.name, shouldShowLoading) {
        if (!shouldShowLoading) {
            showContent = true
            return@LaunchedEffect
        }
        showContent = false
        delay(450)
        showContent = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!showContent && shouldShowLoading) {
            InfiniteProgressIndicator()
        } else {
            NodeGrid(
                proxies = group.proxies,
                selectedProxyName = group.now,
                displayMode = displayMode,
                onProxyClick = onProxyClick,
                isDelayTesting = isDelayTesting,
                onDelayTestClick = onTestDelay,
                listStateKey = "$listStateKeyPrefix:${group.name}",
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
