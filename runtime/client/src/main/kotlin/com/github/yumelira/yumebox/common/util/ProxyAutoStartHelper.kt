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

import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.delay
import timber.log.Timber

object ProxyAutoStartHelper {

    private const val TAG = "ProxyAutoStartHelper"

    suspend fun checkAndAutoStart(
        proxyFacade: ProxyFacade,
        profilesRepository: ProfilesRepository,
        appSettingsStorage: AppSettingsStorage,
        networkSettingsStorage: NetworkSettingsStorage,
        serviceCache: MMKV,
        isBootCompleted: Boolean = false
    ) {
        runCatching {
            val automaticRestart = appSettingsStorage.automaticRestart.value
            if (!automaticRestart) {
                return
            }

            if (serviceCache.decodeBool("service_running", false)) {
                return
            }

            val profileId = getProfileToStart(profilesRepository)
            if (profileId == null) {
                Timber.tag(TAG).w("没有可用的配置文件，无法自动启动")
                return
            }

            if (isBootCompleted) {
                delay(3000)
            }

            val useTun = networkSettingsStorage.proxyMode.value == com.github.yumelira.yumebox.data.model.ProxyMode.Tun
            
            // Set active profile and start
            profilesRepository.setActiveProfile(java.util.UUID.fromString(profileId))
            proxyFacade.startProxy(useTun)
            
            Timber.tag(TAG).i("自动启动代理成功: profileId=$profileId, useTun=$useTun")
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "自动启动代理失败: ${e.message}")
        }
    }

    private suspend fun getProfileToStart(profilesRepository: ProfilesRepository): String? {
        val activeProfile = profilesRepository.queryActiveProfile()
        
        // 只允许自动启动已激活的配置，默认不自动选择任何配置
        if (activeProfile != null) {
            return activeProfile.uuid.toString()
        }

        return null
    }
}

