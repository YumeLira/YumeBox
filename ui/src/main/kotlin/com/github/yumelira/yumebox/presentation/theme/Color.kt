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

package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.github.yumelira.yumebox.data.model.AppColorTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryVariant: Color,
    val onPrimaryVariant: Color,
    val disabledPrimary: Color,
    val disabledOnPrimary: Color,
    val disabledPrimaryButton: Color,
    val disabledOnPrimaryButton: Color,
    val disabledPrimarySlider: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val tertiaryContainerVariant: Color,
    val onBackgroundVariant: Color,
)

private data class ThemePalette(
    val light: ThemeColors,
    val dark: ThemeColors,
)

const val DEFAULT_THEME_SEED_ARGB: Long = 0xFFFFFFFFL

private val DefaultColorTheme = AppColorTheme.ClassicMonochrome

private fun ThemeColors.toLightScheme() = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryVariant = primaryVariant,
    onPrimaryVariant = onPrimaryVariant,
    disabledPrimary = disabledPrimary,
    disabledOnPrimary = disabledOnPrimary,
    disabledPrimaryButton = disabledPrimaryButton,
    disabledOnPrimaryButton = disabledOnPrimaryButton,
    disabledPrimarySlider = disabledPrimarySlider,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    tertiaryContainerVariant = tertiaryContainerVariant,
    onBackgroundVariant = onBackgroundVariant,
)

private fun ThemeColors.toDarkScheme() = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryVariant = primaryVariant,
    onPrimaryVariant = onPrimaryVariant,
    disabledPrimary = disabledPrimary,
    disabledOnPrimary = disabledOnPrimary,
    disabledPrimaryButton = disabledPrimaryButton,
    disabledOnPrimaryButton = disabledOnPrimaryButton,
    disabledPrimarySlider = disabledPrimarySlider,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    tertiaryContainerVariant = tertiaryContainerVariant,
    onBackgroundVariant = onBackgroundVariant,
)

private fun ThemePalette.toColorScheme(isDark: Boolean) =
    if (isDark) dark.toDarkScheme() else light.toLightScheme()

private val themePalettes = mapOf(
    // 保持原始黑白方案，不做改动
    AppColorTheme.ClassicMonochrome to ThemePalette(
        light = ThemeColors(
            primary = Color(0xFF000000),
            onPrimary = Color.White,
            primaryVariant = Color(0xFF222222),
            onPrimaryVariant = Color(0xFFAAAAAA),
            disabledPrimary = Color(0xFFBDBDBD),
            disabledOnPrimary = Color(0xFFE0E0E0),
            disabledPrimaryButton = Color(0xFFBDBDBD),
            disabledOnPrimaryButton = Color(0xFFEEEEEE),
            disabledPrimarySlider = Color(0xFFDCDCDC),
            primaryContainer = Color(0xFFF0F0F0),
            onPrimaryContainer = Color(0xFF000000),
            tertiaryContainer = Color(0xFFF8F8F8),
            onTertiaryContainer = Color(0xFF000000),
            tertiaryContainerVariant = Color(0xFFF8F8F8),
            onBackgroundVariant = Color(0xFF000000),
        ),
        dark = ThemeColors(
            primary = Color.White,
            onPrimary = Color(0xFF000000),
            primaryVariant = Color(0xFFE0E0E0),
            onPrimaryVariant = Color(0xFF555555),
            disabledPrimary = Color(0xFF333333),
            disabledOnPrimary = Color(0xFF757575),
            disabledPrimaryButton = Color(0xFF333333),
            disabledOnPrimaryButton = Color(0xFF757575),
            disabledPrimarySlider = Color(0xFF444444),
            primaryContainer = Color(0xFF252525),
            onPrimaryContainer = Color.White,
            tertiaryContainer = Color(0xFF1C1C1C),
            onTertiaryContainer = Color.White,
            tertiaryContainerVariant = Color(0xFF303030),
            onBackgroundVariant = Color(0xFFE0E0E0),
        ),
    ),
)

private val defaultPalette = themePalettes.getValue(DefaultColorTheme)
fun colorSchemeForTheme(theme: AppColorTheme, isDark: Boolean) =
    (themePalettes[theme] ?: defaultPalette).toColorScheme(isDark)

// --- Seed -> Full Palette Algorithm ---
// 独立算法：给一个主题色，推导完整 light/dark 取色。
fun colorSchemeFromSeed(seed: Color, isDark: Boolean) =
    derivePaletteFromSeed(seed).toColorScheme(isDark)

fun colorFromArgb(argb: Long): Color = Color(argb.toInt())

fun colorToArgbLong(color: Color): Long = color.toArgb().toLong()

fun isDefaultThemeSeedArgb(argb: Long): Boolean {
    val rgb = argb and 0x00FFFFFFL
    return rgb == 0x000000L || rgb == 0xFFFFFFL
}

private fun derivePaletteFromSeed(seed: Color): ThemePalette {
    val lightBase = themePalettes.getValue(AppColorTheme.ClassicMonochrome).light
    val darkBase = themePalettes.getValue(AppColorTheme.ClassicMonochrome).dark
    return ThemePalette(
        light = deriveThemeColors(base = lightBase, seed = seed, dark = false),
        dark = deriveThemeColors(base = darkBase, seed = seed, dark = true),
    )
}

private fun deriveThemeColors(base: ThemeColors, seed: Color, dark: Boolean): ThemeColors {
    val primary = if (dark) seed.mix(Color.White, 0.20f) else seed.mix(Color.Black, 0.05f)
    val primaryVariant = if (dark) seed.mix(Color.White, 0.36f) else seed.mix(Color.White, 0.25f)

    val onPrimary = if (dark) Color(0xFFFDFDFD) else Color(0xFFFDFDFD)
    val onPrimaryVariant = if (dark) Color(0xFFF2F2F2) else seed.mix(Color.Black, 0.72f)

    val disabledPrimary = if (dark) {
        base.disabledPrimary.mix(seed, 0.18f)
    } else {
        base.disabledPrimary.mix(seed, 0.14f)
    }
    val disabledOnPrimary = if (dark) {
        base.disabledOnPrimary.mix(seed, 0.08f)
    } else {
        base.disabledOnPrimary.mix(seed, 0.06f)
    }
    val disabledPrimaryButton = if (dark) {
        base.disabledPrimaryButton.mix(seed, 0.16f)
    } else {
        base.disabledPrimaryButton.mix(seed, 0.12f)
    }
    val disabledOnPrimaryButton = if (dark) {
        base.disabledOnPrimaryButton.mix(seed, 0.06f)
    } else {
        base.disabledOnPrimaryButton.mix(seed, 0.05f)
    }
    val disabledPrimarySlider = if (dark) {
        base.disabledPrimarySlider.mix(seed, 0.14f)
    } else {
        base.disabledPrimarySlider.mix(seed, 0.10f)
    }

    val primaryContainer = if (dark) {
        base.primaryContainer.mix(seed, 0.18f)
    } else {
        base.primaryContainer.mix(seed, 0.14f)
    }

    val tertiaryContainer = if (dark) {
        base.tertiaryContainer.mix(seed, 0.13f)
    } else {
        base.tertiaryContainer.mix(seed, 0.16f)
    }

    val tertiaryContainerVariant = if (dark) {
        base.tertiaryContainerVariant.mix(seed, 0.15f)
    } else {
        base.tertiaryContainerVariant.mix(seed, 0.13f)
    }

    val onPrimaryContainer = if (dark) Color(0xFFF2F2F2) else seed.mix(Color.Black, 0.80f)
    val onTertiaryContainer = if (dark) seed.mix(Color.White, 0.22f) else seed.mix(Color.Black, 0.30f)

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryVariant,
        onPrimaryVariant = onPrimaryVariant,
        disabledPrimary = disabledPrimary,
        disabledOnPrimary = disabledOnPrimary,
        disabledPrimaryButton = disabledPrimaryButton,
        disabledOnPrimaryButton = disabledOnPrimaryButton,
        disabledPrimarySlider = disabledPrimarySlider,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainerVariant,
        onBackgroundVariant = if (dark) seed.mix(Color.White, 0.25f) else seed.mix(Color.Black, 0.18f),
    )
}

private fun Color.mix(other: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha + (other.alpha - alpha) * t,
    )
}

private fun Color.autoOnColor(): Color = if (luminance() > 0.52f) Color.Black else Color.White
