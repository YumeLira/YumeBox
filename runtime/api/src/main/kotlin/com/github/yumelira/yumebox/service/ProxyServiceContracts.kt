package com.github.yumelira.yumebox.service

object ProxyServiceContracts {
    const val ACTION_PROXY_STARTED = "com.github.yumelira.yumebox.action.PROXY_STARTED"
    const val ACTION_PROXY_STOPPED = "com.github.yumelira.yumebox.action.PROXY_STOPPED"
    const val ACTION_PROXY_GROUPS_UPDATED = "com.github.yumelira.yumebox.action.PROXY_GROUPS_UPDATED"
    const val ACTION_PROFILE_LOADED = "com.github.yumelira.yumebox.action.PROFILE_LOADED"
    const val ACTION_PROFILE_CHANGED = "com.github.yumelira.yumebox.action.PROFILE_CHANGED"
    const val ACTION_REQUEST_STOP = "com.github.yumelira.yumebox.action.REQUEST_STOP"

    const val ACTION_PATCH_SELECTOR = "com.github.yumelira.yumebox.action.PATCH_SELECTOR"
    const val EXTRA_GROUP_NAME = "group_name"
    const val EXTRA_PROXY_NAME = "proxy_name"
    const val EXTRA_PROFILE_ID = "profile_id"
    const val EXTRA_START_PROXY = "start_proxy"

    const val ACTION_PATCH_OVERRIDE = "com.github.yumelira.yumebox.action.PATCH_OVERRIDE"
    const val ACTION_CLEAR_OVERRIDE = "com.github.yumelira.yumebox.action.CLEAR_OVERRIDE"
    const val EXTRA_OVERRIDE_SLOT = "override_slot"
    const val EXTRA_OVERRIDE_CONFIG = "override_config"

    const val ACTION_HEALTH_CHECK = "com.github.yumelira.yumebox.action.HEALTH_CHECK"
    const val ACTION_HEALTH_CHECK_ALL = "com.github.yumelira.yumebox.action.HEALTH_CHECK_ALL"
    const val EXTRA_HEALTH_CHECK_GROUP = "health_check_group"

    fun intentSelf(action: String, packageName: String? = null): android.content.Intent {
        return android.content.Intent(action).apply {
            if (!packageName.isNullOrBlank()) {
                setPackage(packageName)
            }
        }
    }
}
