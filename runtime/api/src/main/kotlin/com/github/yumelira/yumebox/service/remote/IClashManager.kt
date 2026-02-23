package com.github.yumelira.yumebox.service.remote

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.*

interface IClashManager {
    fun queryTunnelState(): TunnelState
    fun queryTrafficNow(): Long
    fun queryTrafficTotal(): Long
    fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String>
    fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup
    fun queryConfiguration(): UiConfiguration
    fun queryProviders(): ProviderList

    fun patchSelector(group: String, name: String): Boolean

    suspend fun healthCheck(group: String)
    suspend fun updateProvider(type: Provider.Type, name: String)

    fun queryOverride(slot: Clash.OverrideSlot): ConfigurationOverride
    fun patchOverride(slot: Clash.OverrideSlot, configuration: ConfigurationOverride)
    fun clearOverride(slot: Clash.OverrideSlot)

    fun requestStop()

    fun setLogObserver(observer: ILogObserver?)
}
