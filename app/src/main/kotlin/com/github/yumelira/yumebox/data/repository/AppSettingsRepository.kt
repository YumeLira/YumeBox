package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme

class AppSettingsRepository(
    private val storage: AppSettingsStorage,
) {
    val onboardingCompleted: Preference<Boolean> = storage.onboardingCompleted
    val privacyPolicyAccepted: Preference<Boolean> = storage.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = storage.themeMode
    val colorTheme: Preference<AppColorTheme> = storage.colorTheme
    val themeSeedColorArgb: Preference<Long> = storage.themeSeedColorArgb
    val automaticRestart: Preference<Boolean> = storage.automaticRestart
    val hideAppIcon: Preference<Boolean> = storage.hideAppIcon
    val showTrafficNotification: Preference<Boolean> = storage.showTrafficNotification
    val bottomBarFloating: Preference<Boolean> = storage.bottomBarFloating
    val showDivider: Preference<Boolean> = storage.showDivider
    val bottomBarAutoHide: Preference<Boolean> = storage.bottomBarAutoHide

    val iconWithSelectedLabel: Preference<Boolean> = storage.iconWithSelectedLabel

    val oneWord: Preference<String> = storage.oneWord
    val oneWordAuthor: Preference<String> = storage.oneWordAuthor
    val customUserAgent: Preference<String> = storage.customUserAgent

    fun applyCustomUserAgent(userAgent: String) {
        customUserAgent.set(userAgent)
        Clash.setCustomUserAgent(userAgent)
    }
}
