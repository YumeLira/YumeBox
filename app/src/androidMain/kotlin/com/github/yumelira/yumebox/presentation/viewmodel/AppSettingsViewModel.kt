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
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme


class AppSettingsViewModel(
    storage: AppSettingsStorage,
) : ViewModel() {


    val themeMode: Preference<ThemeMode> = storage.themeMode
    val colorTheme: Preference<AppColorTheme> = storage.colorTheme
    val automaticRestart: Preference<Boolean> = storage.automaticRestart
    val hideAppIcon: Preference<Boolean> = storage.hideAppIcon
    val showTrafficNotification: Preference<Boolean> = storage.showTrafficNotification
    val bottomBarFloating: Preference<Boolean> = storage.bottomBarFloating
    val showDivider: Preference<Boolean> = storage.showDivider
    val bottomBarAutoHide: Preference<Boolean> = storage.bottomBarAutoHide

    val oneWord: Preference<String> = storage.oneWord
    val oneWordAuthor: Preference<String> = storage.oneWordAuthor
    val customUserAgent: Preference<String> = storage.customUserAgent


    fun onThemeModeChange(mode: ThemeMode) = themeMode.set(mode)
    fun onColorThemeChange(theme: AppColorTheme) = colorTheme.set(theme)
    fun onBottomBarAutoHideChange(enabled: Boolean) = bottomBarAutoHide.set(enabled)
    fun onAutomaticRestartChange(enabled: Boolean) = automaticRestart.set(enabled)
    fun onHideAppIconChange(hide: Boolean) = hideAppIcon.set(hide)
    fun onShowTrafficNotificationChange(show: Boolean) = showTrafficNotification.set(show)
    fun onBottomBarFloatingChange(floating: Boolean) = bottomBarFloating.set(floating)
    fun onShowDividerChange(show: Boolean) = showDivider.set(show)

    fun onOneWordChange(text: String) = oneWord.set(text)
    fun onOneWordAuthorChange(author: String) = oneWordAuthor.set(author)
    fun onCustomUserAgentChange(userAgent: String) = customUserAgent.set(userAgent)
}
