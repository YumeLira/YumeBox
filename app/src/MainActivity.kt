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

package com.github.yumeyucca.yumebox

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.retainedComponent
import com.github.yumeyucca.yumebox.common.util.AppLanguageManager
import com.github.yumeyucca.yumebox.common.util.AutoStartDependencies
import com.github.yumeyucca.yumebox.common.util.IntentController
import com.github.yumeyucca.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.core.util.StartupTaskCoordinator
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.di.APPLICATION_SCOPE_NAME
import com.github.yumeyucca.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumeyucca.yumebox.presentation.component.LocalTopBarHazeStyle
import com.github.yumeyucca.yumebox.presentation.component.ToastDialogHost
import com.github.yumeyucca.yumebox.presentation.navigation.AppNavContainer
import com.github.yumeyucca.yumebox.presentation.navigation.AppNavigationComponent
import com.github.yumeyucca.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumeyucca.yumebox.presentation.theme.YumeHaze
import com.github.yumeyucca.yumebox.presentation.theme.YumeTheme
import com.github.yumeyucca.yumebox.runtime.service.WifiAutomationService
import com.github.yumeyucca.yumebox.screen.moe.HomePreviewGuideDialog
import com.github.yumeyucca.yumebox.screen.moe.SystemWallpaperAccess
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import com.tencent.mmkv.MMKV
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.qualifier.named
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : FragmentActivity() {
    private var systemWallpaperPermissionRequestHandled = false
    private val appSettingsStore: AppSettingsStore by inject()
    private val navigationComponent: AppNavigationComponent by lazy {
        retainedComponent { componentContext ->
            AppNavigationComponent(
                componentContext = componentContext,
                predictiveBackEnabledAtLaunch = appSettingsStore.predictiveBackEnabled.value,
            )
        }
    }

    companion object {
        private const val REQUEST_STARTUP_PERMISSIONS = 1001
        private const val REQUEST_SYSTEM_WALLPAPER_PERMISSION = 1002
        private const val MIUI_GET_INSTALLED_APPS_PERMISSION =
            "com.android.permission.GET_INSTALLED_APPS"
        private const val EXTRA_EXIT_UI_WHEN_BACKGROUND = "exit_ui_when_background"
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()

        fun clearPendingImportUrl() {
            _pendingImportUrl.value = null
        }

        private val _pendingDeepLink = MutableStateFlow<String?>(null)
        val pendingDeepLink: StateFlow<String?> = _pendingDeepLink.asStateFlow()

        fun clearPendingDeepLink() {
            _pendingDeepLink.value = null
        }
    }

    private val appSettingsStorage: com.github.yumeyucca.yumebox.data.store.AppSettingsStore by
    inject()
    private val featureStore: FeatureStore by inject()
    private val networkSettingsStorage:
            com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore by
    inject()
    private val profilesRepository: com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository by
    inject()
    private val proxyFacade: com.github.yumeyucca.yumebox.runtime.client.ProxyFacade by inject()
    private val serviceCache: MMKV by inject(qualifier = named("service_cache"))
    private val applicationScope: CoroutineScope by
    inject(qualifier = named(APPLICATION_SCOPE_NAME))

    private lateinit var intentController: IntentController

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        applyExcludeFromRecents(appSettingsStorage.excludeFromRecents.value)

        intentController = IntentController(this, lifecycleScope)
        handleIntent(intent)

        val startupPermissionsRequested = requestStartupPermissions()
        if (!startupPermissionsRequested) {
            window.decorView.post(::requestSystemWallpaperPermissionIfNeeded)
        }

        if (networkSettingsStorage.wifiAutomationEnabled.value) {
            WifiAutomationService.start(this)
        }

        val showHomeGuideInitially =
            savedInstanceState == null && !appSettingsStorage.homePreviewGuideShown.value

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val themeSeedColorArgb =
                appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value
            val invertOnPrimaryColors =
                appSettingsViewModel.invertOnPrimaryColors.state.collectAsState().value
            val excludeFromRecents =
                appSettingsViewModel.excludeFromRecents.state.collectAsState().value
            val topBarBlurEnabled =
                appSettingsViewModel.topBarBlurEnabled.state.collectAsState().value
            val pageScale = appSettingsViewModel.pageScale.state.collectAsState().value
            val useSystemWallpaper =
                appSettingsViewModel.useSystemWallpaper.state.collectAsState().value

            LaunchedEffect(excludeFromRecents) {
                this@MainActivity.applyExcludeFromRecents(excludeFromRecents)
            }

            ProvideAndroidPlatformTheme {
                val systemDensity = LocalDensity.current
                val scaledDensity =
                    remember(systemDensity, pageScale) {
                        Density(systemDensity.density * pageScale, systemDensity.fontScale)
                    }
                CompositionLocalProvider(LocalDensity provides scaledDensity) {
                    YumeTheme(
                        themeMode = themeMode,
                        themeSeedColorArgb = themeSeedColorArgb,
                        invertOnPrimaryColors = invertOnPrimaryColors,
                    ) {
                        val topBarHazeState = remember { HazeState() }
                        val topBarBackground = MiuixTheme.colorScheme.surface
                        val topBarHazeStyle = YumeHaze.topBarStyle(topBarBackground)
                        CompositionLocalProvider(
                            LocalTopBarHazeState provides
                                    if (topBarBlurEnabled) topBarHazeState else null,
                            LocalTopBarHazeStyle provides
                                    if (topBarBlurEnabled) topBarHazeStyle else null,
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MiuixTheme.colorScheme.surface,
                            ) {
                                AppNavContainer(navigationComponent)
                                ToastDialogHost()

                                var showHomeGuide by remember {
                                    mutableStateOf(showHomeGuideInitially)
                                }
                                HomePreviewGuideDialog(
                                    show = showHomeGuide,
                                    useSystemWallpaper = useSystemWallpaper,
                                    onUseSystemWallpaperChange =
                                        appSettingsViewModel::onUseSystemWallpaperChange,
                                    onDismissRequest = {
                                        showHomeGuide = false
                                        appSettingsStorage.homePreviewGuideShown.set(true)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        applicationScope.launch {
            if (!AutoStartSessionGate.tryBeginAutoActions()) {
                return@launch
            }
            var handled = false
            try {
                StartupTaskCoordinator.awaitWarmup()
                with(
                    AutoStartDependencies(
                        featureStore = featureStore,
                        proxyFacade = proxyFacade,
                        profilesRepository = profilesRepository,
                        appSettingsStorage = appSettingsStorage,
                        networkSettingsStorage = networkSettingsStorage,
                        serviceCache = serviceCache,
                    )
                ) {
                    ProxyAutoStartHelper.checkAndAutoStart(this@MainActivity)
                }
                handled = true
            } finally {
                AutoStartSessionGate.finishAutoActions(markHandled = handled)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STARTUP_PERMISSIONS) {
            window.decorView.post(::requestSystemWallpaperPermissionIfNeeded)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level < TRIM_MEMORY_UI_HIDDEN || isFinishing) {
            return
        }
        if (!featureStore.exitUiWhenBackground.value) {
            return
        }
        if (proxyFacade.runtimeSnapshot.value.running) {
            finishAndRemoveTask()
        }
    }

    private fun handleIntent(intent: Intent?) {
        val safeIntent = intent ?: return
        if (safeIntent.getBooleanExtra(EXTRA_EXIT_UI_WHEN_BACKGROUND, false)) {
            finishAndRemoveTask()
            return
        }
        safeIntent.data?.let { handleDeepLinkUri(it) }

        intentController.handleIntent(safeIntent)
    }

    private fun handleDeepLinkUri(uri: Uri) {
        when (uri.scheme) {
            "clash",
            "clashmeta" -> {
                if (uri.host != "install-config") return
                val configUrl = uri.getQueryParameter("url")
                if (!configUrl.isNullOrBlank()) {
                    _pendingImportUrl.value = configUrl
                }
            }

            "yumebox" -> _pendingDeepLink.value = uri.toString()
        }
    }

    /**
     * On launch, auto-request the two runtime permissions the app needs: notifications (Android
     * 13+) and the MIUI dynamic "get installed apps" permission. Both are fired in a single system
     * dialog sequence; permissions that aren't runtime-requestable on this device/OS are simply
     * skipped.
     */
    private fun requestStartupPermissions(): Boolean {
        val permissions = buildList {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (
                isMiuiGetInstalledAppsDynamicSupported() &&
                checkSelfPermission(MIUI_GET_INSTALLED_APPS_PERMISSION) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                add(MIUI_GET_INSTALLED_APPS_PERMISSION)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), REQUEST_STARTUP_PERMISSIONS)
        }
        return permissions.isNotEmpty()
    }

    private fun requestSystemWallpaperPermissionIfNeeded() {
        if (systemWallpaperPermissionRequestHandled) return
        systemWallpaperPermissionRequestHandled = true
        if (!appSettingsStore.useSystemWallpaper.value) return
        if (SystemWallpaperAccess.isGranted(this)) {
            appSettingsStore.systemWallpaperPermissionRequested.set(true)
            return
        }

        appSettingsStore.systemWallpaperPermissionRequested.set(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startActivity(SystemWallpaperAccess.settingsIntent(this))
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_SYSTEM_WALLPAPER_PERMISSION,
            )
        }
    }

    private fun isMiuiGetInstalledAppsDynamicSupported(): Boolean = runCatching {
        packageManager.getPermissionInfo(MIUI_GET_INSTALLED_APPS_PERMISSION, 0).packageName ==
                "com.lbe.security.miui"
    }
        .getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun applyExcludeFromRecents(exclude: Boolean) {
        runCatching {
            val am = getSystemService(ActivityManager::class.java) ?: return@runCatching
            val currentTaskId = taskId
            val task =
                am.appTasks.firstOrNull { appTask: ActivityManager.AppTask ->
                    val taskInfo = appTask.taskInfo ?: return@firstOrNull false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        taskInfo.taskId == currentTaskId
                    } else {
                        taskInfo.id == currentTaskId
                    }
                }
            task?.setExcludeFromRecents(exclude)
        }
    }
}
