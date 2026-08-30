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

@file:Suppress("FunctionName", "UsePropertyAccessSyntax")

package com.github.yumeyucca.yumebox.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Settings2
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme.spacing
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AccessControlScreen(navigator: Navigator) {
    val density = LocalDensity.current
    val scrollBehavior = MiuixScrollBehavior()
    val spacing = spacing
    val mainLikePadding = rememberStandalonePageMainPadding()
    val viewModel = koinViewModel<AccessControlViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()

    LaunchedEffect(viewModel) { viewModel.refreshSelection() }

    var showSortMenu by remember { mutableStateOf(false) }
    var showOpsMenu by remember { mutableStateOf(false) }
    val mainListState = rememberLazyListState()
    var searchStatus by remember {
        mutableStateOf(
            SearchStatus(
                label = YumeTxt.AccessControl.Search.Placeholder,
                searchText = uiState.searchQuery,
            )
        )
    }
    val dynamicTopPadding by remember {
        derivedStateOf { spacing.space12 * (1f - scrollBehavior.state.collapsedFraction) }
    }
    val listStartPadding = spacing.screenHorizontal
    val listEndPadding = spacing.screenHorizontal
    val currentSearchStatus =
        remember(searchStatus, filteredApps) {
            searchStatus.copy(
                resultStatus =
                    when {
                        searchStatus.searchText.isBlank() -> SearchStatus.ResultStatus.DEFAULT
                        filteredApps.isEmpty() -> SearchStatus.ResultStatus.EMPTY
                        else -> SearchStatus.ResultStatus.SHOW
                    }
            )
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
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

    // Jump the main list back to the top whenever the order changes (sort mode, selected-first,
    // or re-entry refresh of initial selection). LazyColumn keeps the previous viewport anchor
    // when keyed items reorder, so without this the selected-first rows can be at the top while
    LaunchedEffect(
        uiState.sortMode,
        uiState.selectedFirst,
        uiState.initialSelectedPackages,
        uiState.isLoading,
    ) {
        if (uiState.isLoading) return@LaunchedEffect
        mainListState.scrollToItem(0)
        // LazyColumn may re-apply its key-based scroll anchor on the next layout pass.
        withFrameNanos {}
        mainListState
            .takeUnless { it.firstVisibleItemIndex == 0 && it.firstVisibleItemScrollOffset == 0 }
            ?.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                currentSearchStatus.TopAppBarAnim {
                    TopBar(
                        title = YumeTxt.AccessControl.Title,
                        scrollBehavior = scrollBehavior,
                        actions = {
                            Box {
                                IconButton(
                                    modifier = Modifier.padding(end = spacing.space12),
                                    onClick = { showSortMenu = true },
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Sort,
                                        contentDescription =
                                            YumeTxt.AccessControl.Settings.SortMode,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                                AccessControlSortMenu(
                                    show = showSortMenu,
                                    sortMode = uiState.sortMode,
                                    onDismiss = { showSortMenu = false },
                                    onSortModeChange = viewModel::onSortModeChange,
                                )
                            }
                            Box {
                                IconButton(onClick = { showOpsMenu = true }) {
                                    Icon(
                                        imageVector = Yume.Settings2,
                                        contentDescription = YumeTxt.AccessControl.Settings.Title,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                                AccessControlOperationsMenu(
                                    show = showOpsMenu,
                                    uiState = uiState,
                                    onDismiss = { showOpsMenu = false },
                                    actions =
                                        remember(viewModel) {
                                            AccessControlMenuActions(
                                                onShowSystemAppsChange =
                                                    viewModel::onShowSystemAppsChange,
                                                onSelectedFirstChange =
                                                    viewModel::onSelectedFirstChange,
                                                onSelectAll = viewModel::selectAll,
                                                onDeselectAll = viewModel::deselectAll,
                                                onInvertSelection = viewModel::invertSelection,
                                                onSelectChinaApps =
                                                    viewModel::selectChinaAppsInCurrentList,
                                                onSelectNonChinaApps =
                                                    viewModel::selectNonChinaAppsInCurrentList,
                                                onImportPackages = viewModel::importPackages,
                                                onExportPackages = viewModel::exportPackages,
                                            )
                                        },
                                )
                            }
                        },
                        bottomContent = {
                            Box(
                                modifier =
                                    Modifier
                                        .alpha(
                                            if (currentSearchStatus.isCollapsed()) 1f else 0f
                                        )
                                        .onGloballyPositioned { coordinates ->
                                            with(density) {
                                                val collapsedBarOffset =
                                                    coordinates.positionInWindow().y.toDp()
                                                if (
                                                    currentSearchStatus.offsetY !=
                                                    collapsedBarOffset
                                                ) {
                                                    searchStatus =
                                                        currentSearchStatus.copy(
                                                            offsetY = collapsedBarOffset
                                                        )
                                                }
                                            }
                                        }
                                        .then(
                                            if (currentSearchStatus.isCollapsed()) {
                                                Modifier.pointerInput(currentSearchStatus.current) {
                                                    detectTapGestures {
                                                        searchStatus =
                                                            currentSearchStatus.copy(
                                                                current =
                                                                    SearchStatus.Status.EXPANDING
                                                            )
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                            ) {
                                AccessControlCollapsedSearchBar(
                                    label = currentSearchStatus.label,
                                    topPadding = dynamicTopPadding,
                                    startPadding = listStartPadding,
                                    endPadding = listEndPadding,
                                )
                            }
                        },
                    )
                }
            }
        ) { innerPadding ->
            val combinedInnerPadding = combinePaddingValues(innerPadding, mainLikePadding)
            val listBottomPadding = combinedInnerPadding.calculateBottomPadding()

            if (uiState.isLoading) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                top = combinedInnerPadding.calculateTopPadding(),
                                start = listStartPadding,
                                end = listEndPadding,
                                bottom = listBottomPadding,
                            ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    InfiniteProgressIndicator(color = MiuixTheme.colorScheme.onSurface)
                }
            } else if (currentSearchStatus.shouldCollapse()) {
                // Render the list during COLLAPSING too (not just at the COLLAPSED end state) so it
                // is
                // already settled underneath the fading search overlay — otherwise it pops in on
                // the
                // final frame and looks like a refresh flash.
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = combinedInnerPadding,
                    lazyListState = mainListState,
                    contentPadding =
                        PaddingValues(
                            top = combinedInnerPadding.calculateTopPadding() + spacing.space6,
                            bottom = listBottomPadding,
                            start = listStartPadding,
                            end = listEndPadding,
                        ),
                ) {
                    accessControlAppItems(filteredApps, uiState, viewModel)
                }
            }
        }

        currentSearchStatus.SearchPager(
            onSearchStatusChange = { searchStatus = it },
            padding =
                SearchBarPadding(
                    top = dynamicTopPadding,
                    start = listStartPadding,
                    end = listEndPadding,
                ),
            emptyResult = {
                AccessControlSearchEmptyState(
                    text = YumeTxt.AccessControl.Search.Empty,
                    modifier = Modifier.padding(bottom = mainLikePadding.calculateBottomPadding()),
                )
            },
        ) {
            val searchListState = rememberLazyListState()
            val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

            LaunchedEffect(currentSearchStatus.searchText) {
                if (currentSearchStatus.searchText.isNotBlank()) {
                    searchListState.scrollToItem(0)
                }
            }

            LazyColumn(
                state = searchListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = listStartPadding,
                        end = listEndPadding,
                        top = spacing.space6,
                        bottom = maxOf(mainLikePadding.calculateBottomPadding(), imeBottomPadding),
                    ),
            ) {
                accessControlAppItems(filteredApps, uiState, viewModel)
            }
        }
    }
}

@Composable
private fun AccessControlCollapsedSearchBar(
    label: String,
    topPadding: androidx.compose.ui.unit.Dp,
    startPadding: androidx.compose.ui.unit.Dp,
    endPadding: androidx.compose.ui.unit.Dp,
) {
    InputField(
        query = "",
        onQueryChange = {},
        label = label,
        leadingIcon = {
            Icon(
                imageVector = MiuixIcons.Basic.Search,
                contentDescription = YumeTxt.Component.Editor.Action.Search,
                modifier =
                    Modifier
                        .size(AppTheme.sizes.searchIconTouchTarget)
                        .padding(start = spacing.space16, end = spacing.space8),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = endPadding)
                .padding(top = topPadding, bottom = AppTheme.sizes.searchBarBottomPadding),
        onSearch = {},
        enabled = false,
        expanded = false,
        onExpandedChange = {},
    )
}
