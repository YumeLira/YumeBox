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

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.IntentController
import com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumelira.yumebox.common.util.WebViewUtils.getPanelUrl
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.screen.ProxyPager
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.NavigationTransitions
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumelira.yumebox.screen.HomePager
import com.github.yumelira.yumebox.screen.ProfilesPager
import com.github.yumelira.yumebox.screen.SettingPager
import com.github.yumelira.yumebox.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ProvidersScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.tencent.mmkv.MMKV
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.qualifier.named
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()
        fun clearPendingImportUrl() {
            _pendingImportUrl.value = null
        }
    }

    private val appSettingsStorage: com.github.yumelira.yumebox.data.store.AppSettingsStorage by inject()
    private val networkSettingsStorage: com.github.yumelira.yumebox.data.store.NetworkSettingsStorage by inject()
    private val profilesRepository: com.github.yumelira.yumebox.runtime.client.ProfilesRepository by inject()
    private val proxyFacade: com.github.yumelira.yumebox.runtime.client.ProxyFacade by inject()
    private val serviceCache: MMKV by inject(qualifier = named("service_cache"))


    private lateinit var intentController: IntentController

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupGate.loadPrimary()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        applyExcludeFromRecents(appSettingsStorage.excludeFromRecents.value)

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

//        val config = ClarityConfig(
//            projectId = "v4e5psv4w6",
//            logLevel = if (BuildConfig.DEBUG) LogLevel.Verbose else LogLevel.None
//        )
//        Clarity.initialize(applicationContext, config)

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value
            val themeSeedColorArgb = appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value
            val excludeFromRecents = appSettingsViewModel.excludeFromRecents.state.collectAsState().value

            LaunchedEffect(excludeFromRecents) {
                this@MainActivity.applyExcludeFromRecents(excludeFromRecents)
            }

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    colorTheme = colorTheme,
                    themeSeedColorArgb = themeSeedColorArgb,
                ) {
                    val topBarHazeState = remember { HazeState() }
                    val topBarBackground = MiuixTheme.colorScheme.surface
                    val topBarHazeStyle = remember(topBarBackground) {
                        HazeStyle(
                            backgroundColor = topBarBackground,
                            tint = HazeTint(topBarBackground.copy(0.8f)),
                        )
                    }
                    val navController = rememberNavController()

                    CompositionLocalProvider(
                        LocalTopBarHazeState provides topBarHazeState,
                        LocalTopBarHazeStyle provides topBarHazeStyle,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(), color = MiuixTheme.colorScheme.surface
                        ) {
                            DestinationsNavHost(
                                navGraph = NavGraphs.root,
                                navController = navController,
                                defaultTransitions = NavigationTransitions.defaultStyle,
                            )
                            EmasUpdateDialogHost(currentVersionName = BuildConfig.VERSION_NAME)
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                ProxyAutoStartHelper.checkAndAutoStart(
                    proxyFacade = proxyFacade,
                    profilesRepository = profilesRepository,
                    appSettingsStorage = appSettingsStorage,
                    networkSettingsStorage = networkSettingsStorage,
                    serviceCache = serviceCache,
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

    @Suppress("DEPRECATION")
    private fun applyExcludeFromRecents(exclude: Boolean) {

        runCatching {
            val am = getSystemService(ActivityManager::class.java) ?: return@runCatching
            val currentTaskId = taskId
            val task = am.appTasks.firstOrNull { appTask: ActivityManager.AppTask ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appTask.taskInfo.taskId == currentTaskId
                } else {
                    appTask.taskInfo.id == currentTaskId
                }
            }
            task?.setExcludeFromRecents(exclude)
        }
    }
}

@Composable
@Destination<RootGraph>
fun MainScreen(
    navigator: DestinationsNavigator,
    initialPage: Int = 0,
) {
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 3), pageCount = { 4 })
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(MiuixTheme.colorScheme.background.copy(0.8f)),
    )

    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val featureViewModel = koinViewModel<FeatureViewModel>()
    val bottomBarAutoHide by appSettingsViewModel.bottomBarAutoHide.state.collectAsState()
    val bottomBarFloating by appSettingsViewModel.bottomBarFloating.state.collectAsState()
    val showDivider by appSettingsViewModel.showDivider.state.collectAsState()
    val iconWithSelectedLabel by appSettingsViewModel.iconWithSelectedLabel.state.collectAsState()
    val selectedPanelType by featureViewModel.selectedPanelType.state.collectAsState()
    val panelOpenMode by featureViewModel.panelOpenMode.state.collectAsState()

    val pagerFlingAnimationSpec = remember {
        spring<Float>(
            dampingRatio = 0.85f,
            stiffness = 380f
        )
    }

    val pagerClickAnimationSpec: (Int, Int) -> androidx.compose.animation.core.AnimationSpec<Float> =
        remember {
            { fromPage: Int, toPage: Int ->
                val distance = abs(fromPage - toPage)
                val durationMillis = when (distance) {
                    0 -> AnimationSpecs.DURATION_INSTANT
                    1 -> 360
                    else -> (360 + (distance - 1) * 70).coerceAtMost(520)
                }
                tween<Float>(
                    durationMillis = durationMillis,
                    easing = AnimationSpecs.Legacy
                )
            }
        }

    var pageChangeJob by remember { mutableStateOf<Job?>(null) }

    val handlePageChange: (Int) -> Unit =
        remember(pagerState, coroutineScope, pagerClickAnimationSpec) {
            { page ->
                if (page == pagerState.currentPage && !pagerState.isScrollInProgress) return@remember

                pageChangeJob?.cancel()
                pageChangeJob = coroutineScope.launch {
                    val fromPage =
                        if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
                    pagerState.animateScrollToPage(
                        page = page,
                        animationSpec = pagerClickAnimationSpec(fromPage, page)
                    )
                }
            }
        }

    BackHandler {
        if (pagerState.currentPage != 0) {
            handlePageChange(0)
        } else {
            activity?.finish()
        }
    }

    CompositionLocalProvider(
        LocalPagerState provides pagerState,
        LocalHandlePageChange provides handlePageChange,
        LocalNavigator provides navigator,
    ) {
        val bottomBarScrollBehavior = rememberBottomBarScrollBehavior(
            autoHideEnabled = bottomBarAutoHide
        )

        LaunchedEffect(bottomBarAutoHide) {
            bottomBarScrollBehavior.isAutoHideEnabled = bottomBarAutoHide
        }

        Scaffold(
            bottomBar = {
                BottomBarContent(
                    hazeState = hazeState,
                    hazeStyle = hazeStyle,
                    bottomBarFloating = bottomBarFloating,
                    showDivider = showDivider,
                    iconWithSelectedLabel = iconWithSelectedLabel,
                    isVisible = bottomBarScrollBehavior.isBottomBarVisible
                )
            },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier
                    .hazeSource(state = hazeState)
                    .nestedScroll(bottomBarScrollBehavior.nestedScrollConnection),
                state = pagerState,
                beyondViewportPageCount = 2,
                userScrollEnabled = true,
                pageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
                    state = pagerState,
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal
                ),
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = pagerFlingAnimationSpec
                ),
            ) { page ->
                when (page) {
                    0 -> HomePager(innerPadding)
                    1 -> ProxyPager(
                        mainInnerPadding = innerPadding,
                        onNavigateToProviders = {
                            navigator.navigate(ProvidersScreenDestination) {
                                launchSingleTop = true
                            }
                        },
                        onOpenPanel = onOpenPanel@{
                            val context = activity ?: return@onOpenPanel
                            val panelUrl = getPanelUrl(selectedPanelType)
                            if (panelUrl.isEmpty()) return@onOpenPanel
                            when (panelOpenMode) {
                                LinkOpenMode.IN_APP -> WebViewActivity.start(context, panelUrl)
                                LinkOpenMode.EXTERNAL_BROWSER -> openUrl(context, panelUrl)
                            }
                        },
                        isActive = page == pagerState.currentPage,
                    )

                    2 -> ProfilesPager(innerPadding)
                    3 -> SettingPager(innerPadding)
                }
            }
        }
    }
}

