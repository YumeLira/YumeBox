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

package com.github.yumelira.yumebox.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.state.ToggleableState
import androidx.core.graphics.drawable.toBitmap
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.OverrideAnimatedFab
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SearchPager
import com.github.yumelira.yumebox.presentation.component.SearchStatus
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopAppBarAnim
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberOverrideFabController
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Settings-2`
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.AppTheme.spacing
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AccessControlScreen(navigator: DestinationsNavigator) {
    val density = LocalDensity.current
    val scrollBehavior = MiuixScrollBehavior()
    val spacing = spacing
    val mainLikePadding = rememberStandalonePageMainPadding()
    val viewModel = koinViewModel<AccessControlViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val settingsFabController = rememberOverrideFabController()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var searchStatus by remember {
        mutableStateOf(
            SearchStatus(
                label = MLang.AccessControl.Search.Placeholder,
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
    val showSettingsFab by
        remember(currentSearchStatus, showSettingsSheet) {
            derivedStateOf { currentSearchStatus.isCollapsed() && !showSettingsSheet }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                OverrideAnimatedFab(
                    controller = settingsFabController,
                    visible = showSettingsFab,
                    imageVector = Yume.`Settings-2`,
                    contentDescription = MLang.AccessControl.Settings.Title,
                    onClick = { showSettingsSheet = true },
                )
            },
            topBar = {
                currentSearchStatus.TopAppBarAnim {
                    TopBar(
                        title = MLang.AccessControl.Title,
                        scrollBehavior = scrollBehavior,
                        bottomContent = {
                            Box(
                                modifier =
                                    Modifier.alpha(
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
            },
        ) { innerPadding ->
            val combinedInnerPadding = combinePaddingValues(innerPadding, mainLikePadding)
            val listBottomPadding = combinedInnerPadding.calculateBottomPadding()

            if (uiState.isLoading) {
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(
                                top = combinedInnerPadding.calculateTopPadding(),
                                start = listStartPadding,
                                end = listEndPadding,
                                bottom = listBottomPadding,
                            ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        MLang.AccessControl.AppList.Loading,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            } else if (currentSearchStatus.isCollapsed()) {
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = combinedInnerPadding,
                    onScrollDirectionChanged = settingsFabController::onScrollDirectionChanged,
                    contentPadding =
                        PaddingValues(
                            top = combinedInnerPadding.calculateTopPadding() + spacing.space6,
                            bottom = listBottomPadding,
                            start = listStartPadding,
                            end = listEndPadding,
                        ),
                ) {
                    item {
                        Title(
                            MLang.AccessControl.AppList.Title.format(uiState.selectedPackages.size)
                        )
                    }

                    items(items = filteredApps, key = { it.packageName }) { app ->
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
                    text = MLang.AccessControl.Search.Empty,
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
                items(items = filteredApps, key = { it.packageName }) { app ->
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
                contentDescription = MLang.Component.Editor.Action.Search,
                modifier =
                    Modifier.size(AppTheme.sizes.searchIconTouchTarget)
                        .padding(start = spacing.space16, end = spacing.space8),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
        modifier =
            Modifier.fillMaxWidth()
                .padding(start = startPadding, end = endPadding)
                .padding(top = topPadding, bottom = AppTheme.sizes.searchBarBottomPadding),
        onSearch = {},
        enabled = false,
        expanded = false,
        onExpandedChange = {},
    )
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
    val clipboardManager =
        remember(context) {
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
    val sortModeEntries = remember { AccessControlViewModel.SortMode.entries }

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = MLang.AccessControl.Settings.Title,
        onDismissRequest = onDismiss,
        enableNestedScroll = true,
        renderInRootScaffold = false,
    ) {
        Column {
            top.yukonga.miuix.kmp.basic.Card {
                SwitchPreference(
                    title = MLang.AccessControl.Settings.ShowSystemApps,
                    checked = uiState.showSystemApps,
                    onCheckedChange = onShowSystemAppsChange,
                )
                SwitchPreference(
                    title = MLang.AccessControl.Settings.SelectedFirst,
                    checked = uiState.selectedFirst,
                    onCheckedChange = onSelectedFirstChange,
                )
                WindowDropdownPreference(
                    title = MLang.AccessControl.Settings.SortMode,
                    summary =
                        MLang.AccessControl.Settings.SortModeCurrent.format(
                            uiState.sortMode.displayName
                        ),
                    items = sortModeEntries.map { it.displayName },
                    selectedIndex = sortModeEntries.indexOf(uiState.sortMode).coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        sortModeEntries.getOrNull(index)?.let(onSortModeChange)
                    },
                )
                WindowDropdownPreference(
                    title = MLang.AccessControl.Settings.BatchOperation,
                    items =
                        listOf(
                            MLang.AccessControl.Settings.SelectAll,
                            MLang.AccessControl.Settings.DeselectAll,
                            MLang.AccessControl.Settings.Invert,
                        ),
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
                    title = MLang.AccessControl.Settings.RegionQuickSelect,
                    items =
                        listOf(
                            MLang.AccessControl.Settings.ChinaApps,
                            MLang.AccessControl.Settings.OverseasApps,
                        ),
                    selectedIndex = 0,
                    onSelectedIndexChange = { index ->
                        val (label, selectedCount) =
                            when (index) {
                                0 -> MLang.AccessControl.Settings.ChinaApps to onSelectChinaApps()
                                1 ->
                                    MLang.AccessControl.Settings.OverseasApps to
                                        onSelectNonChinaApps()
                                else -> "" to 0
                            }
                        context.toast(
                            MLang.AccessControl.Settings.RegionSelectResult.format(
                                label,
                                selectedCount,
                            )
                        )
                    },
                )
                WindowDropdownPreference(
                    title = MLang.AccessControl.Settings.ImportExport,
                    items =
                        listOf(
                            MLang.AccessControl.Settings.Import,
                            MLang.AccessControl.Settings.Export,
                        ),
                    selectedIndex = 0,
                    onSelectedIndexChange = { index ->
                        when (index) {
                            0 -> {
                                val text =
                                    clipboardManager.primaryClip
                                        ?.takeIf { it.itemCount > 0 }
                                        ?.getItemAt(0)
                                        ?.text
                                        ?.toString()
                                        .orEmpty()
                                if (text.isNotEmpty()) {
                                    val count = onImportPackages(text)
                                    context.toast(
                                        MLang.AccessControl.Settings.ImportSuccess.format(count)
                                    )
                                } else {
                                    context.toast(MLang.AccessControl.Settings.ImportFailed)
                                }
                            }

                            1 -> {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("packages", onExportPackages())
                                )
                                context.toast(
                                    MLang.AccessControl.Settings.ExportSuccess.format(
                                        uiState.selectedPackages.size
                                    )
                                )
                            }
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.space24))

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space12)) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(MLang.AccessControl.Button.Cancel)
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(MLang.AccessControl.Button.Confirm, color = MiuixTheme.colorScheme.onPrimary)
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
    val spacing = spacing
    val componentSizes = AppTheme.sizes

    Card(modifier = Modifier.padding(vertical = spacing.space4), applyHorizontalPadding = false) {
        BasicComponent(
            title = app.label,
            summary = app.packageName,
            startAction = {
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = app.label,
                    imageSize = componentSizes.iconBadgeLarge,
                    bitmapSize = 80,
                    modifier = Modifier.padding(end = spacing.space12),
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
    imageSize: androidx.compose.ui.unit.Dp,
    bitmapSize: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconBitmap by
        produceState<ImageBitmap?>(initialValue = null, key1 = packageName, key2 = bitmapSize) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            context.packageManager
                                .getApplicationIcon(packageName)
                                .toBitmap(width = bitmapSize, height = bitmapSize)
                                .asImageBitmap()
                        }
                        .getOrNull()
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
private fun SearchEmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
