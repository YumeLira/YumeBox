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

package com.github.yumelira.yumebox

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.common.util.IntentController
import com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumelira.yumebox.core.util.AutoStartSessionGate
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.di.APPLICATION_SCOPE_NAME
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import com.github.yumelira.yumebox.presentation.component.StartupBiometricContent
import com.github.yumelira.yumebox.presentation.component.ToastDialogHost
import com.github.yumelira.yumebox.presentation.component.rememberStartupBiometricGateState
import com.github.yumelira.yumebox.presentation.theme.NavigationTransitions
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.screen.onboarding.OnboardingLauncher
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.tencent.mmkv.MMKV
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
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

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
        private const val EXTRA_EXIT_UI_WHEN_BACKGROUND = "exit_ui_when_background"
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()

        fun clearPendingImportUrl() {
            _pendingImportUrl.value = null
        }
    }

    private val appSettingsStorage: com.github.yumelira.yumebox.data.store.AppSettingsStore by
        inject()
    private val featureStore: FeatureStore by inject()
    private val networkSettingsStorage:
        com.github.yumelira.yumebox.data.store.NetworkSettingsStore by
        inject()
    private val profilesRepository: com.github.yumelira.yumebox.runtime.client.ProfilesRepository by
        inject()
    private val proxyFacade: com.github.yumelira.yumebox.runtime.client.ProxyFacade by inject()
    private val serviceCache: MMKV by inject(qualifier = named("service_cache"))
    private val applicationScope: CoroutineScope by
        inject(qualifier = named(APPLICATION_SCOPE_NAME))

    private lateinit var intentController: IntentController

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupGate.loadPrimary()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        applyExcludeFromRecents(appSettingsStorage.excludeFromRecents.value)
        applyScreenshotProtection(appSettingsStorage.screenshotProtectionEnabled.value)

        intentController = IntentController(lifecycleScope)
        handleIntent(intent)

        if (!appSettingsStorage.initialSetupCompleted.value) {
            OnboardingLauncher.start(this, previewMode = false)
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION,
                )
            }
        }

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val themeSeedColorArgb =
                appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value
            val invertOnPrimaryColors =
                appSettingsViewModel.invertOnPrimaryColors.state.collectAsState().value
            val smoothCornerEnabled =
                appSettingsViewModel.smoothCornerEnabled.state.collectAsState().value
            val excludeFromRecents =
                appSettingsViewModel.excludeFromRecents.state.collectAsState().value
            val topBarBlurEnabled =
                appSettingsViewModel.topBarBlurEnabled.state.collectAsState().value
            val pageScale = appSettingsViewModel.pageScale.state.collectAsState().value
            val screenshotProtectionEnabled =
                appSettingsViewModel.screenshotProtectionEnabled.state.collectAsState().value
            val biometricUnlockEnabled by
                appSettingsViewModel.biometricUnlockEnabled.state.collectAsState()

            val biometricGateState =
                rememberStartupBiometricGateState(
                    activity = this@MainActivity,
                    biometricUnlockEnabled = biometricUnlockEnabled,
                )

            LaunchedEffect(excludeFromRecents) {
                this@MainActivity.applyExcludeFromRecents(excludeFromRecents)
            }

            LaunchedEffect(screenshotProtectionEnabled) {
                this@MainActivity.applyScreenshotProtection(screenshotProtectionEnabled)
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
                        smoothCornerEnabled = smoothCornerEnabled,
                    ) {
                        if (!biometricGateState.isAuthenticated) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MiuixTheme.colorScheme.surface,
                            ) {
                                StartupBiometricContent(
                                    isAuthenticating = biometricGateState.isAuthenticating,
                                    biometricErrorMessage =
                                        biometricGateState.biometricErrorMessage,
                                    onRetry = biometricGateState.retryAuthentication,
                                    onExit = { finishAndRemoveTask() },
                                )
                            }
                        } else {
                            val topBarHazeState = remember { HazeState() }
                            val topBarBackground = MiuixTheme.colorScheme.surface
                            val topBarHazeStyle =
                                remember(topBarBackground) {
                                    HazeStyle(
                                        backgroundColor = topBarBackground,
                                        tint = HazeTint(topBarBackground.copy(0.8f)),
                                    )
                                }
                            val navController = rememberNavController()

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
                                    DestinationsNavHost(
                                        navGraph = NavGraphs.root,
                                        navController = navController,
                                        defaultTransitions = NavigationTransitions.defaultStyle,
                                    )
                                    ToastDialogHost()
                                }
                            }
                        }
                    }
                }
            }
        }

        applicationScope.launch {
            if (!AutoStartSessionGate.tryBeginForegroundAutoActions()) {
                return@launch
            }
            var handled = false
            try {
                StartupTaskCoordinator.awaitRuntimeWarmup()
                ProxyAutoStartHelper.checkAndAutoStart(
                    context = this@MainActivity,
                    featureStore = featureStore,
                    proxyFacade = proxyFacade,
                    profilesRepository = profilesRepository,
                    appSettingsStorage = appSettingsStorage,
                    networkSettingsStorage = networkSettingsStorage,
                    serviceCache = serviceCache,
                )
                handled = true
            } finally {
                AutoStartSessionGate.finishForegroundAutoActions(markHandled = handled)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
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
        intent?.let { safeIntent ->
            if (safeIntent.getBooleanExtra(EXTRA_EXIT_UI_WHEN_BACKGROUND, false)) {
                finishAndRemoveTask()
                return
            }
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

    private fun applyScreenshotProtection(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

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
