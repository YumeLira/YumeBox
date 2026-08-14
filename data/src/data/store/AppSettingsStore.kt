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

package com.github.yumeyucca.yumebox.data.store

import com.github.yumeyucca.yumebox.data.model.AppColorTheme
import com.github.yumeyucca.yumebox.data.model.AppLanguage
import com.github.yumeyucca.yumebox.data.model.ThemeMode
import com.tencent.mmkv.MMKV

class AppSettingsStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {
    val themeMode by enumFlow(ThemeMode.Auto)
    val appLanguage by enumFlow(AppLanguage.System)
    val colorTheme by enumFlow(AppColorTheme.ClassicMonochrome)
    val themeAccentColorArgb by longFlow(0xFF016888)
    val invertOnPrimaryColors by boolFlow(false)
    val homePreviewGuideShown by boolFlow(false)
    val automaticRestart by boolFlow(false)
    val autoUpdateCurrentProfileOnStart by boolFlow(true)
    val excludeFromRecents by boolFlow(false)
    val showTrafficNotification by boolFlow(true)
    val bottomBarAutoHide by boolFlow(true)
    val topBarBlurEnabled by boolFlow(false)
    val classicHomeEnabled by boolFlow(false)
    val useSystemWallpaper by boolFlow(true)
    val systemWallpaperPermissionRequested by boolFlow(false)
    val moeWallpaperUri by strFlow("")
    val moeWallpaperSourceUri by strFlow("")
    val moeWallpaperZoom by floatFlow(1.0f)
    val moeWallpaperBiasX by floatFlow(0.0f)
    val moeWallpaperBiasY by floatFlow(0.0f)
    val moeHomeQuote by strFlow("君の隣、感じちゃうの")
    val moeSidebarExpanded by boolFlow(true)
    val pageScale by floatFlow(1.0f)
    val predictiveBackEnabled by boolFlow(false)
    val predictiveBackMaxProgress by floatFlow(50.0f)
    val singleNodeTest by boolFlow(true)

    val customUserAgent by strFlow("")

    init {
        migrateLegacyHomeKeys()
        resetHomePreviewGuideForWallpaperUpdate()
        if (!mmkv.containsKey("useSystemWallpaper") &&
            mmkv.decodeString("moeWallpaperUri").orEmpty().isNotBlank()
        ) {
            mmkv.encode("useSystemWallpaper", false)
        }
    }

    private fun resetHomePreviewGuideForWallpaperUpdate() {
        if (mmkv.decodeInt("homePreviewGuideVersion", 0) >= 1) return
        mmkv.encode("homePreviewGuideShown", false)
        mmkv.encode("homePreviewGuideVersion", 1)
    }

    /**
     * One-time rename migration: pre-rename builds persisted the home/wallpaper preferences under
     * `acg*` keys. Copy any legacy value onto the new `moe*` key when the new key is still absent,
     * so upgrading users keep their saved quote, author, wallpaper and crop framing.
     */
    private fun migrateLegacyHomeKeys() {
        fun moveString(old: String, new: String) {
            if (mmkv.containsKey(old) && !mmkv.containsKey(new)) {
                mmkv.decodeString(old)?.let { mmkv.encode(new, it) }
            }
        }

        fun moveFloat(old: String, new: String) {
            if (mmkv.containsKey(old) && !mmkv.containsKey(new)) {
                mmkv.encode(new, mmkv.decodeFloat(old, 0f))
            }
        }

        fun moveBool(old: String, new: String) {
            if (mmkv.containsKey(old) && !mmkv.containsKey(new)) {
                mmkv.encode(new, mmkv.decodeBool(old, false))
            }
        }
        moveString("acgWallpaperUri", "moeWallpaperUri")
        moveString("acgWallpaperSourceUri", "moeWallpaperSourceUri")
        moveString("acgHomeQuote", "moeHomeQuote")
        moveFloat("acgWallpaperZoom", "moeWallpaperZoom")
        moveFloat("acgWallpaperBiasX", "moeWallpaperBiasX")
        moveFloat("acgWallpaperBiasY", "moeWallpaperBiasY")
        moveBool("acgSidebarExpanded", "moeSidebarExpanded")
    }
}
