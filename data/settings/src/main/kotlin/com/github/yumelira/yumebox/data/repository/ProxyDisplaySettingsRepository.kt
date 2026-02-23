package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxySortMode

class ProxyDisplaySettingsRepository(
    private val store: ProxyDisplaySettingsStore,
) {
    val proxyMode: Preference<TunnelState.Mode> = store.proxyMode
    val displayMode: Preference<ProxyDisplayMode> = store.displayMode
    val sortMode: Preference<ProxySortMode> = store.sortMode
    val sheetHeightFraction: Preference<Float> = store.sheetHeightFraction
}
