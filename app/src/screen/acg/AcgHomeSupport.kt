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

package com.github.yumelira.yumebox.screen.acg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.presentation.theme.Opacity
import com.github.yumelira.yumebox.presentation.theme.Radii
import com.github.yumelira.yumebox.presentation.theme.Sizes
import com.github.yumelira.yumebox.presentation.theme.Spacing
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val acgSpacing = Spacing()
private val acgRadii = Radii()
private val acgSizes = Sizes()
private val acgOpacity = Opacity()

internal object AcgUi {
    object Shape {
        val hero = RoundedCornerShape(acgSpacing.space28)
        val launchButton = RoundedCornerShape(acgRadii.full)
    }

    object Sidebar {
        const val fraction = 0.25f
        val contentOverlap = acgSpacing.space28
        val innerHorizontalPadding = acgSpacing.space12
        val visibleOpticalOffset = -acgSpacing.space4
        val topInset = acgSizes.heroStartButtonSize - acgSpacing.space2
        val bottomInset = acgSpacing.space32 + acgSpacing.space6
        val statsWidth = acgSizes.nodeDelayColumnWidth
        val timeGap = acgSpacing.space12
        val timeValueHeight = acgSizes.searchFieldMinHeight + acgSpacing.space4 / 2
        val modeTopGap = acgSpacing.space18
        val modeFontSize = 15.sp
        val dividerWidth = acgSizes.settingsIconGlyphSize
        val dividerHeight = acgSpacing.space2
        val iconSpacing = acgSpacing.space32 + acgSpacing.space6
        val iconPillHorizontalPadding = acgSpacing.space14
        val iconPillVerticalPadding = acgSpacing.space18
        val iconSize = acgSpacing.space28
        val modeLetterSpacing = 1.4.sp
        const val iconPillAlpha = 0.14f
        val digitLetterSpacing = 1.6.sp
        const val timeAlpha = 0.96f
        const val dividerAlpha = 0.62f
        const val iconAlpha = 0.88f
        val collapsedVisibleWidth = acgSpacing.space8
    }

    object Hero {
        val containerHorizontalInset = acgSpacing.space12
        val contentHorizontalInset = acgSpacing.space12
        val trafficRowGap = acgSpacing.space28
        val trafficBottomInset = acgSpacing.space12
        val runtimeInfoTopGap = acgSpacing.space16
        val delayWidth = acgSizes.nodeDelayColumnWidth
        val belowHeroTopGap = acgSpacing.space8
        val belowHeroContentGap = acgSpacing.space12
        val infoPlaceholderAlpha = acgOpacity.surfaceSoft
        val infoRowMinHeight = acgSpacing.space24
        val infoPlaceholderNodeWidth = acgSizes.homeIdleTopPadding - acgSpacing.space8
    }

    object Button {
        val bottomInset = acgSpacing.space28
        val fixedWidth = acgSizes.homeIdleTopPadding + acgSpacing.space4
        val horizontalPadding = acgSizes.settingsIconGlyphSize
        val verticalPadding = acgSpacing.space14 + acgSpacing.space2 / 2
        const val pressedScale = 0.94f
    }

    object Quote {
        val contentGap = acgSpacing.space12
        val authorTopGap = acgSpacing.space14
        val textSize = 23.sp
        val lineHeight = 31.sp
        val authorSize = 16.sp
        val authorAlpha = acgOpacity.elevatedSurface
    }

    object Traffic {
        val itemGap = acgSpacing.space6
        val labelBottomPadding = acgSpacing.space3
    }

    object Info {
        val trailingPadding = acgSpacing.space16
        val blockGap = acgSpacing.space8
    }
}

internal fun calculateAcgSidebarLaneStart(visibleWidth: Dp): Dp {
    val visibleContentWidth =
        (visibleWidth - AcgUi.Sidebar.innerHorizontalPadding).coerceAtLeast(UiDp.dp0)
    val visibleCenterLine = visibleContentWidth / 2
    val centerLine = visibleCenterLine + AcgUi.Sidebar.visibleOpticalOffset
    return centerLine - (AcgUi.Sidebar.statsWidth / 2)
}

@Composable
internal fun AcgSidebarRail(
    topValue: String,
    bottomValue: String,
    proxyMode: ProxyMode,
    icons: List<AcgSidebarIconItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(AcgUi.Sidebar.topInset))

        AcgSidebarValueStack(topValue = topValue, bottomValue = bottomValue)

        AcgSidebarModeText(
            mode = proxyMode.toAcgDisplayName(),
            modifier = Modifier.padding(top = AcgUi.Sidebar.modeTopGap),
        )

        Spacer(modifier = Modifier.weight(1f))

        AcgSidebarIconRail(icons = icons)

        Spacer(modifier = Modifier.height(AcgUi.Sidebar.bottomInset))
    }
}

@Composable
private fun AcgSidebarValueStack(topValue: String, bottomValue: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AcgUi.Sidebar.timeGap),
    ) {
        AcgSidebarTimeValue(value = topValue)
        Box(
            modifier =
                Modifier.width(AcgUi.Sidebar.dividerWidth)
                    .height(AcgUi.Sidebar.dividerHeight)
                    .background(Color.White.copy(alpha = AcgUi.Sidebar.dividerAlpha))
        )
        AcgSidebarTimeValue(value = bottomValue)
    }
}

@Composable
private fun AcgSidebarIconRail(icons: List<AcgSidebarIconItem>) {
    Column(
        modifier =
            Modifier.clip(AcgUi.Shape.launchButton)
                .background(Color.White.copy(alpha = AcgUi.Sidebar.iconPillAlpha))
                .padding(
                    horizontal = AcgUi.Sidebar.iconPillHorizontalPadding,
                    vertical = AcgUi.Sidebar.iconPillVerticalPadding,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AcgUi.Sidebar.iconSpacing),
    ) {
        icons.forEach { item -> AcgSidebarIconItemView(item = item) }
    }
}

@Composable
private fun AcgSidebarIconItemView(item: AcgSidebarIconItem) {
    Icon(
        imageVector = item.icon,
        contentDescription = null,
        tint = Color.White.copy(alpha = AcgUi.Sidebar.iconAlpha),
        modifier =
            Modifier.size(AcgUi.Sidebar.iconSize)
                .clickable(
                    interactionSource =
                        remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        },
                    indication = null,
                    onClick = item.onClick,
                ),
    )
}

@Composable
private fun AcgSidebarModeText(mode: String, modifier: Modifier = Modifier) {
    Text(
        text = mode.uppercase(),
        modifier = modifier,
        style =
            MiuixTheme.textStyles.footnote1.copy(
                fontSize = AcgUi.Sidebar.modeFontSize,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = AcgUi.Sidebar.modeLetterSpacing,
            ),
        color = Color.White.copy(alpha = 0.96f),
    )
}

@Composable
private fun AcgSidebarTimeValue(value: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(AcgUi.Sidebar.timeValueHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            color = Color.White.copy(alpha = AcgUi.Sidebar.timeAlpha),
            style = MiuixTheme.textStyles.title1,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            fontSize = 37.sp,
            letterSpacing = AcgUi.Sidebar.digitLetterSpacing,
            softWrap = false,
        )
    }
}

internal enum class AcgWallpaperQualityMode {
    Foreground,
    BackgroundBlur,
}

internal fun lerpFloat(start: Float, stop: Float, progress: Float): Float =
    start + (stop - start) * progress

internal fun lerpDp(start: Dp, stop: Dp, progress: Float): Dp = start + (stop - start) * progress

internal fun calculateHomeVisibility(currentPage: Int, currentPageOffsetFraction: Float): Float {
    val offset = abs(currentPage.toFloat() + currentPageOffsetFraction)
    return 1f - offset.coerceIn(0f, 1f)
}

internal data class AcgSidebarIconItem(val icon: ImageVector, val onClick: () -> Unit)

internal data class AcgQuote(val text: String, val author: String)

internal fun ProxyMode.toAcgDisplayName(): String =
    when (this) {
        ProxyMode.Tun -> MLang.Home.ProxyMode.Vpn
        ProxyMode.RootTun -> MLang.Home.ProxyMode.Tun
        ProxyMode.Http -> MLang.Home.ProxyMode.Http
    }

internal data class AcgDurationPair(val top: String = "00", val bottom: String = "00")

internal fun formatAcgDuration(elapsedMillis: Long): AcgDurationPair {
    val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
    val totalMinutes = totalSeconds / 60L
    val totalHours = totalMinutes / 60L
    return if (totalMinutes < 60L) {
        AcgDurationPair(
            top = totalMinutes.toString().padStart(2, '0'),
            bottom = (totalSeconds % 60L).toString().padStart(2, '0'),
        )
    } else {
        AcgDurationPair(
            top = totalHours.toString().padStart(2, '0'),
            bottom = (totalMinutes % 60L).toString().padStart(2, '0'),
        )
    }
}
