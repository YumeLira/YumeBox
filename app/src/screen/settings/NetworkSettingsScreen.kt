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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumelira.yumebox.common.util.VpnUtils
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.model.RootTunDnsMode
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumelira.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumelira.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.service.root.RootAccessSupport
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AccessControlScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
@Destination<RootGraph>
fun NetworkSettingsScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val tunServiceOptionsUiState by viewModel.tunServiceOptionsUiState.collectAsState()
    val rootTunServiceOptionsUiState by viewModel.rootTunServiceOptionsUiState.collectAsState()
    val accessControlMode by viewModel.accessControlMode.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.errors.collect { message -> context.toast(message) } }

    val vpnPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.onProxyModeChange(ProxyMode.Tun)
            } else {
                context.toast(MLang.NetworkSettings.Error.VpnDenied)
            }
        }

    Scaffold(
        topBar = { TopBar(title = MLang.NetworkSettings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                NetworkVpnServiceSection(
                    viewModel = viewModel,
                    configuredMode = uiState.configuredMode,
                    vpnPermissionLauncher = vpnPermissionLauncher,
                )
            }
            item {
                NetworkServiceOptionsSection(
                    viewModel = viewModel,
                    uiState = uiState,
                    tunServiceOptionsUiState = tunServiceOptionsUiState,
                    rootTunServiceOptionsUiState = rootTunServiceOptionsUiState,
                )
            }
            item {
                NetworkProxyOptionsSection(
                    navigator = navigator,
                    accessControlMode = accessControlMode,
                    showAccessControlMode = uiState.showAccessControlMode,
                    onAccessControlModeChange = viewModel::onAccessControlModeChange,
                )
            }
        }
    }
}

@Composable
private fun NetworkVpnServiceSection(
    viewModel: NetworkSettingsViewModel,
    configuredMode: ProxyMode,
    vpnPermissionLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Title(MLang.NetworkSettings.Section.VpnService)
    Card {
        PreferenceEnumItem(
            title = MLang.NetworkSettings.VpnService.RouteTrafficTitle,
            summary = MLang.NetworkSettings.VpnService.RouteTrafficSummary,
            currentValue = configuredMode,
            items =
                listOf(
                    MLang.NetworkSettings.VpnService.SystemProxy,
                    MLang.NetworkSettings.VpnService.VpnMode,
                    MLang.NetworkSettings.VpnService.RootTunMode,
                ),
            values = listOf(ProxyMode.Http, ProxyMode.Tun, ProxyMode.RootTun),
            onValueChange = { mode ->
                when (mode) {
                    ProxyMode.Tun -> {
                        if (!VpnUtils.checkVpnPermission(context)) {
                            VpnUtils.getVpnPermissionIntent(context)
                                ?.let(vpnPermissionLauncher::launch)
                                ?: viewModel.onProxyModeChange(mode)
                        } else {
                            viewModel.onProxyModeChange(mode)
                        }
                    }

                    ProxyMode.RootTun -> {
                        coroutineScope.launch {
                            val rootStatus = RootAccessSupport.evaluateAsync(context)
                            if (!rootStatus.canStartRootTun) {
                                context.toast(rootStatus.rootTunBlockedMessage())
                                return@launch
                            }
                            viewModel.onProxyModeChange(mode)
                        }
                    }

                    ProxyMode.Http -> {
                        viewModel.onProxyModeChange(mode)
                    }
                }
            },
        )
    }
}

@Composable
private fun NetworkServiceOptionsSection(
    viewModel: NetworkSettingsViewModel,
    uiState: NetworkSettingsUiState,
    tunServiceOptionsUiState: TunServiceOptionsUiState,
    rootTunServiceOptionsUiState: RootTunServiceOptionsUiState,
) {
    if (!uiState.showServiceOptions) return

    val commonActions =
        remember(viewModel) {
            CommonTunOptionActions(
                onBypassPrivateNetworkChange = viewModel::onBypassPrivateNetworkChange,
                onDnsHijackChange = viewModel::onDnsHijackChange,
                onEnableIPv6Change = viewModel::onEnableIPv6Change,
                onTunStackChange = viewModel::onTunStackChange,
            )
        }

    Title(MLang.NetworkSettings.Section.VpnOptions)
    Card {
        when (uiState.configuredMode) {
            ProxyMode.Tun -> {
                TunServiceOptions(
                    state = tunServiceOptionsUiState,
                    actions =
                        TunServiceOptionActions(
                            common = commonActions,
                            onAllowBypassChange = viewModel::onAllowBypassChange,
                            onSystemProxyChange = viewModel::onSystemProxyChange,
                        ),
                )
            }

            ProxyMode.RootTun -> {
                RootTunServiceOptions(
                    state = rootTunServiceOptionsUiState,
                    showFakeIpRange = uiState.showFakeIpRange,
                    actions =
                        remember(viewModel, commonActions) {
                            RootTunServiceOptionActions(
                                common = commonActions,
                                onRootTunAutoRouteChange = viewModel::onRootTunAutoRouteChange,
                                onRootTunStrictRouteChange = viewModel::onRootTunStrictRouteChange,
                                onRootTunAutoRedirectChange =
                                    viewModel::onRootTunAutoRedirectChange,
                                onRootTunDnsModeChange = viewModel::onRootTunDnsModeChange,
                                onRootTunIfNameDraftChange = viewModel::onRootTunIfNameDraftChange,
                                onRootTunMtuDraftChange = viewModel::onRootTunMtuDraftChange,
                                onRootTunFakeIpRangeDraftChange =
                                    viewModel::onRootTunFakeIpRangeDraftChange,
                                onRootTunFakeIpRange6DraftChange =
                                    viewModel::onRootTunFakeIpRange6DraftChange,
                                commitRootTunIfName = viewModel::commitRootTunIfName,
                                commitRootTunMtu = viewModel::commitRootTunMtu,
                                commitRootTunFakeIpRange = viewModel::commitRootTunFakeIpRange,
                                commitRootTunFakeIpRange6 = viewModel::commitRootTunFakeIpRange6,
                            )
                        },
                )
            }

            ProxyMode.Http -> Unit
        }
    }
}

@Composable
private fun NetworkProxyOptionsSection(
    navigator: DestinationsNavigator,
    accessControlMode: AccessControlMode,
    showAccessControlMode: Boolean,
    onAccessControlModeChange: (AccessControlMode) -> Unit,
) {
    Title(MLang.NetworkSettings.Section.ProxyOptions)
    Card {
        if (showAccessControlMode) {
            PreferenceEnumItem(
                title = MLang.NetworkSettings.ProxyOptions.AccessControlModeTitle,
                currentValue = accessControlMode,
                items =
                    listOf(
                        MLang.NetworkSettings.ProxyOptions.AllowAll,
                        MLang.NetworkSettings.ProxyOptions.AllowSelected,
                        MLang.NetworkSettings.ProxyOptions.RejectSelected,
                    ),
                values = AccessControlMode.entries,
                onValueChange = onAccessControlModeChange,
            )
        }
        PreferenceArrowItem(
            title = MLang.NetworkSettings.ProxyOptions.ManageAccessControlTitle,
            summary = MLang.NetworkSettings.ProxyOptions.ManageAccessControlSummary,
            onClick = { navigator.navigate(AccessControlScreenDestination) },
        )
    }
}

@Composable
private fun TunServiceOptions(state: TunServiceOptionsUiState, actions: TunServiceOptionActions) {
    CommonTunServiceOptions(
        state = state.common,
        actions = actions.common,
        extraOptions = {
            PreferenceSwitchItem(
                title = MLang.NetworkSettings.VpnOptions.AllowBypassTitle,
                summary = MLang.NetworkSettings.VpnOptions.AllowBypassSummary,
                checked = state.allowBypass,
                onCheckedChange = actions.onAllowBypassChange,
            )
            PreferenceSwitchItem(
                title = MLang.NetworkSettings.VpnOptions.SystemProxyTitle,
                summary = MLang.NetworkSettings.VpnOptions.SystemProxySummary,
                checked = state.systemProxy,
                onCheckedChange = actions.onSystemProxyChange,
            )
        },
    )
}

@Composable
private fun RootTunServiceOptions(
    state: RootTunServiceOptionsUiState,
    showFakeIpRange: Boolean,
    actions: RootTunServiceOptionActions,
) {
    CommonTunServiceOptions(
        state = state.common,
        actions = actions.common,
        extraOptions = {
            RootTunAdvancedOptions(
                state = state,
                showFakeIpRange = showFakeIpRange,
                actions = actions,
            )
        },
    )
}

@Composable
private fun RootTunAdvancedOptions(
    state: RootTunServiceOptionsUiState,
    showFakeIpRange: Boolean,
    actions: RootTunServiceOptionActions,
) {
    var editDialog by remember { mutableStateOf<RootTunEditDialogState?>(null) }

    RootTunIdentityOptions(
        rootTunIfNameDraft = state.rootTunIfNameDraft,
        rootTunMtuDraft = state.rootTunMtuDraft,
        onEditIfName = { editDialog = RootTunEditDialogState.IfName },
        onEditMtu = { editDialog = RootTunEditDialogState.Mtu },
    )
    RootTunRoutingOptions(
        rootTunAutoRoute = state.rootTunAutoRoute,
        rootTunStrictRoute = state.rootTunStrictRoute,
        rootTunAutoRedirect = state.rootTunAutoRedirect,
        rootTunDnsMode = state.rootTunDnsMode,
        onRootTunAutoRouteChange = actions.onRootTunAutoRouteChange,
        onRootTunStrictRouteChange = actions.onRootTunStrictRouteChange,
        onRootTunAutoRedirectChange = actions.onRootTunAutoRedirectChange,
        onRootTunDnsModeChange = actions.onRootTunDnsModeChange,
    )
    RootTunFakeIpOptions(
        showFakeIpRange = showFakeIpRange,
        rootTunFakeIpRangeDraft = state.rootTunFakeIpRangeDraft,
        rootTunFakeIpRange6Draft = state.rootTunFakeIpRange6Draft,
        onEditFakeIpRange = { editDialog = RootTunEditDialogState.FakeIpRange },
        onEditFakeIpRange6 = { editDialog = RootTunEditDialogState.FakeIpRange6 },
    )

    RootTunEditDialogs(
        editDialog = editDialog,
        state = state,
        actions = actions,
        onDismiss = { editDialog = null },
    )
}

@Composable
private fun RootTunIdentityOptions(
    rootTunIfNameDraft: String,
    rootTunMtuDraft: String,
    onEditIfName: () -> Unit,
    onEditMtu: () -> Unit,
) {
    PreferenceArrowItem(
        title = MLang.NetworkSettings.RootTun.IfNameTitle,
        summary = rootTunIfNameDraft.ifBlank { MLang.NetworkSettings.RootTun.IfNameSummary },
        onClick = onEditIfName,
    )
    PreferenceArrowItem(
        title = MLang.NetworkSettings.RootTun.MtuTitle,
        summary = rootTunMtuDraft.ifBlank { MLang.NetworkSettings.RootTun.MtuSummary },
        onClick = onEditMtu,
    )
}

@Composable
private fun RootTunRoutingOptions(
    rootTunAutoRoute: Boolean,
    rootTunStrictRoute: Boolean,
    rootTunAutoRedirect: Boolean,
    rootTunDnsMode: RootTunDnsMode,
    onRootTunAutoRouteChange: (Boolean) -> Unit,
    onRootTunStrictRouteChange: (Boolean) -> Unit,
    onRootTunAutoRedirectChange: (Boolean) -> Unit,
    onRootTunDnsModeChange: (RootTunDnsMode) -> Unit,
) {
    PreferenceSwitchItem(
        title = MLang.NetworkSettings.RootTun.AutoRouteTitle,
        summary = MLang.NetworkSettings.RootTun.AutoRouteSummary,
        checked = rootTunAutoRoute,
        onCheckedChange = onRootTunAutoRouteChange,
    )
    PreferenceSwitchItem(
        title = MLang.NetworkSettings.RootTun.StrictRouteTitle,
        summary = MLang.NetworkSettings.RootTun.StrictRouteSummary,
        checked = rootTunStrictRoute,
        onCheckedChange = onRootTunStrictRouteChange,
    )
    PreferenceSwitchItem(
        title = MLang.NetworkSettings.RootTun.AutoRedirectTitle,
        summary = MLang.NetworkSettings.RootTun.AutoRedirectSummary,
        checked = rootTunAutoRedirect,
        onCheckedChange = onRootTunAutoRedirectChange,
    )
    PreferenceEnumItem(
        title = MLang.NetworkSettings.RootTun.DnsModeTitle,
        summary = MLang.NetworkSettings.RootTun.DnsModeSummary,
        currentValue = rootTunDnsMode,
        items =
            listOf(
                MLang.NetworkSettings.RootTun.DnsModeRedirHost,
                MLang.NetworkSettings.RootTun.DnsModeFakeIp,
            ),
        values = RootTunDnsMode.entries,
        onValueChange = onRootTunDnsModeChange,
    )
}

@Composable
private fun RootTunFakeIpOptions(
    showFakeIpRange: Boolean,
    rootTunFakeIpRangeDraft: String,
    rootTunFakeIpRange6Draft: String,
    onEditFakeIpRange: () -> Unit,
    onEditFakeIpRange6: () -> Unit,
) {
    AnimatedVisibility(
        visible = showFakeIpRange,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            PreferenceArrowItem(
                title = MLang.NetworkSettings.RootTun.FakeIpRangeTitle,
                summary =
                    rootTunFakeIpRangeDraft.ifBlank {
                        MLang.NetworkSettings.RootTun.FakeIpRangeSummary
                    },
                onClick = onEditFakeIpRange,
            )
            PreferenceArrowItem(
                title = MLang.NetworkSettings.RootTun.FakeIpRange6Title,
                summary =
                    rootTunFakeIpRange6Draft.ifBlank {
                        MLang.NetworkSettings.RootTun.FakeIpRange6Summary
                    },
                onClick = onEditFakeIpRange6,
            )
        }
    }
}

@Composable
private fun RootTunEditDialogs(
    editDialog: RootTunEditDialogState?,
    state: RootTunServiceOptionsUiState,
    actions: RootTunServiceOptionActions,
    onDismiss: () -> Unit,
) {
    when (editDialog) {
        RootTunEditDialogState.IfName ->
            RootTunTextEditDialog(
                title = MLang.NetworkSettings.RootTun.IfNameTitle,
                value = state.rootTunIfNameDraft,
                onValueChange = actions.onRootTunIfNameDraftChange,
                onDismiss = onDismiss,
                onCommit = actions.commitRootTunIfName,
            )

        RootTunEditDialogState.Mtu ->
            RootTunTextEditDialog(
                title = MLang.NetworkSettings.RootTun.MtuTitle,
                value = state.rootTunMtuDraft,
                onValueChange = actions.onRootTunMtuDraftChange,
                onDismiss = onDismiss,
                onCommit = actions.commitRootTunMtu,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )

        RootTunEditDialogState.FakeIpRange ->
            RootTunTextEditDialog(
                title = MLang.NetworkSettings.RootTun.FakeIpRangeTitle,
                value = state.rootTunFakeIpRangeDraft,
                onValueChange = actions.onRootTunFakeIpRangeDraftChange,
                onDismiss = onDismiss,
                onCommit = actions.commitRootTunFakeIpRange,
            )

        RootTunEditDialogState.FakeIpRange6 ->
            RootTunTextEditDialog(
                title = MLang.NetworkSettings.RootTun.FakeIpRange6Title,
                value = state.rootTunFakeIpRange6Draft,
                onValueChange = actions.onRootTunFakeIpRange6DraftChange,
                onDismiss = onDismiss,
                onCommit = actions.commitRootTunFakeIpRange6,
            )

        null -> Unit
    }
}

@Composable
private fun CommonTunServiceOptions(
    state: CommonTunOptionsUiState,
    actions: CommonTunOptionActions,
    extraOptions: @Composable ColumnScope.() -> Unit = {},
) {
    Column {
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.VpnOptions.BypassPrivateTitle,
            summary = MLang.NetworkSettings.VpnOptions.BypassPrivateSummary,
            checked = state.bypassPrivateNetwork,
            onCheckedChange = actions.onBypassPrivateNetworkChange,
        )
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.VpnOptions.DnsHijackTitle,
            summary = MLang.NetworkSettings.VpnOptions.DnsHijackSummary,
            checked = state.dnsHijack,
            onCheckedChange = actions.onDnsHijackChange,
        )
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.VpnOptions.EnableIpv6Title,
            summary = MLang.NetworkSettings.VpnOptions.EnableIpv6Summary,
            checked = state.enableIPv6,
            onCheckedChange = actions.onEnableIPv6Change,
        )
        PreferenceEnumItem(
            title = MLang.NetworkSettings.ProxyOptions.TunStackTitle,
            currentValue = state.tunStack,
            items = listOf("System", "GVisor", "Mixed"),
            values = TunStack.entries,
            onValueChange = actions.onTunStackChange,
        )
        extraOptions()
    }
}

@Composable
private fun RootTunTextEditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
) {
    val focusManager = LocalFocusManager.current
    var localTextFieldValue by
        remember(title) {
            mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
        }

    AppTextFieldDialog(
        show = true,
        title = title,
        textFieldValue = localTextFieldValue,
        onTextFieldValueChange = { updatedTextFieldValue ->
            localTextFieldValue = updatedTextFieldValue
            onValueChange(updatedTextFieldValue.text)
        },
        onDismissRequest = onDismiss,
        onConfirm = {
            onCommit()
            focusManager.clearFocus()
            onDismiss()
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions =
            KeyboardActions(
                onDone = {
                    onCommit()
                    onDismiss()
                    focusManager.clearFocus()
                }
            ),
    )
}

private data class CommonTunOptionActions(
    val onBypassPrivateNetworkChange: (Boolean) -> Unit,
    val onDnsHijackChange: (Boolean) -> Unit,
    val onEnableIPv6Change: (Boolean) -> Unit,
    val onTunStackChange: (TunStack) -> Unit,
)

private data class TunServiceOptionActions(
    val common: CommonTunOptionActions,
    val onAllowBypassChange: (Boolean) -> Unit,
    val onSystemProxyChange: (Boolean) -> Unit,
)

private data class RootTunServiceOptionActions(
    val common: CommonTunOptionActions,
    val onRootTunAutoRouteChange: (Boolean) -> Unit,
    val onRootTunStrictRouteChange: (Boolean) -> Unit,
    val onRootTunAutoRedirectChange: (Boolean) -> Unit,
    val onRootTunDnsModeChange: (RootTunDnsMode) -> Unit,
    val onRootTunIfNameDraftChange: (String) -> Unit,
    val onRootTunMtuDraftChange: (String) -> Unit,
    val onRootTunFakeIpRangeDraftChange: (String) -> Unit,
    val onRootTunFakeIpRange6DraftChange: (String) -> Unit,
    val commitRootTunIfName: () -> Unit,
    val commitRootTunMtu: () -> Unit,
    val commitRootTunFakeIpRange: () -> Unit,
    val commitRootTunFakeIpRange6: () -> Unit,
)

private enum class RootTunEditDialogState {
    IfName,
    Mtu,
    FakeIpRange,
    FakeIpRange6,
}
