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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.VpnUtils
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.NetworkSettingsViewModel
import com.github.yumelira.yumebox.service.NetworkServiceManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AccessControlScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch

@Composable
@Destination<RootGraph>
fun NetworkSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val serviceState by viewModel.serviceState.collectAsState()
    val context = LocalContext.current


    val proxyMode by viewModel.proxyMode.state.collectAsState()
    val bypassPrivateNetwork by viewModel.bypassPrivateNetwork.state.collectAsState()
    val dnsHijack by viewModel.dnsHijack.state.collectAsState()
    val allowBypass by viewModel.allowBypass.state.collectAsState()
    val enableIPv6 by viewModel.enableIPv6.state.collectAsState()
    val systemProxy by viewModel.systemProxy.state.collectAsState()
    val tunStack by viewModel.tunStack.state.collectAsState()
    val accessControlMode by viewModel.accessControlMode.state.collectAsState()


    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startService(proxyMode)
        } else {
            Toast.makeText(context, MLang.NetworkSettings.Error.VpnDenied, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = MLang.NetworkSettings.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                SmallTitle(MLang.NetworkSettings.Section.VpnService)
                Card {
                    EnumSelector(
                        title = MLang.NetworkSettings.VpnService.RouteTrafficTitle,
                        summary = MLang.NetworkSettings.VpnService.RouteTrafficSummary,
                        currentValue = proxyMode,
                        items = listOf(
                            MLang.NetworkSettings.VpnService.VpnMode,
                            MLang.NetworkSettings.VpnService.SystemProxy
                        ),
                        values = ProxyMode.entries,
                        onValueChange = { mode ->
                            if (mode == ProxyMode.Tun && !VpnUtils.checkVpnPermission(context)) {

                                val intent = VpnUtils.getVpnPermissionIntent(context)
                                if (intent != null) {
                                    vpnPermissionLauncher.launch(intent)
                                } else {

                                    viewModel.onProxyModeChange(mode)
                                }
                            } else {
                                viewModel.onProxyModeChange(mode)
                            }
                        },
                    )
                }

                SmallTitle(MLang.NetworkSettings.Section.VpnOptions)
                Card {
                    SuperSwitch(
                        title = MLang.NetworkSettings.VpnOptions.BypassPrivateTitle,
                        summary = MLang.NetworkSettings.VpnOptions.BypassPrivateSummary,
                        checked = bypassPrivateNetwork,
                        onCheckedChange = { viewModel.onBypassPrivateNetworkChange(it) }
                    )
                    SuperSwitch(
                        title = MLang.NetworkSettings.VpnOptions.DnsHijackTitle,
                        summary = MLang.NetworkSettings.VpnOptions.DnsHijackSummary,
                        checked = dnsHijack,
                        onCheckedChange = { viewModel.onDnsHijackChange(it) }
                    )
                    SuperSwitch(
                        title = MLang.NetworkSettings.VpnOptions.AllowBypassTitle,
                        summary = MLang.NetworkSettings.VpnOptions.AllowBypassSummary,
                        checked = allowBypass,
                        onCheckedChange = { viewModel.onAllowBypassChange(it) }
                    )
                    SuperSwitch(
                        title = MLang.NetworkSettings.VpnOptions.EnableIpv6Title,
                        summary = MLang.NetworkSettings.VpnOptions.EnableIpv6Summary,
                        checked = enableIPv6,
                        onCheckedChange = { viewModel.onEnableIPv6Change(it) }
                    )
                    SuperSwitch(
                        title = MLang.NetworkSettings.VpnOptions.SystemProxyTitle,
                        summary = MLang.NetworkSettings.VpnOptions.SystemProxySummary,
                        checked = systemProxy,
                        onCheckedChange = { viewModel.onSystemProxyChange(it) }
                    )
                }

                SmallTitle(MLang.NetworkSettings.Section.ProxyOptions)
                Card {
                    EnumSelector(
                        title = MLang.NetworkSettings.ProxyOptions.TunStackTitle,
                        currentValue = tunStack,
                        items = listOf("System", "GVisor", "Mixed"),
                        values = TunStack.entries,
                        onValueChange = { viewModel.onTunStackChange(it) },
                    )
                    EnumSelector(
                        title = MLang.NetworkSettings.ProxyOptions.AccessControlModeTitle,
                        currentValue = accessControlMode,
                        items = listOf(
                            MLang.NetworkSettings.ProxyOptions.AllowAll,
                            MLang.NetworkSettings.ProxyOptions.AllowSelected,
                            MLang.NetworkSettings.ProxyOptions.RejectSelected
                        ),
                        values = AccessControlMode.entries,
                        onValueChange = { viewModel.onAccessControlModeChange(it) },
                    )
                    SuperArrow(
                        title = MLang.NetworkSettings.ProxyOptions.ManageAccessControlTitle,
                        summary = MLang.NetworkSettings.ProxyOptions.ManageAccessControlSummary,
                        onClick = {

                            navigator.navigate(AccessControlScreenDestination)
                        }
                    )
                }


                if (uiState.needsRestart && serviceState == NetworkServiceManager.ServiceState.Running) {
                    Card {
                        SuperArrow(
                            title = MLang.NetworkSettings.RestartService.Title,
                            summary = MLang.NetworkSettings.RestartService.Summary,
                            onClick = { viewModel.restartService() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
