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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.ramcosta.composedestinations.generated.destinations.TrafficStatisticsScreenDestination
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.screen.home.HomeIdleContent
import com.github.yumelira.yumebox.presentation.screen.home.HomeRunningContent
import com.github.yumelira.yumebox.presentation.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class HomeDisplayState {
    Idle,
    Running
}

@Composable
fun HomePager(mainInnerPadding: PaddingValues) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val navigator = LocalNavigator.current

    val displayRunning by homeViewModel.displayRunning.collectAsState()
    val trafficNow by homeViewModel.trafficNow.collectAsState()
    val ipMonitoringState by homeViewModel.ipMonitoringState.collectAsState()
    val tunnelState by homeViewModel.tunnelState.collectAsState()
    val currentProfile by homeViewModel.currentProfile.collectAsState()
    val oneWord by homeViewModel.oneWord.collectAsState()
    val oneWordAuthor by homeViewModel.oneWordAuthor.collectAsState()
    val selectedServerName by homeViewModel.selectedServerName.collectAsState()
    val selectedServerPing by homeViewModel.selectedServerPing.collectAsState()
    val speedHistory by homeViewModel.speedHistory.collectAsState()

    val displayState = if (displayRunning) HomeDisplayState.Running else HomeDisplayState.Idle

    var pendingProfileId by remember { mutableStateOf<String?>(null) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingProfileId?.let { profileId ->
                homeViewModel.startProxy(profileId, useTunMode = true)
            }
        }
        pendingProfileId = null
    }

    LaunchedEffect(Unit) {
        homeViewModel.vpnPrepareIntent.collect { intent ->
            vpnPermissionLauncher.launch(intent)
        }
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = { TopBar(title = "YumeBox", scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
            topPadding = 20.dp,
            enableBottomBarAutoHide = true,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING)
                        .padding(top = AppConstants.UI.DEFAULT_VERTICAL_SPACING),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(AppConstants.UI.DEFAULT_VERTICAL_SPACING)
                ) {
                    AnimatedContent(
                        targetState = displayState,
                        transitionSpec = { createHomeTransitionSpec() },
                        label = "HomeContentTransition"
                    ) { state ->
                        when (state) {
                            HomeDisplayState.Idle -> HomeIdleContent(
                                oneWord = oneWord,
                                author = oneWordAuthor
                            )
                            HomeDisplayState.Running -> HomeRunningContent(
                                trafficNow = trafficNow,
                                profileName = currentProfile?.name,
                                tunnelMode = tunnelState?.mode,
                                serverName = selectedServerName,
                                serverPing = selectedServerPing,
                                ipMonitoringState = ipMonitoringState,
                                speedHistory = speedHistory,
                                onChartClick = {
                                    navigator.navigate(TrafficStatisticsScreenDestination) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<HomeDisplayState>.createHomeTransitionSpec(): ContentTransform {
    val animDuration = 300
    return when {
        targetState == HomeDisplayState.Idle -> {
            (fadeIn(animationSpec = tween(animDuration)) +
             scaleIn(initialScale = 0.92f, animationSpec = tween(animDuration))).togetherWith(
                fadeOut(animationSpec = tween(animDuration)) +
                scaleOut(targetScale = 1.08f, animationSpec = tween(animDuration))
            )
        }
        else -> {
            (fadeIn(animationSpec = tween(animDuration)) +
             scaleIn(initialScale = 0.92f, animationSpec = tween(animDuration))).togetherWith(
                fadeOut(animationSpec = tween(animDuration)) +
                scaleOut(targetScale = 1.08f, animationSpec = tween(animDuration))
            )
        }
    }
}
