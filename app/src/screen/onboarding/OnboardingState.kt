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

package com.github.yumelira.yumebox.screen.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel

private const val MIUI_GET_INSTALLED_APPS_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"

internal data class PrivacyAcceptedState(
    val accepted: Boolean,
    val onAcceptedChange: (Boolean) -> Unit,
)

internal data class ThemeCustomizationState(
    val themeMode: ThemeMode,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val themeSeedColorArgb: Long,
    val onThemeSeedColorChange: (Long) -> Unit,
)

@Composable
internal fun rememberPermissionState(
    context: Context,
    lifecycleOwner: LifecycleOwner,
): PermissionState {
    val miuiDynamicSupported = remember { isMiuiGetInstalledAppsDynamicSupported(context) }

    var notificationGranted by remember { mutableStateOf(isNotificationGranted(context)) }
    var appListGranted by remember {
        mutableStateOf(isAppListPermissionGranted(context, miuiDynamicSupported))
    }

    val requestNotificationPermission =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                notificationGranted = granted
                if (!granted) {
                    openAppNotificationSettings(context)
                }
            },
        )

    val requestMiuiGetInstalledAppsPermission =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                appListGranted = granted
                if (!granted) {
                    openAppDetailsSettings(context)
                }
            },
        )

    DisposableEffect(lifecycleOwner, context, miuiDynamicSupported) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = isNotificationGranted(context)
                appListGranted = isAppListPermissionGranted(context, miuiDynamicSupported)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return PermissionState(
        notificationGranted = notificationGranted,
        appListGranted = appListGranted,
        miuiDynamicSupported = miuiDynamicSupported,
        onRequestNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openAppNotificationSettings(context)
            }
        },
        onRequestAppList = {
            if (miuiDynamicSupported) {
                requestMiuiGetInstalledAppsPermission.launch(MIUI_GET_INSTALLED_APPS_PERMISSION)
            } else {
                openAppDetailsSettings(context)
            }
        },
    )
}

@Composable
internal fun rememberPrivacyAcceptedState(
    appSettingsViewModel: AppSettingsViewModel
): PrivacyAcceptedState {
    val accepted by appSettingsViewModel.privacyPolicyAccepted.state.collectAsState()
    return PrivacyAcceptedState(
        accepted = accepted,
        onAcceptedChange = appSettingsViewModel::setPrivacyPolicyAccepted,
    )
}

@Composable
internal fun rememberThemeCustomizationState(
    appSettingsViewModel: AppSettingsViewModel
): ThemeCustomizationState {
    val themeMode by appSettingsViewModel.themeMode.state.collectAsState()
    val themeSeedColorArgb by appSettingsViewModel.themeSeedColorArgb.state.collectAsState()
    return ThemeCustomizationState(
        themeMode = themeMode,
        onThemeModeChange = appSettingsViewModel::onThemeModeChange,
        themeSeedColorArgb = themeSeedColorArgb,
        onThemeSeedColorChange = appSettingsViewModel::onThemeSeedColorChange,
    )
}

internal fun openAppNotificationSettings(context: Context) {
    val intent =
        Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(intent) }
        .recoverCatching { openAppDetailsSettings(context) }
}

internal fun openAppDetailsSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(intent)
}

internal fun isMiuiGetInstalledAppsDynamicSupported(context: Context): Boolean {
    return runCatching {
            val permissionInfo =
                context.packageManager.getPermissionInfo(MIUI_GET_INSTALLED_APPS_PERMISSION, 0)
            permissionInfo.packageName == "com.lbe.security.miui"
        }
        .getOrDefault(false)
}

internal fun isAppListPermissionGranted(context: Context, miuiDynamicSupported: Boolean): Boolean {
    if (miuiDynamicSupported) {
        return context.checkSelfPermission(MIUI_GET_INSTALLED_APPS_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
    }

    return runCatching {
            context.packageManager.getInstalledApplications(0).any {
                it.packageName != context.packageName
            }
        }
        .getOrDefault(false)
}

internal fun isNotificationGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}
