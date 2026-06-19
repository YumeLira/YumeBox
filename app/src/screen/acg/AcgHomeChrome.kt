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

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.formatBytesForDisplay
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.presentation.component.CountryFlagCircle
import com.github.yumelira.yumebox.presentation.icon.ShellIcons
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Waiting
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import com.github.yumelira.yumebox.screen.home.HomeProxyControlState
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AcgSidebarDecoration(
    backdrop: LayerBackdrop,
    blurEnabled: Boolean,
    blurProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val spacing = AppTheme.spacing
    val surface = MiuixTheme.colorScheme.surface
    val isDarkSurface = surface.luminance() < 0.5f
    val glassBase =
        if (isDarkSurface) {
            Color.Black.copy(alpha = 0.24f)
        } else {
            surface.copy(alpha = 0.13f)
        }
    val glassTint = Color.Black.copy(alpha = 0.10f)
    val glassGradientStart =
        if (isDarkSurface) {
            Color.Black.copy(alpha = 0.36f)
        } else {
            surface.copy(alpha = 0.23f)
        }
    val glassGradientEnd =
        if (isDarkSurface) {
            surface.copy(alpha = 0.18f)
        } else {
            surface.copy(alpha = 0.16f)
        }
    val clampedBlurProgress = blurProgress.coerceIn(0f, 1f)
    val blurRadiusPx = lerpFloat(30f, 52f, clampedBlurProgress)
    val blurColors =
        BlurDefaults.blurColors(
            blendColors =
                listOf(
                    BlendColorEntry(color = glassBase, mode = BlurBlendMode.SrcOver),
                    BlendColorEntry(color = glassTint, mode = BlurBlendMode.SrcOver),
                ),
            saturation = if (isDarkSurface) 1.06f else 1.02f,
            contrast = if (isDarkSurface) 1.08f else 1.10f,
            brightness = if (isDarkSurface) 0.00f else -0.05f,
        )
    val blurModifier =
        if (blurEnabled) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = blurRadiusPx,
                noiseCoefficient = 0f,
                colors = blurColors,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            )
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .then(blurModifier)
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(glassGradientStart, glassGradientEnd)
                        ),
                    shape = RectangleShape,
                )
                .padding(
                    horizontal = AcgUi.Sidebar.innerHorizontalPadding,
                    vertical = spacing.space24,
                ),
        content = content,
    )
}

@Composable
internal fun AcgSidebarContent(
    topValue: String,
    bottomValue: String,
    proxyMode: ProxyMode,
    icons: List<AcgSidebarIconItem>,
    visibleWidth: Dp,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AcgSidebarRail(
            topValue = topValue,
            bottomValue = bottomValue,
            proxyMode = proxyMode,
            icons = icons,
            modifier =
                Modifier.fillMaxHeight()
                    .width(AcgUi.Sidebar.statsWidth)
                    .offset(x = calculateAcgSidebarLaneStart(visibleWidth)),
        )
    }
}

@Composable
internal fun AcgQuoteText(quote: AcgQuote, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AcgUi.Quote.contentGap),
    ) {
        Text(
            text = quote.text,
            color = color,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Medium,
            fontSize = AcgUi.Quote.textSize,
            lineHeight = AcgUi.Quote.lineHeight,
            softWrap = true,
            overflow = TextOverflow.Clip,
        )
        Text(
            text = "— ${quote.author}",
            modifier = Modifier.align(Alignment.End).padding(top = AcgUi.Quote.authorTopGap),
            color = color.copy(alpha = AcgUi.Quote.authorAlpha),
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Medium,
            fontSize = AcgUi.Quote.authorSize,
            softWrap = false,
        )
    }
}

@Composable
internal fun AcgLaunchButton(
    controlState: HomeProxyControlState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isRunning = controlState == HomeProxyControlState.Running
    val background =
        if (isRunning) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.onBackground
        }
    val contentColor =
        if (isRunning) {
            MiuixTheme.colorScheme.onPrimary
        } else {
            MiuixTheme.colorScheme.background
        }
    val pressScale by
        animateFloatAsState(
            targetValue = if (isPressed && enabled) AcgUi.Button.pressedScale else 1f,
            animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
            label = "acg_launch_button_press_scale",
        )

    Box(
        modifier =
            Modifier.graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .width(AcgUi.Button.fixedWidth)
                .clip(AcgUi.Shape.launchButton)
                .background(background, AcgUi.Shape.launchButton)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(
                    horizontal = AcgUi.Button.horizontalPadding,
                    vertical = AcgUi.Button.verticalPadding,
                )
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space10),
        ) {
            Icon(
                imageVector =
                    when (controlState) {
                        HomeProxyControlState.Idle -> ShellIcons.StartProxy
                        HomeProxyControlState.Running -> ShellIcons.StopProxy
                        HomeProxyControlState.Connecting,
                        HomeProxyControlState.Disconnecting -> Yume.Waiting
                    },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(spacing.space18),
            )
            Box(modifier = Modifier.height(22.dp), contentAlignment = Alignment.CenterStart) {
                AnimatedContent(
                    targetState = controlState,
                    transitionSpec = {
                        (slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec =
                                    tween(
                                        durationMillis = AnimationSpecs.DURATION_FAST,
                                        easing = AnimationSpecs.EmphasizedDecelerate,
                                    ),
                            ) +
                                fadeIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = AnimationSpecs.DURATION_FAST,
                                            easing = AnimationSpecs.EnterEasing,
                                        )
                                ))
                            .togetherWith(
                                slideOutVertically(
                                    targetOffsetY = { -it / 2 },
                                    animationSpec =
                                        tween(
                                            durationMillis = AnimationSpecs.DURATION_INSTANT,
                                            easing = AnimationSpecs.EmphasizedAccelerate,
                                        ),
                                ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = AnimationSpecs.DURATION_INSTANT,
                                                easing = AnimationSpecs.ExitEasing,
                                            )
                                    )
                            )
                            .using(SizeTransform(clip = false))
                    },
                    label = "acg_launch_button_text",
                ) { state ->
                    Text(
                        text =
                            when (state) {
                                HomeProxyControlState.Idle -> MLang.Home.Control.Start
                                HomeProxyControlState.Connecting -> MLang.Home.Status.Connecting
                                HomeProxyControlState.Running -> MLang.Home.Control.Stop
                                HomeProxyControlState.Disconnecting ->
                                    MLang.Home.Status.Disconnecting
                            },
                        color = contentColor,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AcgTrafficStrip(
    downloadSpeed: Long,
    uploadSpeed: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AcgUi.Hero.trafficRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            AcgTrafficItem(label = MLang.Home.Traffic.UpShort, speed = uploadSpeed)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            AcgTrafficItem(label = MLang.Home.Traffic.DownShort, speed = downloadSpeed)
        }
    }
}

@Composable
private fun AcgTrafficItem(label: String, speed: Long) {
    val (value, unit) = formatBytesForDisplay(speed)
    val onSurface = MiuixTheme.colorScheme.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(AcgUi.Traffic.itemGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            color = onSurface.copy(alpha = 0.62f),
            style = MiuixTheme.textStyles.footnote1,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = AcgUi.Traffic.labelBottomPadding),
        )
        Text(
            text = value,
            color = onSurface,
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
        )
        Text(
            text = unit,
            color = onSurface.copy(alpha = 0.55f),
            style = MiuixTheme.textStyles.footnote1,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = AcgUi.Traffic.labelBottomPadding),
        )
    }
}

@Composable
internal fun AcgHomeInfoPanel(
    serverName: String?,
    serverPing: Int?,
    modifier: Modifier = Modifier,
) {
    val flaggedNode = remember(serverName) { serverName?.let(::extractFlaggedName) }
    val resolvedNodeName = flaggedNode?.displayName ?: serverName.orEmpty().ifBlank { "" }
    val resolvedPing =
        serverPing
            ?.takeIf { it in 1..1000 }
            ?.let { ping -> MLang.Home.NodeInfo.DelayValue.format(ping) }
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = AcgUi.Hero.infoRowMinHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (resolvedNodeName.isNotBlank()) {
            AcgInfoBlock(
                value = resolvedNodeName,
                modifier = Modifier.weight(1f).padding(end = AcgUi.Info.trailingPadding),
                leading = {
                    flaggedNode?.countryCode?.let { countryCode ->
                        CountryFlagCircle(
                            countryCode = countryCode,
                            size = AppTheme.spacing.space16,
                        )
                    }
                },
            )
        } else {
            Spacer(modifier = Modifier.weight(1f).padding(end = AcgUi.Info.trailingPadding))
        }

        if (resolvedPing != null) {
            AcgInfoBlock(
                value = resolvedPing,
                modifier = Modifier.width(AcgUi.Hero.delayWidth),
                valueColor =
                    when {
                        serverPing < 500 -> AppTheme.colors.acg.pingExcellent
                        else -> AppTheme.colors.acg.pingWarning
                    },
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun AcgInfoBlock(
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    valueFontFamily: FontFamily? = null,
    alignEnd: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                space = AcgUi.Info.blockGap,
                alignment = if (alignEnd) Alignment.End else Alignment.Start,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(
            text = value,
            color = valueColor,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            fontFamily = valueFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = if (alignEnd) Modifier else Modifier.weight(1f),
        )
    }
}
