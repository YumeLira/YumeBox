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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun HomePreviewGuideDialog(
    show: Boolean,
    useSystemWallpaper: Boolean,
    onUseSystemWallpaperChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var page by remember { mutableIntStateOf(0) }
    var accessGranted by remember { mutableStateOf(SystemWallpaperAccess.isGranted(context)) }
    LaunchedEffect(show) {
        if (show) page = 0
    }
    DisposableEffect(lifecycleOwner, page) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = SystemWallpaperAccess.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val configuration = LocalConfiguration.current
    // Keep the dialog body within ~70% of screen height so title/buttons stay reachable.
    val contentMaxHeight = (configuration.screenHeightDp * 0.70f).dp

    WindowDialog(
        show = show,
        modifier = Modifier,
        title =
            when (page) {
                0 -> YumeTxt.Home.PreviewGuide.Title
                1 -> YumeTxt.Home.PreviewGuide.WallpaperTitle
                2 -> YumeTxt.Home.PreviewGuide.SwipeTitle
                else -> YumeTxt.Home.PreviewGuide.AddTitle
            },
        titleColor = DialogDefaults.titleColor(),
        summary =
            when (page) {
                0 -> YumeTxt.Home.PreviewGuide.Description
                1 -> YumeTxt.Home.PreviewGuide.WallpaperDescription
                2 -> YumeTxt.Home.PreviewGuide.SwipeDescription
                else -> YumeTxt.Home.PreviewGuide.AddDescription
            },
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = {},
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = contentMaxHeight)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space18),
            ) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(320)) { w -> if (forward) w else -w } +
                                fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(320)) { w -> if (forward) -w else w } +
                                        fadeOut(tween(180))) using
                                SizeTransform(clip = false)
                    },
                    label = "guide_page",
                ) { p ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (p) {
                            0 -> MoeHomeSkeletonMockup(demo = HomeMockupDemo.StartButton)
                            1 -> MoeHomeSkeletonMockup(demo = HomeMockupDemo.Wallpaper)
                            2 -> HomeToNodeSwipeMockup()
                            else -> MoeHomeSkeletonMockup(
                                demo = HomeMockupDemo.StartButton,
                                animateLaunch = false,
                                markWallpaper = true,
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier =
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == page) colorScheme.primary
                                        else colorScheme.onSurface.copy(alpha = opacity.muted)
                                    )
                        )
                    }
                }

                if (page < 3) {
                    GuideButton(
                        text = YumeTxt.Home.PreviewGuide.Next,
                        onClick = { page += 1 },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space12)) {
                        SystemWallpaperPreferenceItem(
                            checked = useSystemWallpaper,
                            onCheckedChange = onUseSystemWallpaperChange,
                        )
                        GuideButton(
                            text = YumeTxt.Home.PreviewGuide.Start,
                            onClick = { if (accessGranted) onDismissRequest() },
                            enabled = accessGranted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun GuideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}
