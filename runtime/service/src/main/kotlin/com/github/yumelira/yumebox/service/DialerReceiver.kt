package com.github.yumelira.yumebox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import com.github.yumelira.yumebox.service.common.constants.Components
import timber.log.Timber

class DialerReceiver : BroadcastReceiver() {

    companion object {
        private const val SECRET_CODE = "*#*#0721#*#*"
        private const val ACTION_NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.provider.Telephony.SECRET_CODE" -> {
                startMainActivity(context)
            }

            ACTION_NEW_OUTGOING_CALL -> {
                val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                if (SECRET_CODE == phoneNumber) {
                    setResultData(null)
                    startMainActivity(context)
                }
            }
        }
    }

    private fun startMainActivity(context: Context) {
        try {
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                component = Components.MAIN_ACTIVITY
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launchIntent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "启动主界面失败")
        } catch (e: SecurityException) {
            Timber.e(e, "启动主界面失败")
        }
    }
}
