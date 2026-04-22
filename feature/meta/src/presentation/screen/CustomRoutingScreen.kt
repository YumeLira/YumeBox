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

package com.github.yumelira.yumebox.feature.meta.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.feature.meta.presentation.util.OverridePresetItem
import com.github.yumelira.yumebox.feature.meta.presentation.util.OverridePresetRegion
import com.github.yumelira.yumebox.feature.meta.presentation.util.OverridePresetTemplateSelection
import com.github.yumelira.yumebox.feature.meta.presentation.util.orderedBasePresetItems
import com.github.yumelira.yumebox.feature.meta.presentation.util.orderedPresetRegions
import com.github.yumelira.yumebox.feature.meta.presentation.util.orderedServicePresetItems
import com.github.yumelira.yumebox.feature.meta.presentation.util.presetGroupTypeIconUrl
import com.github.yumelira.yumebox.feature.meta.presentation.util.sortPresetItems
import com.github.yumelira.yumebox.feature.meta.presentation.util.sortPresetRegions
import com.github.yumelira.yumebox.feature.meta.presentation.viewmodel.CustomRoutingViewModel
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.RoutingSwitchCard
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Edit
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CustomRoutingScreen(
    onNavigateBack: () -> Unit,
    onOpenYamlEditor: (
        title: String,
        content: String,
        onSave: suspend (String) -> Unit,
    ) -> Unit,
) {
    val viewModel: CustomRoutingViewModel = koinViewModel()
    val presetSelection by viewModel.presetSelection.collectAsState()
    val customRoutingContent by viewModel.customRoutingContent.collectAsState()
    val templateRoundTripSafe by viewModel.templateRoundTripSafe.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedUrlTestRegions = remember { mutableStateListOf<OverridePresetRegion>() }
    val selectedFallbackRegions = remember { mutableStateListOf<OverridePresetRegion>() }
    val enabledItems = remember { mutableStateListOf<OverridePresetItem>() }
    var enableUrlTestGroup by remember { mutableStateOf(true) }
    var enableFallbackGroup by remember { mutableStateOf(false) }
    var isDirty by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(presetSelection) {
        selectedUrlTestRegions.clear()
        selectedUrlTestRegions.addAll(sortPresetRegions(presetSelection.urlTestRegions))
        selectedFallbackRegions.clear()
        selectedFallbackRegions.addAll(sortPresetRegions(presetSelection.fallbackRegions))
        enabledItems.clear()
        enabledItems.addAll(sortPresetItems(presetSelection.enabledItems))
        enableUrlTestGroup = presetSelection.enableUrlTestGroup
        enableFallbackGroup = presetSelection.enableFallbackGroup
        isDirty = false
    }

    fun saveAndExit() {
        if (isSaving) return
        if (!isDirty) {
            onNavigateBack()
            return
        }

        val updatedSelection = OverridePresetTemplateSelection(
            urlTestRegions = selectedUrlTestRegions.toSet(),
            fallbackRegions = selectedFallbackRegions.toSet(),
            enabledItems = enabledItems.toSet(),
            enableUrlTestGroup = enableUrlTestGroup,
            enableFallbackGroup = enableFallbackGroup,
        )
        scope.launch {
            isSaving = true
            viewModel.savePresetSelection(updatedSelection)
                .onSuccess {
                    isDirty = false
                    onNavigateBack()
                }
                .onFailure { error ->
                    context.toast(error.message ?: "保存失败")
                }
            isSaving = false
        }
    }

    BackHandler {
        saveAndExit()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.MetaFeature.CustomRouting.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        enabled = !isSaving,
                        onClick = {
                            onOpenYamlEditor(
                                MLang.MetaFeature.CustomRouting.EditYaml,
                                customRoutingContent,
                            ) { content ->
                                viewModel.saveCustomRoutingYaml(content)
                                    .getOrElse { throw it }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Yume.Edit,
                            contentDescription = "Edit",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val mainPadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainPadding),
        ) {
            item(key = "custom-routing-description") {
                Card(modifier = Modifier.padding(top = 16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = MLang.MetaFeature.CustomRouting.Description,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }

            item(key = "group-type") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.GroupTypeTitle,
                    items = listOf("urltest", "fallback"),
                    iconUrl = ::presetGroupTypeIconUrl,
                    itemTitle = { type ->
                        if (type == "urltest") {
                            MLang.MetaFeature.CustomRouting.GroupTypeUrlTest
                        } else {
                            MLang.MetaFeature.CustomRouting.GroupTypeFallback
                        }
                    },
                    isChecked = { type ->
                        if (type == "urltest") enableUrlTestGroup else enableFallbackGroup
                    },
                    onCheckedChange = { type, checked ->
                        if (type == "urltest") {
                            enableUrlTestGroup = checked
                        } else {
                            enableFallbackGroup = checked
                        }
                        isDirty = true
                    },
                )
            }

            item(key = "urltest-regions") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.UrlTestRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    itemTitle = OverridePresetRegion::displayName,
                    isChecked = { region -> region in selectedUrlTestRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedUrlTestRegions, region, checked)
                        isDirty = true
                    },
                )
            }

            item(key = "fallback-regions") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.FallbackRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    itemTitle = OverridePresetRegion::displayName,
                    isChecked = { region -> region in selectedFallbackRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedFallbackRegions, region, checked)
                        isDirty = true
                    },
                )
            }

            item(key = "base-items") {
                RoutingSwitchCard(
                    title = MLang.Override.Draft.BasicRouting,
                    items = orderedBasePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    itemTitle = OverridePresetItem::title,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked ->
                        toggleSelection(enabledItems, item, checked)
                        isDirty = true
                    },
                )
            }

            item(key = "service-items") {
                RoutingSwitchCard(
                    title = MLang.Override.Draft.ServiceRouting,
                    items = orderedServicePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    itemTitle = OverridePresetItem::title,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked ->
                        toggleSelection(enabledItems, item, checked)
                        isDirty = true
                    },
                )
            }
        }
    }
}

private fun <T> toggleSelection(
    items: MutableList<T>,
    item: T,
    checked: Boolean,
) {
    if (checked) {
        if (item !in items) {
            items.add(item)
        }
    } else {
        items.remove(item)
    }
}
