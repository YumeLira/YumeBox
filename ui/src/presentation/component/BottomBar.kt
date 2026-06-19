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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Arrow-down-up`
import com.github.yumelira.yumebox.presentation.icon.yume.Bolt
import com.github.yumelira.yumebox.presentation.icon.yume.House
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.kyant.shapes.Capsule
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.*
import dev.oom_wg.purejoy.mlang.MLang
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.shapes.SmoothUnevenRoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainPagerState(val pagerState: PagerState, private val coroutineScope: CoroutineScope) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages =
            targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = MainBottomBarDefaults.PagerAnimationSpec,
                )
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
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
): MainPagerState {
    return remember(pagerState, coroutineScope) { MainPagerState(pagerState, coroutineScope) }
}

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalMainPagerState =
    compositionLocalOf<MainPagerState> { error("LocalMainPagerState is not provided") }
val LocalHandlePageChange =
    compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator =
    compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }
val LocalBottomBarHazeState = compositionLocalOf<HazeState?> { null }
val LocalBottomBarHazeStyle = compositionLocalOf<HazeStyle?> { null }
val LocalBottomBarUseLegacyStyle = compositionLocalOf { false }

object MainBottomBarDefaults {
    val CornerRadius = UiDp.dp28
    val Shape = SmoothUnevenRoundedCornerShape(topStart = CornerRadius, topEnd = CornerRadius)
    val BorderWidth = UiDp.dp0_26
    val OutlineHorizontalInset = UiDp.dp0
    val ItemHeight = UiDp.dp60
    val IconSize = UiDp.dp26
    val LabelFontSize = 11.5.sp
    val IconLabelSpacing = UiDp.dp3
    val HorizontalPadding = UiDp.dp48
    val TopPadding = UiDp.dp6
    val FloatingBottomPadding = UiDp.dp12
    val EnterOffset = UiDp.dp68
    val ExitOffset = UiDp.dp84
    val ModernReservedHeight = UiDp.dp64
    val LegacyReservedHeight = UiDp.dp68
    val PagerAnimationSpec: AnimationSpec<Float> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Int.VisibilityThreshold.toFloat(),
        )
}

@Composable
fun rememberMainPagerFlingBehavior(pagerState: PagerState): TargetedFlingBehavior {
    return PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = MainBottomBarDefaults.PagerAnimationSpec,
    )
}

@Composable
fun rememberBottomBarReservedHeight(
    useLegacyStyle: Boolean = LocalBottomBarUseLegacyStyle.current
): Dp {
    val density = LocalDensity.current
    val systemBottomInset =
        with(density) {
            max(
                    WindowInsets.navigationBars.getBottom(this),
                    WindowInsets.systemGestures.getBottom(this),
                )
                .toDp()
        }
    return remember(systemBottomInset, useLegacyStyle) {
        if (useLegacyStyle) {
            MainBottomBarDefaults.LegacyReservedHeight + systemBottomInset
        } else {
            MainBottomBarDefaults.ModernReservedHeight
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
private fun Modifier.bottomBarHazeEffect(state: HazeState?, style: HazeStyle?): Modifier {
    if (state == null || style == null) return this

    return hazeEffect(state) {
        this.style = style
        blurRadius = UiDp.dp26
        inputScale = HazeInputScale.Fixed(0.24f)
        noiseFactor = 0f
        forceInvalidateOnPreDraw = false
    }
}

private fun Modifier.bottomBarOutline(
    shape: Shape,
    color: Color,
    edgeFadeAlpha: Float,
    middleFadeAlpha: Float,
): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
        val strokeWidth = max(MainBottomBarDefaults.BorderWidth.toPx(), 1f)
        val outline = shape.createOutline(size, layoutDirection, this)
        val topBorderHeight = MainBottomBarDefaults.CornerRadius.toPx() + strokeWidth * 2f
        val fadeMaskBrush =
            Brush.horizontalGradient(
                colorStops =
                    arrayOf(
                        0f to Color.Transparent,
                        0.01f to Black.copy(alpha = edgeFadeAlpha),
                        0.025f to Black.copy(alpha = middleFadeAlpha),
                        0.045f to Black,
                        0.955f to Black,
                        0.975f to Black.copy(alpha = middleFadeAlpha),
                        0.99f to Black.copy(alpha = edgeFadeAlpha),
                        1f to Color.Transparent,
                    )
            )
        val layerBounds = Rect(0f, 0f, size.width, size.height)
        val layerPaint = Paint()

        onDrawWithContent {
            drawContent()
            drawIntoCanvas { canvas -> canvas.saveLayer(layerBounds, layerPaint) }
            clipRect(bottom = topBorderHeight) {
                drawOutline(outline = outline, color = color, style = Stroke(width = strokeWidth))
            }
            drawIntoCanvas { canvas ->
                drawRect(brush = fadeMaskBrush, size = size, blendMode = BlendMode.DstIn)
                canvas.restore()
            }
        }
    }

@Composable
fun BottomBarContent(isVisible: Boolean = true, useLegacyStyle: Boolean = false) {
    if (useLegacyStyle) {
        LegacyBottomBarContent(isVisible = isVisible)
    } else {
        ModernBottomBarContent(isVisible = isVisible)
    }
}

@Composable
private fun ModernBottomBarContent(isVisible: Boolean = true) {
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    val mainPagerState = LocalMainPagerState.current
    val hazeState = LocalBottomBarHazeState.current
    val hazeStyle = LocalBottomBarHazeStyle.current
    val page by remember(mainPagerState) { derivedStateOf { mainPagerState.selectedPage } }
    val bottomBarVisible = isVisible && (bottomBarScrollBehavior?.isBottomBarVisible ?: true)
    val hazeEnabled = hazeState != null && hazeStyle != null
    val opacity = AppTheme.opacity
    val colorScheme = MiuixTheme.colorScheme
    val isDarkSurface = colorScheme.background.luminance() < 0.5f
    val outlineColor =
        if (isDarkSurface) {
            White.copy(alpha = opacity.brightOutline)
        } else {
            Black.copy(alpha = opacity.mutedStrong)
        }
    val selectedColor = colorScheme.primary
    val unselectedColor = colorScheme.onSurface.copy(alpha = opacity.secondaryText)
    val barSurfaceColor = colorScheme.background
    val barSurfaceAlpha =
        if (hazeEnabled) {
            opacity.elevatedSurface
        } else {
            1f
        }

    val handlePageChange = LocalHandlePageChange.current
    val onItemClick: (Int) -> Unit = { index ->
        if (index != mainPagerState.selectedPage) {
            handlePageChange(index)
        }
    }

    AnimatedVisibility(
        visible = bottomBarVisible,
        enter =
            fadeIn(
                animationSpec =
                    tween(durationMillis = 240, easing = AnimationSpecs.EmphasizedDecelerate)
            ) +
                slideInVertically(
                    animationSpec =
                        tween(durationMillis = 240, easing = AnimationSpecs.EmphasizedDecelerate),
                    initialOffsetY = { fullHeight -> (fullHeight * 0.92f).toInt() },
                ),
        exit =
            fadeOut(
                animationSpec =
                    tween(durationMillis = 180, easing = AnimationSpecs.EmphasizedAccelerate)
            ) +
                slideOutVertically(
                    animationSpec =
                        tween(durationMillis = 220, easing = AnimationSpecs.EmphasizedAccelerate),
                    targetOffsetY = { fullHeight -> (fullHeight * 1.08f).toInt() },
                ),
    ) {
        val barShape = MainBottomBarDefaults.Shape
        val surfaceModifier =
            Modifier.fillMaxWidth()
                .clip(barShape)
                .then(
                    if (hazeEnabled) {
                        Modifier.bottomBarHazeEffect(hazeState, hazeStyle)
                    } else {
                        Modifier
                    }
                )
                .background(barSurfaceColor.copy(alpha = barSurfaceAlpha))
                .bottomBarOutline(
                    shape = barShape,
                    color = outlineColor,
                    edgeFadeAlpha = opacity.lightOverlay,
                    middleFadeAlpha = opacity.accent,
                )

        BottomBarLayout(modifier = surfaceModifier) {
            BottomBarDestination.entries.forEachIndexed { index, destination ->
                BottomBarItem(
                    selected = page == index,
                    onClick = { onItemClick(index) },
                    icon = destination.icon,
                    label = destination.label,
                    enabled = bottomBarVisible,
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                )
            }
        }
    }
}

@Composable
private fun BottomBarLayout(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val captionBarBottomPadding =
        WindowInsets.captionBar
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MainBottomBarDefaults.ItemHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )

        val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
        Spacer(
            modifier =
                Modifier.fillMaxWidth()
                    .height(
                        navigationBarsPadding.calculateBottomPadding() + captionBarBottomPadding
                    )
                    .pointerInput(Unit) { detectTapGestures {} }
        )
    }
}

@Composable
private fun RowScope.BottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
) {
    val opacity = AppTheme.opacity
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tint =
        when {
            isPressed ->
                if (selected) {
                    selectedColor.copy(alpha = opacity.medium)
                } else {
                    unselectedColor.copy(alpha = opacity.secondaryText)
                }

            selected -> selectedColor
            else -> unselectedColor
        }

    Column(
        modifier =
            Modifier.weight(1f)
                .fillMaxHeight()
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) {
                    this.selected = selected
                    this.role = Role.Tab
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(MainBottomBarDefaults.IconSize),
        )
        Spacer(modifier = Modifier.height(MainBottomBarDefaults.IconLabelSpacing))
        BasicText(
            text = label,
            style =
                TextStyle(
                    color = tint,
                    fontSize = MainBottomBarDefaults.LabelFontSize,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

@Composable
private fun LegacyBottomBarContent(isVisible: Boolean = true) {
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    val mainPagerState = LocalMainPagerState.current
    val pagerState = mainPagerState.pagerState
    val page by remember(mainPagerState) { derivedStateOf { mainPagerState.selectedPage } }
    val indicatorProgress by
        remember(pagerState) {
            derivedStateOf {
                (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction).coerceIn(
                    0f,
                    (BottomBarDestination.entries.size - 1).toFloat(),
                )
            }
        }
    val bottomBarVisible = isVisible && (bottomBarScrollBehavior?.isBottomBarVisible ?: true)
    val density = LocalDensity.current
    val opacity = AppTheme.opacity
    val enterOffsetPx =
        remember(density) { with(density) { MainBottomBarDefaults.EnterOffset.toPx() } }
    val exitOffsetPx =
        remember(density) { with(density) { MainBottomBarDefaults.ExitOffset.toPx() } }
    val animatedTranslationY = remember { Animatable(if (bottomBarVisible) 0f else exitOffsetPx) }
    val animatedScale by
        animateFloatAsState(
            targetValue = if (bottomBarVisible) 1f else 0.98f,
            animationSpec =
                tween(
                    durationMillis = 240,
                    easing =
                        if (bottomBarVisible) {
                            AnimationSpecs.EmphasizedDecelerate
                        } else {
                            AnimationSpecs.EmphasizedAccelerate
                        },
                ),
            label = "legacy_bottom_bar_scale",
        )
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

    LaunchedEffect(bottomBarVisible, enterOffsetPx, exitOffsetPx) {
        if (bottomBarVisible) {
            animatedTranslationY.snapTo(enterOffsetPx)
            animatedTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
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
    val onItemClick: (Int) -> Unit = { index ->
        if (index != mainPagerState.selectedPage) {
            handlePageChange(index)
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
        selectedIndex = page,
        indicatorProgress = indicatorProgress,
        tabsCount = BottomBarDestination.entries.size,
        containerColor = containerColor,
        indicatorContainerColor = indicatorContainerColor,
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = MainBottomBarDefaults.HorizontalPadding,
                    end = MainBottomBarDefaults.HorizontalPadding,
                    top = MainBottomBarDefaults.TopPadding,
                    bottom = bottomSafeInset + MainBottomBarDefaults.FloatingBottomPadding,
                )
                .graphicsLayer {
                    alpha = animatedAlpha
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationY = animatedTranslationY.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
    ) {
        BottomBarDestination.entries.forEachIndexed { index, destination ->
            val itemColor = if (page == index) selectedColor else unselectedColor
            LegacyBottomNavigationTabItem(
                enabled = bottomBarVisible,
                onClick = { onItemClick(index) },
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
                            fontWeight =
                                if (page == index) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                )
            }
        }
    }
}

@Composable
private fun LegacyBottomNavigationBar(
    selectedIndex: Int,
    indicatorProgress: Float,
    tabsCount: Int,
    containerColor: Color,
    indicatorContainerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val opacity = AppTheme.opacity
    val density = LocalDensity.current
    val isLightTheme = !isSystemInDarkTheme()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val surfaceWidthPx = remember { mutableIntStateOf(0) }
    val safeSelectedIndex = selectedIndex.coerceIn(0, tabsCount - 1)
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
    val indicatorScale = remember { Animatable(1f) }
    val borderShadowColor =
        if (isLightTheme) {
            Black.copy(alpha = opacity.subtle)
        } else {
            Black.copy(alpha = opacity.surfaceSoft)
        }
    val outerBorderColor =
        if (isLightTheme) {
            White.copy(alpha = opacity.disabledSecondary)
        } else {
            Black.copy(alpha = opacity.mediumOverlay)
        }
    val innerBorderColor =
        if (isLightTheme) {
            Black.copy(alpha = opacity.ultraSubtle)
        } else {
            White.copy(alpha = opacity.verySubtle)
        }

    LaunchedEffect(safeSelectedIndex) {
        launch {
            indicatorScale.animateTo(0.9f, tween(120, easing = FastOutSlowInEasing))
            indicatorScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
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
                modifier = Modifier.padding(UiDp.dp4).align(Alignment.CenterStart),
                indicatorOffsetPx = indicatorOffsetPx,
                indicatorWidthPx = tabWidthPx,
                indicatorScale = indicatorScale.value,
                indicatorContainerColor = indicatorContainerColor,
            )
        }

        Row(
            modifier =
                Modifier.padding(UiDp.dp4)
                    .height(UiDp.dp48)
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )

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
    indicatorScale: Float,
    indicatorContainerColor: Color,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            modifier
                .offset { IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                .width(with(density) { indicatorWidthPx.toDp() })
                .height(UiDp.dp48)
                .graphicsLayer {
                    scaleX = indicatorScale
                    scaleY = indicatorScale
                }
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
            Modifier.matchParentSize()
                .border(width = UiDp.dp0_3, color = outerBorderColor, shape = Capsule())
    )

    Box(
        modifier =
            Modifier.matchParentSize()
                .padding(UiDp.dp1)
                .border(width = UiDp.dp0_2, color = innerBorderColor, shape = Capsule())
    )
}

@Composable
private fun RowScope.LegacyBottomNavigationTabItem(
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
                .weight(1f),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp2, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

enum class BottomBarDestination(val icon: ImageVector) {
    Home(Yume.House),
    Proxy(Yume.`Arrow-down-up`),
    Config(Yume.`Package-check`),
    Setting(Yume.Bolt);

    val label: String
        get() =
            when (this) {
                Home -> MLang.Component.BottomBar.Home
                Proxy -> MLang.Component.BottomBar.Proxy
                Config -> MLang.Component.BottomBar.Config
                Setting -> MLang.Component.BottomBar.Setting
            }
}
