package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference

class NetworkSettingsRepository(
    private val storage: NetworkSettingsStorage,
) {
    val proxyMode: Preference<ProxyMode> = storage.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = storage.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = storage.dnsHijack
    val allowBypass: Preference<Boolean> = storage.allowBypass
    val enableIPv6: Preference<Boolean> = storage.enableIPv6
    val systemProxy: Preference<Boolean> = storage.systemProxy
    val tunStack: Preference<TunStack> = storage.tunStack
    val accessControlMode: Preference<AccessControlMode> = storage.accessControlMode
    val accessControlPackages: Preference<Set<String>> = storage.accessControlPackages
}
