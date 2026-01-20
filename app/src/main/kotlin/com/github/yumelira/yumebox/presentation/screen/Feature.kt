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
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.data.model.AutoCloseMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.WindowDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch

@Composable
fun formatDownloadSpeed(speed: String): String {
    return speed
}

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
    val backendPort by viewModel.backendPort.state.collectAsState()
    val frontendPort by viewModel.frontendPort.state.collectAsState()
    val autoCloseMode by viewModel.autoCloseMode.collectAsState()

    val host = if (allowLanAccess) "0.0.0.0" else "127.0.0.1"
    val frontendUrl = "http://${host}:${frontendPort}"
    "http://${host}:${backendPort}"


    val isDownloadingPanel by viewModel.isDownloadingPanel.collectAsState()


    val isDownloadingSubStoreFrontend by viewModel.isDownloadingSubStoreFrontend.collectAsState()
    val isDownloadingSubStoreBackend by viewModel.isDownloadingSubStoreBackend.collectAsState()

    val isExtensionInstalled by viewModel.isExtensionInstalled.collectAsState()
    val isJavetLoaded by viewModel.isJavetLoaded.collectAsState()
    val isSubStoreInitialized by viewModel.isSubStoreInitialized.collectAsState()


    val selectedPanelType by viewModel.selectedPanelType.state.collectAsState()
    val panelInstallStatus by viewModel.panelInstallStatus.collectAsState()


    val panelDisplayNames = listOf("Zashboard", "MetaCubeXD")


    LaunchedEffect(Unit) {
        viewModel.initializePanelPaths()
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
                val isPanelInstalled: Boolean =
                    if (selectedPanelType < panelInstallStatus.size) {
                        panelInstallStatus[selectedPanelType]
                    } else {
                        false
                    }
                val statusText =
                    if (isPanelInstalled) MLang.Feature.Panel.Installed else MLang.Feature.Panel.NotInstalled
                val currentPanelName =
                    if (selectedPanelType < panelDisplayNames.size) {
                        panelDisplayNames[selectedPanelType]
                    } else {
                        MLang.Feature.Panel.Unknown
                    }

                val downloadButtonText =
                    if (isPanelInstalled) MLang.Feature.Panel.Redownload else MLang.Feature.Panel.Download

                SmallTitle(MLang.Feature.Panel.Section)
                Card {
                    WindowDropdown(
                        title = MLang.Feature.Panel.SelectPanel,
                        summary = "$currentPanelName - $statusText",
                        items = panelDisplayNames,
                        selectedIndex = selectedPanelType,
                        onSelectedIndexChange = {
                            viewModel.setSelectedPanelType(it)
                        },
                    )

                    SuperArrow(
                        title = downloadButtonText,
                        summary = if (isDownloadingPanel) {
                            MLang.Feature.Panel.Downloading
                        } else {
                            val status =
                                if (isPanelInstalled) MLang.Feature.Panel.WillOverwrite else MLang.Feature.Panel.NotInstalledHint
                            "$currentPanelName $status"
                        },
                        onClick = {
                            if (!isDownloadingPanel) {
                                viewModel.downloadExternalPanelEnhanced(selectedPanelType)
                            }
                        },
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
