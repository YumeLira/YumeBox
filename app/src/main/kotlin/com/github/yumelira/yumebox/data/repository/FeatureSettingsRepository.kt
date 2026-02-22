package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.Preference

class FeatureSettingsRepository(
    private val store: FeatureStore,
) {
    val allowLanAccess: Preference<Boolean> = store.allowLanAccess
    val backendPort: Preference<Int> = store.backendPort
    val frontendPort: Preference<Int> = store.frontendPort
    val selectedPanelType: Preference<Int> = store.selectedPanelType
    val panelOpenMode: Preference<LinkOpenMode> = store.panelOpenMode
}
