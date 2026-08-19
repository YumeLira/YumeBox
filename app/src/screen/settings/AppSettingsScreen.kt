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

@file:Suppress("FunctionName", "ConvertLongToDuration")

package com.github.yumeyucca.yumebox.screen.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.core.net.toUri
import com.github.yumeyucca.yumebox.common.util.LocaleUtil
import com.github.yumeyucca.yumebox.common.util.openUrl
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.model.AppLanguage
import com.github.yumeyucca.yumebox.data.model.ThemeMode
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.screen.moe.SystemWallpaperPreferenceItem
import com.github.yumeyucca.yumebox.runtime.api.Intents
import com.github.yumeyucca.yumebox.screen.settings.component.ThemeColorPickerItem
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppSettingsScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()

    Scaffold(
        topBar = { TopBar(title = YumeTxt.AppSettings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item { AppBehaviorSettingsSection(viewModel) }
            item { AppInterfaceSettingsSection(viewModel = viewModel) }
            item { AppPrivacySettingsSection(viewModel) }
            item { AppServiceSettingsSection(viewModel) }
            item { AppNetworkSettingsSection(viewModel) }
        }
    }
}

@Composable
private fun AppBehaviorSettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.behaviorSectionState.collectAsState()
    val automaticRestart = section.automaticRestart
    val autoUpdateCurrentProfileOnStart = section.autoUpdateCurrentProfileOnStart
    val isChineseLocale = remember { LocaleUtil.isChineseLocale() }

    Title(YumeTxt.AppSettings.Section.Behavior)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Behavior.AutoStartTitle,
            checked = automaticRestart,
            onCheckedChange = viewModel::onAutomaticRestartChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Behavior.AutoUpdateOnStartTitle,
            checked = autoUpdateCurrentProfileOnStart,
            onCheckedChange = viewModel::onAutoUpdateCurrentProfileOnStartChange,
        )
        if (isChineseLocale) {
            PreferenceSwitchItem(
                title = YumeTxt.AppSettings.Behavior.OneChinaTitle,
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
    val section by viewModel.interfaceSectionState.collectAsState()
    val themeMode = section.themeMode
    val appLanguage = section.appLanguage
    val themeSeedColorArgb = section.themeSeedColorArgb
    val invertOnPrimaryColors = section.invertOnPrimaryColors
    val bottomBarAutoHide = section.bottomBarAutoHide
    val topBarBlurEnabled = section.topBarBlurEnabled
    val pageScale = section.pageScale
    val predictiveBackEnabled by viewModel.predictiveBackEnabled.state.collectAsState()
    val predictiveBackMaxProgress by viewModel.predictiveBackMaxProgress.state.collectAsState()
    val classicHomeEnabled = section.classicHomeEnabled
    val useSystemWallpaper = section.useSystemWallpaper

    Title(YumeTxt.AppSettings.Interface.ColorThemeTitle)
    AppCard {
        PreferenceEnumItem(
            title = YumeTxt.AppSettings.Interface.ThemeModeTitle,
            currentValue = themeMode,
            items =
                listOf(
                    YumeTxt.AppSettings.Interface.ThemeModeSystem,
                    YumeTxt.AppSettings.Interface.ThemeModeLight,
                    YumeTxt.AppSettings.Interface.ThemeModeDark,
                ),
            values = ThemeMode.entries,
            onValueChange = viewModel::onThemeModeChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.ThemeColorPolarityInvertTitle,
            checked = invertOnPrimaryColors,
            onCheckedChange = viewModel::onInvertOnPrimaryColorsChange,
        )
        ThemeColorPickerItem(
            themeSeedColorArgb = themeSeedColorArgb,
            onThemeSeedColorChange = viewModel::onThemeSeedColorChange,
        )
    }
    Title(YumeTxt.AppSettings.Section.Interface)
    AppCard {
        PreferenceEnumItem(
            title = YumeTxt.AppSettings.Interface.LanguageTitle,
            currentValue = appLanguage,
            items =
                listOf(
                    YumeTxt.AppSettings.Interface.LanguageSystem,
                    YumeTxt.AppSettings.Interface.LanguageChinese,
                    YumeTxt.AppSettings.Interface.LanguageChineseTraditional,
                    YumeTxt.AppSettings.Interface.LanguageEnglish,
                    YumeTxt.AppSettings.Interface.LanguageJapanese,
                    YumeTxt.AppSettings.Interface.LanguageRussian,
                ),
            values = AppLanguage.entries,
            onValueChange = viewModel::onAppLanguageChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.AutoHideNavbarTitle,
            checked = bottomBarAutoHide,
            onCheckedChange = viewModel::onBottomBarAutoHideChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.TopBarBlurTitle,
            checked = topBarBlurEnabled,
            onCheckedChange = viewModel::onTopBarBlurEnabledChange,
        )
        PageScalePreferenceItem(pageScale = pageScale, onApply = viewModel::onPageScaleChange)
    }
    Title(YumeTxt.AppSettings.Section.Navigation)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.PredictiveBackTitle,
            checked = predictiveBackEnabled,
            onCheckedChange = { enabled ->
                viewModel.onPredictiveBackEnabledChange(enabled)
                context.toast(YumeTxt.AppSettings.Interface.PredictiveBackRestartSummary)
            },
        )
        PredictiveBackProgressPreferenceItem(
            progress = predictiveBackMaxProgress,
            onApply = viewModel::onPredictiveBackMaxProgressChange,
        )
    }
    Title(YumeTxt.AppSettings.Section.Home)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.ClassicHomeTitle,
            checked = classicHomeEnabled,
            onCheckedChange = viewModel::onClassicHomeEnabledChange,
        )
        SystemWallpaperPreferenceItem(
            checked = useSystemWallpaper,
            onCheckedChange = viewModel::onUseSystemWallpaperChange,
        )
        PreferenceArrowItem(
            title = YumeTxt.AppSettings.Interface.CustomIconTitle,
            onClick = { openUrl(context, "https://yumebox.gal.tf/guide/icon-builder") },
        )
    }
}

@Composable
private fun AppPrivacySettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.privacySectionState.collectAsState()

    Title(YumeTxt.AppSettings.Section.Privacy)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Privacy.HideFromRecentsTitle,
            checked = section.excludeFromRecents,
            onCheckedChange = viewModel::onExcludeFromRecentsChange,
        )
    }
}

@Composable
private fun AppServiceSettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val section by viewModel.serviceSectionState.collectAsState()
    val showTrafficNotification = section.showTrafficNotification
    val singleNodeTest = section.singleNodeTest
    val exitUiWhenBackground = section.exitUiWhenBackground

    Title(YumeTxt.AppSettings.Section.Service)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.ServiceSection.TrafficNotificationTitle,
            checked = showTrafficNotification,
            onCheckedChange = viewModel::onShowTrafficNotificationChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.ServiceSection.SingleNodeTestTitle,
            checked = singleNodeTest,
            onCheckedChange = viewModel::onSingleNodeTestChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.ServiceSection.ExitUiWhenBackgroundTitle,
            checked = exitUiWhenBackground,
            onCheckedChange = viewModel::onExitUiWhenBackgroundChange,
        )
        PreferenceArrowItem(
            title = YumeTxt.AppSettings.ServiceSection.BatteryOptimizationTitle,
            onClick = {
                if (isBatteryOptimizationIgnored(context)) {
                    context.toast(YumeTxt.AppSettings.ServiceSection.BatteryOptimizationAlreadyDisabled)
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    )
                }
            },
        )
    }
}

@Composable
private fun AppNetworkSettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.networkSectionState.collectAsState()

    Title(YumeTxt.AppSettings.Section.Network)
    AppCard {
        CustomUserAgentPreferenceItem(
            customUserAgent = section.customUserAgent,
            onConfirm = viewModel::applyCustomUserAgent,
        )
    }
}

@SuppressLint("BatteryLife")
private fun isBatteryOptimizationIgnored(context: android.content.Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}


@Composable
private fun PageScalePreferenceItem(pageScale: Float, onApply: (Float) -> Unit) {
    var pageScaleLocal by remember(pageScale) { mutableFloatStateOf(pageScale) }
    val pageScalePercentText = remember(pageScaleLocal) { "${(pageScaleLocal * 100).toInt()}%" }
    val showPageScaleDialogState = remember { mutableStateOf(false) }

    PreferenceArrowItem(
        title = YumeTxt.AppSettings.Interface.PageScaleTitle,
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
private fun PredictiveBackProgressPreferenceItem(
    progress: Float,
    onApply: (Float) -> Unit,
) {
    var localProgress by remember(progress) { mutableFloatStateOf(progress) }
    val progressText = remember(localProgress) { "${localProgress.toInt()}%" }

    PreferenceArrowItem(
        title = YumeTxt.AppSettings.Interface.PredictiveBackProgressTitle,
        endActions = {
            Text(
                text = progressText,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = {},
        bottomAction = {
            Slider(
                value = localProgress,
                onValueChange = { localProgress = it },
                onValueChangeFinished = { onApply(localProgress) },
                valueRange = 1f..100f,
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            )
        },
    )
}

@Composable
private fun CustomUserAgentPreferenceItem(customUserAgent: String, onConfirm: (String) -> Unit) {
    val customUserAgentSummary =
        remember(customUserAgent) {
            customUserAgent.ifEmpty { YumeTxt.AppSettings.Network.CustomUserAgentSummaryDefault }
        }
    val showEditCustomUserAgentDialog = remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var localTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(text = customUserAgent, selection = TextRange(customUserAgent.length))
        )
    }

    PreferenceArrowItem(
        title = YumeTxt.AppSettings.Network.CustomUserAgentTitle,
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
        title = YumeTxt.AppSettings.EditDialog.UserAgentTitle,
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
        onImeAction = {
            onConfirm(localTextFieldValue.text)
            focusManager.clearFocus()
            showEditCustomUserAgentDialog.value = false
        },
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
        title = YumeTxt.AppSettings.Interface.PageScaleTitle,
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
        summary = YumeTxt.AppSettings.Interface.PageScaleDialogSummary,
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
