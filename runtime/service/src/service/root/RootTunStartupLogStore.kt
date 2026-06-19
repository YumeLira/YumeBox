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

package com.github.yumelira.yumebox.service.root

import android.content.Context
import com.github.yumelira.yumebox.service.runtime.records.ProfileStore
import com.github.yumelira.yumebox.service.runtime.session.RuntimeStartupLogStore

class RootTunStartupLogStore(context: Context) {
    private val delegate = RuntimeStartupLogStore(context, RuntimeStartupLogStore.Scope.ROOT_TUN)

    fun path(): String = delegate.path()

    fun clear() {
        delegate.clear()
    }

    fun append(line: String) {
        delegate.append(line)
    }

    fun snapshot(): String {
        return delegate.snapshot()
    }

    fun formatProfilesStoreLine(): String {
        val keyCount = runCatching { ProfileStore.countStoredKeys() }.getOrDefault(0)

        return "<MMKV_IO.cpp:133::loadFromFile> loaded [profiles] with $keyCount key-values"
    }
}
