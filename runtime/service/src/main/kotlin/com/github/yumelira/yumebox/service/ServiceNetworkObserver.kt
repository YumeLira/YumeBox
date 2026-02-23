package com.github.yumelira.yumebox.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.github.yumelira.yumebox.core.Clash
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

private data class NetworkInfo(
    @Volatile var losingMs: Long = 0,
    @Volatile var dnsList: List<InetAddress> = emptyList()
) {
    fun isAvailable(): Boolean = losingMs < System.currentTimeMillis()
}

class ServiceNetworkObserver(context: Context) {
    private val networkInfos = ConcurrentHashMap<Network, NetworkInfo>()
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private var lastDnsList = emptyList<String>()

    private val request = NetworkRequest.Builder().apply {
        addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
        }
        addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    }.build()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            networkInfos[network] = NetworkInfo()
            updateDns()
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            networkInfos[network]?.losingMs = System.currentTimeMillis() + maxMsToLive
            updateDns()
        }

        override fun onLost(network: Network) {
            networkInfos.remove(network)
            updateDns()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            networkInfos[network]?.dnsList = linkProperties.dnsServers
            updateDns()
        }
    }

    fun start() {
        updateDns()
        connectivity?.registerNetworkCallback(request, callback)
    }

    fun stop() {
        runCatching { connectivity?.unregisterNetworkCallback(callback) }
        networkInfos.clear()
        updateDns()
    }

    private fun updateDns() {
        val dnsList = selectDns().map { it.asSocketAddressText(53) }
        if (dnsList == lastDnsList) return
        lastDnsList = dnsList
        Clash.notifyDnsChanged(dnsList)
    }

    private fun selectDns(): List<InetAddress> {
        val entry = networkInfos.asSequence().minByOrNull { networkToScore(it) }
        return entry?.value?.dnsList ?: emptyList()
    }

    private fun networkToScore(entry: Map.Entry<Network, NetworkInfo>): Int {
        val capabilities = connectivity?.getNetworkCapabilities(entry.key)
        return when {
            capabilities == null -> 100
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> 90
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 0
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 1
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB) -> 2
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> 3
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 4
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE) -> 5
            else -> 20
        } + if (entry.value.isAvailable()) 0 else 10
    }
}

private fun InetAddress.asSocketAddressText(port: Int): String = when (this) {
    is Inet6Address -> "[${numericToTextFormat(this)}]:$port"
    is Inet4Address -> "${this.hostAddress}:$port"
    else -> throw IllegalArgumentException("Unsupported Inet type ${this.javaClass}")
}

private fun numericToTextFormat(address: Inet6Address): String {
    val src = address.address
    val sb = StringBuilder(39)
    for (i in 0 until 8) {
        sb.append(
            Integer.toHexString(
                src[i shl 1].toInt() shl 8 and 0xff00 or (src[(i shl 1) + 1].toInt() and 0xff)
            )
        )
        if (i < 7) {
            sb.append(":")
        }
    }
    if (address.scopeId > 0) {
        sb.append("%")
        sb.append(address.scopeId)
    }
    return sb.toString()
}
