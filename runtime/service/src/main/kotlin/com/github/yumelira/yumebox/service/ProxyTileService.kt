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
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.yumelira.yumebox.runtime.service.R
import com.github.yumelira.yumebox.service.common.constants.Components
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import timber.log.Timber

@SuppressLint("NewApi")
class ProxyTileService : TileService() {

    companion object {
        private const val TAG = "ProxyTileService"
    }

    private val profileManager by lazy { ProfileManager(applicationContext) }
    private val clashManager by lazy { ClashManager(applicationContext) }
    private val tileLabelText: String by lazy {
        applicationInfo.loadLabel(packageManager)?.toString().orEmpty().ifBlank { "YumeBox" }
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
            while (isActive) {
                updateTileState(StatusProvider.serviceRunning)
                delay(500)
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
            val isRunning = StatusProvider.serviceRunning

            // Sync with actual state if inconsistent
            val tileState = qsTile?.state
            if ((isRunning && tileState == Tile.STATE_INACTIVE) || (!isRunning && tileState == Tile.STATE_ACTIVE)) {
                updateTileState(isRunning)
                return@launch
            }

            try {
                if (isRunning) {
                    updateTilePendingState(isStarting = false)
                    withContext(Dispatchers.IO) {
                        clashManager.requestStop()
                    }
                } else {
                    val activeProfile = withContext(Dispatchers.IO) {
                        profileManager.queryActive()
                    }
                    if (activeProfile == null) {
                        updateTileInactiveState(subtitle = MLang.Service.Tile.ClickToOpen)
                        // Open app to select profile
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            component = Components.MAIN_ACTIVITY
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivityAndCollapseCompat(intent, requestCode = 1001)
                        return@launch
                    }

                    val vpnIntent = VpnService.prepare(this@ProxyTileService)
                    if (vpnIntent != null) {
                        updateTileInactiveState(subtitle = MLang.Service.Tile.ClickToOpen)
                        vpnIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivityAndCollapseCompat(vpnIntent, requestCode = 1002)
                        return@launch
                    }

                    updateTilePendingState(isStarting = true)
                    val startIntent = Intent(this@ProxyTileService, TunService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(startIntent)
                    } else {
                        startService(startIntent)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling proxy from tile")
            } finally {
                delay(300)
                updateTileState(StatusProvider.serviceRunning)
            }
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

