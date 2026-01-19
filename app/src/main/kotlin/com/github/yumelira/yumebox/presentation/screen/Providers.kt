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

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.viewmodel.ProvidersViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.DropdownImpl
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
@Destination<RootGraph>
fun ProvidersScreen(navigator: DestinationsNavigator) {
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
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "外部资源",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon(navigator = navigator)
                },
                actions = {
                    if (isRunning && providers.any { it.vehicleType == Provider.VehicleType.HTTP }) {
                        RotatingRefreshButton(
                            isRotating = uiState.isUpdatingAll,
                            onClick = { viewModel.updateAllProviders() },
                            modifier = Modifier.padding(end = 24.dp),
                            contentDescription = "更新全部"
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        if (!isRunning) {
            CenteredText(
                firstLine = "代理未启动",
                secondLine = "请先启动代理服务以查看外部资源"
            )
        } else if (providers.isEmpty() && !uiState.isLoading) {
            CenteredText(
                firstLine = "暂无外部资源",
                secondLine = "当前配置未包含外部资源"
            )
        } else {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = innerPadding,
            ) {
                val proxyProviders = providers.filter { it.type == Provider.Type.Proxy }
                val ruleProviders = providers.filter { it.type == Provider.Type.Rule }

                if (proxyProviders.isNotEmpty()) {
                    item {
                        SmallTitle("代理提供者 (${proxyProviders.size})")
                    }
                    proxyProviders.forEach { provider ->
                        val providerKey = "${provider.type}_${provider.name}"
                        item(key = providerKey) {
                            ProviderCard(
                                provider = provider,
                                isUpdating = uiState.updatingProviders.contains(providerKey),
                                onUpdate = { viewModel.updateProvider(provider) },
                                onUpload = { uri -> viewModel.uploadProviderFile(context, provider, uri) }
                            )
                        }
                    }
                }

                if (ruleProviders.isNotEmpty()) {
                    item {
                        SmallTitle("规则提供者 (${ruleProviders.size})")
                    }
                    ruleProviders.forEach { provider ->
                        val providerKey = "${provider.type}_${provider.name}"
                        item(key = providerKey) {
                            ProviderCard(
                                provider = provider,
                                isUpdating = uiState.updatingProviders.contains(providerKey),
                                onUpdate = { viewModel.updateProvider(provider) },
                                onUpload = { uri -> viewModel.uploadProviderFile(context, provider, uri) }
                            )
                        }
                    }
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
    onUpload: (Uri) -> Unit
) {
    val showPopup = remember { mutableStateOf(false) }
    val colorScheme = MiuixTheme.colorScheme
    val updateBg = remember(colorScheme) { colorScheme.tertiaryContainer.copy(alpha = 0.6f) }
    val updateTint = remember(colorScheme) { colorScheme.onTertiaryContainer.copy(alpha = 0.8f) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onUpload(it) }
    }

    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider.vehicleType.name,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (provider.updatedAt > 0) {
                        Text(
                            text = "•",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = formatTimestamp(provider.updatedAt),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (provider.path.isNotBlank()) {
                Box {
                    IconButton(
                        backgroundColor = updateBg,
                        minHeight = 35.dp,
                        minWidth = 35.dp,
                        enabled = !isUpdating,
                        onClick = { showPopup.value = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = MiuixIcons.Useful.Edit,
                                tint = updateTint,
                                contentDescription = "操作",
                            )
                            Text(
                                modifier = Modifier.padding(end = 3.dp),
                                text = "操作",
                                color = updateTint,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }

                    val items = listOf("更新", "上传")
                    var selectedIndex by remember { mutableStateOf(0) }

                    ListPopup(
                        show = showPopup,
                        popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                        alignment = PopupPositionProvider.Align.Right,
                        onDismissRequest = { showPopup.value = false }
                    ) {
                        ListPopupColumn {
                            items.forEachIndexed { index, item ->
                                DropdownImpl(
                                    text = item,
                                    optionSize = items.size,
                                    isSelected = selectedIndex == index,
                                    onSelectedIndexChange = {
                                        selectedIndex = index
                                        showPopup.value = false
                                        when (index) {
                                            0 -> onUpdate()
                                            1 -> filePicker.launch("*/*")
                                        }
                                    },
                                    index = index
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
