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

package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Imported
import java.util.*

object ImportedDao {

    fun queryAll(): List<Imported> {
        return ProfileStore.loadImported()
    }

    fun queryByUUID(uuid: UUID): Imported? {
        return ProfileStore.loadImported().find { it.uuid == uuid }
    }

    fun insert(imported: Imported): Long {
        val list = ProfileStore.loadImported().toMutableList()
        list.add(imported)
        ProfileStore.saveImported(list)
        return 1L
    }

    fun update(imported: Imported) {
        val list = ProfileStore.loadImported().toMutableList()
        val index = list.indexOfFirst { it.uuid == imported.uuid }
        if (index >= 0) {
            list[index] = imported
            ProfileStore.saveImported(list)
        }
    }

    fun remove(uuid: UUID) {
        val list = ProfileStore.loadImported().toMutableList()
        list.removeAll { it.uuid == uuid }
        ProfileStore.saveImported(list)
    }

    fun removeAll(uuids: Collection<UUID>) {
        val list = ProfileStore.loadImported().toMutableList()
        list.removeAll { it.uuid in uuids }
        ProfileStore.saveImported(list)
    }

    fun exists(uuid: UUID): Boolean {
        return ProfileStore.loadImported().any { it.uuid == uuid }
    }

    fun queryAllUUIDs(): List<UUID> {
        return ProfileStore.loadImported().map { it.uuid }
    }
}
