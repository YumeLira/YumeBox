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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.model.GeoFileType
import com.github.yumelira.yumebox.core.model.GeoXItem
import com.github.yumelira.yumebox.core.model.geoXItems
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.substore.util.SubStoreDownloadClient
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ConnectionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TrafficStatisticsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
@Destination<RootGraph>
fun MetaFeatureScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadClient: SubStoreDownloadClient = koinInject()

    val showGeoXDownloadSheet = remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(title = MLang.MetaFeature.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(MLang.MetaFeature.Section.ConnectionAndTraffic)
                Card {
                    ArrowPreference(
                        title = MLang.Connection.Title,
                        summary = MLang.Connection.Summary,
                        onClick = {
                            navigator.navigate(ConnectionScreenDestination) {
                                launchSingleTop = true
                            }
                        },
                    )
                    ArrowPreference(
                        title = MLang.TrafficStatistics.Title,
                        summary = MLang.TrafficStatistics.EntrySummary,
                        onClick = {
                            navigator.navigate(TrafficStatisticsScreenDestination) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
            item {
                Title(MLang.MetaFeature.Section.CustomRouting)
                Card {
                    ArrowPreference(
                        title = MLang.MetaFeature.CustomRouting.Title,
                        summary = MLang.MetaFeature.CustomRouting.Summary,
                        onClick = {
                            navigator.navigate(
                                com.ramcosta.composedestinations.generated.destinations
                                    .CustomRoutingRouteDestination
                            ) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
            item {
                Title(MLang.MetaFeature.Section.GeoXUpdate)
                Card {
                    ArrowPreference(
                        title = MLang.MetaFeature.GeoX.OnlineUpdateTitle,
                        summary = MLang.MetaFeature.GeoX.OnlineUpdateSummary,
                        onClick = { showGeoXDownloadSheet.value = true },
                    )
                }
            }
        }

        GeoXDownloadSheet(
            show = showGeoXDownloadSheet,
            context = context,
            scope = scope,
            downloadClient = downloadClient,
        )
    }
}

@Composable
private fun GeoXDownloadSheet(
    show: MutableState<Boolean>,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    downloadClient: SubStoreDownloadClient,
) {
    val selectedItems = remember { mutableStateMapOf<GeoFileType, Boolean>() }

    AppActionBottomSheet(
        show = show.value,
        title = MLang.MetaFeature.Download.DialogTitle,
        onDismissRequest = { show.value = false },
        startAction = { AppBottomSheetCloseAction(onClick = { show.value = false }) },
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = selectedItems.values.any { it },
                onClick = {
                    val itemsToDownload = geoXItems.filter { selectedItems[it.type] == true }
                    if (itemsToDownload.isEmpty()) {
                        return@AppBottomSheetConfirmAction
                    }
                    show.value = false
                    downloadGeoXFiles(context, scope, downloadClient, itemsToDownload)
                },
            )
        },
        content = {
            Column {
                geoXItems.forEach { item ->
                    BasicComponent(
                        title = item.title,
                        endActions = {
                            Checkbox(
                                state = ToggleableState(selectedItems[item.type] ?: false),
                                onClick = {
                                    selectedItems[item.type] = !(selectedItems[item.type] ?: false)
                                },
                            )
                        },
                        onClick = {
                            selectedItems[item.type] = !(selectedItems[item.type] ?: false)
                        },
                    )
                }
            }
        },
    )
}

private fun downloadGeoXFiles(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    downloadClient: SubStoreDownloadClient,
    items: List<GeoXItem>,
) {
    scope.launch {
        var successCount = 0
        withContext(Dispatchers.IO) {
            val runtimeHome = context.runtimeHomeDir
            runtimeHome.mkdirs()
            items.forEach { item ->
                val targetFile = File(runtimeHome, item.fileName)
                if (downloadClient.download(item.url, targetFile)) {
                    successCount++
                }
            }
        }
        context.toast(MLang.MetaFeature.Download.DownloadComplete.format(successCount, items.size))
    }
}
