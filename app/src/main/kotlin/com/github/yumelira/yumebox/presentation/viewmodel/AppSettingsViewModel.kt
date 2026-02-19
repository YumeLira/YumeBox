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

package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.repository.AppSettingsRepository
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme


class AppSettingsViewModel(
    private val repository: AppSettingsRepository,
) : ViewModel() {

    val onboardingCompleted: Preference<Boolean> = repository.onboardingCompleted
    val privacyPolicyAccepted: Preference<Boolean> = repository.privacyPolicyAccepted

    val themeMode: Preference<ThemeMode> = repository.themeMode
    val colorTheme: Preference<AppColorTheme> = repository.colorTheme
    val themeSeedColorArgb: Preference<Long> = repository.themeSeedColorArgb
    val automaticRestart: Preference<Boolean> = repository.automaticRestart
    val hideAppIcon: Preference<Boolean> = repository.hideAppIcon
    val showTrafficNotification: Preference<Boolean> = repository.showTrafficNotification
    val bottomBarFloating: Preference<Boolean> = repository.bottomBarFloating
    val showDivider: Preference<Boolean> = repository.showDivider
    val bottomBarAutoHide: Preference<Boolean> = repository.bottomBarAutoHide

    val iconWithSelectedLabel: Preference<Boolean> = repository.iconWithSelectedLabel

    val oneWord: Preference<String> = repository.oneWord
    val oneWordAuthor: Preference<String> = repository.oneWordAuthor
    val customUserAgent: Preference<String> = repository.customUserAgent


    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)
    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)
    fun onThemeSeedColorChange(argb: Long) = themeSeedColorArgb.set(argb)
    fun resetThemeSeedColor() = themeSeedColorArgb.set(0xFFFFFFFFL)
    fun onBottomBarAutoHideChange(enabled: Boolean) = bottomBarAutoHide.set(enabled)
    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)
    fun onHideAppIconChange(hide: Boolean) = hideAppIcon.set(hide)
    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)
    fun onBottomBarFloatingChange(floating: Boolean) = bottomBarFloating.set(floating)
    fun onShowDividerChange(show: Boolean) = showDivider.set(show)

    fun onOneWordChange(text: String) = oneWord.set(text)
    fun onOneWordAuthorChange(author: String) = oneWordAuthor.set(author)
    fun applyCustomUserAgent(userAgent: String) = repository.applyCustomUserAgent(userAgent)

    fun setOnboardingCompleted(completed: Boolean) = onboardingCompleted.set(completed)
    fun setPrivacyPolicyAccepted(accepted: Boolean) = privacyPolicyAccepted.set(accepted)
}
