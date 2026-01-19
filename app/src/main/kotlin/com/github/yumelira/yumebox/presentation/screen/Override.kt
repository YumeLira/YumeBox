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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Restore

@Composable
@Destination<RootGraph>
fun OverrideScreen(navigator: DestinationsNavigator) {
    val viewModel: OverrideViewModel = koinViewModel()
    val scrollBehavior = MiuixScrollBehavior()

    val configuration by viewModel.configuration.collectAsState()
    val showResetDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Override.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 24.dp), onClick = { showResetDialog.value = true }) {
                        Icon(MiuixIcons.Useful.Restore, contentDescription = MLang.Component.Navigation.Refresh)
                    }
                },
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                SmallTitle(MLang.Override.Section.General)
                Card {
                    PortInput(
                        title = MLang.Override.General.HttpPort,
                        value = configuration.httpPort,
                        onValueChange = { viewModel.setHttpPort(it) },
                    )
                    PortInput(
                        title = MLang.Override.General.SocksPort,
                        value = configuration.socksPort,
                        onValueChange = { viewModel.setSocksPort(it) },
                    )
                    PortInput(
                        title = MLang.Override.General.MixedPort,
                        value = configuration.mixedPort,
                        onValueChange = { viewModel.setMixedPort(it) },
                    )
                    PortInput(
                        title = MLang.Override.General.RedirectPort,
                        value = configuration.redirectPort,
                        onValueChange = { viewModel.setRedirectPort(it) },
                    )
                    PortInput(
                        title = MLang.Override.General.TproxyPort,
                        value = configuration.tproxyPort,
                        onValueChange = { viewModel.setTproxyPort(it) },
                    )
                    NullableBooleanSelector(
                        title = MLang.Override.General.AllowLan,
                        value = configuration.allowLan,
                        onValueChange = { viewModel.setAllowLan(it) },
                    )
                    NullableBooleanSelector(
                        title = MLang.Override.General.Ipv6,
                        value = configuration.ipv6,
                        onValueChange = { viewModel.setIpv6(it) },
                    )
                    NullableEnumSelector(
                        title = MLang.Override.General.ProxyMode,
                        value = configuration.mode,
                        items = listOf(
                            MLang.Component.Selector.NotModify,
                            MLang.Proxy.Mode.Direct,
                            MLang.Proxy.Mode.Global,
                            MLang.Proxy.Mode.Rule
                        ),
                        values = listOf(null, TunnelState.Mode.Direct, TunnelState.Mode.Global, TunnelState.Mode.Rule),
                        onValueChange = { viewModel.setMode(it) },
                    )
                    NullableEnumSelector(
                        title = MLang.Override.General.LogLevel,
                        value = configuration.logLevel,
                        items = listOf(
                            MLang.Component.Selector.NotModify,
                            "Info",
                            "Warning",
                            "Error",
                            "Debug",
                            "Silent"
                        ),
                        values = listOf(
                            null,
                            LogMessage.Level.Info,
                            LogMessage.Level.Warning,
                            LogMessage.Level.Error,
                            LogMessage.Level.Debug,
                            LogMessage.Level.Silent
                        ),
                        onValueChange = { viewModel.setLogLevel(it) },
                    )
                }
            }

            item {
                SmallTitle(MLang.Override.Section.AuthHosts)
                Card {
                    StringListInput(
                        title = MLang.Override.AuthHosts.Authentication,
                        value = configuration.authentication,
                        placeholder = MLang.Override.AuthHosts.AuthenticationHint,
                        navigator = navigator,
                        onValueChange = { viewModel.setAuthentication(it) },
                    )
                    StringMapInput(
                        title = MLang.Override.AuthHosts.HostsMapping,
                        value = configuration.hosts,
                        keyPlaceholder = MLang.Override.AuthHosts.HostsKeyHint,
                        valuePlaceholder = MLang.Override.AuthHosts.HostsValueHint,
                        navigator = navigator,
                        onValueChange = { viewModel.setHosts(it) },
                    )
                }
            }

            item {
                SmallTitle(MLang.Override.Section.ExternalController)
                Card {
                    StringInput(
                        title = MLang.Override.ExternalController.Address,
                        value = configuration.externalController,
                        placeholder = MLang.Override.ExternalController.AddressHint,
                        onValueChange = { viewModel.setExternalController(it) },
                    )
                    StringInput(
                        title = MLang.Override.ExternalController.Tls,
                        value = configuration.externalControllerTLS,
                        placeholder = MLang.Override.ExternalController.TlsHint,
                        onValueChange = { viewModel.setExternalControllerTLS(it) },
                    )
                    StringInput(
                        title = MLang.Override.ExternalController.ApiSecret,
                        value = configuration.secret,
                        placeholder = MLang.Override.ExternalController.ApiSecretHint,
                        onValueChange = { viewModel.setSecret(it) },
                    )
                    StringListInput(
                        title = MLang.Override.ExternalController.CorsAllowOrigins,
                        value = configuration.externalControllerCors.allowOrigins,
                        placeholder = MLang.Override.ExternalController.CorsAllowOriginsHint,
                        navigator = navigator,
                        onValueChange = { viewModel.setExternalControllerCorsAllowOrigins(it) },
                    )
                    NullableBooleanSelector(
                        title = MLang.Override.ExternalController.CorsAllowPrivate,
                        value = configuration.externalControllerCors.allowPrivateNetwork,
                        onValueChange = { viewModel.setExternalControllerCorsAllowPrivateNetwork(it) },
                    )
                }
            }

            item {
                SmallTitle(MLang.Override.Section.Dns)
                Card {
                    NullableEnumSelector(
                        title = MLang.Override.Dns.Policy,
                        value = configuration.dns.enable,
                        items = listOf(
                            MLang.Override.Dns.PolicyNotModify,
                            MLang.Override.Dns.PolicyForceEnable,
                            MLang.Override.Dns.PolicyUseBuiltin
                        ),
                        values = listOf(null, true, false),
                        onValueChange = { viewModel.setDnsEnable(it) },
                    )
                    if (configuration.dns.enable != false) {
                        NullableBooleanSelector(
                            title = MLang.Override.Dns.PreferH3,
                            value = configuration.dns.preferH3,
                            onValueChange = { viewModel.setDnsPreferH3(it) },
                        )
                        StringInput(
                            title = MLang.Override.Dns.Listen,
                            value = configuration.dns.listen,
                            placeholder = MLang.Override.Dns.ListenHint,
                            onValueChange = { viewModel.setDnsListen(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.Override.Dns.Ipv6,
                            value = configuration.dns.ipv6,
                            onValueChange = { viewModel.setDnsIpv6(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.Override.Dns.UseHosts,
                            value = configuration.dns.useHosts,
                            onValueChange = { viewModel.setDnsUseHosts(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.Override.Dns.AppendSystem,
                            value = configuration.app.appendSystemDns,
                            onValueChange = { viewModel.setAppendSystemDns(it) },
                        )
                        NullableEnumSelector(
                            title = MLang.Override.Dns.EnhancedMode,
                            value = configuration.dns.enhancedMode,
                            items = listOf(
                                MLang.Override.Dns.EnhancedNotModify,
                                MLang.Override.Dns.EnhancedDisable,
                                MLang.Override.Dns.EnhancedFakeip,
                                MLang.Override.Dns.EnhancedMapping
                            ),
                            values = listOf(
                                null,
                                ConfigurationOverride.DnsEnhancedMode.None,
                                ConfigurationOverride.DnsEnhancedMode.FakeIp,
                                ConfigurationOverride.DnsEnhancedMode.Mapping
                            ),
                            onValueChange = { viewModel.setDnsEnhancedMode(it) },
                        )
                        StringListInput(
                            title = MLang.Override.Dns.Servers,
                            value = configuration.dns.nameServer,
                            placeholder = MLang.Override.Dns.ServersHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setDnsNameServer(it) },
                        )
                        StringListInput(
                            title = MLang.Override.Dns.Fallback,
                            value = configuration.dns.fallback,
                            placeholder = MLang.Override.Dns.FallbackHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setDnsFallback(it) },
                        )
                        StringListInput(
                            title = MLang.Override.Dns.Default,
                            value = configuration.dns.defaultServer,
                            placeholder = MLang.Override.Dns.DefaultHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setDnsDefaultServer(it) },
                        )
                        StringListInput(
                            title = MLang.Override.Dns.FakeipFilter,
                            value = configuration.dns.fakeIpFilter,
                            placeholder = MLang.Override.Dns.FakeipFilterHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setDnsFakeIpFilter(it) },
                        )
                        NullableEnumSelector(
                            title = MLang.Override.Dns.FakeipFilterMode,
                            value = configuration.dns.fakeIPFilterMode,
                            items = listOf(
                                MLang.Override.Dns.EnhancedNotModify,
                                MLang.Override.Dns.FakeipBlacklist,
                                MLang.Override.Dns.FakeipWhitelist
                            ),
                            values = listOf(
                                null,
                                ConfigurationOverride.FilterMode.BlackList,
                                ConfigurationOverride.FilterMode.WhiteList
                            ),
                            onValueChange = { viewModel.setDnsFakeIpFilterMode(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.Override.Dns.FallbackGeoip,
                            value = configuration.dns.fallbackFilter.geoIp,
                            onValueChange = { viewModel.setDnsFallbackGeoIp(it) },
                        )
                        StringInput(
                            title = MLang.Override.Dns.FallbackGeoipCode,
                            value = configuration.dns.fallbackFilter.geoIpCode,
                            placeholder = MLang.Override.Dns.FallbackGeoipCodeHint,
                            onValueChange = { viewModel.setDnsFallbackGeoIpCode(it) },
                        )
                        StringListInput(
                            title = MLang.Override.Dns.FallbackDomain,
                            value = configuration.dns.fallbackFilter.domain,
                            placeholder = MLang.Override.Dns.FallbackDomainHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setDnsFallbackDomain(it) },
                        )
                        StringListInput(
                            title = MLang.Override.Dns.FallbackIpcidr,
                            value = configuration.dns.fallbackFilter.ipcidr,
                            placeholder = MLang.Override.Dns.FallbackIpcidrHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setDnsFallbackIpcidr(it) },
                        )
                        StringMapInput(
                            title = MLang.Override.Dns.NameserverPolicy,
                            value = configuration.dns.nameserverPolicy,
                            keyPlaceholder = MLang.Override.Dns.NameserverPolicyKey,
                            valuePlaceholder = MLang.Override.Dns.NameserverPolicyValue,
                            navigator = navigator,
                            onValueChange = { viewModel.setDnsNameserverPolicy(it) },
                        )
                    }
                }
            }
        }
    }

    ConfirmDialog(
        show = showResetDialog,
        title = MLang.Override.ResetDialog.Title,
        message = MLang.Override.ResetDialog.Message,
        onConfirm = {
            viewModel.resetConfiguration()
            showResetDialog.value = false
        },
        onDismiss = { showResetDialog.value = false },
    )
}
