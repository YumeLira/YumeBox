@file:Suppress("DEPRECATION")

package com.github.yumelira.yumebox.service.common.compat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Handler

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    filter: IntentFilter,
    permission: String? = null,
    handler: Handler? = null
) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        registerReceiver(receiver, filter, permission, handler,
            if (permission == null) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED
        )
    else
        registerReceiver(receiver, filter, permission, handler)
