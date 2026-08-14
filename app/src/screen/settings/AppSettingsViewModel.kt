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

@file:Suppress("UnusedSymbol", "CanBeParameter")

package com.github.yumeyucca.yumebox.screen.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.util.moeWallpaperFile
import com.github.yumeyucca.yumebox.data.controller.AppSettingsController
import com.github.yumeyucca.yumebox.data.model.AppColorTheme
import com.github.yumeyucca.yumebox.data.model.AppLanguage
import com.github.yumeyucca.yumebox.data.model.ThemeMode
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.data.store.Preference
import com.github.yumeyucca.yumebox.presentation.theme.DEFAULT_CUSTOM_THEME_SEED_ARGB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tf.gal.yumebox.locale.YumeTxt

class AppSettingsViewModel(
    private val application: Application,
    private val settings: AppSettingsStore,
    private val featureStore: FeatureStore,
    private val controller: AppSettingsController,
) : ViewModel() {
    val themeMode: Preference<ThemeMode> = settings.themeMode
    val appLanguage: Preference<AppLanguage> = settings.appLanguage
    val colorTheme: Preference<AppColorTheme> = settings.colorTheme
    val themeSeedColorArgb: Preference<Long> = settings.themeAccentColorArgb
    val invertOnPrimaryColors: Preference<Boolean> = settings.invertOnPrimaryColors
    val automaticRestart: Preference<Boolean> = settings.automaticRestart
    val autoUpdateCurrentProfileOnStart: Preference<Boolean> =
        settings.autoUpdateCurrentProfileOnStart
    val excludeFromRecents: Preference<Boolean> = settings.excludeFromRecents
    val showTrafficNotification: Preference<Boolean> = settings.showTrafficNotification
    val bottomBarAutoHide: Preference<Boolean> = settings.bottomBarAutoHide
    val topBarBlurEnabled: Preference<Boolean> = settings.topBarBlurEnabled
    val classicHomeEnabled: Preference<Boolean> = settings.classicHomeEnabled
    val useSystemWallpaper: Preference<Boolean> = settings.useSystemWallpaper
    val moeWallpaperUri: Preference<String> = settings.moeWallpaperUri
    val moeWallpaperSourceUri: Preference<String> = settings.moeWallpaperSourceUri
    val moeWallpaperZoom: Preference<Float> = settings.moeWallpaperZoom
    val moeWallpaperBiasX: Preference<Float> = settings.moeWallpaperBiasX
    val moeWallpaperBiasY: Preference<Float> = settings.moeWallpaperBiasY
    val moeHomeQuote: Preference<String> = settings.moeHomeQuote
    val moeSidebarExpanded: Preference<Boolean> = settings.moeSidebarExpanded
    val pageScale: Preference<Float> = settings.pageScale
    val predictiveBackEnabled: Preference<Boolean> = settings.predictiveBackEnabled
    val predictiveBackMaxProgress: Preference<Float> = settings.predictiveBackMaxProgress
    val singleNodeTest: Preference<Boolean> = settings.singleNodeTest
    val exitUiWhenBackground: Preference<Boolean> = featureStore.exitUiWhenBackground

    val customUserAgent: Preference<String> = settings.customUserAgent

    data class BehaviorSectionState(
        val automaticRestart: Boolean = false,
        val autoUpdateCurrentProfileOnStart: Boolean = false,
    )

    val behaviorSectionState: StateFlow<BehaviorSectionState> =
        combine(automaticRestart.state, autoUpdateCurrentProfileOnStart.state) { restart, autoUpdate
            ->
            BehaviorSectionState(
                automaticRestart = restart,
                autoUpdateCurrentProfileOnStart = autoUpdate,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                BehaviorSectionState(
                    automaticRestart = automaticRestart.value,
                    autoUpdateCurrentProfileOnStart = autoUpdateCurrentProfileOnStart.value,
                ),
            )

    data class InterfaceSectionState(
        val themeMode: ThemeMode = ThemeMode.Auto,
        val appLanguage: AppLanguage = AppLanguage.System,
        val themeSeedColorArgb: Long = 0L,
        val invertOnPrimaryColors: Boolean = false,
        val bottomBarAutoHide: Boolean = false,
        val topBarBlurEnabled: Boolean = false,
        val pageScale: Float = 1f,
        val classicHomeEnabled: Boolean = false,
        val useSystemWallpaper: Boolean = true,
    )

    val interfaceSectionState: StateFlow<InterfaceSectionState> =
        combine(
            combine(
                themeMode.state,
                appLanguage.state,
                themeSeedColorArgb.state,
                invertOnPrimaryColors.state,
                bottomBarAutoHide.state,
            ) { theme, lang, seed, invert, bottomBar ->
                InterfaceSectionState(
                    themeMode = theme,
                    appLanguage = lang,
                    themeSeedColorArgb = seed,
                    invertOnPrimaryColors = invert,
                    bottomBarAutoHide = bottomBar,
                )
            },
            combine(
                topBarBlurEnabled.state,
                pageScale.state,
                classicHomeEnabled.state,
                useSystemWallpaper.state,
            ) { blur, scale, classic, systemWallpaper ->
                InterfaceSectionExtras(
                    topBarBlurEnabled = blur,
                    pageScale = scale,
                    classicHomeEnabled = classic,
                    useSystemWallpaper = systemWallpaper,
                )
            },
        ) { base, extra ->
            base.copy(
                topBarBlurEnabled = extra.topBarBlurEnabled,
                pageScale = extra.pageScale,
                classicHomeEnabled = extra.classicHomeEnabled,
                useSystemWallpaper = extra.useSystemWallpaper,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                InterfaceSectionState(
                    themeMode = themeMode.value,
                    appLanguage = appLanguage.value,
                    themeSeedColorArgb = themeSeedColorArgb.value,
                    invertOnPrimaryColors = invertOnPrimaryColors.value,
                    bottomBarAutoHide = bottomBarAutoHide.value,
                    topBarBlurEnabled = topBarBlurEnabled.value,
                    pageScale = pageScale.value,
                    classicHomeEnabled = classicHomeEnabled.value,
                    useSystemWallpaper = useSystemWallpaper.value,
                ),
            )

    private data class InterfaceSectionExtras(
        val topBarBlurEnabled: Boolean,
        val pageScale: Float,
        val classicHomeEnabled: Boolean,
        val useSystemWallpaper: Boolean,
    )

    data class ServiceSectionState(
        val showTrafficNotification: Boolean = false,
        val singleNodeTest: Boolean = false,
        val exitUiWhenBackground: Boolean = false,
    )

    val serviceSectionState: StateFlow<ServiceSectionState> =
        combine(
            showTrafficNotification.state,
            singleNodeTest.state,
            exitUiWhenBackground.state,
        ) { traffic, single, exitUi ->
            ServiceSectionState(
                showTrafficNotification = traffic,
                singleNodeTest = single,
                exitUiWhenBackground = exitUi,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                ServiceSectionState(
                    showTrafficNotification = showTrafficNotification.value,
                    singleNodeTest = singleNodeTest.value,
                    exitUiWhenBackground = exitUiWhenBackground.value,
                ),
            )

    data class PrivacySectionState(
        val excludeFromRecents: Boolean = false,
    )

    val privacySectionState: StateFlow<PrivacySectionState> =
        excludeFromRecents.state.map { exclude ->
            PrivacySectionState(excludeFromRecents = exclude)
        }
            .stateInWhileSubscribed(
                viewModelScope,
                PrivacySectionState(
                    excludeFromRecents = excludeFromRecents.value,
                ),
            )

    data class NetworkSectionState(val customUserAgent: String = "")

    val networkSectionState: StateFlow<NetworkSectionState> =
        customUserAgent.state
            .map { NetworkSectionState(customUserAgent = it) }
            .stateInWhileSubscribed(
                viewModelScope,
                NetworkSectionState(customUserAgent = customUserAgent.value),
            )

    data class MoeHomeSectionState(
        val themeMode: ThemeMode = ThemeMode.Auto,
        val classicHomeEnabled: Boolean = false,
        val useSystemWallpaper: Boolean = true,
        val moeHomeQuote: String = "",
        val sidebarExpanded: Boolean = false,
    )

    val moeHomeSectionState: StateFlow<MoeHomeSectionState> =
        combine(
            themeMode.state,
            classicHomeEnabled.state,
            useSystemWallpaper.state,
            moeHomeQuote.state,
            moeSidebarExpanded.state,
        ) { theme, classic, systemWallpaper, quote, sidebar ->
            MoeHomeSectionState(
                themeMode = theme,
                classicHomeEnabled = classic,
                useSystemWallpaper = systemWallpaper,
                moeHomeQuote = quote,
                sidebarExpanded = sidebar,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                MoeHomeSectionState(
                    themeMode = themeMode.value,
                    classicHomeEnabled = classicHomeEnabled.value,
                    useSystemWallpaper = useSystemWallpaper.value,
                    moeHomeQuote = moeHomeQuote.value,
                    sidebarExpanded = moeSidebarExpanded.value,
                ),
            )

    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)

    fun onAppLanguageChange(language: AppLanguage) = controller.applyAppLanguage(language)

    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)

    fun onThemeSeedColorChange(argb: Long) = themeSeedColorArgb.set(argb)

    fun onInvertOnPrimaryColorsChange(enabled: Boolean) = invertOnPrimaryColors.set(enabled)

    fun resetThemeSeedColor() = themeSeedColorArgb.set(DEFAULT_CUSTOM_THEME_SEED_ARGB)

    fun onBottomBarAutoHideChange(enabled: Boolean) = bottomBarAutoHide.set(enabled)

    fun onTopBarBlurEnabledChange(enabled: Boolean) = topBarBlurEnabled.set(enabled)

    fun onPredictiveBackMaxProgressChange(progress: Float) =
        predictiveBackMaxProgress.set(progress.coerceIn(1f, 100f))

    fun onPredictiveBackEnabledChange(enabled: Boolean) = predictiveBackEnabled.set(enabled)

    fun onClassicHomeEnabledChange(enabled: Boolean) = classicHomeEnabled.set(enabled)

    fun onUseSystemWallpaperChange(enabled: Boolean) = useSystemWallpaper.set(enabled)

    fun onMoeWallpaperUriChange(uri: String) = moeWallpaperUri.set(uri)

    /**
     * Persists the selected Moe wallpaper by copying [sourceUri] into the app-private files dir and
     * storing the resulting `file://` path as [moeWallpaperUri], while remembering the original
     * source in [moeWallpaperSourceUri] for lazy re-import. If the copy fails the original source
     * URI is persisted directly (degraded but working) and a toast is shown.
     */
    fun applyMoeWallpaper(sourceUri: String, onApplied: () -> Unit) {
        viewModelScope.launch {
            useSystemWallpaper.set(false)
            val localPath = MoeWallpaperImporter.importToLocal(application, sourceUri)
            if (localPath != null) {
                moeWallpaperUri.set(localPath)
                moeWallpaperSourceUri.set(sourceUri)
            } else {
                moeWallpaperUri.set(sourceUri)
                moeWallpaperSourceUri.set(sourceUri)
                application.toast(YumeTxt.AppSettings.Interface.HomeWallpaperImportFailed)
            }
            onApplied()
        }
    }

    fun onMoeWallpaperCropChange(zoom: Float, biasX: Float, biasY: Float) {
        moeWallpaperZoom.set(zoom.coerceIn(1f, 5f))
        moeWallpaperBiasX.set(biasX.coerceIn(-1f, 1f))
        moeWallpaperBiasY.set(biasY.coerceIn(-1f, 1f))
    }

    fun onMoeHomeQuoteChange(quote: String) = moeHomeQuote.set(quote)


    fun onMoeSidebarExpandedChange(expanded: Boolean) = moeSidebarExpanded.set(expanded)

    fun clearMoeWallpaperUri() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { application.moeWallpaperFile().delete() } }
            moeWallpaperUri.set("")
            moeWallpaperSourceUri.set("")
            onMoeWallpaperCropChange(zoom = 1f, biasX = 0f, biasY = 0f)
        }
    }

    fun onPageScaleChange(scale: Float) = pageScale.set(scale)

    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)

    fun onAutoUpdateCurrentProfileOnStartChange(enabled: Boolean) =
        autoUpdateCurrentProfileOnStart.set(enabled)



    fun onExcludeFromRecentsChange(exclude: Boolean) = excludeFromRecents.set(exclude)

    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)

    fun onSingleNodeTestChange(enabled: Boolean) = singleNodeTest.set(enabled)

    fun onExitUiWhenBackgroundChange(enabled: Boolean) = exitUiWhenBackground.set(enabled)

    fun applyCustomUserAgent(userAgent: String) = controller.applyCustomUserAgent(userAgent)
}
