package com.github.yumelira.yumebox.service.common.util

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.tencent.mmkv.MMKV

object CoreRuntimeConfig {
    private const val SETTINGS_STORE_ID = "settings"
    private const val CUSTOM_USER_AGENT_KEY = "customUserAgent"

    fun applyCustomUserAgentIfPresent(context: Context) {
        val settings = MMKV.mmkvWithID(SETTINGS_STORE_ID, MMKV.MULTI_PROCESS_MODE) ?: return
        val userAgent = settings.decodeString(CUSTOM_USER_AGENT_KEY, "").orEmpty()
        if (userAgent.isNotBlank()) {
            Clash.setCustomUserAgent(userAgent)
        }
    }
}
