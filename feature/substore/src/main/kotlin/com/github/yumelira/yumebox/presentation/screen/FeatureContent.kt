package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumelira.yumebox.data.model.AutoCloseMode
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.extra.WindowDropdown

@Composable
fun FeatureContent(
    onOpenExternalUrl: (String) -> Unit,
    onOpenInAppUrl: (String) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
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
                        onCheckedChange = { viewModel.setAllowLanAccess(it) },
                    )
                }
            }

            item {
                val currentPanelName = panelDisplayNames.getOrElse(selectedPanelType) {
                    MLang.Feature.Panel.Unknown
                }
                val panelUrl = panelUrlFor(selectedPanelType)
                val panelOpenModeItems = listOf(
                    MLang.ProfilesPage.LinkSettings.OpenModeInApp,
                    MLang.ProfilesPage.LinkSettings.OpenModeExternal,
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
                        onSelectedIndexChange = { viewModel.setSelectedPanelType(it) },
                    )

                    BasicComponent(
                        title = "URL",
                        summary = panelUrl.ifEmpty { currentPanelName },
                        onClick = {
                            if (panelUrl.isBlank()) return@BasicComponent
                            when (panelOpenMode) {
                                LinkOpenMode.IN_APP -> onOpenInAppUrl(panelUrl)
                                LinkOpenMode.EXTERNAL_BROWSER -> onOpenExternalUrl(panelUrl)
                            }
                        },
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
                                },
                            )
                        },
                    )
                }
            }

            item {
                SmallTitle(MLang.Feature.SubStore.SectionHint)
                Card {
                    SuperArrow(
                        title = if (isExtensionInstalled) {
                            MLang.Feature.SubStore.ExtensionInstalled
                        } else {
                            MLang.Feature.SubStore.ExtensionInstall
                        },
                        summary = when {
                            isExtensionInstalled && isJavetLoaded -> MLang.Feature.SubStore.JavetAvailable
                            isExtensionInstalled -> MLang.Feature.SubStore.JavetPending
                            else -> MLang.Feature.SubStore.DownloadHint
                        },
                        onClick = {
                            if (!isExtensionInstalled) {
                                onOpenExternalUrl("https://github.com/YumeLira/YumeBox/releases/tag/Expand")
                            } else {
                                viewModel.refreshExtensionStatus()
                            }
                        },
                    )
                    SuperArrow(
                        title = MLang.Feature.SubStore.DownloadResources,
                        summary = MLang.Feature.SubStore.DownloadResourcesSummary,
                        onClick = { viewModel.downloadSubStoreAll() },
                        enabled = !isDownloadingSubStoreFrontend && !isDownloadingSubStoreBackend,
                    )
                }
            }
        }
    }
}

private fun panelUrlFor(panelType: Int): String {
    return when (panelType) {
        0 -> "https://board.zash.run.place"
        1 -> "https://metacubex.github.io/metacubexd"
        else -> "https://board.zash.run.place"
    }
}
