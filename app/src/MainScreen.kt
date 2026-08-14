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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox


import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.yumeyucca.yumebox.common.util.openUrl
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.data.store.LinkOpenMode
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.screen.ProxyPager
import com.github.yumeyucca.yumebox.presentation.theme.YumeHaze
import com.github.yumeyucca.yumebox.presentation.webview.WebViewUtils
import com.github.yumeyucca.yumebox.screen.home.HomePager
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.screen.moe.MoeHomePage
import com.github.yumeyucca.yumebox.screen.moe.LocalUseSystemWallpaper
import com.github.yumeyucca.yumebox.screen.moe.calculateHomeVisibility
import com.github.yumeyucca.yumebox.screen.profiles.ProfilesPager
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import com.github.yumeyucca.yumebox.screen.settings.SettingPager
import dev.chrisbanes.haze.HazeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MainScreen(
    navigator: Navigator,
    initialPage: Int = 0,
    onMainPageChanged: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val windowLayoutMode = rememberWindowLayoutMode()
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val featureStore = koinInject<FeatureStore>()
    val nodeSession by homeViewModel.nodeSession.collectAsState()
    val initialDestination =
        BottomBarDestination.entries.getOrElse(initialPage.coerceIn(0, 3)) {
            BottomBarDestination.Home
        }
    // Providers is opened above a freshly-created Main(Proxy) route on compact layouts.
    val initialProxyRequested = remember { initialDestination == BottomBarDestination.Proxy }
    var proxyDestinationCommitted by remember { mutableStateOf(initialProxyRequested) }
    var pendingDestination by remember { mutableStateOf<BottomBarDestination?>(null) }
    val visibleDestinations =
        remember(proxyDestinationCommitted) {
            BottomBarDestination.entries.filter { destination ->
                destination != BottomBarDestination.Proxy || proxyDestinationCommitted
            }
        }
    val initialMainPage =
        visibleDestinations.indexOf(initialDestination).takeIf { it >= 0 } ?: 0
    val pagerState =
        rememberPagerState(
            initialPage = initialMainPage,
            pageCount = { visibleDestinations.size },
        )
    val mainPagerState = rememberMainPagerState(pagerState)
    val hazeState = remember { HazeState() }
    var previousDestinations by remember { mutableStateOf(visibleDestinations) }
    var settledDestination by remember { mutableStateOf(visibleDestinations[initialMainPage]) }

    LaunchedEffect(nodeSession.everReady, mainPagerState.pagerState.isScrollInProgress) {
        // Insert the proxy page only after the first node load and after a swipe has settled.
        if (!nodeSession.everReady && !initialProxyRequested) {
            proxyDestinationCommitted = false
        } else if (!mainPagerState.pagerState.isScrollInProgress) {
            proxyDestinationCommitted = true
        }
    }

    val bottomBarAutoHideEnabled by appSettingsViewModel.bottomBarAutoHide.state.collectAsState()
    val topBarBlurEnabled by appSettingsViewModel.topBarBlurEnabled.state.collectAsState()
    val classicHomeEnabled by appSettingsViewModel.classicHomeEnabled.state.collectAsState()
    val moeWallpaperUri by appSettingsViewModel.moeWallpaperUri.state.collectAsState()
    val moeWallpaperZoom by appSettingsViewModel.moeWallpaperZoom.state.collectAsState()
    val moeWallpaperBiasX by appSettingsViewModel.moeWallpaperBiasX.state.collectAsState()
    val moeWallpaperBiasY by appSettingsViewModel.moeWallpaperBiasY.state.collectAsState()
    val useSystemWallpaper by appSettingsViewModel.useSystemWallpaper.state.collectAsState()
    val selectedPanelType by featureStore.selectedPanelType.state.collectAsState()
    val panelOpenMode by featureStore.panelOpenMode.state.collectAsState()
    val panelUrl = remember(selectedPanelType) { WebViewUtils.getPanelUrl(selectedPanelType) }
    val openNetworkPanel: () -> Unit = {
        panelUrl.takeIf { it.isNotBlank() }?.let { url ->
            when (panelOpenMode) {
                LinkOpenMode.IN_APP -> WebViewActivity.start(context, url)
                LinkOpenMode.EXTERNAL_BROWSER -> openUrl(context, url)
            }
        }
    }
    val bottomBarScrollBehavior =
        rememberBottomBarScrollBehavior(autoHideEnabled = bottomBarAutoHideEnabled)
    val selectedDestination by
    remember(mainPagerState, visibleDestinations) {
        derivedStateOf {
            if (previousDestinations != visibleDestinations) {
                previousDestinations.getOrNull(mainPagerState.selectedPage) ?: settledDestination
            } else {
                visibleDestinations.getOrElse(mainPagerState.selectedPage) {
                    BottomBarDestination.Home
                }
            }
        }
    }
    val homeVisibility by
    remember(mainPagerState) {
        derivedStateOf {
            calculateHomeVisibility(
                currentPage = mainPagerState.pagerState.currentPage,
                currentPageOffsetFraction = mainPagerState.pagerState.currentPageOffsetFraction,
            )
        }
    }
    // Floating nav bar (with the proxy FAB) shows on the classic home and every other page; the
    // default home has its own chrome, so it stays hidden there.
    val bottomBarVisible by
    remember(classicHomeEnabled, settledDestination, selectedDestination) {
        derivedStateOf {
            when {
                // Dual-pane shell: still float the left-pane bottom bar when not on Moe home.
                classicHomeEnabled -> true
                selectedDestination == BottomBarDestination.Home -> false
                settledDestination != BottomBarDestination.Home -> true
                else -> false
            }
        }
    }
    val bottomBarBackground =
        MiuixTheme.colorScheme.run {
            surface.takeIf { background.luminance() < 0.5f } ?: background
        }
    // Key off the actual theme background, not isSystemInDarkTheme(): the in-app theme can
    // disagree with the system setting, and a white scrim on a dark UI is glaring.
    val bottomBarScrimColor =
        bottomBarBackground.takeIf { MiuixTheme.colorScheme.background.luminance() < 0.5f }
            ?: Color.White
    val bottomBarHazeStyle = YumeHaze.bottomBarStyle(bottomBarBackground)

    LaunchedEffect(visibleDestinations) {
        val pending = pendingDestination
        val currentDestination =
            pending?.takeIf { it in visibleDestinations }
                ?: previousDestinations.getOrNull(mainPagerState.pagerState.currentPage)
                ?: settledDestination
        val targetDestination =
            currentDestination.takeIf { it in visibleDestinations } ?: BottomBarDestination.Config
        val targetPage = visibleDestinations.indexOf(targetDestination)
        if (mainPagerState.pagerState.currentPage != targetPage) {
            mainPagerState.pagerState.requestScrollToPage(targetPage)
        }
        mainPagerState.syncPage()
        settledDestination = targetDestination
        previousDestinations = visibleDestinations
        if (pending == targetDestination) pendingDestination = null
    }

    LaunchedEffect(mainPagerState.pagerState.currentPage) { mainPagerState.syncPage() }

    LaunchedEffect(settledDestination) { onMainPageChanged(settledDestination.ordinal) }

    LaunchedEffect(
        mainPagerState.pagerState.currentPage,
        mainPagerState.pagerState.isScrollInProgress,
    ) {
        if (!mainPagerState.pagerState.isScrollInProgress) {
            settledDestination =
                visibleDestinations.getOrElse(mainPagerState.pagerState.currentPage) {
                    BottomBarDestination.Home
                }
        }
    }

    val vpnPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            homeViewModel.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
        }

    LaunchedEffect(homeViewModel) {
        homeViewModel.vpnPrepareIntent.collect { intent -> vpnPermissionLauncher.launch(intent) }
    }

    val handlePageChange: (BottomBarDestination) -> Unit =
        remember(mainPagerState, visibleDestinations) {
            { destination ->
                if (destination !in visibleDestinations) {
                    pendingDestination = destination
                } else {
                    pendingDestination = null
                }
                val targetDestination =
                    destination.takeIf { it in visibleDestinations } ?: BottomBarDestination.Config
                mainPagerState.animateToPage(visibleDestinations.indexOf(targetDestination))
            }
        }

    val usesSplitShell = windowLayoutMode.usesSplitShell
    val detailNavigator = remember { Navigator(listOf(Route.About)) }
    val detailBackStack = detailNavigator.backStack

    // Leaving the proxy tab should not leave Providers stuck on the right pane.
    LaunchedEffect(settledDestination) {
        if (
            settledDestination != BottomBarDestination.Proxy &&
            detailBackStack.lastOrNull() is Route.Providers
        ) {
            detailNavigator.replaceAll(listOf(Route.About))
        }
    }

    val pendingDeepLink by MainActivity.pendingDeepLink.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        val uri = pendingDeepLink?.toUri() ?: return@LaunchedEffect
        when (uri.host) {
            "page" ->
                when (uri.lastPathSegment) {
                    "home" -> handlePageChange(BottomBarDestination.Home)
                    "proxy" -> handlePageChange(BottomBarDestination.Proxy)
                    "profiles" -> handlePageChange(BottomBarDestination.Config)
                    "settings" -> handlePageChange(BottomBarDestination.Setting)
                }

            "screen" -> {
                val route: Route? =
                    when (uri.lastPathSegment) {
                        "appsettings" -> Route.AppSettings
                        "network" -> Route.NetworkSettings
                        "about" -> Route.About
                        "access" -> Route.AccessControl
                        "traffic" -> Route.TrafficStatistics
                        "connection" -> Route.Connection
                        "log" -> Route.Log
                        "override" -> Route.Override
                        "providers" -> Route.Providers
                        else -> null
                    }
                route?.let { target ->
                    if (usesSplitShell) {
                        detailNavigator.replaceAll(listOf(target))
                    } else {
                        navigator.push(target)
                    }
                }
            }
        }
        MainActivity.clearPendingDeepLink()
    }

    MainScreenBackHandler(
        mainPagerState = mainPagerState,
        canPopRoute = navigator.backStack.size > 1,
    )

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalDetailNavigator provides if (usesSplitShell) detailNavigator else null,
        LocalPagerState provides mainPagerState.pagerState,
        LocalMainPagerState provides mainPagerState,
        LocalHandlePageChange provides handlePageChange,
        LocalBottomBarScrollBehavior provides bottomBarScrollBehavior,
        LocalBottomBarHazeState provides if (topBarBlurEnabled) hazeState else null,
        LocalBottomBarHazeStyle provides if (topBarBlurEnabled) bottomBarHazeStyle else null,
        LocalUseSystemWallpaper provides useSystemWallpaper,
    ) {
        MainContentHost(
            usesSplitShell = usesSplitShell,
            windowLayoutMode = windowLayoutMode,
            mainPagerState = mainPagerState,
            visibleDestinations = visibleDestinations,
            previousDestinations = previousDestinations,
            settledDestination = settledDestination,
            bottomBarVisible = bottomBarVisible,
            topBarBlurEnabled = topBarBlurEnabled,
            hazeState = hazeState,
            bottomBarScrimColor = bottomBarScrimColor,
            classicHomeEnabled = classicHomeEnabled,
            moeWallpaperUri = moeWallpaperUri,
            moeWallpaperZoom = moeWallpaperZoom,
            moeWallpaperBiasX = moeWallpaperBiasX,
            moeWallpaperBiasY = moeWallpaperBiasY,
            homeVisibility = homeVisibility,
            navigator = navigator,
            detailBackStack = detailBackStack,
            detailNavigator = detailNavigator,
            onOpenPanel = openNetworkPanel,
        )
    }
}

@Composable
private fun MainScreenBackHandler(mainPagerState: MainPagerState, canPopRoute: Boolean) {
    val canReturnHome by
    remember(mainPagerState, canPopRoute) {
        derivedStateOf { mainPagerState.selectedPage != 0 && !canPopRoute }
    }
    androidx.activity.compose.BackHandler(enabled = canReturnHome) { mainPagerState.animateToPage(0) }
}

internal data class MainRootPageState(
    val destination: BottomBarDestination,
    val mainInnerPadding: PaddingValues,
    val classicHomeEnabled: Boolean,
    val moeWallpaperUri: String,
    val moeWallpaperZoom: Float,
    val moeWallpaperBiasX: Float,
    val moeWallpaperBiasY: Float,
    val navigator: Navigator,
    val homePageProgress: Float,
    val selectedDestination: BottomBarDestination,
    val windowLayoutMode: WindowLayoutMode,
    val onOpenPanel: () -> Unit,
)

@Composable
internal fun MainRootPageContent(state: MainRootPageState) {
    val detailNavigator = LocalDetailNavigator.current
    val openProvidersFromProxy: () -> Unit = {
        detailNavigator?.replaceAll(listOf(Route.About, Route.Providers))
            ?: state.navigator.replaceAll(
                listOf(
                    Route.Main(initialPage = BottomBarDestination.Proxy.ordinal),
                    Route.Providers,
                )
            )
    }
    when (state.destination) {
        BottomBarDestination.Home -> {
            if (state.classicHomeEnabled) {
                HomePager(
                    mainInnerPadding = state.mainInnerPadding,
                    isActive = state.selectedDestination == BottomBarDestination.Home,
                    onOpenPanel = state.onOpenPanel,
                )
            } else {
                MoeHomePage(
                    mainInnerPadding = state.mainInnerPadding,
                    wallpaperUri = state.moeWallpaperUri,
                    wallpaperZoom = state.moeWallpaperZoom,
                    wallpaperBiasX = state.moeWallpaperBiasX,
                    wallpaperBiasY = state.moeWallpaperBiasY,
                    isActive = state.selectedDestination == BottomBarDestination.Home,
                    pageProgress = state.homePageProgress,
                    onOpenPanel = state.onOpenPanel,
                    windowLayoutMode = state.windowLayoutMode,
                )
            }
        }

        BottomBarDestination.Proxy ->
            ProxyPager(
                mainInnerPadding = state.mainInnerPadding,
                onNavigateToProviders = openProvidersFromProxy,
                isActive = state.selectedDestination == BottomBarDestination.Proxy,
                windowLayoutMode = state.windowLayoutMode,
            )

        BottomBarDestination.Config -> ProfilesPager(state.mainInnerPadding, state.windowLayoutMode)
        BottomBarDestination.Setting -> SettingPager(state.mainInnerPadding, state.windowLayoutMode)
    }
}
