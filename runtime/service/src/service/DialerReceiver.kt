/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                    resultData = null
                    startMainActivity(context)
                }
            }
        }
    }

    private fun startMainActivity(context: Context) {
        runCatching {
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                component = Components.MAIN_ACTIVITY
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launchIntent)
        }.onFailure { error ->
            Timber.e(error, "Open main activity failed")
        }
    }
}
