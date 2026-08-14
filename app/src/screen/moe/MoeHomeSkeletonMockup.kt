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


import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Which guided demo the home skeleton mockup plays. */
internal enum class HomeMockupDemo {
    /** The bottom launch capsule is highlighted and taps in a repeating rhythm. */
    StartButton,

    /** A plain launch row; instead the hero is long-pressed and a photo-picker sheet slides up. */
    Wallpaper,
}

@Composable
internal fun MoeHomeSkeletonMockup(
    demo: HomeMockupDemo = HomeMockupDemo.StartButton,
    animateLaunch: Boolean = true,
    markWallpaper: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val palette = mockupPalette()

    val wallpaper = demo == HomeMockupDemo.Wallpaper

    // The wallpaper demo drives a single looped cycle: long-press the hero, then raise the picker.
    val transition = rememberInfiniteTransition(label = "home_wallpaper")
    val sheetProgress by
    transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 4200
                        0f at 0
                        0f at 1500
                        1f at 1900 using FastOutSlowInEasing
                        1f at 3500
                        0f at 3900 using FastOutSlowInEasing
                        0f at 4200
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "wallpaper_sheet_progress",
    )
    val heroLongPressScale by
    transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 4200
                        1f at 0
                        1f at 500
                        0.82f at 760 using FastOutSlowInEasing
                        0.82f at 1450
                        1f at 1680 using FastOutSlowInEasing
                        1f at 4200
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "wallpaper_hero_long_press_scale",
    )
    val heroRippleScale by
    transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 2.1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 4200
                        0.6f at 0
                        0.6f at 760
                        2.1f at 1500 using FastOutSlowInEasing
                        2.1f at 4200
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "wallpaper_hero_ripple_scale",
    )
    val heroRippleAlpha by
    transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 4200
                        0f at 0
                        0f at 650
                        0.45f at 760
                        0f at 1500 using FastOutSlowInEasing
                        0f at 4200
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "wallpaper_hero_ripple_alpha",
    )

    MockupPhoneFrame(palette = palette, modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left sidebar (~22%): vertical clock near the top, nav icon rail pinned to the bottom.
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.22f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(10.dp))
                // Vertical clock: one continuous stroke ("一笔带过").
                Box(
                    Modifier
                        .width(13.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.maskStrong)
                )

                Spacer(Modifier.weight(1f))

                // Nav icon rail: three icons stacked near the bottom.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    repeat(3) {
                        Box(
                            Modifier
                                .size(13.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.mask)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            // Right content (~78%): hero (traffic + node info) + quote + launch controls row.
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.78f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(2f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(palette.maskStrong)
                ) {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Traffic strip: UP on the left, DOWN on the right.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                Modifier
                                    .width(30.dp)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.onHero)
                            )
                            Box(
                                Modifier
                                    .width(30.dp)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.onHero)
                            )
                        }
                        // Node info: flag circle + node name on the left, ping on the right.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(palette.onHero)
                                )
                                Box(
                                    Modifier
                                        .width(46.dp)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.onHero)
                                )
                            }
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.onHero)
                            )
                        }
                    }

                    // Wallpaper demo: a looping long-press effect centered on the hero.
                    if (wallpaper) {
                        Box(
                            modifier = Modifier.align(Alignment.Center),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .graphicsLayer {
                                            scaleX = heroRippleScale
                                            scaleY = heroRippleScale
                                            alpha = heroRippleAlpha
                                        }
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.primary)
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .graphicsLayer {
                                            scaleX = heroLongPressScale
                                            scaleY = heroLongPressScale
                                        }
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.surface)
                                        .border(2.dp, colorScheme.primary, CircleShape)
                            )
                        }
                    }
                    if (markWallpaper) {
                        Text(
                            text = "System Wallpaper",
                            color = Color.White,
                            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                        )
                    }
                }

                // Bottom third: quote block + the launch controls row (config circle + capsule).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Quote block: three left-aligned lines + a right-aligned author line.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.92f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.mask)
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.mask)
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.5f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.mask)
                        )
                        Box(
                            Modifier
                                .align(Alignment.End)
                                .fillMaxWidth(0.3f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.mask)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (wallpaper || !animateLaunch) {
                        PlainLaunchRow()
                    } else {
                        HighlightedLaunchRow(animate = animateLaunch)
                    }

                    Spacer(Modifier.height(5.dp))
                }
            }
        }

        // Wallpaper demo: dim scrim + the photo-picker bottom sheet sliding up. Clipped to the
        // phone-frame's inner radius so they follow the rounded corners instead of sharp edges.
        if (wallpaper) {
            Box(modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(15.dp))) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.40f * sheetProgress))
                )
                PhotoPickerSheet(
                    sheetProgress = sheetProgress,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun HighlightedLaunchRow(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val colorScheme = MiuixTheme.colorScheme
    val palette = mockupPalette()

    val transition = rememberInfiniteTransition(label = "start_chip")
    // A press that reads as a tap: hold, snap down, snap back, then rest until the next cycle.
    val animatedPressScale by
    transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 1500
                        1f at 0
                        1f at 200
                        0.9f at 360 using FastOutSlowInEasing
                        1f at 560
                        1f at 1500
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "press_scale",
    )
    val animatedRippleScale by
    transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 2.6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "ripple_scale",
    )
    val animatedRippleAlpha by
    transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "ripple_alpha",
    )
    val pressScale = animatedPressScale.takeIf { animate } ?: 1f
    val rippleScale = animatedRippleScale.takeIf { animate } ?: 0.6f
    val rippleAlpha = animatedRippleAlpha.takeIf { animate } ?: 0f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // Config button: a bordered surface circle with an icon glyph in the middle.
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface)
                    .border(1.dp, palette.frameBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.maskStrong))
        }

        // Launch capsule filling the remaining width, pressing on each tap.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .graphicsLayer {
                            scaleX = rippleScale
                            scaleY = rippleScale
                            alpha = rippleAlpha
                        }
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .height(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colorScheme.surface)
                        .border(1.dp, palette.frameBorder, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = YumeTxt.Home.Control.Start,
                    color = colorScheme.onSurface.copy(alpha = 0.72f),
                    style =
                        MiuixTheme.textStyles.footnote1.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                )
            }

            // Prominent touch dot over the capsule's center, pressing in sync with it.
            Box(
                modifier =
                    Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                        .border(2.dp, colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
private fun PlainLaunchRow(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val palette = mockupPalette()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // Config button: a bordered surface circle with an icon glyph in the middle.
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface)
                    .border(1.dp, palette.frameBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.maskStrong))
        }

        // Launch capsule filling the remaining width, without any tap animation.
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colorScheme.surface)
                    .border(1.dp, palette.frameBorder, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = YumeTxt.Home.Control.Start,
                color = colorScheme.onSurface.copy(alpha = 0.72f),
                style =
                    MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
    }
}

@Composable
private fun PhotoPickerSheet(sheetProgress: Float, modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity
    val palette = mockupPalette()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .graphicsLayer { translationY = (1f - sheetProgress) * size.height }
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colorScheme.surface)
                .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .width(22.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.mask)
            )

            // Tab pills: "全部" highlighted, "影集" muted.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(colorScheme.primary.copy(alpha = opacity.subtleStrong))
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(palette.mask)
                )
            }

            // Notice bar.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.mask)
            )

            // Photo grid: 3 rows of 3 tiles filling the freed space; the first tile is the camera.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(3) { col ->
                            val isCamera = row == 0 && col == 0
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (isCamera) palette.mask else palette.maskStrong
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCamera) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(palette.maskStrong)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
