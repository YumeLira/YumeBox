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

package com.github.yumelira.yumebox.screen.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.common.util.BiometricHelper
import com.github.yumelira.yumebox.common.util.LocaleUtil
import com.github.yumelira.yumebox.common.util.PredictiveBackCompat
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumelira.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumelira.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumelira.yumebox.presentation.component.PreferenceValueItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TextEditBottomSheet
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.WarningBottomSheet
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AcgWallpaperCropScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AppSettingsScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()

    Scaffold(
        topBar = { TopBar(title = MLang.AppSettings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item { AppBehaviorSettingsSection(viewModel) }
            item { AppInterfaceSettingsSection(viewModel) }
            item { AppPrivacySettingsSection(viewModel) }
            item { AppServiceSettingsSection(viewModel) }
            item { AppNetworkSettingsSection(viewModel) }
            item { AppExperimentalSettingsSection(viewModel = viewModel, navigator = navigator) }
        }
    }
}

@Composable
private fun AppBehaviorSettingsSection(viewModel: AppSettingsViewModel) {
    val automaticRestart by viewModel.automaticRestart.state.collectAsState()
    val autoUpdateCurrentProfileOnStart by
        viewModel.autoUpdateCurrentProfileOnStart.state.collectAsState()
    val isChineseLocale = remember { LocaleUtil.isChineseLocale() }

    Title(MLang.AppSettings.Section.Behavior)
    Card {
        PreferenceSwitchItem(
            title = MLang.AppSettings.Behavior.AutoStartTitle,
            summary = MLang.AppSettings.Behavior.AutoStartSummary,
            checked = automaticRestart,
            onCheckedChange = viewModel::onAutomaticRestartChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Behavior.AutoUpdateOnStartTitle,
            summary = MLang.AppSettings.Behavior.AutoUpdateOnStartSummary,
            checked = autoUpdateCurrentProfileOnStart,
            onCheckedChange = viewModel::onAutoUpdateCurrentProfileOnStartChange,
        )
        if (isChineseLocale) {
            PreferenceSwitchItem(
                title = MLang.AppSettings.Behavior.OneChinaTitle,
                summary = MLang.AppSettings.Behavior.OneChinaSummary,
                checked = true,
                onCheckedChange = {},
                enabled = false,
            )
        }
    }
}

@Composable
private fun AppInterfaceSettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.state.collectAsState()
    val appLanguage by viewModel.appLanguage.state.collectAsState()
    val themeSeedColorArgb by viewModel.themeSeedColorArgb.state.collectAsState()
    val invertOnPrimaryColors by viewModel.invertOnPrimaryColors.state.collectAsState()
    val predictiveBackEnabled by viewModel.predictiveBackEnabled.state.collectAsState()
    val smoothCornerEnabled by viewModel.smoothCornerEnabled.state.collectAsState()
    val bottomBarAutoHide by viewModel.bottomBarAutoHide.state.collectAsState()
    val bottomBarUseLegacyStyle by viewModel.bottomBarUseLegacyStyle.state.collectAsState()
    val topBarBlurEnabled by viewModel.topBarBlurEnabled.state.collectAsState()
    val pageScale by viewModel.pageScale.state.collectAsState()

    Title(MLang.AppSettings.Interface.ColorThemeTitle)
    Card {
        PreferenceEnumItem(
            title = MLang.AppSettings.Interface.ThemeModeTitle,
            summary = MLang.AppSettings.Interface.ThemeModeSummary,
            currentValue = themeMode,
            items =
                listOf(
                    MLang.AppSettings.Interface.ThemeModeSystem,
                    MLang.AppSettings.Interface.ThemeModeLight,
                    MLang.AppSettings.Interface.ThemeModeDark,
                ),
            values = ThemeMode.entries,
            onValueChange = viewModel::onThemeModeChange,
        )
        ThemeColorPickerItem(
            themeSeedColorArgb = themeSeedColorArgb,
            onThemeSeedColorChange = viewModel::onThemeSeedColorChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.ThemeColorPolarityInvertTitle,
            summary = MLang.AppSettings.Interface.ThemeColorPolarityInvertSummary,
            checked = invertOnPrimaryColors,
            onCheckedChange = viewModel::onInvertOnPrimaryColorsChange,
        )
    }
    Title(MLang.AppSettings.Section.Interface)
    Card {
        PreferenceEnumItem(
            title = MLang.AppSettings.Interface.LanguageTitle,
            summary = MLang.AppSettings.Interface.LanguageSummary,
            currentValue = appLanguage,
            items =
                listOf(
                    MLang.AppSettings.Interface.LanguageSystem,
                    MLang.AppSettings.Interface.LanguageChinese,
                    MLang.AppSettings.Interface.LanguageEnglish,
                ),
            values = AppLanguage.entries,
            onValueChange = viewModel::onAppLanguageChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.AutoHideNavbarTitle,
            summary = MLang.AppSettings.Interface.AutoHideNavbarSummary,
            checked = bottomBarAutoHide,
            onCheckedChange = viewModel::onBottomBarAutoHideChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.LegacyNavbarStyleTitle,
            summary = MLang.AppSettings.Interface.LegacyNavbarStyleSummary,
            checked = bottomBarUseLegacyStyle,
            onCheckedChange = viewModel::onBottomBarUseLegacyStyleChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.TopBarBlurTitle,
            summary = MLang.AppSettings.Interface.TopBarBlurSummary,
            checked = topBarBlurEnabled,
            onCheckedChange = viewModel::onTopBarBlurEnabledChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.PredictiveBackTitle,
            summary = MLang.AppSettings.Interface.PredictiveBackSummary,
            checked = predictiveBackEnabled,
            onCheckedChange = { enabled ->
                viewModel.onPredictiveBackEnabledChange(enabled)
                PredictiveBackCompat.apply(context.applicationInfo, enabled)
                BiometricHelper.findFragmentActivity(context)?.recreate()
            },
            enabled = PredictiveBackCompat.isSupported,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.SmoothCornerTitle,
            summary = MLang.AppSettings.Interface.SmoothCornerSummary,
            checked = smoothCornerEnabled,
            onCheckedChange = viewModel::onSmoothCornerEnabledChange,
        )
        PageScalePreferenceItem(pageScale = pageScale, onApply = viewModel::onPageScaleChange)
    }
}

@Composable
private fun AppPrivacySettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val excludeFromRecents by viewModel.excludeFromRecents.state.collectAsState()

    Title(MLang.AppSettings.Section.Privacy)
    Card {
        BiometricProtectedPreferenceSwitch(
            checkedFlow = viewModel.biometricUnlockEnabled.state,
            title = MLang.AppSettings.Privacy.BiometricUnlockTitle,
            summary = MLang.AppSettings.Privacy.BiometricUnlockSummary,
            enableTitle = MLang.AppSettings.Privacy.BiometricDialogTitleEnable,
            disableTitle = MLang.AppSettings.Privacy.BiometricDialogTitleDisable,
            onConfirmedChange = viewModel::onBiometricUnlockEnabledChange,
        )
        BiometricProtectedPreferenceSwitch(
            checkedFlow = viewModel.screenshotProtectionEnabled.state,
            title = MLang.AppSettings.Privacy.ScreenshotProtectionTitle,
            summary = MLang.AppSettings.Privacy.ScreenshotProtectionSummary,
            enableTitle = MLang.AppSettings.Privacy.ScreenshotDialogTitleEnable,
            disableTitle = MLang.AppSettings.Privacy.ScreenshotDialogTitleDisable,
            onConfirmedChange = viewModel::onScreenshotProtectionEnabledChange,
        )
        HideAppIconPreferenceItem(
            hideAppIconFlow = viewModel.hideAppIcon.state,
            onHideAppIconChange = viewModel::onHideAppIconChange,
            context = context,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Privacy.HideFromRecentsTitle,
            summary = MLang.AppSettings.Privacy.HideFromRecentsSummary,
            checked = excludeFromRecents,
            onCheckedChange = viewModel::onExcludeFromRecentsChange,
        )
    }
}

@Composable
private fun AppServiceSettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val showTrafficNotification by viewModel.showTrafficNotification.state.collectAsState()
    val singleNodeTest by viewModel.singleNodeTest.state.collectAsState()
    val exitUiWhenBackground by viewModel.exitUiWhenBackground.state.collectAsState()
    var batteryOptimizationIgnored by remember {
        mutableStateOf(isBatteryOptimizationIgnored(context))
    }
    val batteryOptimizationSummary =
        remember(batteryOptimizationIgnored) {
            if (batteryOptimizationIgnored) {
                MLang.AppSettings.ServiceSection.BatteryOptimizationSummaryEnabled
            } else {
                MLang.AppSettings.ServiceSection.BatteryOptimizationSummaryDisabled
            }
        }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Title(MLang.AppSettings.Section.Service)
    Card {
        PreferenceSwitchItem(
            title = MLang.AppSettings.ServiceSection.TrafficNotificationTitle,
            summary = MLang.AppSettings.ServiceSection.TrafficNotificationSummary,
            checked = showTrafficNotification,
            onCheckedChange = viewModel::onShowTrafficNotificationChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.ServiceSection.SingleNodeTestTitle,
            summary = MLang.AppSettings.ServiceSection.SingleNodeTestSummary,
            checked = singleNodeTest,
            onCheckedChange = viewModel::onSingleNodeTestChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.ServiceSection.ExitUiWhenBackgroundTitle,
            summary = MLang.AppSettings.ServiceSection.ExitUiWhenBackgroundSummary,
            checked = exitUiWhenBackground,
            onCheckedChange = viewModel::onExitUiWhenBackgroundChange,
        )
        PreferenceArrowItem(
            title = MLang.AppSettings.ServiceSection.BatteryOptimizationTitle,
            summary = batteryOptimizationSummary,
            onClick = {
                if (!openBatteryOptimizationSettings(context, batteryOptimizationIgnored)) {
                    context.toast(MLang.Util.Error.UnknownError)
                }
            },
        )
    }
}

@Composable
private fun AppNetworkSettingsSection(viewModel: AppSettingsViewModel) {
    val customUserAgent by viewModel.customUserAgent.state.collectAsState()

    Title(MLang.AppSettings.Section.Network)
    Card {
        CustomUserAgentPreferenceItem(
            customUserAgent = customUserAgent,
            onConfirm = viewModel::applyCustomUserAgent,
        )
    }
}

@Composable
private fun AppExperimentalSettingsSection(
    viewModel: AppSettingsViewModel,
    navigator: DestinationsNavigator,
) {
    val acgMainUiEnabled by viewModel.acgMainUiEnabled.state.collectAsState()
    val acgHomeQuote by viewModel.acgHomeQuote.state.collectAsState()
    val acgHomeQuoteAuthor by viewModel.acgHomeQuoteAuthor.state.collectAsState()
    val acgSidebarExpanded by viewModel.acgSidebarExpanded.state.collectAsState()
    val acgQuoteSummary =
        remember(acgHomeQuote) {
            acgHomeQuote.ifBlank { MLang.AppSettings.Experimental.AcgQuoteDefault }
        }
    val acgQuoteAuthorSummary =
        remember(acgHomeQuoteAuthor) {
            acgHomeQuoteAuthor.ifBlank { MLang.AppSettings.Experimental.AcgQuoteAuthorDefault }
        }
    Title(MLang.AppSettings.Section.Experimental)
    Card {
        PreferenceSwitchItem(
            title = MLang.AppSettings.Experimental.AcgHomeTitle,
            summary = MLang.AppSettings.Experimental.AcgHomeSummary,
            checked = acgMainUiEnabled,
            onCheckedChange = viewModel::onAcgMainUiEnabledChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Experimental.AcgSidebarExpandedTitle,
            summary = MLang.AppSettings.Experimental.AcgSidebarExpandedSummary,
            checked = acgSidebarExpanded,
            onCheckedChange = viewModel::onAcgSidebarExpandedChange,
        )
        AcgQuotePreferenceItem(
            title = MLang.AppSettings.Experimental.AcgQuoteTitle,
            summary = acgQuoteSummary,
            dialogTitle = MLang.AppSettings.Experimental.EditAcgQuoteTitle,
            currentValue = acgHomeQuote,
            onConfirm = viewModel::onAcgHomeQuoteChange,
        )
        AcgQuotePreferenceItem(
            title = MLang.AppSettings.Experimental.AcgQuoteAuthorTitle,
            summary = acgQuoteAuthorSummary,
            dialogTitle = MLang.AppSettings.Experimental.EditAcgQuoteAuthorTitle,
            currentValue = acgHomeQuoteAuthor,
            onConfirm = viewModel::onAcgHomeQuoteAuthorChange,
        )
        AcgWallpaperPreferenceItem(
            navigator = navigator,
            wallpaperZoom = viewModel.acgWallpaperZoom.value,
            wallpaperBiasX = viewModel.acgWallpaperBiasX.value,
            wallpaperBiasY = viewModel.acgWallpaperBiasY.value,
        )
    }
}

@Composable
private fun BiometricProtectedPreferenceSwitch(
    checkedFlow: kotlinx.coroutines.flow.StateFlow<Boolean>,
    title: String,
    summary: String,
    enableTitle: String,
    disableTitle: String,
    onConfirmedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val checked by checkedFlow.collectAsState()
    val showUnavailableDialogState = remember { mutableStateOf(false) }
    var unavailableMessage by remember { mutableStateOf("") }

    PreferenceSwitchItem(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = { targetState ->
            requestBiometricConfirmation(
                context = context,
                title = if (targetState) enableTitle else disableTitle,
                allowBypassWhenUnavailable = !targetState,
                onUnavailable = { message ->
                    unavailableMessage = message
                    showUnavailableDialogState.value = true
                },
                onSuccess = { onConfirmedChange(targetState) },
            )
        },
    )

    WarningBottomSheet(
        show = showUnavailableDialogState,
        title = MLang.AppSettings.Privacy.BiometricUnavailableTitle,
        messages =
            listOf(
                unavailableMessage.ifBlank { MLang.AppSettings.Privacy.BiometricUnavailableMessage }
            ),
        onConfirm = { showUnavailableDialogState.value = false },
    )
}

@Composable
private fun HideAppIconPreferenceItem(
    hideAppIconFlow: kotlinx.coroutines.flow.StateFlow<Boolean>,
    onHideAppIconChange: (Boolean) -> Unit,
    context: android.content.Context,
) {
    val hideAppIcon by hideAppIconFlow.collectAsState()
    val showHideIconDialogState = remember { mutableStateOf(false) }

    PreferenceSwitchItem(
        title = MLang.AppSettings.Privacy.HideIconTitle,
        summary = MLang.AppSettings.Privacy.HideIconSummary,
        checked = hideAppIcon,
        onCheckedChange = { checked ->
            if (checked) {
                showHideIconDialogState.value = true
            } else {
                onHideAppIconChange(false)
                AppIconHelper.toggleIcon(context, false)
            }
        },
    )

    WarningBottomSheet(
        show = showHideIconDialogState,
        title = MLang.AppSettings.WarningDialog.Title,
        messages =
            listOf(
                MLang.AppSettings.WarningDialog.HideIconMsg1,
                MLang.AppSettings.WarningDialog.HideIconMsg2,
            ),
        onConfirm = {
            onHideAppIconChange(true)
            AppIconHelper.toggleIcon(context, true)
        },
    )
}

@Composable
private fun AcgQuotePreferenceItem(
    title: String,
    summary: String,
    dialogTitle: String,
    currentValue: String,
    onConfirm: (String) -> Unit,
) {
    val showEditDialogState = remember { mutableStateOf(false) }
    val textFieldState = remember { mutableStateOf(TextFieldValue()) }

    PreferenceValueItem(
        title = title,
        summary = summary,
        onClick = {
            textFieldState.value = TextFieldValue(currentValue)
            showEditDialogState.value = true
        },
    )

    TextEditBottomSheet(
        show = showEditDialogState,
        title = dialogTitle,
        textFieldValue = textFieldState,
        onConfirm = onConfirm,
    )
}

@Composable
private fun AcgWallpaperPreferenceItem(
    navigator: DestinationsNavigator,
    wallpaperZoom: Float,
    wallpaperBiasX: Float,
    wallpaperBiasY: Float,
) {
    val context = LocalContext.current
    val wallpaperPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
            uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            navigator.navigate(
                AcgWallpaperCropScreenDestination(
                    wallpaperUri = uri.toString(),
                    initialZoom = wallpaperZoom,
                    initialBiasX = wallpaperBiasX,
                    initialBiasY = wallpaperBiasY,
                )
            ) {
                launchSingleTop = true
            }
        }

    PreferenceArrowItem(
        title = MLang.AppSettings.Experimental.WallpaperTitle,
        summary = MLang.AppSettings.Experimental.WallpaperSummary,
        onClick = {
            wallpaperPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
    )
}

private fun requestBiometricConfirmation(
    context: android.content.Context,
    title: String,
    allowBypassWhenUnavailable: Boolean = false,
    onUnavailable: (String) -> Unit,
    onSuccess: () -> Unit,
) {
    val activity = BiometricHelper.findFragmentActivity(context)
    if (activity == null) {
        if (allowBypassWhenUnavailable) {
            onSuccess()
        } else {
            onUnavailable(MLang.AppSettings.Privacy.BiometricUnavailableMessage)
        }
        return
    }

    if (!BiometricHelper.canAuthenticate(activity)) {
        if (allowBypassWhenUnavailable) {
            onSuccess()
        } else {
            onUnavailable(BiometricHelper.getAuthenticationStatusMessage(activity))
        }
        return
    }

    BiometricHelper.authenticate(activity = activity, title = title, onSuccess = onSuccess)
}

private fun isBatteryOptimizationIgnored(context: android.content.Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatteryOptimizationSettings(
    context: android.content.Context,
    alreadyIgnored: Boolean,
): Boolean {
    val intents = buildList {
        if (!alreadyIgnored) {
            add(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
        add(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    intents.forEach { intent ->
        if (runCatching { context.startActivity(intent) }.isSuccess) {
            return true
        }
    }
    return false
}

@Composable
private fun PageScalePreferenceItem(pageScale: Float, onApply: (Float) -> Unit) {
    var pageScaleLocal by remember(pageScale) { mutableFloatStateOf(pageScale) }
    val pageScalePercentText = remember(pageScaleLocal) { "${(pageScaleLocal * 100).toInt()}%" }
    val showPageScaleDialogState = remember { mutableStateOf(false) }

    PreferenceArrowItem(
        title = MLang.AppSettings.Interface.PageScaleTitle,
        summary = MLang.AppSettings.Interface.PageScaleSummary,
        endActions = {
            Text(
                text = pageScalePercentText,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = { showPageScaleDialogState.value = true },
        holdDownState = showPageScaleDialogState.value,
        bottomAction = {
            Slider(
                value = pageScaleLocal,
                onValueChange = { pageScaleLocal = it },
                onValueChangeFinished = { onApply(pageScaleLocal) },
                valueRange = 0.8f..1.2f,
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            )
        },
    )

    PageScaleDialog(
        show = showPageScaleDialogState.value,
        pageScale = pageScaleLocal,
        onPageScaleChange = { pageScaleLocal = it },
        onApply = onApply,
        onDismissRequest = { showPageScaleDialogState.value = false },
    )
}

@Composable
private fun CustomUserAgentPreferenceItem(customUserAgent: String, onConfirm: (String) -> Unit) {
    val customUserAgentSummary =
        remember(customUserAgent) {
            customUserAgent.ifEmpty { MLang.AppSettings.Network.CustomUserAgentSummaryDefault }
        }
    val showEditCustomUserAgentDialog = remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var localTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(text = customUserAgent, selection = TextRange(customUserAgent.length))
        )
    }

    PreferenceArrowItem(
        title = MLang.AppSettings.Network.CustomUserAgentTitle,
        summary = customUserAgentSummary,
        onClick = {
            localTextFieldValue =
                TextFieldValue(
                    text = customUserAgent,
                    selection = TextRange(customUserAgent.length),
                )
            showEditCustomUserAgentDialog.value = true
        },
        holdDownState = showEditCustomUserAgentDialog.value,
    )

    AppTextFieldDialog(
        show = showEditCustomUserAgentDialog.value,
        title = MLang.AppSettings.EditDialog.UserAgentTitle,
        textFieldValue = localTextFieldValue,
        onTextFieldValueChange = { updatedTextFieldValue ->
            localTextFieldValue = updatedTextFieldValue
        },
        onDismissRequest = {
            showEditCustomUserAgentDialog.value = false
            focusManager.clearFocus()
        },
        onConfirm = {
            onConfirm(localTextFieldValue.text)
            focusManager.clearFocus()
            showEditCustomUserAgentDialog.value = false
        },
        singleLine = true,
        keyboardActions =
            KeyboardActions(
                onDone = {
                    onConfirm(localTextFieldValue.text)
                    focusManager.clearFocus()
                    showEditCustomUserAgentDialog.value = false
                }
            ),
    )
}

@Composable
private fun PageScaleDialog(
    show: Boolean,
    pageScale: Float,
    onPageScaleChange: (Float) -> Unit,
    onApply: (Float) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var scaleText by
        remember(show, pageScale) { mutableStateOf((pageScale * 100).toInt().toString()) }

    AppTextFieldDialog(
        show = show,
        title = MLang.AppSettings.Interface.PageScaleTitle,
        value = scaleText,
        onValueChange = { value ->
            if (value.isEmpty() || value.all(Char::isDigit)) {
                scaleText = value
            }
        },
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val parsedPercent = scaleText.toFloatOrNull() ?: (pageScale * 100)
            val clampedScale = parsedPercent.coerceIn(80f, 120f) / 100f
            onPageScaleChange(clampedScale)
            onApply(clampedScale)
            onDismissRequest()
        },
        summary = MLang.AppSettings.Interface.PageScaleDialogSummary,
        renderInRootScaffold = true,
        singleLine = true,
        trailingIcon = {
            Text(
                text = "%",
                modifier = Modifier.padding(horizontal = UiDp.dp16),
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
    )
}
