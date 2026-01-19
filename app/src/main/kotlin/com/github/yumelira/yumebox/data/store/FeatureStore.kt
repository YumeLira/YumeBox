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

package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV

class FeatureStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val allowLanAccess by boolFlow(false)
    val backendPort by intFlow(8081)
    val frontendPort by intFlow(8080)
    val selectedPanelType by intFlow(0)
    val showWebControlInProxy by boolFlow(false)


    var isFirstOpen by bool(true)

    fun markFirstOpenHandled() {
        isFirstOpen = false
    }

    fun isFirstTimeOpen(): Boolean = isFirstOpen

    fun setLibraryCacheVersion(libraryName: String, version: Int) {
        mmkv.encode("library_version_" + libraryName, version)
    }

    fun getLibraryCacheVersion(libraryName: String): Int {
        return mmkv.decodeInt("library_version_" + libraryName, -1)
    }

    fun setAssetCacheVersion(assetPath: String, version: Int) {
        mmkv.encode("asset_version_" + assetPath, version)
    }

    fun getAssetCacheVersion(assetPath: String): Int {
        return mmkv.decodeInt("asset_version_" + assetPath, -1)
    }

    fun clearAll() {
        mmkv.edit().clear().apply()
    }
}
