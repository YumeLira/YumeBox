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
import android.net.*
import android.os.Build
import androidx.core.content.getSystemService
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.runtime.util.asSocketAddressText
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class NetworkObserveModule(service: Service) : Module<Network>(service) {
    private val connectivity = service.getSystemService<ConnectivityManager>()
    private val networks: Channel<Network> = Channel(Channel.CONFLATED)
    private val request = NetworkRequest.Builder().apply {
        addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
        }
        addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    }.build()

    private data class NetworkInfo(
        @Volatile var losingMs: Long = 0,
        @Volatile var dnsList: List<InetAddress> = emptyList()
    ) {
        fun isAvailable(): Boolean = losingMs < System.currentTimeMillis()
    }

    private val networkInfos = ConcurrentHashMap<Network, NetworkInfo>()

    @Volatile
    private var curDnsList = emptyList<String>()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            networkInfos[network] = NetworkInfo()
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            networkInfos[network]?.losingMs = System.currentTimeMillis() + maxMsToLive
            notifyDnsChange()

            networks.trySend(network)
        }

        override fun onLost(network: Network) {
            networkInfos.remove(network)
            notifyDnsChange()

            networks.trySend(network)
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            networkInfos[network]?.dnsList = linkProperties.dnsServers
            notifyDnsChange()

            networks.trySend(network)
        }

        override fun onUnavailable() {}
    }

    private fun register(): Boolean {
        val connectivityManager = connectivity ?: run {
            Log.w("Register network callback failed: ConnectivityManager unavailable")
            return false
        }
        return try {
            connectivityManager.registerNetworkCallback(request, callback)
            true
        } catch (error: Exception) {
            Log.w("Register network callback failed", error)
            false
        }
    }

    private fun unregister(): Boolean {
        val connectivityManager = connectivity ?: return false
        try {
            connectivityManager.unregisterNetworkCallback(callback)
            return true
        } catch (error: Exception) {
            Log.w("Unregister network callback failed", error)
        }

        return false
    }

    private fun networkToInt(entry: Map.Entry<Network, NetworkInfo>): Int {
        val capabilities = connectivity?.getNetworkCapabilities(entry.key)

        return when {
            capabilities == null -> 100
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> 90
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 0
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 1
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB) -> 2
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> 3
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 4
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE) -> 5

            else -> 20
        } + (if (entry.value.isAvailable()) 0 else 10)
    }

    private fun notifyDnsChange() {
        val dnsList = (networkInfos.asSequence().minByOrNull { networkToInt(it) }?.value?.dnsList
            ?: emptyList()).map { address -> address.asSocketAddressText(53) }
        val prevDnsList = curDnsList
        if (dnsList.isNotEmpty() && prevDnsList != dnsList) {
            curDnsList = dnsList
            Clash.notifyDnsChanged(dnsList)
        }
    }

    override suspend fun run() {
        register()

        try {
            while (true) {
                val quit = select {
                    networks.onReceive {
                        enqueueEvent(it)

                        false
                    }
                }
                if (quit) {
                    return
                }
            }
        } finally {
            withContext(NonCancellable) {
                unregister()
                Clash.notifyDnsChanged(emptyList())
            }
        }
    }
}
