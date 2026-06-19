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

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.util.acgWallpaperFile
import com.github.yumelira.yumebox.data.controller.AppSettingsController
import com.github.yumelira.yumebox.data.model.AppColorTheme
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.presentation.theme.DEFAULT_CUSTOM_THEME_SEED_ARGB
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSettingsViewModel(
    private val application: Application,
    private val settings: AppSettingsStore,
    private val featureStore: FeatureStore,
    private val controller: AppSettingsController,
) : ViewModel() {

    val initialSetupCompleted: Preference<Boolean> = settings.initialSetupCompleted
    val privacyPolicyAccepted: Preference<Boolean> = settings.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = settings.themeMode
    val appLanguage: Preference<AppLanguage> = settings.appLanguage
    val colorTheme: Preference<AppColorTheme> = settings.colorTheme
    val themeSeedColorArgb: Preference<Long> = settings.themeAccentColorArgb
    val invertOnPrimaryColors: Preference<Boolean> = settings.invertOnPrimaryColors
    val automaticRestart: Preference<Boolean> = settings.automaticRestart
    val autoUpdateCurrentProfileOnStart: Preference<Boolean> =
        settings.autoUpdateCurrentProfileOnStart
    val hideAppIcon: Preference<Boolean> = settings.hideAppIcon
    val excludeFromRecents: Preference<Boolean> = settings.excludeFromRecents
    val showTrafficNotification: Preference<Boolean> = settings.showTrafficNotification
    val bottomBarAutoHide: Preference<Boolean> = settings.bottomBarAutoHide
    val bottomBarUseLegacyStyle: Preference<Boolean> = settings.bottomBarUseLegacyStyle
    val topBarBlurEnabled: Preference<Boolean> = settings.topBarBlurEnabled
    val predictiveBackEnabled: Preference<Boolean> = settings.predictiveBackEnabled
    val smoothCornerEnabled: Preference<Boolean> = settings.smoothCornerEnabled
    val acgMainUiEnabled: Preference<Boolean> = settings.acgMainUiEnabled
    val acgWallpaperUri: Preference<String> = settings.acgWallpaperUri
    val acgWallpaperSourceUri: Preference<String> = settings.acgWallpaperSourceUri
    val acgWallpaperZoom: Preference<Float> = settings.acgWallpaperZoom
    val acgWallpaperBiasX: Preference<Float> = settings.acgWallpaperBiasX
    val acgWallpaperBiasY: Preference<Float> = settings.acgWallpaperBiasY
    val acgHomeQuote: Preference<String> = settings.acgHomeQuote
    val acgHomeQuoteAuthor: Preference<String> = settings.acgHomeQuoteAuthor
    val acgSidebarExpanded: Preference<Boolean> = settings.acgSidebarExpanded
    val pageScale: Preference<Float> = settings.pageScale
    val singleNodeTest: Preference<Boolean> = settings.singleNodeTest
    val screenshotProtectionEnabled: Preference<Boolean> = settings.screenshotProtectionEnabled
    val biometricUnlockEnabled: Preference<Boolean> = settings.biometricUnlockEnabled
    val exitUiWhenBackground: Preference<Boolean> = featureStore.exitUiWhenBackground

    val customUserAgent: Preference<String> = settings.customUserAgent

    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)

    fun onAppLanguageChange(language: AppLanguage) = controller.applyAppLanguage(language)

    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)

    fun onThemeSeedColorChange(argb: Long) = themeSeedColorArgb.set(argb)

    fun onInvertOnPrimaryColorsChange(enabled: Boolean) = invertOnPrimaryColors.set(enabled)

    fun resetThemeSeedColor() = themeSeedColorArgb.set(DEFAULT_CUSTOM_THEME_SEED_ARGB)

    fun onBottomBarAutoHideChange(enabled: Boolean) = bottomBarAutoHide.set(enabled)

    fun onBottomBarUseLegacyStyleChange(enabled: Boolean) = bottomBarUseLegacyStyle.set(enabled)

    fun onTopBarBlurEnabledChange(enabled: Boolean) = topBarBlurEnabled.set(enabled)

    fun onPredictiveBackEnabledChange(enabled: Boolean) = predictiveBackEnabled.set(enabled)

    fun onSmoothCornerEnabledChange(enabled: Boolean) = smoothCornerEnabled.set(enabled)

    fun onAcgMainUiEnabledChange(enabled: Boolean) = acgMainUiEnabled.set(enabled)

    fun onAcgWallpaperUriChange(uri: String) = acgWallpaperUri.set(uri)

    /**
     * Persists the selected ACG wallpaper by copying [sourceUri] into the app-private files dir and
     * storing the resulting `file://` path as [acgWallpaperUri], while remembering the original
     * source in [acgWallpaperSourceUri] for lazy re-import. If the copy fails the original source URI
     * is persisted directly (degraded but working) and a toast is shown.
     */
    fun applyAcgWallpaper(sourceUri: String, onApplied: () -> Unit) {
        viewModelScope.launch {
            val localPath = AcgWallpaperImporter.importToLocal(application, sourceUri)
            if (localPath != null) {
                acgWallpaperUri.set(localPath)
                acgWallpaperSourceUri.set(sourceUri)
            } else {
                acgWallpaperUri.set(sourceUri)
                acgWallpaperSourceUri.set(sourceUri)
                application.toast(MLang.AppSettings.Experimental.WallpaperImportFailed)
            }
            onApplied()
        }
    }

    fun onAcgWallpaperCropChange(zoom: Float, biasX: Float, biasY: Float) {
        acgWallpaperZoom.set(zoom.coerceIn(1f, 5f))
        acgWallpaperBiasX.set(biasX.coerceIn(-1f, 1f))
        acgWallpaperBiasY.set(biasY.coerceIn(-1f, 1f))
    }

    fun onAcgHomeQuoteChange(quote: String) = acgHomeQuote.set(quote)

    fun onAcgHomeQuoteAuthorChange(author: String) = acgHomeQuoteAuthor.set(author)

    fun onAcgSidebarExpandedChange(expanded: Boolean) = acgSidebarExpanded.set(expanded)

    fun clearAcgWallpaperUri() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { application.acgWallpaperFile().delete() } }
            acgWallpaperUri.set("")
            acgWallpaperSourceUri.set("")
            onAcgWallpaperCropChange(zoom = 1f, biasX = 0f, biasY = 0f)
        }
    }

    fun onPageScaleChange(scale: Float) = pageScale.set(scale)

    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)

    fun onAutoUpdateCurrentProfileOnStartChange(enabled: Boolean) =
        autoUpdateCurrentProfileOnStart.set(enabled)

    fun onHideAppIconChange(hide: Boolean) = hideAppIcon.set(hide)

    fun onExcludeFromRecentsChange(exclude: Boolean) = excludeFromRecents.set(exclude)

    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)

    fun onSingleNodeTestChange(enabled: Boolean) = singleNodeTest.set(enabled)

    fun onScreenshotProtectionEnabledChange(enabled: Boolean) =
        screenshotProtectionEnabled.set(enabled)

    fun onBiometricUnlockEnabledChange(enabled: Boolean) = biometricUnlockEnabled.set(enabled)

    fun onExitUiWhenBackgroundChange(enabled: Boolean) = exitUiWhenBackground.set(enabled)

    fun applyCustomUserAgent(userAgent: String) = controller.applyCustomUserAgent(userAgent)

    fun setInitialSetupCompleted(completed: Boolean) = initialSetupCompleted.set(completed)

    fun setPrivacyPolicyAccepted(accepted: Boolean) = privacyPolicyAccepted.set(accepted)
}
