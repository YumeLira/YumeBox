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

package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.TunnelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class OverrideViewModel : ViewModel() {

    companion object {
        private const val TAG = "OverrideViewModel"
    }

    private val _configuration = MutableStateFlow(ConfigurationOverride())
    val configuration: StateFlow<ConfigurationOverride> = _configuration.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)

    init {
        loadConfiguration()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            runCatching {
                _isLoading.value = true
                val config = Clash.queryOverride(Clash.OverrideSlot.Persist)
                _configuration.value = config
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "加载覆写配置失败")
            }
            _isLoading.value = false
        }
    }

    fun resetConfiguration() {
        viewModelScope.launch {
            runCatching {
                Clash.clearOverride(Clash.OverrideSlot.Persist)
                _configuration.value = ConfigurationOverride()
                _hasChanges.value = false
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "重置覆写配置失败")
            }
        }
    }


    fun setHttpPort(port: Int?) {
        updateConfig { it.copy(httpPort = port) }
    }

    fun setSocksPort(port: Int?) {
        updateConfig { it.copy(socksPort = port) }
    }

    fun setMixedPort(port: Int?) {
        updateConfig { it.copy(mixedPort = port) }
    }

    fun setRedirectPort(port: Int?) {
        updateConfig { it.copy(redirectPort = port) }
    }

    fun setTproxyPort(port: Int?) {
        updateConfig { it.copy(tproxyPort = port) }
    }

    fun setAllowLan(allow: Boolean?) {
        updateConfig { it.copy(allowLan = allow) }
    }

    fun setIpv6(enabled: Boolean?) {
        updateConfig { it.copy(ipv6 = enabled) }
    }

    fun setMode(mode: TunnelState.Mode?) {
        updateConfig { it.copy(mode = mode) }
    }

    fun setLogLevel(level: LogMessage.Level?) {
        updateConfig { it.copy(logLevel = level) }
    }

    fun setExternalController(address: String?) {
        updateConfig { it.copy(externalController = address) }
    }

    fun setExternalControllerTLS(address: String?) {
        updateConfig { it.copy(externalControllerTLS = address) }
    }

    fun setSecret(secret: String?) {
        updateConfig { it.copy(secret = secret) }
    }


    fun setUnifiedDelay(enabled: Boolean?) {
        updateConfig { it.copy(unifiedDelay = enabled) }
    }

    fun setGeodataMode(enabled: Boolean?) {
        updateConfig { it.copy(geodataMode = enabled) }
    }

    fun setTcpConcurrent(enabled: Boolean?) {
        updateConfig { it.copy(tcpConcurrent = enabled) }
    }

    fun setFindProcessMode(mode: ConfigurationOverride.FindProcessMode?) {
        updateConfig { it.copy(findProcessMode = mode) }
    }


    fun setDnsEnable(enable: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(enable = enable))
        }
    }

    fun setDnsPreferH3(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(preferH3 = enabled))
        }
    }

    fun setDnsListen(address: String?) {
        updateConfig {
            it.copy(dns = it.dns.copy(listen = address))
        }
    }

    fun setDnsIpv6(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(ipv6 = enabled))
        }
    }

    fun setDnsUseHosts(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(useHosts = enabled))
        }
    }

    fun setDnsEnhancedMode(mode: ConfigurationOverride.DnsEnhancedMode?) {
        updateConfig {
            it.copy(dns = it.dns.copy(enhancedMode = mode))
        }
    }

    fun setDnsNameServer(servers: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(nameServer = servers))
        }
    }

    fun setDnsFallback(servers: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(fallback = servers))
        }
    }

    fun setDnsDefaultServer(servers: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(defaultServer = servers))
        }
    }

    fun setDnsFakeIpFilter(filters: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(fakeIpFilter = filters))
        }
    }

    fun setDnsFakeIpFilterMode(mode: ConfigurationOverride.FilterMode?) {
        updateConfig {
            it.copy(dns = it.dns.copy(fakeIPFilterMode = mode))
        }
    }

    fun setDnsFallbackGeoIp(enabled: Boolean?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(geoIp = enabled)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsFallbackGeoIpCode(code: String?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(geoIpCode = code)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsFallbackDomain(domains: List<String>?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(domain = domains)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsFallbackIpcidr(cidrs: List<String>?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(ipcidr = cidrs)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsNameserverPolicy(policy: Map<String, String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(nameserverPolicy = policy))
        }
    }

    fun setAppendSystemDns(enabled: Boolean?) {
        updateConfig {
            it.copy(app = it.app.copy(appendSystemDns = enabled))
        }
    }


    fun setAuthentication(auth: List<String>?) {
        updateConfig { it.copy(authentication = auth) }
    }

    fun setHosts(hosts: Map<String, String>?) {
        updateConfig { it.copy(hosts = hosts) }
    }


    fun setExternalControllerCorsAllowOrigins(origins: List<String>?) {
        updateConfig {
            it.copy(externalControllerCors = it.externalControllerCors.copy(allowOrigins = origins))
        }
    }

    fun setExternalControllerCorsAllowPrivateNetwork(allow: Boolean?) {
        updateConfig {
            it.copy(externalControllerCors = it.externalControllerCors.copy(allowPrivateNetwork = allow))
        }
    }


    fun setSnifferEnable(enable: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(enable = enable))
        }
    }

    fun setSnifferForceDnsMapping(enabled: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(forceDnsMapping = enabled))
        }
    }

    fun setSnifferParsePureIp(enabled: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(parsePureIp = enabled))
        }
    }

    fun setSnifferOverrideDestination(enabled: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(overrideDestination = enabled))
        }
    }

    fun setSnifferHttpPorts(ports: List<String>?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(http = it.sniffer.sniff.http.copy(ports = ports))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferHttpOverride(enabled: Boolean?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(http = it.sniffer.sniff.http.copy(overrideDestination = enabled))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferTlsPorts(ports: List<String>?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(tls = it.sniffer.sniff.tls.copy(ports = ports))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferTlsOverride(enabled: Boolean?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(tls = it.sniffer.sniff.tls.copy(overrideDestination = enabled))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferQuicPorts(ports: List<String>?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(quic = it.sniffer.sniff.quic.copy(ports = ports))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferQuicOverride(enabled: Boolean?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(quic = it.sniffer.sniff.quic.copy(overrideDestination = enabled))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferForceDomain(domains: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(forceDomain = domains))
        }
    }

    fun setSnifferSkipDomain(domains: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(skipDomain = domains))
        }
    }

    fun setSnifferSkipSrcAddress(addresses: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(skipSrcAddress = addresses))
        }
    }

    fun setSnifferSkipDstAddress(addresses: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(skipDstAddress = addresses))
        }
    }


    private fun updateConfig(transform: (ConfigurationOverride) -> ConfigurationOverride) {
        _configuration.value = transform(_configuration.value)
        _hasChanges.value = true
    }

    override fun onCleared() {
        super.onCleared()
        if (_hasChanges.value) {
            runCatching {
                Clash.patchOverride(Clash.OverrideSlot.Persist, _configuration.value)
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "自动保存配置失败")
            }
        }
    }
}
