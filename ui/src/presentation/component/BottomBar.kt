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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.presentation.component


import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ArrowDownUp
import com.github.yumeyucca.yumebox.presentation.icon.yume.Bolt
import com.github.yumeyucca.yumebox.presentation.icon.yume.House
import com.github.yumeyucca.yumebox.presentation.icon.yume.PackageCheck
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.YumeHaze
import com.github.yumeyucca.yumebox.presentation.theme.YumeHaze.chromeEffect
import com.kyant.shapes.Capsule
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import kotlin.math.roundToInt

private const val BottomBarLayoutAnimationDurationMillis = 380
private const val ProxyDestinationRevealDurationMillis = 300

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        val boundedTarget = targetIndex.coerceIn(0, pagerState.pageCount - 1)
        if (boundedTarget == selectedPage && !pagerState.isScrollInProgress) return

        navJob?.cancel()
        selectedPage = boundedTarget
        isNavigating = true
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                // PagerState resolves the target after layout. A hand-calculated pixel distance
                // can be zero when the first composition has not been measured yet.
                pagerState.animateScrollToPage(
                    page = boundedTarget,
                    animationSpec = MainBottomBarDefaults.PagerNavigationAnimationSpec,
                )
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != boundedTarget) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState =
    remember(pagerState, coroutineScope) { MainPagerState(pagerState, coroutineScope) }

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalMainPagerState =
    compositionLocalOf<MainPagerState> { error("LocalMainPagerState is not provided") }
val LocalHandlePageChange =
    compositionLocalOf<(BottomBarDestination) -> Unit> {
        error("LocalHandlePageChange is not provided")
    }
val LocalNavigator = compositionLocalOf<Navigator> { error("LocalNavigator is not provided") }

/**
 * Right-pane navigator for the tablet dual-pane shell.
 *
 * Non-null only when [WindowLayoutMode.usesSplitShell] is active. Treat non-null as the single
 * source of truth for "we are in split shell" — do not re-derive shell mode from width in modules.
 */
val LocalDetailNavigator = compositionLocalOf<Navigator?> { null }

/** True when the composition is hosted inside the tablet dual-pane shell. */
inline val Navigator?.isSplitShell: Boolean
    get() = this != null

val LocalBottomBarHazeState = compositionLocalOf<HazeState?> { null }
val LocalBottomBarHazeStyle = compositionLocalOf<HazeBlurStyle?> { null }

object MainBottomBarDefaults {
    val HorizontalPadding = UiDp.dp48
    val TopPadding = UiDp.dp6
    val FloatingBottomPadding = UiDp.dp12
    val ExitOffset = UiDp.dp84
    val FloatingReservedHeight = UiDp.dp68
    val PagerAnimationSpec: AnimationSpec<Float> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Int.VisibilityThreshold.toFloat(),
        )
    val PagerNavigationAnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = 360, easing = AnimationSpecs.Legacy)
}

@Composable
fun rememberMainPagerFlingBehavior(pagerState: PagerState): TargetedFlingBehavior =
    PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = MainBottomBarDefaults.PagerAnimationSpec,
    )

@Composable
fun rememberBottomBarReservedHeight(): Dp {
    val density = LocalDensity.current
    val systemBottomInset =
        with(density) {
            max(
                WindowInsets.navigationBars.getBottom(this),
                WindowInsets.systemGestures.getBottom(this),
            )
                .toDp()
        }
    return remember(systemBottomInset) {
        MainBottomBarDefaults.FloatingReservedHeight + systemBottomInset
    }
}

private fun Modifier.bottomBarHazeEffect(state: HazeState?, style: HazeBlurStyle?): Modifier =
    chromeEffect(
        state = state,
        style = style,
        blurRadius = UiDp.dp26,
        noiseFactor = YumeHaze.ChromeNoiseFactor,
    )

@Composable
fun BottomBarContent(
    isVisible: Boolean = true,
    destinations: List<BottomBarDestination> = BottomBarDestination.entries,
) {
    FloatingBottomBarContent(isVisible = isVisible, destinations = destinations)
}

@Composable
private fun FloatingBottomBarContent(
    isVisible: Boolean = true,
    destinations: List<BottomBarDestination> = BottomBarDestination.entries,
) {
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    val mainPagerState = LocalMainPagerState.current
    val pagerState = mainPagerState.pagerState
    // Selection and indicator must share the pager's physical position. selectedPage is updated
    // optimistically before navigation starts, which previously recolored an icon while the
    // indicator was still parked on the old page.
    val page by remember(pagerState) { derivedStateOf { pagerState.currentPage } }
    val indicatorProgress by
    remember(pagerState, destinations.size) {
        derivedStateOf {
            (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction).coerceIn(
                0f,
                (destinations.size - 1).toFloat(),
            )
        }
    }
    val bottomBarVisible = isVisible && (bottomBarScrollBehavior?.isBottomBarVisible ?: true)
    val showProxyDestination = BottomBarDestination.Proxy in destinations
    var revealProxyDestination by remember { mutableStateOf(false) }
    LaunchedEffect(showProxyDestination) { revealProxyDestination = showProxyDestination }
    val density = LocalDensity.current
    val opacity = AppTheme.opacity
    val exitOffsetPx =
        remember(density) { with(density) { MainBottomBarDefaults.ExitOffset.toPx() } }
    val animatedTranslationY = remember { Animatable(if (bottomBarVisible) 0f else exitOffsetPx) }
    val animatedAlpha by
    animateFloatAsState(
        targetValue = if (bottomBarVisible) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = 180,
                easing =
                    if (bottomBarVisible) {
                        AnimationSpecs.EmphasizedDecelerate
                    } else {
                        AnimationSpecs.EmphasizedAccelerate
                    },
            ),
        label = "legacy_bottom_bar_alpha",
    )

    LaunchedEffect(bottomBarVisible, exitOffsetPx) {
        if (bottomBarVisible) {
            animatedTranslationY.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(durationMillis = 280, easing = AnimationSpecs.EmphasizedDecelerate),
            )
        } else {
            animatedTranslationY.animateTo(
                targetValue = exitOffsetPx,
                animationSpec =
                    tween(durationMillis = 220, easing = AnimationSpecs.EmphasizedAccelerate),
            )
        }
    }

    val handlePageChange = LocalHandlePageChange.current
    val onItemClick: (BottomBarDestination) -> Unit = { destination ->
        val index = destinations.indexOf(destination)
        if (index != pagerState.currentPage) {
            handlePageChange(destination)
        }
    }

    val bottomSafeInset =
        with(density) {
            val navigationBottom = WindowInsets.navigationBars.getBottom(this)
            val gestureBottom = WindowInsets.systemGestures.getBottom(this)
            max(navigationBottom, gestureBottom).toDp()
        }
    val selectedColor = MiuixTheme.colorScheme.primary
    val unselectedColor = MiuixTheme.colorScheme.onSurface.copy(alpha = opacity.secondaryText)
    val containerColor = MiuixTheme.colorScheme.background
    val indicatorContainerColor = selectedColor.copy(alpha = opacity.subtle)

    LegacyBottomNavigationBar(
        indicatorProgress = indicatorProgress,
        tabsCount = destinations.size,
        containerColor = containerColor,
        indicatorContainerColor = indicatorContainerColor,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = MainBottomBarDefaults.HorizontalPadding,
                    end = MainBottomBarDefaults.HorizontalPadding,
                    top = MainBottomBarDefaults.TopPadding,
                    bottom = bottomSafeInset + MainBottomBarDefaults.FloatingBottomPadding,
                )
                .graphicsLayer {
                    alpha = animatedAlpha
                    translationY = animatedTranslationY.value
                },
    ) { lookaheadScope ->
        destinations.forEach { destination ->
            val proxyItemAlpha by
            animateFloatAsState(
                targetValue =
                    if (destination != BottomBarDestination.Proxy || revealProxyDestination) 1f
                    else 0f,
                animationSpec =
                    tween(
                        durationMillis = ProxyDestinationRevealDurationMillis,
                        easing = AnimationSpecs.EmphasizedDecelerate,
                    ),
                label = "bottom_bar_proxy_item_alpha",
            )
            BottomBarTab(
                destination = destination,
                selected = destinations.getOrNull(page) == destination,
                enabled = bottomBarVisible,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                onClick = { onItemClick(destination) },
                modifier =
                    Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = proxyItemAlpha
                            scaleX =
                                if (destination == BottomBarDestination.Proxy)
                                    0.82f + proxyItemAlpha * 0.18f
                                else 1f
                            scaleY =
                                if (destination == BottomBarDestination.Proxy)
                                    0.82f + proxyItemAlpha * 0.18f
                                else 1f
                        }
                        .animateBounds(
                            lookaheadScope = lookaheadScope,
                            boundsTransform = { _, _ ->
                                tween(
                                    durationMillis = BottomBarLayoutAnimationDurationMillis,
                                    easing = AnimationSpecs.EmphasizedDecelerate,
                                )
                            },
                        ),
            )
        }
    }
}

@Composable
private fun BottomBarTab(
    destination: BottomBarDestination,
    selected: Boolean,
    enabled: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val itemColor = if (selected) selectedColor else unselectedColor
    LegacyBottomNavigationTabItem(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.size(UiDp.dp20), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = itemColor,
            )
        }
        BasicText(
            text = destination.label,
            style =
                TextStyle(
                    color = itemColor,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
        )
    }
}

@Composable
private fun LegacyBottomNavigationBar(
    indicatorProgress: Float,
    tabsCount: Int,
    containerColor: Color,
    indicatorContainerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(LookaheadScope) -> Unit,
) {
    val opacity = AppTheme.opacity
    val density = LocalDensity.current
    val isLightTheme = !isSystemInDarkTheme()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val surfaceWidthPx = remember { mutableIntStateOf(0) }
    val safeIndicatorProgress = indicatorProgress.coerceIn(0f, (tabsCount - 1).toFloat())
    val contentInsetPx = with(density) { (UiDp.dp4 * 2).toPx() }
    val innerWidthPx = (surfaceWidthPx.intValue - contentInsetPx).coerceAtLeast(0f)
    val tabWidthPx = if (tabsCount > 0) innerWidthPx / tabsCount else 0f
    val indicatorOffsetPx =
        if (isLtr) {
            safeIndicatorProgress * tabWidthPx
        } else {
            innerWidthPx - (safeIndicatorProgress + 1f) * tabWidthPx
        }
    val borderShadowColor =
        if (isLightTheme) {
            // White capsule on the near-white page (surface #F7F7F7) has almost no tonal
            // contrast, so the drop shadow has to carry the lift — 0.10 was too faint.
            Black.copy(alpha = opacity.softOverlay)
        } else {
            Black.copy(alpha = opacity.surfaceSoft)
        }
    val outerBorderColor =
        if (isLightTheme) {
            // A white rim on a white capsule is invisible; use a dark hairline so the capsule
            // edge reads clearly against the light page (miuix separates with hairlines, not glow).
            Black.copy(alpha = opacity.subtle)
        } else {
            Black.copy(alpha = opacity.mediumOverlay)
        }
    val innerBorderColor =
        if (isLightTheme) {
            Black.copy(alpha = opacity.ultraSubtle)
        } else {
            White.copy(alpha = opacity.verySubtle)
        }

    Box(
        modifier =
            modifier
                .onSizeChanged { surfaceWidthPx.intValue = it.width }
                .graphicsLayer {
                    shape = Capsule()
                    clip = false
                    shadowElevation = with(density) { UiDp.dp7.toPx() }
                    ambientShadowColor = borderShadowColor
                    spotShadowColor = borderShadowColor
                }
                .height(UiDp.dp56)
                .clip(Capsule())
                .background(containerColor, Capsule()),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (tabWidthPx > 0f) {
            LegacyBottomNavigationIndicator(
                modifier = Modifier
                    .padding(UiDp.dp4)
                    .align(Alignment.CenterStart),
                indicatorOffsetPx = indicatorOffsetPx,
                indicatorWidthPx = tabWidthPx,
                indicatorContainerColor = indicatorContainerColor,
            )
        }

        LookaheadScope {
            Row(
                modifier =
                    Modifier
                        .padding(UiDp.dp4)
                        .height(UiDp.dp48)
                        .fillMaxWidth()
                        .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content(this@LookaheadScope)
            }
        }

        LegacyBottomNavigationBorders(
            outerBorderColor = outerBorderColor,
            innerBorderColor = innerBorderColor,
        )
    }
}

@Composable
private fun LegacyBottomNavigationIndicator(
    modifier: Modifier = Modifier,
    indicatorOffsetPx: Float,
    indicatorWidthPx: Float,
    indicatorContainerColor: Color,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            modifier
                .offset { IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                .width(with(density) { indicatorWidthPx.toDp() })
                .height(UiDp.dp48)
                .background(indicatorContainerColor, Capsule())
    )
}

@Composable
private fun BoxScope.LegacyBottomNavigationBorders(
    outerBorderColor: Color,
    innerBorderColor: Color,
) {
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .border(width = UiDp.dp0_3, color = outerBorderColor, shape = Capsule())
    )

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .padding(UiDp.dp1)
                .border(width = UiDp.dp0_2, color = innerBorderColor, shape = Capsule())
    )
}

@Composable
private fun LegacyBottomNavigationTabItem(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(Capsule())
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .fillMaxHeight()
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp2, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

enum class BottomBarDestination(val icon: ImageVector) {
    Home(Yume.House),
    Proxy(Yume.ArrowDownUp),
    Config(Yume.PackageCheck),
    Setting(Yume.Bolt);

    val label: String
        get() =
            when (this) {
                Home -> YumeTxt.Component.BottomBar.Home
                Proxy -> YumeTxt.Component.BottomBar.Proxy
                Config -> YumeTxt.Component.BottomBar.Config
                Setting -> YumeTxt.Component.BottomBar.Setting
            }
}
