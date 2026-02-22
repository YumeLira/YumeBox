package com.github.yumelira.yumebox.presentation.screen.node

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.Speed
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.hazeSource
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.util.lerp as lerpFloat

private const val NODE_TRANSFORM_DURATION_MS = 520
private const val NODE_CONTENT_REVEAL_THRESHOLD = 0.999f

@Destination<RootGraph>
@Composable
fun ProxyNodeScreen(
    navigator: DestinationsNavigator,
    initialGroupName: String,
    startLeft: Float = NODE_BOUNDS_UNSET,
    startTop: Float = NODE_BOUNDS_UNSET,
    startRight: Float = NODE_BOUNDS_UNSET,
    startBottom: Float = NODE_BOUNDS_UNSET,
) {
    val startBounds = remember(startLeft, startTop, startRight, startBottom) {
        nodeRectFromArgs(startLeft, startTop, startRight, startBottom)
    }

    ProxyNodePage(
        initialGroupName = initialGroupName,
        startBounds = startBounds,
        onBackConfirmed = { navigator.popBackStack() },
    )
}

@Composable
fun ProxyNodePage(
    initialGroupName: String,
    startBounds: Rect?,
    onBackConfirmed: () -> Unit,
    proxyViewModel: ProxyViewModel = koinViewModel(),
) {
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()

    val scrollBehavior = MiuixScrollBehavior()
    val topBarHazeState = LocalTopBarHazeState.current
    val coroutineScope = rememberCoroutineScope()
    val showSortPopup = rememberSaveable { mutableStateOf(false) }
    var hostBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    val rootView = LocalView.current
    var startRectInHost by remember(startBounds) { mutableStateOf<Rect?>(null) }

    val pagerState = rememberPagerState(pageCount = { proxyGroups.size.coerceAtLeast(1) })
    var initialPageApplied by rememberSaveable { mutableStateOf(false) }

    val selectedIndex by remember(proxyGroups, pagerState.currentPage, pagerState.targetPage, pagerState.isScrollInProgress) {
        derivedStateOf {
            if (proxyGroups.isEmpty()) return@derivedStateOf 0
            val activePage = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
            activePage.coerceIn(0, proxyGroups.lastIndex)
        }
    }
    val selectedGroup by remember(proxyGroups, selectedIndex) {
        derivedStateOf { proxyGroups.getOrNull(selectedIndex) }
    }
    var started by rememberSaveable { mutableStateOf(false) }
    var closing by rememberSaveable { mutableStateOf(false) }
    var backDispatched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val progress by animateFloatAsState(
        targetValue = if (closing) 0f else if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = NODE_TRANSFORM_DURATION_MS,
            easing = AnimationSpecs.EmphasizedDecelerate,
        ),
        label = "NodeContainerTransform",
    )
    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        if (!backDispatched) {
            withFrameNanos { }
            backDispatched = true
            onBackConfirmed()
        }
    }

    val requestClose = {
        if (!closing) {
            closing = true
        }
    }

    BackHandler(enabled = !backDispatched) {
        requestClose()
    }

    LaunchedEffect(proxyGroups) {
        if (proxyGroups.isEmpty()) return@LaunchedEffect

        if (!initialPageApplied) {
            val initialIndex = proxyGroups.indexOfFirst { it.name == initialGroupName }.coerceAtLeast(0)
            pagerState.scrollToPage(initialIndex.coerceIn(0, (proxyGroups.size - 1).coerceAtLeast(0)))
            initialPageApplied = true
            return@LaunchedEffect
        }

        if (pagerState.currentPage >= proxyGroups.size) {
            pagerState.scrollToPage((proxyGroups.size - 1).coerceAtLeast(0))
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                hostBoundsInWindow = coordinates.boundsInWindow()
            },
    ) {
        val density = LocalDensity.current
        val fullRect = Rect(0f, 0f, with(density) { maxWidth.toPx() }, with(density) { maxHeight.toPx() })
        val hostBounds = hostBoundsInWindow
        val rootWindowLocation = IntArray(2).also(rootView::getLocationInWindow)
        val hostLeft = hostBounds?.left ?: rootWindowLocation[0].toFloat()
        val hostTop = hostBounds?.top ?: rootWindowLocation[1].toFloat()
        LaunchedEffect(startBounds, hostLeft, hostTop) {
            if (startBounds == null) {
                startRectInHost = null
            } else if (startRectInHost == null) {
                startRectInHost = Rect(
                    left = startBounds.left - hostLeft,
                    top = startBounds.top - hostTop,
                    right = startBounds.right - hostLeft,
                    bottom = startBounds.bottom - hostTop,
                )
            }
        }
        val from = startRectInHost ?: fullRect

        val leftPx = lerpFloat(from.left, fullRect.left, progress)
        val topPx = lerpFloat(from.top, fullRect.top, progress)
        val widthPx = lerpFloat(from.width, fullRect.width, progress)
        val heightPx = lerpFloat(from.height, fullRect.height, progress)
        val cornerRadius = lerp(if (startBounds != null) 18.dp else 0.dp, 0.dp, progress)
        val hasMorph = startBounds != null
        val showPageContent = !hasMorph || (!closing && progress >= NODE_CONTENT_REVEAL_THRESHOLD)
        val showMorphLayer = hasMorph && (
            (!closing && progress < NODE_CONTENT_REVEAL_THRESHOLD) ||
                (closing && progress > 0f)
            )

        Box(modifier = Modifier.fillMaxSize()) {
            if (showPageContent) {
                CompositionLocalProvider(
                    LocalTopBarHazeState provides null,
                    LocalTopBarHazeStyle provides null,
                ) {
                    Scaffold(
                        topBar = {
                            TopBar(
                                title = selectedGroup?.name ?: MLang.Proxy.Title,
                                scrollBehavior = scrollBehavior,
                                navigationIcon = {
                                    IconButton(
                                        modifier = Modifier.padding(start = 24.dp),
                                        onClick = requestClose,
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Back,
                                            contentDescription = MLang.Component.Navigation.Back,
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        modifier = Modifier.padding(end = 16.dp),
                                        onClick = {
                                            selectedGroup?.let { proxyViewModel.testDelay(it.name) }
                                        },
                                    ) {
                                        Icon(
                                            Yume.Speed,
                                            contentDescription = MLang.Proxy.Action.Test,
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier.padding(end = 24.dp),
                                        onClick = { showSortPopup.value = true },
                                    ) {
                                        Icon(
                                            Yume.`List-chevrons-up-down`,
                                            contentDescription = MLang.Proxy.Settings.SortMode,
                                        )
                                    }
                                    NodeSortPopup(
                                        show = showSortPopup,
                                        onDismiss = { showSortPopup.value = false },
                                        sortMode = sortMode,
                                        onSortSelected = proxyViewModel::setSortMode,
                                    )

                                },
                            )
                        },
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                NodeTabs(
                                    groups = proxyGroups,
                                    selectedIndex = selectedIndex,
                                    onSelect = { index ->
                                        if (index in proxyGroups.indices) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                    },
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                                        .let { mod ->
                                            if (topBarHazeState != null) mod.hazeSource(state = topBarHazeState) else mod
                                        },
                                ) {
                                    if (proxyGroups.isEmpty()) {
                                        CenteredText(
                                            firstLine = MLang.Proxy.Empty.NoNodes,
                                            secondLine = MLang.Proxy.Empty.Hint,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize(),
                                        ) { page ->
                                            val group = proxyGroups.getOrNull(page) ?: return@HorizontalPager
                                            NodeList(
                                                group = group,
                                                displayMode = displayMode,
                                                onProxyClick = { proxyName ->
                                                    if (group.type == Proxy.Type.Selector) {
                                                        proxyViewModel.selectProxy(group.name, proxyName)
                                                    } else {
                                                        proxyViewModel.testDelay(group.name)
                                                    }
                                                },
                                                isDelayTesting = testingGroupNames.contains(group.name),
                                                onTestDelay = { proxyViewModel.testDelay(group.name) },
                                                listStateKeyPrefix = "node_page",
                                                contentPadding = PaddingValues(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    top = 0.dp,
                                                    bottom = 92.dp,
                                                ),
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                            }

                            FloatingActionButton(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 20.dp, bottom = 20.dp),
                                onClick = {
                                    selectedGroup?.let { proxyViewModel.testDelay(it.name) }
                                },
                            ) {
                                Icon(
                                    Yume.Speed,
                                    contentDescription = MLang.Proxy.Action.Test,
                                    tint = MiuixTheme.colorScheme.surface,
                                )
                            }
                        }
                    }
                }
            }

            if (showMorphLayer) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { leftPx.toDp() },
                            y = with(density) { topPx.toDp() },
                        )
                        .size(
                            width = with(density) { widthPx.toDp() },
                            height = with(density) { heightPx.toDp() },
                        )
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(MiuixTheme.colorScheme.background),
                )
            }
        }
    }
}
