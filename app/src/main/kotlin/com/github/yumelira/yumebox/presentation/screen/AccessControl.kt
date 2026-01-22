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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.presentation.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SmallTitle
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Settings-2`
import com.github.yumelira.yumebox.presentation.viewmodel.AccessControlViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AccessControlScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AccessControlViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val showSettingsSheet = rememberSaveable { mutableStateOf(false) }
    val searchExpanded = rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.onPermissionResult()
            }
        }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.needsMiuiPermission }
            .collect { needsPermission ->
                if (needsPermission) {
                    permissionLauncher.launch("com.android.permission.GET_INSTALLED_APPS")
                }
            }
    }

    BackHandler(enabled = searchExpanded.value) {
        searchExpanded.value = false
        viewModel.onSearchQueryChange("")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopBar(
                    title = MLang.AccessControl.Title,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(
                            modifier = Modifier.padding(end = 24.dp),
                            onClick = { showSettingsSheet.value = true }
                        ) {
                            Icon(Yume.`Settings-2`, contentDescription = MLang.AccessControl.Settings.Title)
                        }
                    }
                )
            },
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(MLang.AccessControl.AppList.Loading, color = MiuixTheme.colorScheme.onSurface)
                }
            } else {
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    topPadding = 20.dp
                ) {
                    item {
                        var searchText by remember { mutableStateOf("") }
                        var expanded by remember { mutableStateOf(false) }
                        SearchBar(
                            inputField = {
                                InputField(
                                    query = searchText,
                                    onQueryChange = {
                                        searchText = it
                                        viewModel.onSearchQueryChange(it)
                                    },
                                    onSearch = { expanded = false },
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it },
                                    label = MLang.AccessControl.Search.Placeholder
                                )
                            },
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                        }
                    }

                    item {
                        SmallTitle(MLang.AccessControl.AppList.Title.format(uiState.selectedPackages.size))
                    }

                    items(
                        items = uiState.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppCard(
                            app = app,
                            onSelectionChange = { checked ->
                                viewModel.onAppSelectionChange(app.packageName, checked)
                            },
                            onClick = {
                                viewModel.onAppSelectionChange(app.packageName, !app.isSelected)
                            }
                        )
                    }
                }
            }

            WindowBottomSheet(
                show = showSettingsSheet,
                title = MLang.AccessControl.Settings.Title,
                onDismissRequest = { showSettingsSheet.value = false },
                insideMargin = DpSize(32.dp, 16.dp),
            ) {
                val context = LocalContext.current
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                Column {
                    top.yukonga.miuix.kmp.basic.Card {
                        SuperSwitch(
                            title = MLang.AccessControl.Settings.ShowSystemApps,
                            checked = uiState.showSystemApps,
                            onCheckedChange = { viewModel.onShowSystemAppsChange(it) }
                        )
                        SuperSwitch(
                            title = MLang.AccessControl.Settings.DescendingOrder,
                            checked = uiState.descending,
                            onCheckedChange = { viewModel.onDescendingChange(it) }
                        )
                        SuperSwitch(
                            title = MLang.AccessControl.Settings.SelectedFirst,
                            checked = uiState.selectedFirst,
                            onCheckedChange = { viewModel.onSelectedFirstChange(it) }
                        )
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    top.yukonga.miuix.kmp.basic.Card {
                        WindowDropdown(
                            title = MLang.AccessControl.Settings.SortMode,
                            summary = MLang.AccessControl.Settings.SortModeCurrent.format(uiState.sortMode.displayName),
                            items = AccessControlViewModel.SortMode.entries.map { it.displayName },
                            selectedIndex = AccessControlViewModel.SortMode.entries
                                .indexOf(uiState.sortMode)
                                .coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                AccessControlViewModel.SortMode.entries.getOrNull(index)
                                    ?.let { viewModel.onSortModeChange(it) }
                            }
                        )
                        WindowDropdown(
                            title = MLang.AccessControl.Settings.BatchOperation,
                            items = listOf(
                                MLang.AccessControl.Settings.SelectAll,
                                MLang.AccessControl.Settings.DeselectAll,
                                MLang.AccessControl.Settings.Invert
                            ),
                            selectedIndex = 0,
                            onSelectedIndexChange = { index ->
                                when (index) {
                                    0 -> viewModel.selectAll()
                                    1 -> viewModel.deselectAll()
                                    2 -> viewModel.invertSelection()
                                }
                            }
                        )
                        WindowDropdown(
                            title = MLang.AccessControl.Settings.ImportExport,
                            items = listOf(
                                MLang.AccessControl.Settings.Import,
                                MLang.AccessControl.Settings.Export
                            ),
                            selectedIndex = 0,
                            onSelectedIndexChange = { index ->
                                when (index) {
                                    0 -> {
                                        val clipData = clipboardManager.primaryClip
                                        val text = if (clipData != null && clipData.itemCount > 0) {
                                            clipData.getItemAt(0)?.text?.toString() ?: ""
                                        } else {
                                            ""
                                        }
                                        if (text.isNotEmpty()) {
                                            val count = viewModel.importPackages(text)
                                            Toast.makeText(
                                                context,
                                                MLang.AccessControl.Settings.ImportSuccess.format(count),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                MLang.AccessControl.Settings.ImportFailed,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    1 -> {
                                        val exportText = viewModel.exportPackages()
                                        val clip = ClipData.newPlainText("packages", exportText)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            MLang.AccessControl.Settings.ExportSuccess.format(uiState.selectedPackages.size),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showSettingsSheet.value = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(MLang.AccessControl.Button.Cancel)
                    }
                    Button(
                        onClick = { showSettingsSheet.value = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(MLang.AccessControl.Button.Confirm, color = MiuixTheme.colorScheme.background)
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = searchExpanded.value,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        ExpandedSearchOverlay(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            filteredApps = uiState.filteredApps,
            onAppSelectionChange = { packageName, checked ->
                viewModel.onAppSelectionChange(packageName, checked)
            },
            onDismiss = {
                searchExpanded.value = false
                viewModel.onSearchQueryChange("")
            }
        )
    }
}

@Composable
private fun ExpandedSearchOverlay(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredApps: List<AccessControlViewModel.AppInfo>,
    onAppSelectionChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = MLang.AccessControl.Search.Placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MiuixTheme.colorScheme.surface)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    BasicComponent(
                        title = app.label,
                        summary = app.packageName,
                        startAction = {
                            app.icon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap(width = 80, height = 80).asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        endActions = {
                            Checkbox(
                                checked = app.isSelected,
                                onCheckedChange = { checked ->
                                    onAppSelectionChange(app.packageName, checked)
                                }
                            )
                        },
                        onClick = {
                            onAppSelectionChange(app.packageName, !app.isSelected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCard(
    app: AccessControlViewModel.AppInfo,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        BasicComponent(
            title = app.label,
            summary = app.packageName,
            startAction = {
                app.icon?.let { icon ->
                    Image(
                        bitmap = icon.toBitmap(width = 80, height = 80).asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(45.dp)
                            .padding(end = 12.dp)
                    )
                }
            },
            endActions = {
                Checkbox(
                    checked = app.isSelected,
                    onCheckedChange = onSelectionChange
                )
            },
            onClick = onClick
        )
    }
}
