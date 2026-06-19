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

package com.github.yumelira.yumebox.presentation.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Circle-fading-arrow-up`
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.presentation.viewmodel.ProvidersViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import java.text.SimpleDateFormat
import java.util.*
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

private fun Provider.VehicleType.localizedDisplayName(): String =
    when (this) {
        Provider.VehicleType.HTTP -> MLang.Providers.VehicleType.Http
        Provider.VehicleType.File -> MLang.Providers.VehicleType.File
        Provider.VehicleType.Inline -> MLang.Providers.VehicleType.Inline
        Provider.VehicleType.Compatible -> MLang.Providers.VehicleType.Compatible
    }

private data class ProviderSection(val title: String, val providers: List<Provider>)

@Composable
fun ProvidersContent(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<ProvidersViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current

    val providers by viewModel.providers.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            viewModel.refreshProviders()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            context.toast(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            context.toast(it, Toast.LENGTH_LONG)
            viewModel.clearError()
        }
    }

    val updatableProviders =
        remember(providers) { providers.filter { it.vehicleType == Provider.VehicleType.HTTP } }
    val sections =
        remember(providers) {
            val (proxyProviders, ruleProviders) =
                providers.partition { it.type == Provider.Type.Proxy }
            buildList {
                if (proxyProviders.isNotEmpty()) {
                    add(
                        ProviderSection(
                            title = MLang.Providers.Type.ProxyProviders.format(proxyProviders.size),
                            providers = proxyProviders,
                        )
                    )
                }
                if (ruleProviders.isNotEmpty()) {
                    add(
                        ProviderSection(
                            title = MLang.Providers.Type.RuleProviders.format(ruleProviders.size),
                            providers = ruleProviders,
                        )
                    )
                }
            }
        }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Providers.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (isRunning && updatableProviders.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateAllProviders() }) {
                            Icon(
                                imageVector = Yume.`Circle-fading-arrow-up`,
                                contentDescription = MLang.Providers.Action.UpdateAll,
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        if (!isRunning) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NotRunning,
                secondLine = MLang.Providers.Empty.NotRunningHint,
            )
        } else if (providers.isEmpty() && !uiState.isLoading) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NoProviders,
                secondLine = MLang.Providers.Empty.NoProvidersHint,
            )
        } else {
            val mainLikePadding = rememberStandalonePageMainPadding()
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
            ) {
                sections.forEach { section ->
                    providerSection(
                        section = section,
                        isUpdating = { providerKey ->
                            uiState.updatingProviders.contains(providerKey)
                        },
                        onUpdate = { provider -> viewModel.updateProvider(provider) },
                        onUpload = { provider, uri ->
                            viewModel.uploadProviderFile(context, provider, uri)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: Provider,
    isUpdating: Boolean,
    onUpdate: () -> Unit,
    onUpload: (Uri) -> Unit,
) {
    val showPopup = remember { mutableStateOf(false) }
    val colorScheme = MiuixTheme.colorScheme
    val updateBg = remember(colorScheme) { colorScheme.primary.copy(alpha = 0.1f) }
    val updateTint = remember(colorScheme) { colorScheme.primary }

    val filePicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            uri: Uri? ->
            uri?.let { onUpload(it) }
        }

    Card(modifier = Modifier.padding(vertical = UiDp.dp4)) {
        Row(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = UiDp.dp16, vertical = UiDp.dp12),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.size(UiDp.dp4))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = provider.vehicleType.localizedDisplayName(),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    if (provider.updatedAt > 0) {
                        Text(
                            text = "•",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = formatTimestamp(provider.updatedAt),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(UiDp.dp8))

            if (provider.path.isNotBlank()) {
                Box {
                    IconButton(
                        backgroundColor = updateBg,
                        minHeight = UiDp.dp35,
                        minWidth = UiDp.dp35,
                        enabled = !isUpdating,
                        onClick = { showPopup.value = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = UiDp.dp10),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(UiDp.dp2),
                        ) {
                            Icon(
                                modifier = Modifier.size(UiDp.dp20),
                                imageVector = MiuixIcons.Edit,
                                tint = updateTint,
                                contentDescription = MLang.Providers.Action.Operation,
                            )
                            Text(
                                modifier = Modifier.padding(end = UiDp.dp3),
                                text = MLang.Providers.Action.Operation,
                                color = updateTint,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                            )
                        }
                    }

                    val popupItems =
                        listOf(MLang.Providers.Action.Update, MLang.Providers.Action.Upload)

                    WindowListPopup(
                        show = showPopup.value,
                        popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                        alignment = PopupPositionProvider.Align.End,
                        onDismissRequest = { showPopup.value = false },
                    ) {
                        ListPopupColumn {
                            popupItems.forEachIndexed { index, item ->
                                DropdownImpl(
                                    text = item,
                                    optionSize = popupItems.size,
                                    isSelected = false,
                                    onSelectedIndexChange = {
                                        showPopup.value = false
                                        when (index) {
                                            0 -> onUpdate()
                                            1 -> filePicker.launch("*/*")
                                        }
                                    },
                                    index = index,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.providerSection(
    section: ProviderSection,
    isUpdating: (String) -> Boolean,
    onUpdate: (Provider) -> Unit,
    onUpload: (Provider, Uri) -> Unit,
) {
    item(key = "title_${section.title}") { Title(section.title) }
    items(
        items = section.providers,
        key = { provider -> "${provider.type}_${provider.name}" },
        contentType = { "ProviderCard" },
    ) { provider ->
        val providerKey = "${provider.type}_${provider.name}"
        ProviderCard(
            provider = provider,
            isUpdating = isUpdating(providerKey),
            onUpdate = { onUpdate(provider) },
            onUpload = { uri -> onUpload(provider, uri) },
        )
    }
}

private fun formatTimestamp(ts: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
