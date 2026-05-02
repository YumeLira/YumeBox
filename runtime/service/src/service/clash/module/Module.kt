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



package com.github.yumelira.yumebox.service.clash.module

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.yumelira.yumebox.service.common.compat.registerReceiverCompat
import com.github.yumelira.yumebox.service.common.log.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.withContext

abstract class Module<E>(val service: Service) {

    private val events: Channel<E> = Channel(Channel.CONFLATED)
    private val receivers: MutableList<Pair<Context, BroadcastReceiver>> = mutableListOf()

    val onEvent: SelectClause1<E>
        get() = events.onReceive

    protected suspend fun enqueueEvent(event: E) {
        events.send(event)
    }

    protected fun receiveBroadcast(
        capacity: Int = Channel.CONFLATED,
        configure: IntentFilter.() -> Unit
    ): ReceiveChannel<Intent> {
        val filter = IntentFilter().apply(configure)
        val channel = Channel<Intent>(capacity)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) {
                    channel.close()

                    return
                }

                channel.trySend(intent)
            }
        }

        val registerContext = service.applicationContext
        registerContext.registerReceiverCompat(receiver, filter)

        receivers.add(registerContext to receiver)

        return channel
    }

    suspend fun execute() {
        val moduleName = this.javaClass.simpleName

        try {
            run()
        } finally {
            withContext(NonCancellable) {
                val registeredReceivers = receivers.toList()
                receivers.clear()

                registeredReceivers.forEach { (context, receiver) ->
                    receiver.onReceive(null, null)

                    runCatching {
                        context.unregisterReceiver(receiver)
                    }.onFailure { error ->
                        Log.w("$moduleName: unregisterReceiver ignored", error)
                    }
                }
            }
        }
    }

    protected abstract suspend fun run()
}
