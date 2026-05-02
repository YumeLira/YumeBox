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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.common.constants.Components
import com.github.yumelira.yumebox.service.root.RootTunServiceBridge
import com.github.yumelira.yumebox.service.root.RootTunStateStore
import com.github.yumelira.yumebox.service.runtime.session.RuntimeServiceLauncher
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@SuppressLint("NewApi")
class ProxyTileService : TileService() {

    private val profileManager by lazy { ProfileManager(applicationContext) }
    private val clashManager by lazy { ClashManager(applicationContext) }
    private val networkSettingsStorage by lazy {
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings"))
    }
    private val rootTunStateStore by lazy { RootTunStateStore(applicationContext) }
    private val tileLabelText: String by lazy {
        applicationInfo.loadLabel(packageManager).toString().ifBlank { "YumeBox" }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null
    private var toggleJob: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateJob?.cancel()
        updateJob = scope.launch {
            PollingTimers.ticks(PollingTimerSpecs.ProxyTileRefresh).collect {
                updateTileState(currentSnapshot().running)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        updateJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        if (toggleJob?.isActive == true) return

        toggleJob = scope.launch {
            val snapshot = currentSnapshot()
            val isRunning = snapshot.running
            val currentMode = effectiveMode(snapshot)

            val tileState = qsTile?.state
            if ((isRunning && tileState == Tile.STATE_INACTIVE) || (!isRunning && tileState == Tile.STATE_ACTIVE)) {
                updateTileState(isRunning)
                return@launch
            }

            try {
                if (isRunning) {
                    updateTilePendingState(isStarting = false)
                    withContext(Dispatchers.IO) {
                        if (snapshot.owner == RuntimeOwner.RootTun || currentMode == ProxyMode.RootTun) {
                            val result = RootTunServiceBridge.stop(applicationContext)
                            if (!result.success) {
                                error(result.error ?: "RootTun stop failed")
                            }
                        } else {
                            clashManager.requestStop()
                        }
                    }
                } else {
                    val activeProfile = withContext(Dispatchers.IO) {
                        profileManager.queryActive()
                    }
                    if (activeProfile == null) {
                        updateTileInactiveState(subtitle = MLang.Service.Tile.ClickToOpen)

                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            component = Components.MAIN_ACTIVITY
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivityAndCollapseCompat(intent, requestCode = 1001)
                        return@launch
                    }

                    updateTilePendingState(isStarting = true)
                    when (currentMode) {
                        ProxyMode.Tun -> {
                            val vpnIntent = VpnService.prepare(this@ProxyTileService)
                            if (vpnIntent != null) {
                                updateTileInactiveState(subtitle = MLang.Service.Tile.ClickToOpen)
                                vpnIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivityAndCollapseCompat(vpnIntent, requestCode = 1002)
                                return@launch
                            }

                            RuntimeServiceLauncher.start(
                                this@ProxyTileService,
                                ProxyMode.Tun,
                                RuntimeServiceLauncher.SOURCE_TILE,
                            )
                        }
                        ProxyMode.RootTun -> {
                            val result = withContext(Dispatchers.IO) {
                                RootTunServiceBridge.start(applicationContext)
                            }
                            if (!result.success) {
                                error(result.error ?: "RootTun start failed")
                            }
                        }
                        ProxyMode.Http -> {
                            RuntimeServiceLauncher.start(
                                this@ProxyTileService,
                                ProxyMode.Http,
                                RuntimeServiceLauncher.SOURCE_TILE,
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                Timber.e(error, "Error toggling proxy from tile")
            } finally {
                PollingTimers.awaitTick(
                    PollingTimerSpecs.dynamic(
                        name = "proxy_tile_toggle_state_sync",
                        intervalMillis = 300L,
                        initialDelayMillis = 300L,
                    ),
                )
                updateTileState(currentSnapshot().running)
            }
        }
    }

    private fun currentSnapshot(): RuntimeSnapshot {
        val configuredMode = networkSettingsStorage.proxyMode.value
        val rootStatus = rootTunStateStore.snapshot()
        val tunPhase = StatusProvider.queryRuntimePhase(ProxyMode.Tun).toRuntimePhase()
        val httpPhase = StatusProvider.queryRuntimePhase(ProxyMode.Http).toRuntimePhase()
        val owner = when {
            rootStatus.state.isActive || rootStatus.runtimeReady -> RuntimeOwner.RootTun
            tunPhase != RuntimePhase.Idle -> RuntimeOwner.LocalTun
            httpPhase != RuntimePhase.Idle -> RuntimeOwner.LocalHttp
            else -> RuntimeOwner.None
        }

        return if (owner == RuntimeOwner.None) {
            RuntimeSnapshot(
                owner = RuntimeOwner.None,
                phase = RuntimePhase.Idle,
                targetMode = configuredMode,
            )
        } else {
            RuntimeSnapshot(
                owner = owner,
                phase = when (owner) {
                    RuntimeOwner.RootTun -> when (rootStatus.state) {
                        com.github.yumelira.yumebox.service.root.RootTunState.Idle -> RuntimePhase.Idle
                        com.github.yumelira.yumebox.service.root.RootTunState.Starting -> RuntimePhase.Starting
                        com.github.yumelira.yumebox.service.root.RootTunState.Running -> RuntimePhase.Running
                        com.github.yumelira.yumebox.service.root.RootTunState.Stopping -> RuntimePhase.Stopping
                        com.github.yumelira.yumebox.service.root.RootTunState.Failed -> RuntimePhase.Failed
                    }
                    RuntimeOwner.LocalTun -> tunPhase
                    RuntimeOwner.LocalHttp -> httpPhase
                    RuntimeOwner.None -> RuntimePhase.Idle
                },
                targetMode = modeForOwner(owner) ?: configuredMode,
            )
        }
    }

    private fun modeForOwner(owner: RuntimeOwner): ProxyMode? {
        return when (owner) {
            RuntimeOwner.LocalTun -> ProxyMode.Tun
            RuntimeOwner.LocalHttp -> ProxyMode.Http
            RuntimeOwner.RootTun -> ProxyMode.RootTun
            RuntimeOwner.None -> null
        }
    }

    private fun effectiveMode(snapshot: RuntimeSnapshot): ProxyMode {
        return when {
            snapshot.running -> when (snapshot.owner) {
                RuntimeOwner.LocalTun -> ProxyMode.Tun
                RuntimeOwner.LocalHttp -> ProxyMode.Http
                RuntimeOwner.RootTun -> ProxyMode.RootTun
                RuntimeOwner.None -> snapshot.targetMode
            }

            else -> snapshot.targetMode
        }
    }

    private fun LocalRuntimePhase.toRuntimePhase(): RuntimePhase {
        return when (this) {
            LocalRuntimePhase.Idle -> RuntimePhase.Idle
            LocalRuntimePhase.Starting -> RuntimePhase.Starting
            LocalRuntimePhase.Running -> RuntimePhase.Running
            LocalRuntimePhase.Stopping -> RuntimePhase.Stopping
            LocalRuntimePhase.Failed -> RuntimePhase.Failed
        }
    }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        tile.label = tileLabelText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isRunning) {
                MLang.Service.Tile.ClickToStopProxy
            } else {
                MLang.Service.Tile.ClickToStartProxy
            }
        }

        tile.icon = Icon.createWithResource(
            this,
            if (isRunning) R.drawable.ic_logo_service else R.drawable.ic_logo_service
        )

        tile.updateTile()
    }

    private fun updateTilePendingState(isStarting: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isStarting) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = tileLabelText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isStarting) {
                MLang.Service.Tile.Connecting
            } else {
                MLang.Service.Tile.Disconnecting
            }
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
        tile.updateTile()
    }

    private fun updateTileInactiveState(subtitle: String) {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = tileLabelText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)
        tile.updateTile()
    }

    private fun startActivityAndCollapseCompat(intent: Intent, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(this, requestCode, intent, pendingIntentFlags)
            startActivityAndCollapse(pendingIntent)
            return
        }

        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
    }
}
