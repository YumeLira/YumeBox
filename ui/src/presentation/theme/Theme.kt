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

package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.github.yumelira.yumebox.data.model.ThemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalPlatformSystemUiEffect = compositionLocalOf<@Composable () -> Unit> { {} }

@Composable
fun YumeTheme(
    themeMode: ThemeMode? = null,
    themeSeedColorArgb: Long = DEFAULT_THEME_SEED_ARGB,
    invertOnPrimaryColors: Boolean = false,
    smoothCornerEnabled: Boolean = true,
    spacing: Spacing = Spacing(),
    radii: Radii = Radii(),
    sizes: Sizes = Sizes(),
    opacity: Opacity = Opacity(),
    appColors: AppColors = AppColors(),
    content: @Composable () -> Unit,
) {
    LocalPlatformSystemUiEffect.current()
    val effectiveThemeMode = themeMode ?: ThemeMode.Auto
    val isDark =
        when (effectiveThemeMode) {
            ThemeMode.Auto -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    val colorScheme =
        remember(isDark, themeSeedColorArgb, invertOnPrimaryColors) {
            colorSchemeFromSeed(
                seed = colorFromArgb(themeSeedColorArgb),
                isDark = isDark,
                invertOnPrimaryColors = invertOnPrimaryColors,
            )
        }

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalRadii provides radii,
        LocalSizes provides sizes,
        LocalOpacity provides opacity,
        LocalAppColors provides appColors,
    ) {
        MiuixTheme(colors = colorScheme, smoothRounding = smoothCornerEnabled) { content() }
    }
}
