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

package com.github.yumelira.yumebox

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import com.github.yumelira.yumebox.common.util.IntentController
import com.github.yumelira.yumebox.presentation.theme.NavigationTransitions
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.presentation.component.BottomBar
import com.github.yumelira.yumebox.presentation.component.LocalHandlePageChange
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.component.LocalPagerState
import com.github.yumelira.yumebox.presentation.screen.HomePager
import com.github.yumelira.yumebox.presentation.screen.ProfilesPager
import com.github.yumelira.yumebox.presentation.screen.ProxyPager
import com.github.yumelira.yumebox.presentation.screen.SettingPager
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()
        fun clearPendingImportUrl() { _pendingImportUrl.value = null }
    }

    private val appSettingsStorage: com.github.yumelira.yumebox.data.store.AppSettingsStorage by inject()
    private val networkSettingsStorage: com.github.yumelira.yumebox.data.store.NetworkSettingsStorage by inject()
    private val profilesStore: com.github.yumelira.yumebox.data.store.ProfilesStore by inject()
    private val clashManager: com.github.yumelira.yumebox.clash.manager.ClashManager by inject()
    private val proxyConnectionService: com.github.yumelira.yumebox.data.repository.ProxyConnectionService by inject()

    private lateinit var intentController: IntentController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        intentController = IntentController(this, lifecycleScope)
        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION,
                )
            }
        }

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    colorTheme = colorTheme,
                ) {
                    val navController = rememberNavController()

                    Surface(
                        modifier = Modifier.fillMaxSize(), color = MiuixTheme.colorScheme.surface
                    ) {
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            navController = navController,
                            defaultTransitions = NavigationTransitions.defaultStyle,
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(com.github.yumelira.yumebox.common.AppConstants.Timing.AUTO_START_DELAY_MS)
                com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper.checkAndAutoStart(
                    proxyConnectionService = proxyConnectionService,
                    appSettingsStorage = appSettingsStorage,
                    networkSettingsStorage = networkSettingsStorage,
                    profilesStore = profilesStore,
                    clashManager = clashManager,
                    isBootCompleted = false
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let { safeIntent ->
            safeIntent.data?.let { uri ->
                val scheme = uri.scheme
                if (scheme == "clash" || scheme == "clashmeta") {
                    val host = uri.host
                    if (host == "install-config") {
                        val configUrl = uri.getQueryParameter("url")
                        if (!configUrl.isNullOrBlank()) {
                            _pendingImportUrl.value = configUrl
                        }
                    }
                }
            }

            intentController.handleIntent(safeIntent)
        }
    }
}

@Composable
@Destination<RootGraph>(start = true)
fun MainScreen(navigator: DestinationsNavigator) {
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(MiuixTheme.colorScheme.background.copy(0.8f)),
    )

    val handlePageChange: (Int) -> Unit = remember(pagerState, coroutineScope) {
        { page ->
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = page,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        } else {
            activity?.finish()
        }
    }

    CompositionLocalProvider(
        LocalPagerState provides pagerState,
        LocalHandlePageChange provides handlePageChange,
        LocalNavigator provides navigator,
    ) {
        val verticalScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (abs(available.y) > abs(available.x) * 1.5f) {
                        return Velocity(available.x, 0f)
                    }
                    return Velocity.Zero
                }
            }
        }

        Scaffold(
            bottomBar = {
                BottomBar(hazeState, hazeStyle)
            },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier
                    .hazeSource(state = hazeState)
                    .nestedScroll(verticalScrollConnection),
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = true,
                pageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
                    state = pagerState,
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal
                ),
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
            ) { page ->
                when (page) {
                    0 -> HomePager(innerPadding)
                    1 -> ProxyPager(innerPadding, navigator)
                    2 -> ProfilesPager(innerPadding)
                    3 -> SettingPager(innerPadding)
                }
            }
        }
    }
}