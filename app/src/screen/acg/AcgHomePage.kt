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

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.sketch.resize.Scale
import com.github.panpf.sketch.util.Size
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.presentation.component.LocalHandlePageChange
import com.github.yumelira.yumebox.presentation.component.calculateWallpaperViewportLayout
import com.github.yumelira.yumebox.presentation.icon.ShellIcons
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.screen.home.HomeProxyControlState
import com.github.yumelira.yumebox.screen.home.HomeViewModel
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AcgHomePage(
    mainInnerPadding: PaddingValues,
    wallpaperUri: String,
    wallpaperZoom: Float = 1f,
    wallpaperBiasX: Float = 0f,
    wallpaperBiasY: Float = 0f,
    isActive: Boolean,
    pageProgress: Float = 1f,
    sidebarProgress: Float = pageProgress,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controlState by homeViewModel.controlState.collectAsState()
    val profiles by homeViewModel.profiles.collectAsState()
    val profilesLoaded by homeViewModel.profilesLoaded.collectAsState()
    val recommendedProfile by homeViewModel.recommendedProfile.collectAsState()
    val hasEnabledProfile by homeViewModel.hasEnabledProfile.collectAsState(initial = false)
    val selectedServerName by homeViewModel.selectedServerName.collectAsState()
    val selectedServerPing by homeViewModel.selectedServerPing.collectAsState()
    val trafficNow by homeViewModel.trafficNow.collectAsState()
    val proxyMode by homeViewModel.proxyMode.collectAsState()
    val runtimeSnapshot by homeViewModel.runtimeSnapshot.collectAsState()
    val themeMode by appSettingsViewModel.themeMode.state.collectAsState()
    val acgHomeQuote by appSettingsViewModel.acgHomeQuote.state.collectAsState()
    val acgHomeQuoteAuthor by appSettingsViewModel.acgHomeQuoteAuthor.state.collectAsState()
    val sidebarExpanded by appSettingsViewModel.acgSidebarExpanded.state.collectAsState()

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(Unit) { homeViewModel.refreshProxyMode() }

    LaunchedEffect(isActive) {
        homeViewModel.setHomeScreenActive(isActive)
        if (isActive) {
            homeViewModel.reconcileRuntimeState()
            homeViewModel.refreshProxyMode()
        }
    }

    DisposableEffect(homeViewModel) { onDispose { homeViewModel.setHomeScreenActive(false) } }

    DisposableEffect(lifecycleOwner, homeViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.reconcileRuntimeState()
                homeViewModel.refreshProxyMode()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val visualControlState = controlState
    val now by
        produceState(
            initialValue = System.currentTimeMillis(),
            visualControlState,
            runtimeSnapshot.startedAt,
        ) {
            if (
                visualControlState != HomeProxyControlState.Running ||
                    runtimeSnapshot.startedAt == null
            ) {
                value = System.currentTimeMillis()
                return@produceState
            }
            PollingTimers.ticks(PollingTimerSpecs.AcgElapsedClock).collect {
                value = System.currentTimeMillis()
            }
        }
    val startedAt = runtimeSnapshot.startedAt
    val isRunning = visualControlState == HomeProxyControlState.Running
    val elapsedMillis =
        if (isRunning && startedAt != null) {
            (now - startedAt).coerceAtLeast(0L)
        } else {
            0L
        }
    val durationPair =
        remember(elapsedMillis, isRunning) {
            if (isRunning) {
                formatAcgDuration(elapsedMillis)
            } else {
                AcgDurationPair()
            }
        }
    val trafficData =
        remember(trafficNow, isRunning) {
            if (isRunning) TrafficData.from(trafficNow) else TrafficData.ZERO
        }
    val systemDark = isSystemInDarkTheme()
    val isDarkHomeSurface =
        when (themeMode) {
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
            ThemeMode.Auto -> systemDark
        }
    val wallpaperBackdrop = rememberLayerBackdrop()
    val contentSurface = if (isDarkHomeSurface) MiuixTheme.colorScheme.surface else Color.White
    val handlePageChange = LocalHandlePageChange.current
    val sidebarIcons = remember {
        listOf(
            AcgSidebarIconItem(ShellIcons.OpenProxy) { handlePageChange(1) },
            AcgSidebarIconItem(ShellIcons.OpenProfiles) { handlePageChange(2) },
            AcgSidebarIconItem(ShellIcons.OpenSettings) { handlePageChange(3) },
        )
    }
    val quote =
        AcgQuote(
            text = acgHomeQuote.ifBlank { MLang.AppSettings.Experimental.AcgQuoteDefault },
            author =
                acgHomeQuoteAuthor.ifBlank { MLang.AppSettings.Experimental.AcgQuoteAuthorDefault },
        )
    val animatedSidebarToggleProgress by
        animateFloatAsState(
            targetValue = if (sidebarExpanded) 1f else 0f,
            animationSpec =
                tween(
                    durationMillis = if (sidebarExpanded) 420 else 320,
                    easing =
                        if (sidebarExpanded) AnimationSpecs.EmphasizedDecelerate
                        else AnimationSpecs.EmphasizedAccelerate,
                ),
            label = "acg_sidebar_toggle",
        )

    val handleProxyAction: () -> Unit = {
        if (!hasEnabledProfile || recommendedProfile == null) {
            context.toast(MLang.ProfilesVM.Error.ProfileNotExist, Toast.LENGTH_SHORT)
        } else if (visualControlState == HomeProxyControlState.Idle) {
            recommendedProfile?.let { profile ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                homeViewModel.startProxy(profileId = profile.uuid.toString(), mode = null)
            }
        } else if (visualControlState == HomeProxyControlState.Running) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
            scope.launch { homeViewModel.stopProxy() }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidebarWidth = maxWidth * AcgUi.Sidebar.fraction
        val contentStart = (sidebarWidth - AcgUi.Sidebar.contentOverlap).coerceAtLeast(UiDp.dp0)
        val collapsedVisibleWidth = AcgUi.Sidebar.collapsedVisibleWidth
        val heroHeight = maxHeight * 0.66f
        val clampedPageProgress = pageProgress.coerceIn(0f, 1f)
        val clampedSidebarProgress = sidebarProgress.coerceIn(0f, 1f)
        val effectiveSidebarProgress = clampedSidebarProgress * animatedSidebarToggleProgress
        val swipePressProgress = FastOutSlowInEasing.transform(1f - clampedPageProgress)
        val sidebarVisibleWidth =
            lerpDp(collapsedVisibleWidth, contentStart, effectiveSidebarProgress)
        val contentPanelStart = lerpDp(UiDp.dp0, contentStart, effectiveSidebarProgress)
        val sidebarOffset = lerpDp((-56).dp, UiDp.dp0, effectiveSidebarProgress)
        val sidebarAlpha = lerpFloat(0.78f, 1f, effectiveSidebarProgress) * clampedPageProgress
        val contentCorner = lerpDp(UiDp.dp0, UiDp.dp30, effectiveSidebarProgress)
        // 仅在页面横向滑动时缩放，避免侧栏展开/收起过程出现尺寸变化感
        val heroImageScale =
            if (clampedPageProgress >= 0.999f) {
                1f
            } else {
                lerpFloat(1f, 0.965f, swipePressProgress)
            }
        val sidebarBlurReady by
            remember(effectiveSidebarProgress) {
                derivedStateOf {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        effectiveSidebarProgress > 0.03f
                }
            }

        AcgWallpaperBackground(
            wallpaperUri = wallpaperUri,
            wallpaperZoom = wallpaperZoom,
            wallpaperBiasX = wallpaperBiasX,
            wallpaperBiasY = wallpaperBiasY,
            qualityMode = AcgWallpaperQualityMode.BackgroundBlur,
            modifier = Modifier.matchParentSize().layerBackdrop(wallpaperBackdrop),
        )

        AcgSidebarDecoration(
            backdrop = wallpaperBackdrop,
            blurEnabled = sidebarBlurReady,
            blurProgress = effectiveSidebarProgress,
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = with(density) { sidebarOffset.toPx() }
                        alpha = sidebarAlpha
                    },
            content = {
                AcgSidebarContent(
                    topValue = durationPair.top,
                    bottomValue = durationPair.bottom,
                    proxyMode = proxyMode,
                    icons = sidebarIcons,
                    visibleWidth = sidebarVisibleWidth,
                )
            },
        )

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(start = contentPanelStart)
                    .graphicsLayer {
                        shape =
                            RoundedCornerShape(
                                topStart = contentCorner,
                                bottomStart = contentCorner,
                            )
                        clip = true
                    }
                    .background(contentSurface)
        ) {
            Box(
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(
                            start = AcgUi.Hero.containerHorizontalInset,
                            end = AcgUi.Hero.containerHorizontalInset,
                            top = statusBarTop,
                        )
                        .fillMaxHeight(0.66f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    appSettingsViewModel.onAcgSidebarExpandedChange(
                                        !sidebarExpanded
                                    )
                                }
                            )
                        }
                        .graphicsLayer {
                            shape = AcgUi.Shape.hero
                            clip = true
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            scaleX = heroImageScale
                            scaleY = heroImageScale
                        }
            ) {
                AcgWallpaperBackground(
                    wallpaperUri = wallpaperUri,
                    wallpaperZoom = wallpaperZoom,
                    wallpaperBiasX = wallpaperBiasX,
                    wallpaperBiasY = wallpaperBiasY,
                    qualityMode = AcgWallpaperQualityMode.Foreground,
                    modifier = Modifier.matchParentSize(),
                )
                Spacer(
                    modifier =
                        Modifier.matchParentSize()
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colorStops =
                                            arrayOf(
                                                0.0f to Color.Transparent,
                                                0.64f to Color.Transparent,
                                                0.80f to contentSurface.copy(alpha = 0.90f),
                                                1.0f to contentSurface,
                                            )
                                    )
                            )
                )

                AnimatedVisibility(
                    visible = isRunning,
                    modifier =
                        Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(
                                start = AcgUi.Hero.contentHorizontalInset,
                                end = AcgUi.Hero.contentHorizontalInset,
                                bottom = AcgUi.Hero.trafficBottomInset,
                            ),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AcgUi.Hero.runtimeInfoTopGap)
                    ) {
                        AcgTrafficStrip(
                            downloadSpeed = trafficData.download,
                            uploadSpeed = trafficData.upload,
                        )
                        AcgHomeInfoPanel(
                            serverName = selectedServerName.takeIf { isRunning },
                            serverPing = selectedServerPing.takeIf { isRunning },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Column(
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(
                            start =
                                AcgUi.Hero.containerHorizontalInset +
                                    AcgUi.Hero.contentHorizontalInset,
                            end =
                                AcgUi.Hero.containerHorizontalInset +
                                    AcgUi.Hero.contentHorizontalInset,
                            top = statusBarTop + heroHeight + AcgUi.Hero.belowHeroTopGap,
                        ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(AcgUi.Hero.belowHeroContentGap),
            ) {
                AcgQuoteText(
                    quote = quote,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .padding(
                            end = UiDp.dp12,
                            bottom =
                                mainInnerPadding.calculateBottomPadding() + AcgUi.Button.bottomInset,
                        )
            ) {
                AcgLaunchButton(
                    controlState = visualControlState,
                    enabled =
                        profilesLoaded && profiles.isNotEmpty() && visualControlState.canInteract,
                    onClick = handleProxyAction,
                )
            }
        }
    }
}

@Composable
private fun AcgWallpaperBackground(
    wallpaperUri: String,
    wallpaperZoom: Float = 1f,
    wallpaperBiasX: Float = 0f,
    wallpaperBiasY: Float = 0f,
    qualityMode: AcgWallpaperQualityMode = AcgWallpaperQualityMode.Foreground,
    modifier: Modifier = Modifier,
) {
    val clampedZoom = wallpaperZoom.coerceIn(1f, 5f)
    val context = LocalContext.current
    val density = LocalDensity.current

    // Resolve the render model off the compose hot path: a stored file:// copy is used only when it
    // actually exists; a non-blank-but-missing value (cleared cache, partially wiped data, or a
    // legacy content:// that became unreadable) falls back to the bundled asset.
    val model by
        produceState(
            initialValue = wallpaperUri.ifBlank { ACG_BUNDLED_WALLPAPER },
            wallpaperUri,
        ) {
            value =
                withContext(Dispatchers.IO) {
                    resolveAcgWallpaperModel(context, wallpaperUri)
                }
        }

    val imageBounds by
        produceState<Pair<Int, Int>?>(initialValue = null, model) {
            if (model.startsWith("file:///android_asset/")) {
                value = null
                return@produceState
            }
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            context.contentResolver.openInputStream(Uri.parse(model))?.use { input
                                ->
                                val options =
                                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeStream(input, null, options)
                                if (options.outWidth > 0 && options.outHeight > 0) {
                                    options.outWidth to options.outHeight
                                } else {
                                    null
                                }
                            }
                        }
                        .getOrNull()
                }
        }

    BoxWithConstraints(modifier = modifier) {
        val containerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        // 背景模糊层使用低质量采样，内容主图优先原图画质
        val requestWidth = kotlin.math.ceil(containerWidthPx * 1.2f).toInt()
        val requestHeight = kotlin.math.ceil(containerHeightPx * 1.2f).toInt()

        val painter =
            rememberAsyncImagePainter(
                request =
                    ImageRequest(context, model) {
                        scale(Scale.CENTER_CROP)
                        memoryCachePolicy(CachePolicy.DISABLED)
                        downloadCachePolicy(CachePolicy.DISABLED)
                        resultCachePolicy(CachePolicy.DISABLED)
                        if (qualityMode == AcgWallpaperQualityMode.BackgroundBlur) {
                            size(requestWidth, requestHeight)
                            precision(Precision.LESS_PIXELS)
                        } else {
                            size(Size.Origin)
                            precision(Precision.EXACTLY)
                        }
                    }
            )

        val intrinsic = painter.intrinsicSize
        val imageWidthPx =
            intrinsic.width.takeIf { it > 0f && it.isFinite() } ?: imageBounds?.first?.toFloat()
        val imageHeightPx =
            intrinsic.height.takeIf { it > 0f && it.isFinite() } ?: imageBounds?.second?.toFloat()
        val viewportLayout =
            calculateWallpaperViewportLayout(
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                imageWidthPx = imageWidthPx,
                imageHeightPx = imageHeightPx,
                zoom = clampedZoom,
                biasX = wallpaperBiasX,
                biasY = wallpaperBiasY,
            )
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(viewportLayout.biasX, viewportLayout.biasY),
            modifier = Modifier.align(Alignment.Center).fillMaxSize(),
        )
    }
}

private const val ACG_BUNDLED_WALLPAPER = "file:///android_asset/wallpaper.jpg"

/**
 * Maps a stored wallpaper preference value to a Sketch-loadable model. A `file://` path is only used
 * when the backing file exists; otherwise (blank value, missing local copy, or a dead source) the
 * bundled asset is returned. Must be called off the main thread because it touches the filesystem.
 */
private fun resolveAcgWallpaperModel(context: Context, wallpaperUri: String): String {
    if (wallpaperUri.isBlank()) return ACG_BUNDLED_WALLPAPER
    if (wallpaperUri.startsWith("file://")) {
        val path = wallpaperUri.removePrefix("file://")
        if (path.startsWith("/android_asset/")) return wallpaperUri
        return if (File(path).exists()) wallpaperUri else ACG_BUNDLED_WALLPAPER
    }
    // Legacy content:// (pre-feature installs) or other schemes: try to read it; if unreadable,
    // fall back to the bundled asset instead of rendering nothing.
    val readable =
        runCatching {
                context.contentResolver.openInputStream(Uri.parse(wallpaperUri))?.use { true }
                    ?: false
            }
            .getOrDefault(false)
    return if (readable) wallpaperUri else ACG_BUNDLED_WALLPAPER
}
