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



package com.github.yumelira.yumebox.data.gateway

import com.github.yumelira.yumebox.core.util.NetworkInterfaces
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Closeable

@Serializable
data class IpInfo(
    val ip: String,
    @SerialName("country_code")
    val countryCode: String? = null
)

sealed class IpMonitoringState {
    data class Success(val localIp: String?, val externalIp: IpInfo?, val isProxyActive: Boolean = false) :
        IpMonitoringState()

    data class Error(val message: String) : IpMonitoringState()
    object Loading : IpMonitoringState()
}

class NetworkInfoService : Closeable {
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 5000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val _refreshTrigger =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun close() {
        httpClient.close()
    }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    suspend fun getLocalIp(): String? {
        return NetworkInterfaces.getLocalIpAddress()
    }

    suspend fun getExternalIp(): IpInfo? {
        try {
            val response = httpClient.get("https://api.ip.sb/geoip")
            val body = response.bodyAsText()
            val info = json.decodeFromString<IpInfo>(body)
            return info
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            return null
        }
    }

    fun startIpMonitoring(
        isProxyActiveFlow: Flow<Boolean>,
        externalRefreshFlow: Flow<Unit> = emptyFlow(),
    ): Flow<IpMonitoringState> = flow {
        var lastSuccessfulState: IpMonitoringState.Success? = null

        try {
            val localIp = getLocalIp()
            val externalIp = getExternalIp()
            val newState = IpMonitoringState.Success(localIp, externalIp)
            lastSuccessfulState = newState
            emit(newState)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (lastSuccessfulState == null) {
                emit(IpMonitoringState.Error(error.message ?: "Unknown error"))
            }
        }

        val refreshFlow = merge(
            flowOf(Unit),
            _refreshTrigger,
            externalRefreshFlow,
            isProxyActiveFlow
                .distinctUntilChanged()
                .drop(1)
                .map { },
        )

        combine(refreshFlow, isProxyActiveFlow) { _, isProxyActive ->
            try {
                val localIp = getLocalIp()
                val externalIp = getExternalIp()
                val newState = IpMonitoringState.Success(localIp, externalIp, isProxyActive)
                lastSuccessfulState = newState
                newState
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastSuccessfulState?.copy(isProxyActive = isProxyActive)
                    ?: IpMonitoringState.Error(error.message ?: "Unknown error")
            }
        }.collect { state ->
            emit(state)
        }
    }
}
