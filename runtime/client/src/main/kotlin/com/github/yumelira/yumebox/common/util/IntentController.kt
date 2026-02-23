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

package com.github.yumelira.yumebox.common.util

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID

class IntentController(
    private val context: Context, private val scope: CoroutineScope
) : KoinComponent {

    companion object {
        private const val ACTION_START_CLASH = "com.github.yumelira.yumebox.action.START_CLASH"
        private const val ACTION_STOP_CLASH = "com.github.yumelira.yumebox.action.STOP_CLASH"
    }

    private val proxyFacade: ProxyFacade by inject()
    private val profilesRepository: ProfilesRepository by inject()
    private val networkSettingsStorage: NetworkSettingsStorage by inject()

    fun handleIntent(intent: Intent?) {
        intent?.let { safeIntent ->
            when (safeIntent.action) {
                ACTION_START_CLASH -> handleStartClash()
                ACTION_STOP_CLASH -> handleStopClash()
                else -> {
                }
            }
        }
    }

    private fun handleStartClash() {
        scope.launch {
            runCatching {
                val activeProfile = profilesRepository.queryActiveProfile()
                if (activeProfile == null) {
                    Timber.w("No active profile, ignore external START_CLASH")
                    return@launch
                }

                Timber.i("Starting Clash via external intent for profile: ${activeProfile.name}")
                 
                // Start proxy with TUN based on network settings
                val useTun = networkSettingsStorage.proxyMode.value == com.github.yumelira.yumebox.data.model.ProxyMode.Tun
                proxyFacade.startProxy(useTun)
                
                Timber.i("Clash started successfully via external intent")
            }.onFailure { e ->
                Timber.e(e, "Failed to start Clash via external intent")
            }
        }
    }

    private fun handleStopClash() {
        scope.launch {
            Timber.i("Stopping Clash via external intent")
            try {
                proxyFacade.stopProxy()
                Timber.i("Clash stopped successfully via external intent")
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop Clash via external intent")
            }
        }
    }
}

