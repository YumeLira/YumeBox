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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.PROXY_SHEET_HEIGHT_FRACTION_MAX
import com.github.yumelira.yumebox.domain.model.PROXY_SHEET_HEIGHT_FRACTION_MIN
import com.github.yumelira.yumebox.domain.model.normalizeProxySheetHeightFraction
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.Speed
import com.github.yumelira.yumebox.presentation.icon.yume.`Squares-exclude`
import com.github.yumelira.yumebox.presentation.icon.yume.Zashboard
import com.github.yumelira.yumebox.presentation.screen.node.nodeGroupItems
import com.github.yumelira.yumebox.presentation.screen.node.NodeSortPopup
import com.github.yumelira.yumebox.presentation.screen.node.NodeSheetContent
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import dev.chrisbanes.haze.hazeSource
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

private fun fractionToSliderValue(fraction: Float): Float {
    val normalized = normalizeProxySheetHeightFraction(fraction)
    val range = PROXY_SHEET_HEIGHT_FRACTION_MAX - PROXY_SHEET_HEIGHT_FRACTION_MIN
    if (range <= 0f) return 0f
    return ((normalized - PROXY_SHEET_HEIGHT_FRACTION_MIN) / range).coerceIn(0f, 1f)
}

private fun sliderValueToFraction(sliderValue: Float): Float {
    val range = PROXY_SHEET_HEIGHT_FRACTION_MAX - PROXY_SHEET_HEIGHT_FRACTION_MIN
    return normalizeProxySheetHeightFraction(
        PROXY_SHEET_HEIGHT_FRACTION_MIN + sliderValue.coerceIn(0f, 1f) * range
    )
}

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues,
    onNavigateToProviders: () -> Unit,
    onOpenPanel: () -> Unit,
    isActive: Boolean
) {
    val proxyViewModel = koinViewModel<ProxyViewModel>()

    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val sheetHeightFraction by proxyViewModel.sheetHeightFraction.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val topBarHazeState = LocalTopBarHazeState.current

    val showSettingsBottomSheet = rememberSaveable { mutableStateOf(false) }
    val showGroupBottomSheet = rememberSaveable { mutableStateOf(false) }
    val showSortPopup = rememberSaveable { mutableStateOf(false) }
    var sheetGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetGroupSnapshot by remember { mutableStateOf<ProxyGroupInfo?>(null) }
    val onTestDelay = remember { { proxyViewModel.testDelay() } }
    val proxyGroupsByName = remember(proxyGroups) { proxyGroups.associateBy { it.name } }
    val sheetGroup by remember(sheetGroupName, proxyGroupsByName) {
        derivedStateOf {
            val name = sheetGroupName ?: return@derivedStateOf null
            proxyGroupsByName[name]
        }
    }
    LaunchedEffect(sheetGroup) {
        sheetGroup?.let { sheetGroupSnapshot = it }
    }
    val displaySheetGroup = sheetGroup ?: sheetGroupSnapshot
    LaunchedEffect(showGroupBottomSheet.value) {
        if (!showGroupBottomSheet.value) {
            delay(220)
            if (!showGroupBottomSheet.value) {
                sheetGroupName = null
                showSortPopup.value = false
            }
        }
    }

    LaunchedEffect(isActive) {
        proxyViewModel.ensureCoreLoaded(isActive)
    }

    Scaffold(
        topBar = {
            ProxyTopBar(
                scrollBehavior = scrollBehavior,
                onNavigateToProviders = onNavigateToProviders,
                onOpenPanel = onOpenPanel,
                onTestDelay = onTestDelay,
                onShowSettings = { showSettingsBottomSheet.value = true })
        }) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .let { mod -> if (topBarHazeState != null) mod.hazeSource(state = topBarHazeState) else mod }) {
            if (proxyGroups.isEmpty()) {
                CenteredText(
                    firstLine = MLang.Proxy.Empty.NoNodes, secondLine = MLang.Proxy.Empty.Hint
                )
            } else {
                ProxyContent(
                    proxyGroups = proxyGroups,
                    displayMode = displayMode,
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    mainInnerPadding = mainInnerPadding,
                    testingGroupNames = testingGroupNames,
                    onPullRefresh = onTestDelay,
                    onGroupClick = { group ->
                        sheetGroupName = group.name
                        showGroupBottomSheet.value = true
                    },
                    onGroupDelayClick = { group ->
                        proxyViewModel.testDelay(group.name)
                    },
                    onGroupBoundsChanged = { _, _ -> },
                )
            }
        }

        WindowBottomSheet(
            show = showSettingsBottomSheet,
            title = MLang.Proxy.Settings.Title,
            onDismissRequest = { showSettingsBottomSheet.value = false },
            insideMargin = DpSize(32.dp, 16.dp),
            enableNestedScroll = false
        ) {
            ProxySettingsContent(
                proxyViewModel = proxyViewModel, onDismiss = { showSettingsBottomSheet.value = false })
        }

        WindowBottomSheet(
            show = showGroupBottomSheet,
            title = displaySheetGroup?.name.orEmpty(),
            startAction = {
                IconButton(onClick = { showSortPopup.value = true }) {
                    Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Settings.SortMode)
                }

                NodeSortPopup(
                    show = showSortPopup,
                    onDismiss = { showSortPopup.value = false },
                    sortMode = sortMode,
                    onSortSelected = proxyViewModel::setSortMode,
                )
            },
            endAction = {
                val group = displaySheetGroup ?: return@WindowBottomSheet
                IconButton(onClick = { proxyViewModel.testDelay(group.name) }) {
                    Icon(Yume.Speed, contentDescription = MLang.Proxy.Action.Test)
                }
            },
            onDismissRequest = {
                showSortPopup.value = false
                showGroupBottomSheet.value = false
            },
            insideMargin = DpSize(16.dp, 16.dp),
            enableNestedScroll = false
        ) {
            val group = displaySheetGroup ?: return@WindowBottomSheet
            NodeSheetContent(
                group = group,
                displayMode = displayMode,
                onSelectProxy = { proxyName ->
                    proxyViewModel.selectProxy(group.name, proxyName)
                    showGroupBottomSheet.value = false
                },
                isDelayTesting = testingGroupNames.contains(group.name),
                onTestDelay = { proxyViewModel.testDelay(group.name) },
                sheetHeightFraction = sheetHeightFraction,
            )
        }
    }
}

@Composable
private fun ProxyTopBar(
    scrollBehavior: ScrollBehavior,
    onNavigateToProviders: () -> Unit,
    onOpenPanel: () -> Unit,
    onTestDelay: (() -> Unit)?,
    onShowSettings: () -> Unit
) {
    TopBar(title = MLang.Proxy.Title, scrollBehavior = scrollBehavior, navigationIcon = {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(
                modifier = Modifier.padding(start = 24.dp), onClick = onNavigateToProviders
            ) {
                Icon(Yume.`Squares-exclude`, contentDescription = MLang.Proxy.Action.ExternalResources)
            }

            IconButton(onClick = onOpenPanel) {
                Icon(Yume.Zashboard, contentDescription = MLang.Proxy.Action.Panel)
            }
        }
    }, actions = {
        IconButton(
            modifier = Modifier.padding(end = 16.dp), onClick = {
                onTestDelay?.invoke()
            }) {
            Icon(Yume.Speed, contentDescription = MLang.Proxy.Action.Test)
        }

        IconButton(
            modifier = Modifier.padding(end = 24.dp), onClick = onShowSettings
        ) {
            Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Action.Settings)
        }
    })
}

@Composable
private fun ProxyContent(
    proxyGroups: List<ProxyGroupInfo>,
    displayMode: ProxyDisplayMode,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    mainInnerPadding: PaddingValues,
    onPullRefresh: () -> Unit,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupDelayClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String>,
    onGroupBoundsChanged: (String, Rect) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var pullRefreshing by remember { mutableStateOf(false) }
    var pullRefreshObservedTesting by remember { mutableStateOf(false) }
    val refreshTexts = remember {
        listOf(
            MLang.Proxy.PullToRefresh.PullToTestAllGroups,
            MLang.Proxy.PullToRefresh.ReleaseToTestAllGroups,
            MLang.Proxy.PullToRefresh.TestingAllGroups,
            MLang.Proxy.Testing.RequestSent,
        )
    }
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

    PullToRefresh(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = pullRefreshing,
        onRefresh = {
            if (pullRefreshing) return@PullToRefresh
            pullRefreshObservedTesting = false
            pullRefreshing = true
            onPullRefresh()
        },
        pullToRefreshState = pullToRefreshState,
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        topAppBarScrollBehavior = scrollBehavior,
        refreshTexts = refreshTexts,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = mainInnerPadding.calculateBottomPadding(),
            ),
            overscrollEffect = null,
        ) {
            nodeGroupItems(
                groups = proxyGroups,
                displayMode = displayMode,
                onGroupClick = onGroupClick,
                onGroupDelayClick = onGroupDelayClick,
                testingGroupNames = testingGroupNames,
                onGroupBoundsChanged = onGroupBoundsChanged,
            )
        }
    }
}

@Composable
private fun ProxySettingsContent(
    proxyViewModel: ProxyViewModel, onDismiss: () -> Unit
) {
    val currentMode by proxyViewModel.currentMode.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val sheetHeightFraction by proxyViewModel.sheetHeightFraction.collectAsState()

    val modeTabs = remember { listOf(MLang.Proxy.Mode.Rule, MLang.Proxy.Mode.Global, MLang.Proxy.Mode.Direct) }
    val modeValues = remember { listOf(TunnelState.Mode.Rule, TunnelState.Mode.Global, TunnelState.Mode.Direct) }
    val displayModes = remember { listOf(ProxyDisplayMode.SINGLE_DETAILED, ProxyDisplayMode.DOUBLE_DETAILED) }
    val displayTabs = remember { displayModes.map { it.displayName } }
    val selectedDisplayMode = remember(displayMode) {
        when (displayMode) {
            ProxyDisplayMode.SINGLE_SIMPLE -> ProxyDisplayMode.SINGLE_DETAILED
            ProxyDisplayMode.DOUBLE_SIMPLE -> ProxyDisplayMode.DOUBLE_DETAILED
            else -> displayMode
        }
    }
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
            })

        Spacer(Modifier.height(12.dp))

        Text(
            text = MLang.Proxy.Settings.DisplayMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = displayTabs,
            selectedTabIndex = displayModes.indexOf(selectedDisplayMode).coerceAtLeast(0),
            onTabSelected = { index ->
                if (index < displayModes.size) {
                    proxyViewModel.setDisplayMode(displayModes[index])
                }
            })

        Spacer(Modifier.height(12.dp))

        Text(
            text = MLang.Proxy.Settings.SheetHeight.format((sheetHeightFraction * 100f).roundToInt()),
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        var sliderValue by remember(sheetHeightFraction) {
            mutableFloatStateOf(fractionToSliderValue(sheetHeightFraction))
        }
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                sliderValue = value
                proxyViewModel.setSheetHeightFraction(sliderValueToFraction(value))
            },
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss, modifier = Modifier.weight(1f)
            ) {
                Text(MLang.Component.Button.Cancel)
            }

            Button(
                onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background)
            }
        }
    }
}
