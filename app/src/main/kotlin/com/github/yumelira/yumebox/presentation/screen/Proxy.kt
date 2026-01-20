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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.github.yumelira.yumebox.presentation.component.ProxyNodeGrid
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.proxyGroupGridItems
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Activity
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.`Squares-exclude`
import com.github.yumelira.yumebox.presentation.icon.yume.Zashboard
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.HomeViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import com.github.yumelira.yumebox.presentation.webview.WebViewActivity
import com.ramcosta.composedestinations.generated.destinations.ProvidersScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.extra.WindowListPopup
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
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val selectedPanelType by featureViewModel.selectedPanelType.state.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()

    val showSettingsBottomSheet = rememberSaveable { mutableStateOf(false) }
    val showGroupBottomSheet = rememberSaveable { mutableStateOf(false) }
    var sheetGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    val onTestDelay = remember { { proxyViewModel.testDelay() } }

    Scaffold(
        topBar = {
            ProxyTopBar(
                scrollBehavior = scrollBehavior,
                context = context,
                selectedPanelType = selectedPanelType,
                onNavigateToProviders = { navigator.navigate(ProvidersScreenDestination) { launchSingleTop = true } },
                onTestDelay = onTestDelay,
                onShowSettings = { showSettingsBottomSheet.value = true }
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
                    displayMode = displayMode,
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    mainInnerPadding = mainInnerPadding,
                    testingGroupNames = testingGroupNames,
                    onGroupClick = { group ->
                        sheetGroupName = group.name
                        showGroupBottomSheet.value = true
                    },
                    onGroupDelayClick = { group ->
                        proxyViewModel.testDelay(group.name)
                    },
                )
            }
        }

        WindowBottomSheet(
            show = showSettingsBottomSheet,
            title = MLang.Proxy.Settings.Title,
            onDismissRequest = { showSettingsBottomSheet.value = false },
            insideMargin = DpSize(32.dp, 16.dp),
        ) {
            ProxySettingsContent(
                proxyViewModel = proxyViewModel,
                onDismiss = { showSettingsBottomSheet.value = false }
            )
        }

        val proxyGroupsState = rememberUpdatedState(proxyGroups)
        val sheetGroup by remember {
            derivedStateOf {
                val name = sheetGroupName ?: return@derivedStateOf null
                proxyGroupsState.value.firstOrNull { it.name == name }
            }
        }

        WindowBottomSheet(
            show = showGroupBottomSheet,
            title = sheetGroupName.orEmpty(),
            startAction = {
                val showPopup = remember { mutableStateOf(false) }
                val modes = remember {
                    listOf(
                        ProxySortMode.DEFAULT,
                        ProxySortMode.BY_NAME,
                        ProxySortMode.BY_LATENCY,
                    )
                }
                var selectedIndex by remember(sortMode) {
                    mutableStateOf(modes.indexOf(sortMode).coerceAtLeast(0))
                }

                IconButton(onClick = { showPopup.value = true }) {
                    Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Settings.SortMode)
                }

                WindowListPopup  (
                    show = showPopup,
                    popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                    alignment = PopupPositionProvider.Align.Start,
                    onDismissRequest = { showPopup.value = false }
                ) {
                    ListPopupColumn {
                        modes.forEachIndexed { index, mode ->
                            DropdownImpl(
                                text = mode.displayName,
                                optionSize = modes.size,
                                isSelected = selectedIndex == index,
                                onSelectedIndexChange = {
                                    selectedIndex = index
                                    proxyViewModel.setSortMode(mode)
                                    showPopup.value = false
                                },
                                index = index
                            )
                        }
                    }
                }
            },
            endAction = {
                val group = sheetGroup ?: return@WindowBottomSheet
                IconButton(onClick = { proxyViewModel.testDelay(group.name) }) {
                    Icon(Yume.Activity, contentDescription = MLang.Proxy.Action.Test)
                }
            },
            onDismissRequest = { showGroupBottomSheet.value = false },
            insideMargin = DpSize(16.dp, 16.dp),
        ) {
            val group = sheetGroup ?: return@WindowBottomSheet
            ProxyGroupSelectorContent(
                group = group,
                displayMode = displayMode,
                isGroupTesting = testingGroupNames.contains(group.name),
                onGroupDelayClick = { proxyViewModel.testDelay(group.name) },
                onSelectProxy = { proxyName ->
                    proxyViewModel.selectProxy(group.name, proxyName)
                    showGroupBottomSheet.value = false
                },
            )
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
                onClick = {
                    onTestDelay?.invoke()
                }
            ) {
                Icon(Yume.Activity, contentDescription = MLang.Proxy.Action.Test)
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
    displayMode: ProxyDisplayMode,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    mainInnerPadding: PaddingValues,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupDelayClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String>,
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
        proxyGroupGridItems(
            groups = proxyGroups,
            displayMode = displayMode,
            onGroupClick = onGroupClick,
            onGroupDelayClick = onGroupDelayClick,
            testingGroupNames = testingGroupNames,
        )
    }
}

@Composable
private fun ProxyGroupSelectorContent(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    isGroupTesting: Boolean,
    onGroupDelayClick: () -> Unit,
    onSelectProxy: (String) -> Unit,
) {
    val groupName = group.name
    val isSelectable = group.type == Proxy.Type.Selector

    val onProxyClick = remember(groupName, isSelectable, onSelectProxy) {
        if (isSelectable) {
            { proxyName: String -> onSelectProxy(proxyName) }
        } else {
            null
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val minSheetHeight = (maxHeight * 0.42f).coerceAtLeast(280.dp).coerceAtMost(380.dp)
        val maxSheetHeight = (maxHeight * 0.72f).coerceAtLeast(420.dp).coerceAtMost(620.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minSheetHeight, max = maxSheetHeight)
        ) {
            ProxyNodeGrid(
                proxies = group.proxies,
                selectedProxyName = group.now,
                displayMode = displayMode,
                onProxyClick = onProxyClick?.let { click ->
                    { proxy: Proxy -> click(proxy.name) }
                },
                onProxyDelayClick = { onGroupDelayClick() },
                isDelayTesting = isGroupTesting,
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                modifier = Modifier.fillMaxSize(),
            )
        }
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
