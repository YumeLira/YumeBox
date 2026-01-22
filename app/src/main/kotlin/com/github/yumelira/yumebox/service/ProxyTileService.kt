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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.domain.model.RunningMode
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ProxyTileService : TileService() {

    companion object {
        private const val TAG = "ProxyTileService"
    }

    private val clashManager: ClashManager by inject()
    private val profilesStore: ProfilesStore by inject()
    private val proxyConnectionService: ProxyConnectionService by inject()

    private var serviceScope: CoroutineScope? = null
    private var stateObserverJob: Job? = null
    private val isOperating = AtomicBoolean(false)

    override fun onStartListening() {
        super.onStartListening()

        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        startObservingState()
    }

    override fun onStopListening() {
        super.onStopListening()

        stateObserverJob?.cancel()
        stateObserverJob = null
        serviceScope?.cancel()
        serviceScope = null
    }

    override fun onClick() {
        super.onClick()

        if (!isOperating.compareAndSet(false, true)) {
            return
        }

        val isRunning = clashManager.isRunning.value

        if (isRunning) {
            stopProxy()
        } else {
            startProxy()
        }
    }

    private fun startProxy() {
        val profile = profilesStore.getRecommendedProfile()

        if (profile == null) {
            Timber.tag(TAG).w("没有可用的配置文件，打开应用")
            openApp()
            isOperating.set(false)
            return
        }

        updateTileState(TileState.CONNECTING)

        serviceScope?.launch(Dispatchers.IO) {
            runCatching {
                val result = proxyConnectionService.prepareAndStart(profile.id)

                result.fold(onSuccess = { intent ->
                    if (intent != null) {
                        openApp()
                    }
                }, onFailure = { error ->
                    Timber.tag(TAG).e(error, "启动代理失败")
                    updateTileState(TileState.DISCONNECTED)
                })
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "启动代理异常")
                updateTileState(TileState.DISCONNECTED)
            }
            isOperating.set(false)
        }
    }

    private fun stopProxy() {
        updateTileState(TileState.DISCONNECTING)

        serviceScope?.launch(Dispatchers.IO) {
            val currentMode = clashManager.runningMode.value
            val result = proxyConnectionService.stop(currentMode)
            result.onFailure { e ->
                Timber.tag(TAG).e(e, "停止代理异常")
            }
            isOperating.set(false)
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(intent)
        } else {
            @Suppress("DEPRECATION") startActivityAndCollapse(intent)
        }
    }

    private fun startObservingState() {
        stateObserverJob = serviceScope?.launch {
            combine(
                clashManager.isRunning, clashManager.runningMode, profilesStore.profiles
            ) { isRunning, runningMode, profiles ->
                Triple(isRunning, runningMode, profiles)
            }.collect { (isRunning, _, profiles) ->
                val hasProfile = profiles.isNotEmpty()

                val state = when {
                    !hasProfile -> TileState.UNAVAILABLE
                    isRunning -> TileState.CONNECTED
                    else -> TileState.DISCONNECTED
                }

                if (!isOperating.get()) {
                    updateTileState(state)
                }
            }
        }
    }

    private fun updateTileState(state: TileState) {
        val updateBlock = updateBlock@{
            val tile = qsTile ?: return@updateBlock

            when (state) {
            TileState.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = MLang.Service.Tile.Connected
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getRunningModeText()
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }

            TileState.DISCONNECTED -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = MLang.Service.Tile.Disconnected
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = null
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }

            TileState.CONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = MLang.Service.Tile.Connecting
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = null
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }

            TileState.DISCONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = MLang.Service.Tile.Disconnecting
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = null
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }

            TileState.UNAVAILABLE -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = MLang.Service.Tile.Unconfigured
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = MLang.Service.Tile.ClickToOpen
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
            }
        }

            tile.updateTile()
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateBlock()
        } else {
            serviceScope?.launch(Dispatchers.Main.immediate) { updateBlock() }
        }
    }

    private fun getRunningModeText(): String {
        return when (clashManager.runningMode.value) {
            is RunningMode.Tun -> MLang.Service.Tile.ModeVpn
            is RunningMode.Http -> MLang.Service.Tile.ModeHttp
            is RunningMode.None -> ""
        }
    }

    private enum class TileState {
        CONNECTED, DISCONNECTED, CONNECTING, DISCONNECTING, UNAVAILABLE
    }
}
