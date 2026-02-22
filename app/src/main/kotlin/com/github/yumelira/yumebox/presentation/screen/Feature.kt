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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.common.util.WebViewUtils.getPanelUrl
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.data.model.AutoCloseMode
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.extra.WindowDropdown

@Composable
@Destination<RootGraph>
fun FeatureScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val viewModel = koinViewModel<FeatureViewModel>()
    val isServiceRunning by viewModel.serviceRunningState.collectAsState()
    val allowLanAccess by viewModel.allowLanAccess.state.collectAsState()
    val frontendPort by viewModel.frontendPort.state.collectAsState()
    val autoCloseMode by viewModel.autoCloseMode.collectAsState()

    val host = if (allowLanAccess) "0.0.0.0" else "127.0.0.1"
    val frontendUrl = "http://${host}:${frontendPort}"

    val isDownloadingSubStoreFrontend by viewModel.isDownloadingSubStoreFrontend.collectAsState()
    val isDownloadingSubStoreBackend by viewModel.isDownloadingSubStoreBackend.collectAsState()

    val isExtensionInstalled by viewModel.isExtensionInstalled.collectAsState()
    val isJavetLoaded by viewModel.isJavetLoaded.collectAsState()
    val isSubStoreInitialized by viewModel.isSubStoreInitialized.collectAsState()


    val selectedPanelType by viewModel.selectedPanelType.state.collectAsState()
    val panelOpenMode by viewModel.panelOpenMode.state.collectAsState()

    val panelDisplayNames = listOf("Zashboard", "MetaCubeXD")


    LaunchedEffect(Unit) {
        viewModel.initializeSubStoreStatus()
    }

    Scaffold(
        topBar = {
            TopBar(title = MLang.Feature.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                val canStartService = isExtensionInstalled && isSubStoreInitialized
                when {
                    isServiceRunning -> MLang.Feature.ServiceStatus.Running.format(frontendUrl)
                    !isExtensionInstalled -> MLang.Feature.ServiceStatus.NeedExtension
                    !isSubStoreInitialized -> MLang.Feature.ServiceStatus.NeedSubStore
                    else -> MLang.Feature.ServiceStatus.NotRunning
                }
                SmallTitle(MLang.Feature.ServiceStatus.Section)
                Card {
                    val autoCloseItems = AutoCloseMode.entries.map { it.getDisplayName() }
                    val autoCloseValues = AutoCloseMode.entries

                    EnumSelector(
                        title = MLang.Feature.ServiceStatus.SwitchStartSubStore,
                        summary = MLang.Feature.ServiceStatus.AutoCloseModeSummary,
                        currentValue = autoCloseMode,
                        items = autoCloseItems,
                        values = autoCloseValues,
                        onValueChange = { mode ->
                            viewModel.setAutoCloseMode(mode)
                            if (mode != AutoCloseMode.DISABLED && !isServiceRunning && canStartService) {
                                viewModel.startService()
                            } else if (mode == AutoCloseMode.DISABLED && isServiceRunning) {
                                viewModel.stopService()
                            }
                        },
                    )
                    SuperSwitch(
                        title = MLang.Feature.ServiceStatus.AllowLan,
                        summary = MLang.Feature.ServiceStatus.AllowLanSummary,
                        checked = allowLanAccess,
                        onCheckedChange = {
                            viewModel.setAllowLanAccess(it)
                        },
                    )
                }
            }
            item {
                val currentPanelName =
                    if (selectedPanelType < panelDisplayNames.size) {
                        panelDisplayNames[selectedPanelType]
                    } else {
                        MLang.Feature.Panel.Unknown
                    }
                val panelUrl = getPanelUrl(selectedPanelType)
                val panelOpenModeItems = listOf(
                    MLang.ProfilesPage.LinkSettings.OpenModeInApp,
                    MLang.ProfilesPage.LinkSettings.OpenModeExternal
                )
                val panelOpenModeIndex = when (panelOpenMode) {
                    LinkOpenMode.IN_APP -> 0
                    LinkOpenMode.EXTERNAL_BROWSER -> 1
                }

                SmallTitle(MLang.Feature.Panel.Section)
                Card {
                    WindowDropdown(
                        title = MLang.Feature.Panel.SelectPanel,
                        summary = currentPanelName,
                        items = panelDisplayNames,
                        selectedIndex = selectedPanelType,
                        onSelectedIndexChange = {
                            viewModel.setSelectedPanelType(it)
                        },
                    )

                    BasicComponent (
                        title = "URL",
                        summary = panelUrl.ifEmpty { currentPanelName },
                        onClick = {}
                    )

                    WindowDropdown(
                        title = MLang.ProfilesPage.LinkSettings.OpenMode,
                        summary = panelOpenModeItems.getOrElse(panelOpenModeIndex) { panelOpenModeItems.first() },
                        items = panelOpenModeItems,
                        selectedIndex = panelOpenModeIndex,
                        onSelectedIndexChange = { index ->
                            viewModel.setPanelOpenMode(
                                when (index) {
                                    0 -> LinkOpenMode.IN_APP
                                    1 -> LinkOpenMode.EXTERNAL_BROWSER
                                    else -> LinkOpenMode.IN_APP
                                }
                            )
                        }
                    )

                }
            }
            item {
                SmallTitle(MLang.Feature.SubStore.SectionHint)
                Card {
                    SuperArrow(
                        title = if (isExtensionInstalled) MLang.Feature.SubStore.ExtensionInstalled else MLang.Feature.SubStore.ExtensionInstall,
                        summary = when {
                            isExtensionInstalled && isJavetLoaded -> MLang.Feature.SubStore.JavetAvailable
                            isExtensionInstalled -> MLang.Feature.SubStore.JavetPending
                            else -> MLang.Feature.SubStore.DownloadHint
                        },
                        onClick = {
                            if (!isExtensionInstalled) {
                                openUrl(context, "https://github.com/YumeLira/YumeBox/releases/tag/Expand")
                            } else {
                                viewModel.refreshExtensionStatus()
                            }
                        },
                    )
                    SuperArrow(
                        title = MLang.Feature.SubStore.DownloadResources,
                        summary = MLang.Feature.SubStore.DownloadResourcesSummary,
                        onClick = { viewModel.downloadSubStoreAll() },
                        enabled = !isDownloadingSubStoreFrontend && !isDownloadingSubStoreBackend
                    )
                }
            }
        }
    }
}
