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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Settings-2`
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private object AccessControlPageSpacing {
    val contentHorizontal = 12.dp
}

private data class SearchStatus(
    val label: String,
    val searchText: String = "",
    val current: Status = Status.COLLAPSED,
    val offsetY: Dp = 0.dp,
    val resultStatus: ResultStatus = ResultStatus.DEFAULT,
) {
    fun isExpanded() = current == Status.EXPANDED
    fun isCollapsed() = current == Status.COLLAPSED
    fun shouldExpand() = current == Status.EXPANDED || current == Status.EXPANDING
    fun shouldCollapse() = current == Status.COLLAPSED || current == Status.COLLAPSING
    fun isExpanding() = current == Status.EXPANDING

    fun onAnimationComplete(): SearchStatus = when (current) {
        Status.EXPANDING -> copy(current = Status.EXPANDED)
        Status.COLLAPSING -> copy(searchText = "", current = Status.COLLAPSED)
        else -> this
    }

    enum class Status {
        EXPANDED,
        EXPANDING,
        COLLAPSED,
        COLLAPSING,
    }

    enum class ResultStatus {
        DEFAULT,
        EMPTY,
        SHOW,
    }
}

@Destination<RootGraph>
@Composable
fun AccessControlScreen(navigator: DestinationsNavigator) {
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = MiuixScrollBehavior()
    val spacing = LocalSpacing.current
    val mainLikePadding = rememberStandalonePageMainPadding()
    val combinedBottomPadding = mainLikePadding.calculateBottomPadding() + spacing.space12
    val viewModel = koinViewModel<AccessControlViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var searchStatus by remember {
        mutableStateOf(
            SearchStatus(
                label = "搜索应用 / 包名",
                searchText = uiState.searchQuery,
            ),
        )
    }
    val dynamicTopPadding by remember {
        derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) }
    }
    val listStartPadding = AccessControlPageSpacing.contentHorizontal
    val listEndPadding = AccessControlPageSpacing.contentHorizontal
    val currentSearchStatus = remember(searchStatus, filteredApps) {
        searchStatus.copy(
            resultStatus = when {
                searchStatus.searchText.isBlank() -> SearchStatus.ResultStatus.DEFAULT
                filteredApps.isEmpty() -> SearchStatus.ResultStatus.EMPTY
                else -> SearchStatus.ResultStatus.SHOW
            },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                viewModel.onPermissionResult()
            }
        },
    )

    LaunchedEffect(uiState.needsMiuiPermission) {
        if (uiState.needsMiuiPermission) {
            permissionLauncher.launch("com.android.permission.GET_INSTALLED_APPS")
        }
    }

    LaunchedEffect(searchStatus.searchText) {
        if (searchStatus.searchText != uiState.searchQuery) {
            viewModel.onSearchQueryChange(searchStatus.searchText)
        }
    }

    LaunchedEffect(uiState.searchQuery) {
        if (searchStatus.searchText != uiState.searchQuery) {
            searchStatus = searchStatus.copy(searchText = uiState.searchQuery)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                currentSearchStatus.TopAppBarAnim {
                    TopBar(
                        title = "访问控制",
                        scrollBehavior = scrollBehavior,
                        actions = {
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(
                                    Yume.`Settings-2`,
                                    contentDescription = "访问控制设置",
                                )
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            val combinedInnerPadding = combinePaddingValues(innerPadding, mainLikePadding)
            currentSearchStatus.SearchBox(
                onSearchStatusChange = { searchStatus = it },
                searchBarTopPadding = dynamicTopPadding,
                startPadding = listStartPadding,
                endPadding = listEndPadding,
                contentPadding = PaddingValues(
                    top = combinedInnerPadding.calculateTopPadding(),
                    start = combinedInnerPadding.calculateStartPadding(layoutDirection),
                    end = combinedInnerPadding.calculateEndPadding(layoutDirection),
                ),
            ) { boxHeight ->
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = combinedInnerPadding.calculateTopPadding() + boxHeight + 6.dp,
                                start = listStartPadding,
                                end = listEndPadding,
                                bottom = combinedBottomPadding,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("正在读取应用列表")
                    }
                } else {
                    ScreenLazyColumn(
                        scrollBehavior = scrollBehavior,
                        innerPadding = combinedInnerPadding,
                        contentPadding = PaddingValues(
                            top = combinedInnerPadding.calculateTopPadding() + boxHeight + 6.dp,
                            bottom = combinedBottomPadding,
                            start = listStartPadding,
                            end = listEndPadding,
                        ),
                    ) {
                        item {
                            Title("应用列表 (${uiState.selectedPackages.size})")
                        }

                        items(
                            items = filteredApps,
                            key = { it.packageName },
                        ) { app ->
                            AppCard(
                                app = app,
                                selected = app.packageName in uiState.selectedPackages,
                                onSelectionChange = { checked ->
                                    viewModel.onAppSelectionChange(app.packageName, checked)
                                },
                                onClick = {
                                    viewModel.onAppSelectionChange(
                                        app.packageName,
                                        app.packageName !in uiState.selectedPackages,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            AccessControlSettingsSheet(
                show = showSettingsSheet,
                uiState = uiState,
                onDismiss = { showSettingsSheet = false },
                onShowSystemAppsChange = viewModel::onShowSystemAppsChange,
                onSelectedFirstChange = viewModel::onSelectedFirstChange,
                onSortModeChange = viewModel::onSortModeChange,
                onSelectAll = viewModel::selectAll,
                onDeselectAll = viewModel::deselectAll,
                onInvertSelection = viewModel::invertSelection,
                onSelectChinaApps = viewModel::selectChinaAppsInCurrentList,
                onSelectNonChinaApps = viewModel::selectNonChinaAppsInCurrentList,
                onImportPackages = viewModel::importPackages,
                onExportPackages = viewModel::exportPackages,
            )
        }

        currentSearchStatus.SearchPager(
            onSearchStatusChange = { searchStatus = it },
            searchBarTopPadding = dynamicTopPadding,
            startPadding = listStartPadding,
            endPadding = listEndPadding,
            emptyResult = {
                SearchEmptyState(
                    text = "没有匹配到应用",
                    modifier = Modifier.padding(bottom = combinedBottomPadding),
                )
            },
        ) {
            val searchListState = rememberLazyListState()
            LaunchedEffect(currentSearchStatus.searchText) {
                if (currentSearchStatus.searchText.isNotBlank()) {
                    searchListState.scrollToItem(0)
                }
            }
            LazyColumn(
                state = searchListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = listStartPadding,
                    end = listEndPadding,
                    top = 6.dp,
                    bottom = combinedBottomPadding,
                ),
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName },
                ) { app ->
                    AppCard(
                        app = app,
                        selected = app.packageName in uiState.selectedPackages,
                        onSelectionChange = { checked ->
                            viewModel.onAppSelectionChange(app.packageName, checked)
                        },
                        onClick = {
                            viewModel.onAppSelectionChange(
                                app.packageName,
                                app.packageName !in uiState.selectedPackages,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessControlSettingsSheet(
    show: Boolean,
    uiState: AccessControlViewModel.UiState,
    onDismiss: () -> Unit,
    onShowSystemAppsChange: (Boolean) -> Unit,
    onSelectedFirstChange: (Boolean) -> Unit,
    onSortModeChange: (AccessControlViewModel.SortMode) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSelectChinaApps: () -> Int,
    onSelectNonChinaApps: () -> Int,
    onImportPackages: (String) -> Int,
    onExportPackages: () -> String,
) {
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val sortModeEntries = remember { AccessControlViewModel.SortMode.entries }

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = "访问控制设置",
        onDismissRequest = onDismiss,
        enableNestedScroll = true,
    ) {
        Column {
            top.yukonga.miuix.kmp.basic.Card {
                SwitchPreference(
                    title = "显示系统应用",
                    checked = uiState.showSystemApps,
                    onCheckedChange = onShowSystemAppsChange,
                )
                SwitchPreference(
                    title = "已选应用排前",
                    checked = uiState.selectedFirst,
                    onCheckedChange = onSelectedFirstChange,
                )
                WindowDropdownPreference(
                    title = "排序方式",
                    summary = "当前: ${uiState.sortMode.displayName}",
                    items = sortModeEntries.map { it.displayName },
                    selectedIndex = sortModeEntries.indexOf(uiState.sortMode).coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        sortModeEntries.getOrNull(index)?.let(onSortModeChange)
                    },
                )
                WindowDropdownPreference(
                    title = "批量操作",
                    items = listOf("全选", "取消选择", "反选"),
                    selectedIndex = 0,
                    onSelectedIndexChange = { index ->
                        when (index) {
                            0 -> onSelectAll()
                            1 -> onDeselectAll()
                            2 -> onInvertSelection()
                        }
                    },
                )
                WindowDropdownPreference(
                    title = "区域快速选择",
                    items = listOf("中国应用", "海外应用"),
                    selectedIndex = 0,
                    onSelectedIndexChange = { index ->
                        val (label, count) = when (index) {
                            0 -> "中国应用" to onSelectChinaApps()
                            1 -> "海外应用" to onSelectNonChinaApps()
                            else -> "" to 0
                        }
                        if (label.isNotEmpty()) {
                            context.toast("已选择 $label: $count 个")
                        }
                    },
                )
                WindowDropdownPreference(
                    title = "导入 / 导出",
                    items = listOf("从剪贴板导入", "导出到剪贴板"),
                    selectedIndex = 0,
                    onSelectedIndexChange = { index ->
                        when (index) {
                            0 -> {
                                val text = clipboardManager.primaryClip
                                    ?.takeIf { it.itemCount > 0 }
                                    ?.getItemAt(0)
                                    ?.text
                                    ?.toString()
                                    .orEmpty()
                                if (text.isNotEmpty()) {
                                    val count = onImportPackages(text)
                                    context.toast("已导入 $count 个应用")
                                } else {
                                    context.toast("剪贴板里没有可导入的包名")
                                }
                            }

                            1 -> {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("packages", onExportPackages()),
                                )
                                context.toast("已导出 ${uiState.selectedPackages.size} 个应用")
                            }
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("关闭")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    "完成",
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    app: AccessControlViewModel.AppInfo,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        applyHorizontalPadding = false,
    ) {
        BasicComponent(
            title = app.label,
            summary = app.packageName,
            startAction = {
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = app.label,
                    imageSize = 45.dp,
                    bitmapSize = 80,
                    modifier = Modifier.padding(end = 12.dp),
                )
            },
            endActions = {
                Checkbox(
                    state = ToggleableState(selected),
                    onClick = { onSelectionChange(!selected) },
                )
            },
            onClick = onClick,
        )
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    contentDescription: String,
    imageSize: Dp,
    bitmapSize: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = packageName, key2 = bitmapSize) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = bitmapSize, height = bitmapSize)
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    val bitmap = iconBitmap ?: return
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier.size(imageSize),
    )
}

@Composable
private fun SearchEmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun SearchStatus.TopAppBarAnim(
    modifier: Modifier = Modifier,
    visible: Boolean = shouldCollapse(),
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 550 else 0, easing = FastOutSlowInEasing),
        label = "SearchTopBarAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
            content()
        }
    }
}

@Composable
private fun SearchStatus.SearchBox(
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (Dp) -> Unit,
) {
    val searchStatus = this
    val density = LocalDensity.current
    var boxHeight by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
            .padding(top = contentPadding.calculateTopPadding())
            .onGloballyPositioned { coordinates ->
                with(density) {
                    offsetY = coordinates.positionInWindow().y.toDp()
                    boxHeight = coordinates.size.height.toDp()
                }
                if (searchStatus.offsetY != offsetY) {
                    onSearchStatusChange(searchStatus.copy(offsetY = offsetY))
                }
            }
            .pointerInput(searchStatus.current) {
                detectTapGestures {
                    onSearchStatusChange(searchStatus.copy(current = SearchStatus.Status.EXPANDING))
                }
            }
            .background(MiuixTheme.colorScheme.surface),
    ) {
        SearchBarCollapsed(
            label = searchStatus.label,
            searchBarTopPadding = searchBarTopPadding,
            startPadding = startPadding,
            endPadding = endPadding,
            innerPadding = contentPadding,
        )
    }

    AnimatedVisibility(
        visible = searchStatus.shouldCollapse(),
        enter = fadeIn(tween(300, easing = LinearOutSlowInEasing)) +
            slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { -with(density) { offsetY.roundToPx() } },
        exit = fadeOut(tween(300, easing = LinearOutSlowInEasing)) +
            slideOutVertically(tween(300, easing = LinearOutSlowInEasing)) { -with(density) { offsetY.roundToPx() } },
    ) {
        content(boxHeight)
    }
}

@Composable
private fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    defaultResult: @Composable () -> Unit = {},
    emptyResult: @Composable () -> Unit = {},
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    result: @Composable () -> Unit,
) {
    val searchStatus = this
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topPadding by animateDpAsState(
        targetValue = if (searchStatus.shouldExpand()) {
            systemBarsPadding + 5.dp
        } else {
            max(searchStatus.offsetY, 0.dp)
        },
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchTopPadding",
        finishedListener = {
            onSearchStatusChange(searchStatus.onAnimationComplete())
        },
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "SearchSurfaceAlpha",
    )

    BackHandler(enabled = !searchStatus.isCollapsed()) {
        onSearchStatusChange(
            searchStatus.copy(
                searchText = "",
                current = SearchStatus.Status.COLLAPSING,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .background(MiuixTheme.colorScheme.surface.copy(alpha = surfaceAlpha))
            .then(
                if (!searchStatus.isCollapsed()) {
                    Modifier.pointerInput(searchStatus.current) {}
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .then(
                    if (!searchStatus.isCollapsed()) {
                        Modifier.background(MiuixTheme.colorScheme.surface)
                    } else {
                        Modifier
                    },
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!searchStatus.isCollapsed()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MiuixTheme.colorScheme.surface),
                ) {
                    SearchBar(
                        searchStatus = searchStatus,
                        onSearchStatusChange = onSearchStatusChange,
                        searchBarTopPadding = searchBarTopPadding,
                        startPadding = startPadding,
                        endPadding = endPadding,
                    )
                }
            }

            AnimatedVisibility(
                visible = searchStatus.isExpanded() || searchStatus.isExpanding(),
                enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
            ) {
                Text(
                    text = "取消",
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 16.dp, top = searchBarTopPadding, bottom = 6.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = searchStatus.isExpanded(),
                        ) {
                            onSearchStatusChange(
                                searchStatus.copy(
                                    searchText = "",
                                    current = SearchStatus.Status.COLLAPSING,
                                ),
                            )
                        },
                )
            }
        }

        AnimatedVisibility(
            visible = searchStatus.isExpanded(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            when (searchStatus.resultStatus) {
                SearchStatus.ResultStatus.DEFAULT -> defaultResult()
                SearchStatus.ResultStatus.EMPTY -> emptyResult()
                SearchStatus.ResultStatus.SHOW -> result()
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchStatus.searchText)) }

    LaunchedEffect(searchStatus.searchText) {
        if (textFieldValue.text != searchStatus.searchText) {
            textFieldValue = TextFieldValue(searchStatus.searchText)
        }
    }

    LaunchedEffect(searchStatus.current) {
        if (searchStatus.isExpanding()) {
            focusRequester.requestFocus()
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onSearchStatusChange(searchStatus.copy(searchText = it.text))
        },
        singleLine = true,
        textStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = MiuixTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .padding(top = searchBarTopPadding, bottom = 6.dp)
            .heightIn(min = 45.dp)
            .background(MiuixTheme.colorScheme.secondaryContainer, CircleShape)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Basic.Search,
                    contentDescription = "搜索",
                    modifier = Modifier
                        .size(44.dp)
                        .padding(start = 16.dp, end = 8.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = searchStatus.label,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    innerTextField()
                }
                AnimatedVisibility(
                    visible = searchStatus.searchText.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Basic.SearchCleanup,
                        contentDescription = "清空",
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(44.dp)
                            .padding(start = 8.dp, end = 16.dp)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                            ) {
                                textFieldValue = TextFieldValue("")
                                onSearchStatusChange(searchStatus.copy(searchText = ""))
                            },
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchBarCollapsed(
    label: String,
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layoutDirection = LocalLayoutDirection.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            )
            .padding(top = searchBarTopPadding, bottom = 6.dp)
            .height(45.dp)
            .background(MiuixTheme.colorScheme.secondaryContainer, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MiuixIcons.Basic.Search,
            contentDescription = "搜索",
            modifier = Modifier
                .size(44.dp)
                .padding(start = 16.dp, end = 8.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
