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

package com.github.yumelira.yumebox.service.runtime.config

import com.tencent.mmkv.MMKV

class MMKVStoreProvider(private val mmkv: MMKV) : StoreProvider {
    override fun getInt(key: String, defaultValue: Int): Int {
        return mmkv.decodeInt(key, defaultValue)
    }

    override fun setInt(key: String, value: Int) {
        mmkv.encode(key, value)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return mmkv.decodeLong(key, defaultValue)
    }

    override fun setLong(key: String, value: Long) {
        mmkv.encode(key, value)
    }

    override fun getString(key: String, defaultValue: String): String {
        return mmkv.decodeString(key, defaultValue) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        mmkv.encode(key, value)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        return mmkv.decodeStringSet(key, defaultValue) ?: defaultValue
    }

    override fun setStringSet(key: String, value: Set<String>) {
        mmkv.encode(key, value)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return mmkv.decodeBool(key, defaultValue)
    }

    override fun setBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
    }

    override fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    override fun clear() {
        mmkv.clearAll()
    }
}

fun MMKV.asStoreProvider(): StoreProvider {
    return MMKVStoreProvider(this)
}
