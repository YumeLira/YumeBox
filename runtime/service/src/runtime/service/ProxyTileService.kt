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

package com.github.yumelira.yumebox.runtime.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.util.AutoStartSessionGate
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.store.RemoteControllerStore
import com.github.yumelira.yumebox.runtime.api.Components
import com.github.yumelira.yumebox.runtime.api.Intents
import com.github.yumelira.yumebox.runtime.api.RuntimeOwner
import com.github.yumelira.yumebox.runtime.api.RuntimePhase
import com.github.yumelira.yumebox.runtime.api.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.service.profile.ProfileManager
import com.github.yumelira.yumebox.runtime.service.root.RootTunServiceBridge
import com.github.yumelira.yumebox.runtime.service.root.RootTunStatusFlow
import com.github.yumelira.yumebox.runtime.service.session.RuntimeServiceLauncher
import com.github.yumelira.yumebox.runtime.service.util.sendBroadcastSelf
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
    private val networkSettingsStorage by lazy {
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings"))
    }
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
                updateTileState(currentSnapshot().phase.isActiveOrStopping)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        updateJob?.cancel()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onClick() {
        super.onClick()
        if (toggleJob?.isActive == true) return

        toggleJob = scope.launch {
            if (RemoteControllerStore.isActive()) {
                updateTileState(true)
                return@launch
            }
            val snapshot = currentSnapshot()
            val isActive = snapshot.phase.isActiveOrStopping
            val currentMode = effectiveMode(snapshot)

            // If the tile visual state is stale vs the actual runtime state, sync it
            // immediately but still perform the user's requested action — the user's tap
            // is their intent to toggle, not just to reconcile state.
            val tileState = qsTile?.state
            val tileStaleInactive = isActive && tileState == Tile.STATE_INACTIVE
            val tileStaleActive = !isActive && tileState == Tile.STATE_ACTIVE
            if (tileStaleInactive || tileStaleActive) {
                updateTileState(isActive)
            }

            try {
                if (isActive) {
                    AutoStartSessionGate.markManualPaused()
                    updateTilePendingState(isStarting = false)
                    withContext(Dispatchers.IO) {
                        if (
                            snapshot.owner == RuntimeOwner.RootTun ||
                                currentMode == ProxyMode.RootTun
                        ) {
                            val result = RootTunServiceBridge.stop(applicationContext)
                            if (!result.success) {
                                error(result.error ?: "RootTun stop failed")
                            }
                        } else {
                            stopLocalRuntime()
                        }
                    }
                } else {
                    val activeProfile = withContext(Dispatchers.IO) { profileManager.queryActive() }
                    if (activeProfile == null) {
                        updateTileInactiveState(subtitle = MLang.Service.Tile.ClickToOpen)

                        val intent =
                            Intent(Intent.ACTION_MAIN).apply {
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
                            val result =
                                withContext(Dispatchers.IO) {
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
                // fault barrier: toggle spans root bridge / service start; the tile must recover
                // to the real runtime state instead of crashing the SystemUI-bound service.
                Timber.e(error, "Error toggling proxy from tile")
            } finally {
                PollingTimers.awaitTick(
                    PollingTimerSpecs.dynamic(
                        name = "proxy_tile_toggle_state_sync",
                        intervalMillis = 300L,
                        initialDelayMillis = 300L,
                    )
                )
                updateTileState(currentSnapshot().phase.isActiveOrStopping)
            }
        }
    }

    private fun currentSnapshot(): RuntimeSnapshot {
        val configuredMode = networkSettingsStorage.proxyMode.value
        val rootStatus = RootTunStatusFlow.current(applicationContext)
        val tunPhase = StatusProvider.queryRuntimePhase(ProxyMode.Tun)
        val httpPhase = StatusProvider.queryRuntimePhase(ProxyMode.Http)
        val owner =
            when {
                rootStatus.isSessionActive -> RuntimeOwner.RootTun
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
                phase =
                    when (owner) {
                        RuntimeOwner.RootTun -> rootStatus.state
                        RuntimeOwner.LocalTun -> tunPhase
                        RuntimeOwner.LocalHttp -> httpPhase
                        RuntimeOwner.RemoteController -> RuntimePhase.Running
                        RuntimeOwner.None -> RuntimePhase.Idle
                    },
                targetMode = modeForOwner(owner) ?: configuredMode,
            )
        }
    }

    private fun modeForOwner(owner: RuntimeOwner): ProxyMode? =
        when (owner) {
            RuntimeOwner.LocalTun -> ProxyMode.Tun
            RuntimeOwner.LocalHttp -> ProxyMode.Http
            RuntimeOwner.RootTun -> ProxyMode.RootTun
            RuntimeOwner.RemoteController -> null
            RuntimeOwner.None -> null
        }

    // Mirrors the home-screen stop path: ask the runtime service to stop, then apply the same
    // local core/service fallback used by LocalClashManager/ProxyRuntimeControl.
    private fun stopLocalRuntime() {
        runCatching { sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP)) }
        runCatching {
            applicationContext.stopService(Intent(applicationContext, TunService::class.java))
            applicationContext.stopService(Intent(applicationContext, ClashService::class.java))
        }
        // Fallback for an orphaned core only; a live service owns its own core teardown.
        if (
            !StatusProvider.isLocalRuntimeServiceAlive(ProxyMode.Tun) &&
                !StatusProvider.isLocalRuntimeServiceAlive(ProxyMode.Http)
        ) {
            runCatching {
                Clash.stopHttp()
                Clash.stopTun()
                Clash.reset()
            }
        }
    }

    private fun effectiveMode(snapshot: RuntimeSnapshot): ProxyMode =
        if (snapshot.phase.isActiveOrStopping) {
            modeForOwner(snapshot.owner) ?: snapshot.targetMode
        } else {
            snapshot.targetMode
        }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        tile.label = tileLabelText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle =
                if (isRunning) {
                    MLang.Service.Tile.ClickToStopProxy
                } else {
                    MLang.Service.Tile.ClickToStartProxy
                }
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_logo_service)

        tile.updateTile()
    }

    private fun updateTilePendingState(isStarting: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isStarting) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = tileLabelText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle =
                if (isStarting) {
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
            val pendingIntentFlags =
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent =
                PendingIntent.getActivity(this, requestCode, intent, pendingIntentFlags)
            startActivityAndCollapse(pendingIntent)
            return
        }

        @Suppress("DEPRECATION") startActivityAndCollapse(intent)
    }
}
