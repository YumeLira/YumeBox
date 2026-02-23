package com.github.yumelira.yumebox.service.common.constants

import android.content.ComponentName
import com.github.yumelira.yumebox.service.common.util.packageName

object Components {
    val MAIN_ACTIVITY = ComponentName(packageName, "com.github.yumelira.yumebox.MainActivity")
    val PROXY_SHEET_ACTIVITY = ComponentName(
        packageName,
        "com.github.yumelira.yumebox.ProxySheetActivity"
    )
}
