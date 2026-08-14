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

@file:Suppress("DuplicatedCode", "FunctionName")

package com.github.yumeyucca.yumebox.screen.settings


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.CPU
import com.github.yumeyucca.yumebox.presentation.icon.yume.PlaneTakeoff
import com.github.yumeyucca.yumebox.presentation.icon.yume.Tun
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.runtime.service.core.KernelManager
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

/**
 * Network settings entry point. Top: a "run mode" radio picker — one card per mode.
 * `RunMode.VpnService` is always available; root-only cards are greyed out unless
 * root is granted. Below: advanced options (service config + disable-overrides) and access control.
 * The former parallel HTTP "system proxy" run mode is gone.
 */
@Composable
fun NetworkSettingsScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val screen by viewModel.networkScreenState.collectAsState()
    val disableAllOverride = screen.disableAllOverride
    val accessControlMode = screen.accessControlMode
    val runMode = screen.runMode
    // Root Tun is only selectable when root is granted; otherwise its card is greyed
    // out.
    val rootAvailable = screen.rootAvailable
    val ebpfAvailable = screen.ebpfAvailable
    val kernels = screen.kernels
    val context = LocalContext.current
    var showKernelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(title = YumeTxt.NetworkSettings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(YumeTxt.NetworkSettings.RunMode.SectionTitle)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModeCard(
                        icon = Yume.PlaneTakeoff,
                        title = YumeTxt.NetworkSettings.RunMode.VpnServiceTitle,
                        summary = YumeTxt.NetworkSettings.RunMode.VpnServiceSummary,
                        selected = runMode == RunMode.VpnService,
                        enabled = true,
                        onSelect = { viewModel.onRunModeChange(RunMode.VpnService) },
                    )
                    ModeCard(
                        icon = Yume.Tun,
                        title = YumeTxt.NetworkSettings.RunMode.TunTitle,
                        summary = YumeTxt.NetworkSettings.RunMode.TunSummary,
                        selected = runMode == RunMode.Tun,
                        enabled = rootAvailable,
                        onSelect = { viewModel.onRunModeChange(RunMode.Tun) },
                    )
                    ModeCard(
                        icon = Yume.CPU,
                        title = YumeTxt.NetworkSettings.RunMode.EbpfTitle,
                        summary = YumeTxt.NetworkSettings.RunMode.EbpfSummary,
                        selected = runMode == RunMode.Ebpf,
                        enabled = ebpfAvailable,
                        onSelect = { viewModel.onRunModeChange(RunMode.Ebpf) },
                    )
                }
            }
            item {
                Title(YumeTxt.NetworkSettings.Section.Advanced)
                AppCard {
                    PreferenceArrowItem(
                        title = YumeTxt.NetworkSettings.Section.VpnOptions,
                        // Each mode's service config lives behind its own page.
                        onClick = {
                            when (runMode) {
                                RunMode.VpnService -> navigator.push(Route.VpnServiceOptions)
                                RunMode.Tun -> navigator.push(Route.TunServiceOptions)
                                RunMode.Ebpf -> navigator.push(Route.EbpfServiceOptions)
                            }
                        },
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.Advanced.DisableOverrideTitle,
                        checked = disableAllOverride,
                        onCheckedChange = viewModel::onDisableAllOverrideChange,
                    )
                }
            }
            item {
                Title(YumeTxt.NetworkSettings.Section.Kernel)
                AppCard {
                    val installedKernelIds = kernels
                        .filter { KernelManager.isInstalled(context, it.id) }
                        .map { it.id }
                    val locallyInstalledKernelIds = listOf("alpha", "meta", "smart")
                        .filter { KernelManager.isInstalled(context, it) }
                    val kernelIds = listOf("bundled-alpha") +
                        (locallyInstalledKernelIds + installedKernelIds).distinct()
                    WindowDropdownPreference(
                        title = YumeTxt.NetworkSettings.Kernel.ActiveTitle,
                        summary = null,
                        items = kernelIds.map { id ->
                            kernelLabel(id, screen.installedKernelCommits[id])
                        },
                        selectedIndex = kernelIds.indexOf(screen.activeKernelId).coerceAtLeast(0),
                        enabled = !screen.kernelBusy,
                        onSelectedIndexChange = { viewModel.selectKernel(kernelIds[it]) },
                    )
                    PreferenceArrowItem(
                        title = YumeTxt.NetworkSettings.Kernel.RefreshTitle,
                        summary = null,
                        onClick = { showKernelDialog = true },
                    )
                }
            }
            item {
                Title(YumeTxt.NetworkSettings.Section.ProxyOptions)
                AppCard {
                    PreferenceEnumItem(
                        title = YumeTxt.NetworkSettings.ProxyOptions.AccessControlModeTitle,
                        currentValue = accessControlMode,
                        items =
                            listOf(
                                YumeTxt.NetworkSettings.ProxyOptions.AllowAll,
                                YumeTxt.NetworkSettings.ProxyOptions.AllowSelected,
                                YumeTxt.NetworkSettings.ProxyOptions.RejectSelected,
                            ),
                        values = AccessControlMode.entries,
                        onValueChange = viewModel::onAccessControlModeChange,
                    )
                    PreferenceArrowItem(
                        title = YumeTxt.NetworkSettings.ProxyOptions.ManageAccessControlTitle,
                        onClick = { navigator.push(Route.AccessControl) },
                    )
                }
            }
        }

        KernelSelectionDialog(
            show = showKernelDialog,
            screen = screen,
            onRefresh = viewModel::refreshKernels,
            onDownload = { ids ->
                viewModel.downloadKernels(ids) { success ->
                    if (success) showKernelDialog = false
                }
            },
            onDismiss = { showKernelDialog = false },
        )
    }
}

@Composable
private fun kernelLabel(id: String, commit: String? = null): String {
    val name = when (id) {
        "bundled-alpha" -> YumeTxt.NetworkSettings.Kernel.BundledAlpha
        "alpha" -> "Alpha"
        "meta" -> "Meta"
        "smart" -> "Smart"
        else -> YumeTxt.NetworkSettings.Kernel.BundledAlpha
    }
    return if (id == KernelManager.BUNDLED_ALPHA_ID || commit == null) {
        name
    } else {
        "$name-${commit.take(6)}"
    }
}

@Composable
private fun KernelSelectionDialog(
    show: Boolean,
    screen: NetworkSettingsViewModel.NetworkSettingsScreenState,
    onRefresh: () -> Unit,
    onDownload: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = AppTheme.spacing
    var selectedIds by remember(show) { mutableStateOf<Set<String>>(emptySet()) }
    var initialized by remember(show) { mutableStateOf(false) }
    LaunchedEffect(show) {
        if (show) onRefresh()
    }
    LaunchedEffect(show, screen.kernels) {
        if (show && screen.kernels.isNotEmpty() && !initialized) {
            selectedIds = screen.kernels.map { it.id }.toSet()
            initialized = true
        }
    }
    AppDialog(show = show, title = YumeTxt.NetworkSettings.Kernel.DownloadTitle, onDismissRequest = onDismiss) {
        val contentState = when {
            screen.kernelBusy -> 0
            screen.kernels.isEmpty() -> 1
            else -> 2
        }
        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                fadeIn(tween(140)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = false)
            },
            label = "kernel_dialog_content",
        ) { state ->
            when (state) {
                0 ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.space24),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        InfiniteProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                1 ->
                    TextButton(
                        text = YumeTxt.NetworkSettings.Kernel.FetchButton,
                        onClick = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.space8),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                else ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                        screen.kernels.map { it.id to kernelLabel(it.id, it.commit) }
                            .forEach { (id, label) ->
                                BasicComponent(
                                    title = label,
                                    onClick = {
                                        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                    },
                                    endActions = {
                                        Checkbox(
                                            state = ToggleableState(selectedIds.contains(id)),
                                            onClick = {
                                                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                            },
                                        )
                                    },
                                )
                            }
                        TextButton(
                            text = YumeTxt.NetworkSettings.Kernel.DownloadButton,
                            onClick = { onDownload(selectedIds) },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.space16),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
            }
        }
    }
}

/**
 * A run-mode option: its own card with the home-mode icon on the leading side and a trailing
 * selection radio. A disabled mode greys its contents and can't be selected.
 */
@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    summary: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    AppCard {
        BasicComponent(
            enabled = enabled,
            onClick = if (enabled) onSelect else null,
            startAction = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        if (enabled) {
                            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface
                        } else {
                            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.disabledOnSecondaryVariant
                        },
                    modifier = Modifier
                        .padding(start = 4.dp, end = 12.dp)
                        .size(24.dp),
                )
            },
            endActions = {
                RadioButton(
                    selected = selected,
                    onClick = if (enabled) onSelect else null,
                    enabled = enabled,
                )
            },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    fontSize = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (enabled) {
                            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground
                        } else {
                            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.disabledOnSecondaryVariant
                        },
                )
            }
            Text(
                text = summary,
                fontSize = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2.fontSize,
                color =
                    if (enabled) {
                        top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary
                    } else {
                        top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.disabledOnSecondaryVariant
                    },
            )
        }
    }
}
