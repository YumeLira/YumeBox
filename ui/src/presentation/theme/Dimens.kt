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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiDp {
    val dp0: Dp = 0.dp
    val dp0_2: Dp = 0.2.dp
    val dp0_26: Dp = 0.26.dp
    val dp0_3: Dp = 0.3.dp
    val dp0_5: Dp = 0.5.dp
    val dp1: Dp = 1.dp
    val dp1_2: Dp = 1.2.dp
    val dp2: Dp = 2.dp
    val dp2_5: Dp = 2.5.dp
    val dp3: Dp = 3.dp
    val dp4: Dp = 4.dp
    val dp5: Dp = 5.dp
    val dp6: Dp = 6.dp
    val dp7: Dp = 7.dp
    val dp8: Dp = 8.dp
    val dp9: Dp = 9.dp
    val dp10: Dp = 10.dp
    val dp11: Dp = 11.dp
    val dp12: Dp = 12.dp
    val dp13: Dp = 13.dp
    val dp14: Dp = 14.dp
    val dp15: Dp = 15.dp
    val dp16: Dp = 16.dp
    val dp18: Dp = 18.dp
    val dp20: Dp = 20.dp
    val dp22: Dp = 22.dp
    val dp24: Dp = 24.dp
    val dp26: Dp = 26.dp
    val dp28: Dp = 28.dp
    val dp30: Dp = 30.dp
    val dp32: Dp = 32.dp
    val dp34: Dp = 34.dp
    val dp35: Dp = 35.dp
    val dp36: Dp = 36.dp
    val dp40: Dp = 40.dp
    val dp42: Dp = 42.dp
    val dp44: Dp = 44.dp
    val dp45: Dp = 45.dp
    val dp46: Dp = 46.dp
    val dp48: Dp = 48.dp
    val dp52: Dp = 52.dp
    val dp56: Dp = 56.dp
    val dp60: Dp = 60.dp
    val dp64: Dp = 64.dp
    val dp68: Dp = 68.dp
    val dp70: Dp = 70.dp
    val dp72: Dp = 72.dp
    val dp80: Dp = 80.dp
    val dp84: Dp = 84.dp
    val dp85: Dp = 85.dp
    val dp88: Dp = 88.dp
    val dp100: Dp = 100.dp
    val dp108: Dp = 108.dp
    val dp116: Dp = 116.dp
    val dp120: Dp = 120.dp
    val dp122: Dp = 122.dp
    val dp126: Dp = 126.dp
    val dp130: Dp = 130.dp
    val dp140: Dp = 140.dp
    val dp168: Dp = 168.dp
    val dp180: Dp = 180.dp
    val dp188: Dp = 188.dp
    val dp200: Dp = 200.dp
    val dp212: Dp = 212.dp
    val dp220: Dp = 220.dp
    val dp280: Dp = 280.dp
    val dp320: Dp = 320.dp
    val dp360: Dp = 360.dp
    val dp400: Dp = 400.dp
    val dp420: Dp = 420.dp
    val dp450: Dp = 450.dp
    val dp560: Dp = 560.dp
    val dp999: Dp = 999.dp
}

data class Spacing(
    val space0: Dp = UiDp.dp0,
    val space2: Dp = UiDp.dp2,
    val space3: Dp = UiDp.dp3,
    val space4: Dp = UiDp.dp4,
    val space6: Dp = UiDp.dp6,
    val space8: Dp = UiDp.dp8,
    val space10: Dp = UiDp.dp10,
    val space12: Dp = UiDp.dp12,
    val space14: Dp = UiDp.dp14,
    val space16: Dp = UiDp.dp16,
    val space18: Dp = UiDp.dp18,
    val space20: Dp = UiDp.dp20,
    val space24: Dp = UiDp.dp24,
    val space28: Dp = UiDp.dp28,
    val space32: Dp = UiDp.dp32,
    val contentHorizontal: Dp = UiDp.dp16,
    val screenHorizontal: Dp = UiDp.dp12,
    val screenVertical: Dp = UiDp.dp12,
)

data class Radii(
    val radius0: Dp = UiDp.dp0,
    val radius4: Dp = UiDp.dp4,
    val radius8: Dp = UiDp.dp8,
    val radius14: Dp = UiDp.dp14,
    val radius12: Dp = UiDp.dp12,
    val radius16: Dp = UiDp.dp16,
    val radius18: Dp = UiDp.dp18,
    val radius24: Dp = UiDp.dp24,
    val radius36: Dp = UiDp.dp36,
    val full: Dp = UiDp.dp999,
)

data class Sizes(
    val textLineCompactSpacing: Dp = UiDp.dp3,
    val listItemVerticalMinimal: Dp = UiDp.dp5,
    val searchBarTopPadding: Dp = UiDp.dp12,
    val searchBarBottomPadding: Dp = UiDp.dp6,
    val homeTrafficTopPadding: Dp = UiDp.dp60,
    val iconBadgeMedium: Dp = UiDp.dp42,
    val iconBadgeLarge: Dp = UiDp.dp45,
    val searchIconTouchTarget: Dp = UiDp.dp44,
    val searchFieldMinHeight: Dp = UiDp.dp45,
    val homeIdleTopPadding: Dp = UiDp.dp122,
    val homeIdleBottomPadding: Dp = UiDp.dp40,
    val homeIdleAccentLineWidth: Dp = UiDp.dp2,
    val homeIdleAccentLineHeight: Dp = UiDp.dp36,
    val homeIdleAuthorDividerWidth: Dp = UiDp.dp30,
    val homeIdleAuthorDividerHeight: Dp = UiDp.dp1,
    val heroStartButtonSize: Dp = UiDp.dp72,
    val nodeDelayColumnWidth: Dp = UiDp.dp72,
    val speedChartBarGap: Dp = UiDp.dp5,
    val speedChartBarCornerRadius: Dp = UiDp.dp6,
    val trafficChartHeight: Dp = UiDp.dp188,
    val trafficBarChartHeight: Dp = UiDp.dp140,
    val trafficBarWidth: Dp = UiDp.dp20,
    val trafficBarLabelHeight: Dp = UiDp.dp18,
    val trafficDonutDiameter: Dp = UiDp.dp168,
    val trafficDonutStrokeWidth: Dp = UiDp.dp26,
    val floatingActionButtonBottomInset: Dp = UiDp.dp85,
    val settingsIconSlotSize: Dp = UiDp.dp24,
    val settingsIconContainerSize: Dp = UiDp.dp36,
    val settingsIconGlyphSize: Dp = UiDp.dp22,
    val statusCapsuleHeight: Dp = UiDp.dp28,
    val versionBadgeHeight: Dp = UiDp.dp22,
    val thinDividerThickness: Dp = UiDp.dp0_5,
    val compactActionButtonSize: Dp = UiDp.dp35,
    val profileMetaTrailingInset: Dp = UiDp.dp13,
    val dialogSheetMaxHeight: Dp = UiDp.dp450,
    val profileSettingsListMaxHeight: Dp = UiDp.dp420,
    val connectionLeadingIconSize: Dp = UiDp.dp44,
    val connectionLeadingIconCornerRadius: Dp = UiDp.dp14,
    val connectionDetailLabelWidth: Dp = UiDp.dp64,
    val sectionHeaderMinHeight: Dp = UiDp.dp72,
    val nodeCardBorderWidth: Dp = UiDp.dp1,
    val nodeCardPaddingHorizontal: Dp = UiDp.dp16,
    val nodeCardPaddingVertical: Dp = UiDp.dp12,
    val nodeCardContentGap: Dp = UiDp.dp16,
    val nodeCardTitleGap: Dp = UiDp.dp6,
    val nodeCardTrailingGap: Dp = UiDp.dp8,
    val nodeLargeIconSize: Dp = UiDp.dp44,
    val nodeLargeIconCornerRadius: Dp = UiDp.dp14,
    val nodeLargeIconFlagSize: Dp = UiDp.dp28,
    val nodeTagCornerRadius: Dp = UiDp.dp100,
    val nodeTagHorizontalPadding: Dp = UiDp.dp4,
    val nodeTagVerticalPadding: Dp = UiDp.dp2,
    val nodeTagIconSize: Dp = UiDp.dp9,
    val nodeChainNodeCornerRadius: Dp = UiDp.dp8,
    val nodeChainNodeHorizontalPadding: Dp = UiDp.dp8,
    val nodeChainNodeVerticalPadding: Dp = UiDp.dp4,
    val nodeChainIndicatorSize: Dp = UiDp.dp6,
)

data class Opacity(
    val none: Float = 0f,
    val ambientLight: Float = 0.05f,
    val ambientShadow: Float = 0.015f,
    val ultraSubtle: Float = 0.045f,
    val verySubtle: Float = 0.08f,
    val subtle: Float = 0.10f,
    val subtleStrong: Float = 0.12f,
    val lightOverlay: Float = 0.18f,
    val mediumOverlay: Float = 0.20f,
    val softOverlay: Float = 0.16f,
    val surfaceSoft: Float = 0.24f,
    val outline: Float = 0.30f,
    val muted: Float = 0.35f,
    val disabledSecondary: Float = 0.40f,
    val disabledStrong: Float = 0.45f,
    val disabled: Float = 0.38f,
    val emphasizedTrack: Float = 0.48f,
    val medium: Float = 0.50f,
    val accent: Float = 0.58f,
    val mutedStrong: Float = 0.56f,
    val secondaryText: Float = 0.60f,
    val elevatedSurface: Float = 0.68f,
    val subtleText: Float = 0.70f,
    val brightOutline: Float = 0.72f,
    val mediumStrong: Float = 0.75f,
    val prominentText: Float = 0.90f,
    val strong: Float = 0.80f,
    val surfaceVariant: Float = 0.82f,
    val surfaceVariantStrong: Float = 0.84f,
    val high: Float = 0.92f,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalRadii = staticCompositionLocalOf { Radii() }
val LocalSizes = staticCompositionLocalOf { Sizes() }
val LocalOpacity = staticCompositionLocalOf { Opacity() }

object AppTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val radii: Radii
        @Composable get() = LocalRadii.current

    val sizes: Sizes
        @Composable get() = LocalSizes.current

    val opacity: Opacity
        @Composable get() = LocalOpacity.current

    val colors: AppColors
        @Composable get() = LocalAppColors.current
}
