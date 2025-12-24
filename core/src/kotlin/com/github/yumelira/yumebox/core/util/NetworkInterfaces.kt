package com.github.yumelira.yumebox.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkInterfaces {
    suspend fun getLocalIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    val addresses = networkInterface.inetAddresses
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address is InetAddress) {
                            val hostAddress = address.hostAddress
                            if (hostAddress?.contains(':') == false) {
                                return@withContext hostAddress
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}