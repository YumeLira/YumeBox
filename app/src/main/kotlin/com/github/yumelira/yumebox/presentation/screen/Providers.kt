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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.RotatingRefreshButton
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SmallTitle
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.viewmodel.ProvidersViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
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
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                title = MLang.Providers.Title,
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
                            contentDescription = MLang.Providers.Action.UpdateAll
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        if (!isRunning) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NotRunning,
                secondLine = MLang.Providers.Empty.NotRunningHint
            )
        } else if (providers.isEmpty() && !uiState.isLoading) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NoProviders,
                secondLine = MLang.Providers.Empty.NoProvidersHint
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
                        SmallTitle(MLang.Providers.Type.ProxyProviders.format(proxyProviders.size))
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
                        SmallTitle(MLang.Providers.Type.RuleProviders.format(ruleProviders.size))
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
                                imageVector = MiuixIcons.Edit,
                                tint = updateTint,
                                contentDescription = MLang.Providers.Action.Operation,
                            )
                            Text(
                                modifier = Modifier.padding(end = 3.dp),
                                text = MLang.Providers.Action.Operation,
                                color = updateTint,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }

                    val items = listOf(MLang.Providers.Action.Update, MLang.Providers.Action.Upload)
                    var selectedIndex by remember { mutableStateOf(0) }

                    WindowListPopup  (
                        show = showPopup,
                        popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                        alignment = PopupPositionProvider.Align.End,
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
