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

@file:Suppress("FunctionName", "UnnecessaryVariable", "KotlinDeprecation")

package com.github.yumeyucca.yumebox.screen.moe

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.data.model.ThemeMode
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.LocalHandlePageChange
import com.github.yumeyucca.yumebox.presentation.component.LocalNavigator
import com.github.yumeyucca.yumebox.presentation.icon.ShellIcons
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Zashboard
import com.github.yumeyucca.yumebox.presentation.icon.yume.Palette
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.screen.home.HomeProxyControlState
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MoeHomePage(
    mainInnerPadding: PaddingValues,
    wallpaperUri: String,
    wallpaperZoom: Float = 1f,
    wallpaperBiasX: Float = 0f,
    wallpaperBiasY: Float = 0f,
    isActive: Boolean,
    pageProgress: Float = 1f,
    sidebarProgress: Float = pageProgress,
    onOpenPanel: (() -> Unit)? = null,
    windowLayoutMode: com.github.yumeyucca.yumebox.presentation.component.WindowLayoutMode =
        com.github.yumeyucca.yumebox.presentation.component.WindowLayoutMode.Compact,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val screen by homeViewModel.screenState.collectAsState()
    val moe by appSettingsViewModel.moeHomeSectionState.collectAsState()
    val controlState = screen.controlState
    val profiles = screen.profiles
    val profilesLoaded = screen.profilesLoaded
    val recommendedProfile = screen.recommendedProfile
    val hasEnabledProfile = screen.hasEnabledProfile
    val selectedServerName = screen.selectedServerName
    val selectedServerPing = screen.selectedServerPing
    val trafficNow = screen.trafficNow
    val isRemoteController = screen.isRemoteController
    val themeMode = moe.themeMode
    val classicHomeEnabled = moe.classicHomeEnabled
    val moeHomeQuote = moe.moeHomeQuote
    val sidebarExpanded = moe.sidebarExpanded
    val useSystemWallpaper = moe.useSystemWallpaper

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val batteryPercent = rememberMoeBatteryPercent(context)
    var showHomeSettingsSheet by remember { mutableStateOf(false) }
    var wallpaperRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { homeViewModel.refreshProxyMode() }

    LaunchedEffect(isActive) {
        homeViewModel.setHomeScreenActive(isActive)
        if (isActive) {
            homeViewModel.reconcileRuntimeState()
            homeViewModel.refreshProxyMode()
        }
    }

    DisposableEffect(homeViewModel) { onDispose { homeViewModel.setHomeScreenActive(false) } }

    DisposableEffect(lifecycleOwner, homeViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.reconcileRuntimeState()
                homeViewModel.refreshProxyMode()
                wallpaperRefreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(screen.uiError) {
        screen.uiError?.let { error ->
            context.toast(error, Toast.LENGTH_LONG)
            homeViewModel.consumeError()
        }
    }
    LaunchedEffect(screen.uiMessage) {
        screen.uiMessage?.let { message ->
            context.toast(message, Toast.LENGTH_SHORT)
            homeViewModel.consumeMessage()
        }
    }

    val visualControlState = controlState
    // Tick once a second whether running or idle: running drives the elapsed timer, idle drives the
    // wall-clock shown in the rail so it always reflects the real time instead of a frozen 00:00.
    val now by
    produceState(initialValue = System.currentTimeMillis()) {
        PollingTimers.ticks(PollingTimerSpecs.MoeElapsedClock).collect {
            value = System.currentTimeMillis()
        }
    }
    val startedAt = screen.runtimeStartedAt
    val isRunning = visualControlState == HomeProxyControlState.Running
    val elapsedMillis =
        if (isRunning && startedAt != null && !isRemoteController) {
            (now - startedAt).coerceAtLeast(0L)
        } else {
            0L
        }
    val durationPair =
        remember(isRunning, isRemoteController, elapsedMillis, now) {
            if (isRunning && !isRemoteController) {
                formatMoeDuration(elapsedMillis)
            } else {
                formatMoeClock(now)
            }
        }
    val trafficData =
        remember(trafficNow, isRunning) {
            if (isRunning) TrafficData.from(trafficNow) else TrafficData.zero
        }
    val systemDark = isSystemInDarkTheme()
    val isDarkHomeSurface =
        when (themeMode) {
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
            ThemeMode.Auto -> systemDark
        }
    val contentSurface = if (isDarkHomeSurface) MiuixTheme.colorScheme.surface else Color.White
    val handlePageChange = LocalHandlePageChange.current
    val sidebarIcons = remember(onOpenPanel, handlePageChange) {
        listOf(
            MoeSidebarIconItem(Yume.Zashboard) { onOpenPanel?.invoke() },
            MoeSidebarIconItem(ShellIcons.OpenProfiles) {
                handlePageChange(BottomBarDestination.Config)
            },
            MoeSidebarIconItem(ShellIcons.OpenSettings) {
                handlePageChange(BottomBarDestination.Setting)
            },
        )
    }
    val quoteText = moeHomeQuote.ifBlank { YumeTxt.AppSettings.Interface.HomeQuoteDefault }
    val animatedSidebarToggleProgress by
    animateFloatAsState(
        targetValue = if (sidebarExpanded) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = if (sidebarExpanded) 420 else 320,
                easing =
                    if (sidebarExpanded) {
                        AnimationSpecs.EmphasizedDecelerate
                    } else {
                        AnimationSpecs.EmphasizedAccelerate
                    },
            ),
        label = "moe_sidebar_toggle",
    )

    val handleProxyAction: () -> Unit = {
        if (isRemoteController) {
            // Remote controller mode has no local start/stop action here.
        } else if (!hasEnabledProfile || recommendedProfile == null) {
            context.toast(YumeTxt.ProfilesVM.Error.ProfileNotExist, Toast.LENGTH_SHORT)
        } else if (visualControlState == HomeProxyControlState.Idle) {
            val profile = recommendedProfile
            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
            homeViewModel.startProxy(profileId = profile.uuid.toString(), mode = null)
        } else if (visualControlState == HomeProxyControlState.Running) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
            scope.launch { homeViewModel.stopProxy() }
        }
    }

    val navigator = LocalNavigator.current
    val wallpaperPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            navigator.push(
                Route.MoeWallpaperCrop(
                    wallpaperUri = uri.toString(),
                    initialZoom = wallpaperZoom,
                    initialBiasX = wallpaperBiasX,
                    initialBiasY = wallpaperBiasY,
                )
            )
        }
    val launchWallpaperPicker: () -> Unit = {
        wallpaperPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val homeSidebarIcons = remember(sidebarIcons, launchWallpaperPicker) {
        sidebarIcons + MoeSidebarIconItem(Yume.Palette) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            launchWallpaperPicker()
        }
    }

    val layoutState =
        MoeHomeLayoutState(
            wallpaperUri = wallpaperUri,
            wallpaperZoom = wallpaperZoom,
            wallpaperBiasX = wallpaperBiasX,
            wallpaperBiasY = wallpaperBiasY,
            statusBarTop = statusBarTop,
            pageProgress = pageProgress,
            sidebarProgress = sidebarProgress,
            sidebarToggleProgress = animatedSidebarToggleProgress,
            duration = durationPair,
            batteryPercent = batteryPercent,
            sidebarIcons = homeSidebarIcons,
            contentSurface = contentSurface,
            isRunning = isRunning,
            traffic = trafficData,
            selectedServerName = selectedServerName,
            selectedServerPing = selectedServerPing,
            now = now,
            quote = quoteText,
            controlState = visualControlState,
            canLaunch = profilesLoaded && profiles.isNotEmpty() && !isRemoteController,
            isRemoteController = isRemoteController,
            usesTabletLayout = windowLayoutMode.usesNavigationRail,
        )
    val actions =
        MoeHomeActions(
            toggleSidebar = {
                if (!windowLayoutMode.usesNavigationRail) {
                    appSettingsViewModel.onMoeSidebarExpandedChange(!sidebarExpanded)
                }
            },
            pickWallpaper = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                launchWallpaperPicker()
            },
            openSettings = { showHomeSettingsSheet = true },
            toggleProxy = handleProxyAction,
        )
    CompositionLocalProvider(LocalWallpaperRefreshKey provides wallpaperRefreshKey) {
        with(actions) { MoeHomeLayout(layoutState) }
    }

    MoeHomeSettingsSheet(
        show = showHomeSettingsSheet,
        quote = moeHomeQuote,
        classicHomeEnabled = classicHomeEnabled,
        sidebarExpanded = sidebarExpanded,
        useSystemWallpaper = useSystemWallpaper,
        onQuoteChange = appSettingsViewModel::onMoeHomeQuoteChange,
        onClassicHomeEnabledChange = appSettingsViewModel::onClassicHomeEnabledChange,
        onSidebarExpandedChange = appSettingsViewModel::onMoeSidebarExpandedChange,
        onUseSystemWallpaperChange = appSettingsViewModel::onUseSystemWallpaperChange,
        onChangeWallpaper = {
            showHomeSettingsSheet = false
            launchWallpaperPicker()
        },
        onDismiss = { showHomeSettingsSheet = false },
    )
}

@Composable
private fun rememberMoeBatteryPercent(context: Context): Int? {
    val percent by
    produceState<Int?>(initialValue = null, context) {
        val batteryIntent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        value =
            if (level >= 0 && scale > 0) {
                ((level / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                null
            }
    }
    return percent
}
