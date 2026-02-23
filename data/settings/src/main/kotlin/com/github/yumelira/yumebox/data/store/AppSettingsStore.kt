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

import com.github.yumelira.yumebox.data.model.AppColorTheme
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.tencent.mmkv.MMKV

class AppSettingsStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val onboardingCompleted by boolFlow(false)
    val privacyPolicyAccepted by boolFlow(false)

    val themeMode by enumFlow(ThemeMode.Auto)
    val colorTheme by enumFlow(AppColorTheme.ClassicMonochrome)
    val themeSeedColorArgb by longFlow(0xFFFFFFFFL)
    val automaticRestart by boolFlow(false)
    val hideAppIcon by boolFlow(false)
    val excludeFromRecents by boolFlow(false)
    val showTrafficNotification by boolFlow(true)
    val bottomBarFloating by boolFlow(false)
    val showDivider by boolFlow(true)
    val bottomBarAutoHide by boolFlow(true)
    val iconWithSelectedLabel by boolFlow(true)

    val oneWord by strFlow("本当は未来なんかよりも，瞬間の方が欲しいです")

    val oneWordAuthor by strFlow("紅のドレス")

    val customUserAgent by strFlow("")

    fun resetOneWordToDefault() {
        mmkv.remove("oneWord")
        oneWord.refresh()
    }

    fun resetOneWordAuthorToDefault() {
        mmkv.remove("oneWordAuthor")
        oneWordAuthor.refresh()
    }
}
