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

package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.WebViewUtils.getLocalBaseUrl
import com.github.yumelira.yumebox.common.util.WebViewUtils.getPanelUrl
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.ProxyGroupTabs
import com.github.yumelira.yumebox.presentation.component.ProxyNodeCard
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.`Squares-exclude`
import com.github.yumelira.yumebox.presentation.icon.yume.Zap
import com.github.yumelira.yumebox.presentation.icon.yume.Zashboard
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.HomeViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import com.github.yumelira.yumebox.presentation.webview.WebViewActivity
import com.ramcosta.composedestinations.generated.destinations.ProvidersScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    koinViewModel<HomeViewModel>()
    val featureViewModel = koinViewModel<FeatureViewModel>()

    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val selectedGroupIndex by proxyViewModel.selectedGroupIndex.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val uiState by proxyViewModel.uiState.collectAsState()
    val selectedPanelType by featureViewModel.selectedPanelType.state.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val showBottomSheet = rememberSaveable { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val groupCount = proxyGroups.size
    val pagerState = rememberPagerState(initialPage = selectedGroupIndex, pageCount = { groupCount })

    // 代理组状态由 ProxyStateRepository 自动同步，无需手动刷新

    PagerGroupSync(pagerState, selectedGroupIndex, groupCount, proxyViewModel::setSelectedGroup)

    // 监听操作完成，关闭刷新指示器
    LaunchedEffect(uiState.isLoading, isRefreshing) {
        if (!uiState.isLoading && isRefreshing) {
            isRefreshing = false
        }
    }


    val currentGroup by remember(proxyGroups, selectedGroupIndex) {
        derivedStateOf { proxyGroups.getOrNull(selectedGroupIndex) }
    }

    val onTestDelay = remember(currentGroup?.name) {
        currentGroup?.name?.let { name -> { proxyViewModel.testDelay(name) } }
    }

    val onRefresh: () -> Unit = remember(currentGroup?.name) {
        {
            if (!isRefreshing && currentGroup != null) {
                isRefreshing = true
                // 测试当前代理组的延迟
                currentGroup!!.name.let { proxyViewModel.testDelay(it) }
            }
        }
    }

    Scaffold(
        topBar = {
            ProxyTopBar(
                scrollBehavior = scrollBehavior,
                context = context,
                selectedPanelType = selectedPanelType,
                onNavigateToProviders = { navigator.navigate(ProvidersScreenDestination) { launchSingleTop = true } },
                onTestDelay = onTestDelay,
                onShowSettings = { showBottomSheet.value = true }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (proxyGroups.isEmpty()) {
                CenteredText(
                    firstLine = MLang.Proxy.Empty.NoNodes,
                    secondLine = MLang.Proxy.Empty.Hint
                )
            } else {
                ProxyContent(
                    proxyGroups = proxyGroups,
                    selectedGroupIndex = selectedGroupIndex,
                    displayMode = displayMode,
                    pagerState = pagerState,
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    mainInnerPadding = mainInnerPadding,
                    isRefreshing = isRefreshing,
                    pullToRefreshState = pullToRefreshState,
                    onRefresh = onRefresh,
                    onTabSelected = proxyViewModel::setSelectedGroup,
                    onProxySelect = proxyViewModel::selectProxy
                )
            }
        }

        SuperBottomSheet(
            show = showBottomSheet,
            title = MLang.Proxy.Settings.Title,
            onDismissRequest = { showBottomSheet.value = false },
            insideMargin = DpSize(32.dp, 16.dp),
        ) {
            ProxySettingsContent(
                proxyViewModel = proxyViewModel,
                onDismiss = { showBottomSheet.value = false }
            )
        }
    }
}

@Composable
private fun PagerGroupSync(
    pagerState: PagerState,
    selectedGroupIndex: Int,
    groupCount: Int,
    setSelectedGroup: (Int) -> Unit
) {
    LaunchedEffect(groupCount) {
        if (groupCount in 1..selectedGroupIndex) {
            setSelectedGroup(0)
        }
    }

    // 自定义页面切换动画规格
    val pagerAnimationSpec = remember {
        tween<Float>(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        )
    }

    LaunchedEffect(selectedGroupIndex, groupCount) {
        if (groupCount == 0) return@LaunchedEffect

        val targetPage = selectedGroupIndex.coerceIn(0, groupCount - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = pagerAnimationSpec
            )
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && groupCount > 0) {
            val targetPage = pagerState.currentPage
            if (targetPage != selectedGroupIndex && targetPage in 0 until groupCount) {
                setSelectedGroup(targetPage)
            }
        }
    }
}

@Composable
private fun ProxyTopBar(
    scrollBehavior: ScrollBehavior,
    context: android.content.Context,
    selectedPanelType: Int,
    onNavigateToProviders: () -> Unit,
    onTestDelay: (() -> Unit)?,
    onShowSettings: () -> Unit
) {
    TopBar(
        title = MLang.Proxy.Title,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(
                    modifier = Modifier.padding(start = 24.dp),
                    onClick = onNavigateToProviders
                ) {
                    Icon(Yume.`Squares-exclude`, contentDescription = MLang.Proxy.Action.ExternalResources)
                }

                IconButton(
                    onClick = {
                        val panelUrl = getPanelUrl(context, selectedPanelType)
                        val webViewUrl = panelUrl.ifEmpty {
                            val localUrl = getLocalBaseUrl(context)
                            if (localUrl.isNotEmpty()) localUrl + "index.html" else ""
                        }
                        if (webViewUrl.isNotEmpty()) {
                            WebViewActivity.start(context, webViewUrl)
                        }
                    }
                ) {
                    Icon(Yume.Zashboard, contentDescription = MLang.Proxy.Action.Panel)
                }
            }
        },
        actions = {
            IconButton(
                modifier = Modifier.padding(end = 16.dp),
                onClick = { onTestDelay?.invoke() }
            ) {
                Icon(Yume.Zap, contentDescription = MLang.Proxy.Action.Test)
            }

            IconButton(
                modifier = Modifier.padding(end = 24.dp),
                onClick = onShowSettings
            ) {
                Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Action.Settings)
            }
        }
    )
}

@Composable
private fun ProxyContent(
    proxyGroups: List<ProxyGroupInfo>,
    selectedGroupIndex: Int,
    displayMode: ProxyDisplayMode,
    pagerState: PagerState,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    mainInnerPadding: PaddingValues,
    isRefreshing: Boolean,
    pullToRefreshState: PullToRefreshState,
    onRefresh: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onProxySelect: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = innerPadding.calculateTopPadding() + 4.dp)
        ) {
            ProxyGroupTabs(
                groups = proxyGroups,
                selectedIndex = selectedGroupIndex,
                onTabSelected = onTabSelected
            )
        }

        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf(
                MLang.Proxy.PullToRefresh.PullToTest,
                MLang.Proxy.PullToRefresh.ReleaseToTest,
                MLang.Proxy.Refresh.Refreshing,
                MLang.Proxy.PullToRefresh.DelaySuccess
            )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val group = proxyGroups.getOrNull(page) ?: return@HorizontalPager

                ProxyGroupPage(
                    group = group,
                    displayMode = displayMode,
                    scrollBehavior = scrollBehavior,
                    mainInnerPadding = mainInnerPadding,
                    onProxySelect = onProxySelect
                )
            }
        }
    }
}

@Composable
private fun ProxyGroupPage(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    scrollBehavior: ScrollBehavior,
    mainInnerPadding: PaddingValues,
    onProxySelect: (String, String) -> Unit
) {
    val groupName = group.name
    val isSelectable = group.type == Proxy.Type.Selector

    val onProxyClick = remember(groupName, isSelectable) {
        if (isSelectable) { proxyName: String -> onProxySelect(groupName, proxyName) } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = mainInnerPadding.calculateBottomPadding()
        ),
        overscrollEffect = null
    ) {
        if (group.chainPath.isNotEmpty()) {
            item(key = "chain_${groupName}") {
                ProxyChainIndicator(
                    chain = group.chainPath,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (group.now.isBlank()) {
            item(key = "direct_${groupName}") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = MLang.Proxy.Mode.Direct,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item(key = "spacer_${groupName}") {
            Spacer(modifier = Modifier.height(4.dp))
        }

        proxyNodeGridItems(
            groupName = groupName,
            proxies = group.proxies,
            selectedProxyName = group.now,
            displayMode = displayMode,
            onProxyClick = onProxyClick
        )
    }
}

@Composable
private fun ProxySettingsContent(
    proxyViewModel: ProxyViewModel,
    onDismiss: () -> Unit
) {
    val currentMode by proxyViewModel.currentMode.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()

    val modeTabs = remember { listOf(MLang.Proxy.Mode.Rule, MLang.Proxy.Mode.Global, MLang.Proxy.Mode.Direct) }
    val modeValues = remember { listOf(TunnelState.Mode.Rule, TunnelState.Mode.Global, TunnelState.Mode.Direct) }
    val sortTabs = remember { ProxySortMode.entries.map { it.displayName } }
    val displayTabs = remember { ProxyDisplayMode.entries.map { it.displayName } }

    Column {
        Text(
            text = MLang.Proxy.Settings.ProxyMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = modeTabs,
            selectedTabIndex = modeValues.indexOf(currentMode).coerceAtLeast(0),
            onTabSelected = { index ->
                if (index < modeValues.size) {
                    proxyViewModel.patchMode(modeValues[index])
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = MLang.Proxy.Settings.SortMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = sortTabs,
            selectedTabIndex = sortMode.ordinal,
            onTabSelected = { index ->
                if (index < ProxySortMode.entries.size) {
                    proxyViewModel.setSortMode(ProxySortMode.entries[index])
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = MLang.Proxy.Settings.DisplayMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = displayTabs,
            selectedTabIndex = displayMode.ordinal,
            onTabSelected = { index ->
                if (index < ProxyDisplayMode.entries.size) {
                    proxyViewModel.setDisplayMode(ProxyDisplayMode.entries[index])
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(MLang.Component.Button.Cancel)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background)
            }
        }
    }
}


fun LazyListScope.proxyNodeGridItems(
    groupName: String,
    proxies: List<Proxy>,
    selectedProxyName: String,
    displayMode: ProxyDisplayMode,
    onProxyClick: ((String) -> Unit)?
) {
    val columns = if (displayMode.isSingleColumn) 1 else 2
    val showDetail = displayMode.showDetail

    if (columns == 1) {
        items(
            count = proxies.size,
            key = { index -> "${groupName}_${proxies[index].name}_$index" }
        ) { index ->
            val proxy = proxies[index]
            ProxyNodeCard(
                proxy = proxy,
                isSelected = proxy.name == selectedProxyName,
                onClick = onProxyClick?.let { { it(proxy.name) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                isSingleColumn = true,
                showDetail = showDetail
            )
        }
    } else {
        val rowCount = (proxies.size + 1) / 2
        items(
            count = rowCount,
            key = { rowIndex ->
                val startIndex = rowIndex * 2
                val first = proxies.getOrNull(startIndex)?.name ?: ""
                val second = proxies.getOrNull(startIndex + 1)?.name ?: ""
                "${groupName}_${first}_${second}_$rowIndex"
            }
        ) { rowIndex ->
            val startIndex = rowIndex * 2
            val firstProxy = proxies[startIndex]
            val secondProxy = proxies.getOrNull(startIndex + 1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProxyNodeCard(
                        proxy = firstProxy,
                        isSelected = firstProxy.name == selectedProxyName,
                        onClick = onProxyClick?.let { { it(firstProxy.name) } },
                        isSingleColumn = false,
                        showDetail = showDetail
                    )
                }

                if (secondProxy != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProxyNodeCard(
                            proxy = secondProxy,
                            isSelected = secondProxy.name == selectedProxyName,
                            onClick = onProxyClick?.let { { it(secondProxy.name) } },
                            isSingleColumn = false,
                            showDetail = showDetail
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProxyChainIndicator(
    chain: List<String>, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chain.forEachIndexed { index, nodeName ->
                Text(
                    text = nodeName, style = MiuixTheme.textStyles.body2, color = if (index == chain.lastIndex) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    }
                )

                if (index < chain.lastIndex) {
                    Text(
                        text = "→",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }
    }
}
