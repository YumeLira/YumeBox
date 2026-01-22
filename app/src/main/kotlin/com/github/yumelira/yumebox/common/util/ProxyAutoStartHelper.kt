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

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import kotlinx.coroutines.delay
import timber.log.Timber

object ProxyAutoStartHelper {

    private const val TAG = "ProxyAutoStartHelper"

    suspend fun checkAndAutoStart(
        proxyConnectionService: ProxyConnectionService,
        appSettingsStorage: AppSettingsStorage,
        networkSettingsStorage: NetworkSettingsStorage,
        profilesStore: ProfilesStore,
        clashManager: ClashManager,
        isBootCompleted: Boolean = false
    ) {
        runCatching {
            val automaticRestart = appSettingsStorage.automaticRestart.value
            if (!automaticRestart) {
                return
            }

            if (clashManager.isRunning.value) {
                return
            }

            val profileId = getProfileToStart(profilesStore)
            if (profileId == null) {
                Timber.tag(TAG).w("没有可用的配置文件，无法自动启动")
                return
            }

            if (isBootCompleted) {
                delay(3000)
            }

            val proxyMode = networkSettingsStorage.proxyMode.value

            val result = proxyConnectionService.startDirect(
                profileId = profileId, mode = proxyMode
            )

            if (result.isFailure) {
                Timber.tag(TAG).e("自动启动代理失败: ${result.exceptionOrNull()?.message}")
            }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "自动启动代理失败: ${e.message}")
        }
    }

    private fun getProfileToStart(profilesStore: ProfilesStore): String? {
        val profiles = profilesStore.getAllProfiles()
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty()) {
            val lastUsedProfile = profiles.find { it.id == lastUsedId }
            if (lastUsedProfile != null) {
                return lastUsedId
            }
        }

        val enabledProfile = profiles.find { it.enabled }
        if (enabledProfile != null) {
            return enabledProfile.id
        }

        val firstProfile = profiles.firstOrNull()
        if (firstProfile != null) {
            return firstProfile.id
        }

        return null
    }
}
