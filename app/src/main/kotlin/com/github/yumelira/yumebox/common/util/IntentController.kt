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
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.domain.facade.ProfilesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class IntentController(
    private val context: Context, private val scope: CoroutineScope
) : KoinComponent {

    companion object {
        private const val ACTION_START_CLASH = "com.github.yumelira.yumebox.action.START_CLASH"
        private const val ACTION_STOP_CLASH = "com.github.yumelira.yumebox.action.STOP_CLASH"
    }

    private val proxyConnectionService: ProxyConnectionService by inject()
    private val profilesRepository: ProfilesRepository by inject()
    private val networkSettingsStorage: NetworkSettingsStorage by inject()
    private val profilesStore: ProfilesStore by inject()
    private val clashManager: ClashManager by inject()

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
            val profiles = profilesRepository.profiles.value
            val recommendedProfile = profilesRepository.recommendedProfile.value
            val targetProfile = recommendedProfile ?: profiles.find { it.enabled }

            if (targetProfile == null) {
                Timber.w("No available profile to start Clash via external intent")
                return@launch
            }

            Timber.i("Starting Clash via external intent for profile: ${targetProfile.name}")

            val proxyMode = networkSettingsStorage.proxyMode.value
            val result = proxyConnectionService.startDirect(
                profileId = targetProfile.id, mode = proxyMode
            )

            if (result.isSuccess) {
                Timber.i("Clash started successfully via external intent")
            } else {
                Timber.e("Failed to start Clash via external intent: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun handleStopClash() {
        scope.launch {
            Timber.i("Stopping Clash via external intent")
            val currentRunningMode = clashManager.runningMode.value
            val result = proxyConnectionService.stop(currentRunningMode)
            result.onFailure { e ->
                Timber.e(e, "Failed to stop Clash via external intent")
            }
        }
    }
}
