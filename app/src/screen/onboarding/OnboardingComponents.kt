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

package com.github.yumelira.yumebox.screen.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.icon.ShellIcons
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun StartupTypewriterWord(phrases: List<String>, modifier: Modifier = Modifier) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity

    var phraseIndex by remember(phrases) { mutableStateOf(0) }
    var visibleLength by remember(phrases) { mutableStateOf(0) }
    var deleting by remember(phrases) { mutableStateOf(false) }

    LaunchedEffect(phrases) {
        while (true) {
            val currentText = phrases.getOrElse(phraseIndex) { "" }
            if (!deleting) {
                if (visibleLength < currentText.length) {
                    delay(95)
                    visibleLength += 1
                } else {
                    delay(1850)
                    deleting = true
                }
            } else {
                if (visibleLength > 0) {
                    delay(65)
                    visibleLength -= 1
                } else {
                    delay(700)
                    deleting = false
                    phraseIndex = (phraseIndex + 1) % phrases.size
                }
            }
        }
    }

    val currentText = phrases.getOrElse(phraseIndex) { "" }
    val displayText = remember(currentText, visibleLength) { currentText.take(visibleLength) }
    val showCursor = visibleLength < currentText.length || deleting

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayText,
            style =
                MiuixTheme.textStyles.title1.copy(
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp,
                ),
            color = MiuixTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (showCursor) {
            Box(
                modifier =
                    Modifier.padding(
                            start = spacing.space4,
                            top = AppTheme.sizes.textLineCompactSpacing,
                        )
                        .width(UiDp.dp1_2)
                        .height(UiDp.dp46)
                        .background(
                            color = MiuixTheme.colorScheme.onSurface.copy(alpha = opacity.high),
                            shape = RoundedCornerShape(50),
                        )
            )
        }
    }
}

@Composable
internal fun HeroStartButton(enabled: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val componentSizes = AppTheme.sizes

    val pulseTransition = rememberInfiniteTransition(label = "startup_button_pulse")
    val pulseScale by
        pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.045f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "startup_button_scale",
        )

    Box(
        modifier =
            modifier
                .size(componentSizes.heroStartButtonSize)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onStart)
                .graphicsLayer(
                    alpha = if (enabled) 1f else 0.45f,
                    scaleX = if (enabled) pulseScale else 1f,
                    scaleY = if (enabled) pulseScale else 1f,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            modifier =
                Modifier.matchParentSize()
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary)
        )
        Icon(
            imageVector = ShellIcons.NavigateForward,
            contentDescription = "Start",
            tint = MiuixTheme.colorScheme.onPrimary,
            modifier = Modifier.size(componentSizes.settingsIconGlyphSize),
        )
    }
}

@Composable
internal fun DetailPreviewBadge(icon: ImageVector) {
    Box(modifier = Modifier.size(DetailPreviewBadgeSize), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(DetailPreviewIconSize),
        )
    }
}

@Composable
internal fun DetailGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val opacity = AppTheme.opacity

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(SectionShape)
                .background(
                    MiuixTheme.colorScheme.surfaceVariant.copy(alpha = opacity.surfaceVariant)
                ),
        content = content,
    )
}

@Composable
internal fun DetailDivider() {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = spacing.space18),
        thickness = componentSizes.thinDividerThickness,
        color = MiuixTheme.colorScheme.outline.copy(alpha = opacity.surfaceSoft),
    )
}

@Composable
internal fun PermissionRow(
    icon: ImageVector,
    title: String,
    summary: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space18, vertical = spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space14),
    ) {
        Box(
            modifier =
                Modifier.size(componentSizes.iconBadgeMedium)
                    .clip(RoundedCornerShape(radii.radius18))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtle)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(spacing.space20),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        if (granted) {
            Text(
                text = MLang.Onboarding.Permission.Common.Granted,
                style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                color = MiuixTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = ShellIcons.NavigateForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(spacing.space18),
            )
        }
    }
}

@Composable
internal fun ProjectLinkRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space18, vertical = spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space14),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(componentSizes.settingsIconGlyphSize),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        Icon(
            imageVector = ShellIcons.NavigateForward,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(spacing.space18),
        )
    }
}

@Composable
internal fun PrimaryFooterAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(radii.radius24))
                .background(MiuixTheme.colorScheme.primary)
                .graphicsLayer(alpha = if (enabled) 1f else opacity.disabledStrong)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = spacing.space20, vertical = spacing.space16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
            color = MiuixTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
internal fun SecondaryFooterAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(radii.radius24))
                .background(
                    MiuixTheme.colorScheme.surfaceVariant.copy(alpha = opacity.surfaceVariantStrong)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space20, vertical = spacing.space16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun SecondaryLinkAction(text: String, onClick: () -> Unit) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii

    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(radii.radius18))
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space10, vertical = spacing.space6),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
