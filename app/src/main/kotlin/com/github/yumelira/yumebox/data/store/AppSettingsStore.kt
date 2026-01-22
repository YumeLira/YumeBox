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

package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme
import com.tencent.mmkv.MMKV

class AppSettingsStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val onboardingCompleted by boolFlow(false)
    val privacyPolicyAccepted by boolFlow(false)

    val themeMode by enumFlow(ThemeMode.Auto)
    val colorTheme by enumFlow(AppColorTheme.ClassicMonochrome)
    val appLanguage by enumFlow(AppLanguage.System)
    val automaticRestart by boolFlow(false)
    val hideAppIcon by boolFlow(false)
    val showTrafficNotification by boolFlow(true)
    val bottomBarFloating by boolFlow(false)
    val showDivider by boolFlow(true)
    val bottomBarAutoHide by boolFlow(true)

    val oneWord by strFlow("古が遺す呪われた種が、もう一度僕らを苦しめても")

    val oneWordAuthor by strFlow("イノチの灯し方")

    val customUserAgent by strFlow("")
}
