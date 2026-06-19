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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val PagePadding = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING
internal val DetailWidth = UiDp.dp560
internal val SectionShape = RoundedCornerShape(UiDp.dp36)
internal const val RevealDurationMs = 420
internal const val LinkTermsTag = "terms"
internal const val LinkPolicyTag = "policy"
internal val DetailPreviewBadgeSize = UiDp.dp108
internal val DetailPreviewIconSize = UiDp.dp68
internal val StartupTypewriterPhrases = listOf("YumeBox", "Hello Word")

@Composable
internal fun DreamBackdrop(modifier: Modifier = Modifier, boosted: Boolean = true) {
    val opacity = AppTheme.opacity

    val surface = MiuixTheme.colorScheme.surface
    val primary = MiuixTheme.colorScheme.primary
    val baseTint =
        remember(surface, boosted) { lerp(surface, primary, if (boosted) 0.035f else 0.02f) }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush =
                Brush.linearGradient(
                    colors = listOf(surface, baseTint, lerp(surface, primary, 0.02f)),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                )
        )
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color.White.copy(alpha = opacity.ambientLight),
                            Color.Transparent,
                            Color.Black.copy(alpha = opacity.ambientShadow),
                        )
                )
        )
    }
}

@Composable
internal fun DetailBackdrop(modifier: Modifier = Modifier) {
    val opacity = AppTheme.opacity

    val surface = MiuixTheme.colorScheme.surface
    val primary = MiuixTheme.colorScheme.primary
    val accent = remember(surface) { lerp(surface, primary, 0.045f) }

    Box(modifier = modifier.fillMaxSize().background(surface)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(accent.copy(alpha = opacity.subtleStrong), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.28f,
                    )
            )
        }
    }
}

@Composable
internal fun RevealBlock(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            fadeIn(
                animationSpec =
                    tween(durationMillis = RevealDurationMs, easing = LinearOutSlowInEasing)
            ) +
                slideInVertically(
                    animationSpec =
                        tween(durationMillis = RevealDurationMs, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 7 },
                ),
    ) {
        content()
    }
}

@Composable
internal fun RevealScaleBlock(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            fadeIn(
                animationSpec =
                    tween(durationMillis = RevealDurationMs, easing = LinearOutSlowInEasing)
            ) +
                scaleIn(
                    animationSpec =
                        tween(durationMillis = RevealDurationMs, easing = FastOutSlowInEasing),
                    initialScale = 0.92f,
                ),
    ) {
        content()
    }
}
