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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
const val DEFAULT_CUSTOM_THEME_SEED_ARGB: Long = 0xFF138A74L

data class TrafficColors(
    val download: Color = Color(0xFF5B8FF9),
    val upload: Color = Color(0xFF5AD8A6),
    val unattributed: Color = Color(0xFFD97706),
    val other: Color = Color(0xFF94A3B8),
    val unknown: Color = Color(0xFF64748B),
    val donutTrackForeground: Color = Color(0x1F8A94A6),
    val donutTrackBackground: Color = Color(0x148A94A6),
)

data class LogLevelColors(
    val debug: Color = Color(0xFF9E9E9E),
    val warning: Color = Color(0xFFFF9800),
    val error: Color = Color(0xFFF44336),
    val neutral: Color = Color(0xFF9E9E9E),
)

data class LatencyColors(
    val fast: Color = Color(0xFF007906),
    val moderate: Color = Color(0xFFFFB300),
    val slow: Color = Color(0xFFE53935),
    val timeout: Color = Color(0xFF9E9E9E),
)

data class ProtocolColors(
    val tcp: Color = Color(0xFF2196F3),
    val udp: Color = Color(0xFF4CAF50),
    val http: Color = Color(0xFF9E9E9E),
    val https: Color = Color(0xFF00BCD4),
    val unknown: Color = Color(0xFF9E9E9E),
)

data class ConnectionColors(
    val chainArrow: Color = Color(0xFF6B7280),
    val chainInactiveText: Color = Color(0xFF6B7280),
    val chainActive: Color = Color(0xFF00BFA5),
) {
    val chainInactive: Color
        get() = chainInactiveText
}

data class StatusColors(
    val destructive: Color = Color(0xFFFF3B30),
    val destructiveContainer: Color = Color(0x1AFF3B30),
)

data class StateColors(
    val danger: Color = Color(0xFFFF3B30),
    val subtleDivider: Color = Color(0xFFC7C7CC),
    val neutralPlaceholderBackground: Color = Color(0xFFE0E0E0),
)

data class AcgColors(
    val pingExcellent: Color = Color(0xFF0E7A34),
    val pingWarning: Color = Color(0xFFB87900),
)

data class EditorColors(
    val darkBackground: Color = Color(0xFF1E1E1E),
    val darkText: Color = Color(0xFFD4D4D4),
    val darkLineNumber: Color = Color(0xFF858585),
    val darkLineNumberBackground: Color = Color(0xFF1E1E1E),
    val darkCurrentLine: Color = Color(0xFF2D2D2D),
    val darkSelectionBackground: Color = Color(0xFF264F78),
    val darkTextActionBackground: Color = Color(0xFF2D2D2D),
    val darkTextActionIcon: Color = Color(0xFFD4D4D4),
    val lightBackground: Color = Color.White,
    val lightText: Color = Color(0xFF1E1E1E),
    val lightLineNumber: Color = Color(0xFF6E6E6E),
    val lightLineNumberBackground: Color = Color(0xFFF0F0F0),
    val lightCurrentLine: Color = Color(0xFFF5F5F5),
    val lightSelectionBackground: Color = Color(0xFFADD6FF),
    val lightTextActionBackground: Color = Color(0xFFF0F0F0),
    val lightTextActionIcon: Color = Color(0xFF333333),
    val accent: Color = Color(0xFF007ACC),
    val delimiterDark: Color = Color(0xFF569CD6),
    val delimiterLight: Color = Color(0xFF0000FF),
    val delimiterBackground: Color = Color(0x2646A2D4),
)

data class AppColors(
    val traffic: TrafficColors = TrafficColors(),
    val logLevel: LogLevelColors = LogLevelColors(),
    val latency: LatencyColors = LatencyColors(),
    val protocol: ProtocolColors = ProtocolColors(),
    val connection: ConnectionColors = ConnectionColors(),
    val status: StatusColors = StatusColors(),
    val state: StateColors = StateColors(),
    val acg: AcgColors = AcgColors(),
    val editor: EditorColors = EditorColors(),
) {
    val neutralPlaceholderBackground: Color
        get() = state.neutralPlaceholderBackground
}

val LocalAppColors = staticCompositionLocalOf { AppColors() }

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

private val basePalette = ThemePalette(
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
)

fun colorSchemeFromSeed(
    seed: Color,
    isDark: Boolean,
    invertOnPrimaryColors: Boolean = false,
) = derivePaletteFromSeed(
    seed = seed,
    invertOnPrimaryColors = invertOnPrimaryColors,
).toColorScheme(isDark)

fun colorFromArgb(argb: Long): Color = Color(argb.toInt())

fun colorToArgbLong(color: Color): Long = color.toArgb().toLong()

private fun derivePaletteFromSeed(
    seed: Color,
    invertOnPrimaryColors: Boolean,
): ThemePalette {
    return ThemePalette(
        light = deriveThemeColors(
            base = basePalette.light,
            seed = seed,
            dark = false,
            invertOnPrimaryColors = invertOnPrimaryColors,
        ),
        dark = deriveThemeColors(
            base = basePalette.dark,
            seed = seed,
            dark = true,
            invertOnPrimaryColors = invertOnPrimaryColors,
        ),
    )
}

private fun deriveThemeColors(
    base: ThemeColors,
    seed: Color,
    dark: Boolean,
    invertOnPrimaryColors: Boolean,
): ThemeColors {
    val primary = if (dark) seed else seed.mix(Color.Black, 0.05f)
    val primaryVariant = if (dark) seed.mix(Color.White, 0.36f) else seed.mix(Color.White, 0.25f)

    val defaultOnPrimary = primary.autoOnColor(threshold = if (dark) 0.72f else 0.52f)
    val defaultOnPrimaryVariant = primaryVariant.autoOnColor(threshold = if (dark) 0.68f else 0.52f)

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

    val defaultOnPrimaryContainer = primaryContainer.autoOnColor()
    val onTertiaryContainer = tertiaryContainer.autoOnColor()

    val onPrimary = if (invertOnPrimaryColors) defaultOnPrimary.invertBlackWhite() else defaultOnPrimary
    val onPrimaryVariant = if (invertOnPrimaryColors) {
        defaultOnPrimaryVariant.invertBlackWhite()
    } else {
        defaultOnPrimaryVariant
    }
    val onPrimaryContainer = if (invertOnPrimaryColors) {
        defaultOnPrimaryContainer.invertBlackWhite()
    } else {
        defaultOnPrimaryContainer
    }

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

private fun Color.autoOnColor(threshold: Float = 0.52f): Color =
    if (luminance() > threshold) Color.Black else Color.White

private fun Color.invertBlackWhite(): Color =
    if (this == Color.Black) Color.White else Color.Black
