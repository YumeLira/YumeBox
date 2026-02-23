package com.github.yumelira.yumebox.presentation.util

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun WindowBlurEffect(useBlur: Boolean, blurRadius: Int = 30) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val view = LocalView.current
    val window = (view.parent as? View)?.context?.let { parentContext ->
        var currentContext: android.content.Context = parentContext
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                return@let currentContext.window
            }
            currentContext = currentContext.baseContext
        }
        null
    } ?: return

    SideEffect {
        if (useBlur) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = blurRadius
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 0
        }
        window.attributes = window.attributes
    }
}
