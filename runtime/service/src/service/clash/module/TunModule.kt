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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.service.clash.module

import android.net.VpnService
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.util.parseInetSocketAddress
import com.github.yumelira.yumebox.service.common.util.SocketOwnerResolver
import java.net.InetSocketAddress
import java.security.SecureRandom
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class TunModule(private val vpn: VpnService) : Module<Unit>(vpn) {
    data class TunDevice(
        val fd: Int,
        var stack: String,
        val gateway: String,
        val portal: String,
        val dns: String,
    )

    private val ownerResolver = SocketOwnerResolver(vpn)
    private val close = Channel<Unit>(Channel.CONFLATED)

    override suspend fun run() {
        try {
            return close.receive()
        } finally {
            withContext(NonCancellable) { requestStop() }
        }
    }

    fun listenHttp(): InetSocketAddress? {
        val r = { 1 + random.nextInt(199) }
        val listenAt = "127.${r()}.${r()}.${r()}:0"
        val address = Clash.startHttp(listenAt)

        return address?.let(::parseInetSocketAddress)
    }

    fun attach(device: TunDevice) {
        Clash.startTun(
            fd = device.fd,
            stack = device.stack,
            gateway = device.gateway,
            portal = device.portal,
            dns = device.dns,
            markSocket = vpn::protect,
            querySocketOwner = ownerResolver::queryOwner,
        )
    }

    suspend fun close() {
        close.send(Unit)
    }

    companion object {
        private val random = SecureRandom()

        fun requestStop() {
            Clash.stopHttp()
            Clash.stopTun()
        }
    }
}
