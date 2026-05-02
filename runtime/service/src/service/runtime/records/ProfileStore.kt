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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Imported
import com.github.yumelira.yumebox.service.runtime.entity.Selection
import com.github.yumelira.yumebox.service.runtime.util.UUIDSerializer
import com.tencent.mmkv.MMKV
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.*

object ProfileStore {
    private const val IMPORTED_KEY = "imported"
    private const val SELECTIONS_KEY = "selections"
    private const val PROFILE_ORDER_KEY = "profile_order"
    private const val SELECTION_SCOPE_KEY_PREFIX = "selection_scope_key:"
    private const val SELECTION_MEMORY_MIGRATION_VERSION_KEY = "selection_memory_migration_version"
    private const val SELECTION_MEMORY_MIGRATION_VERSION = 1

    private val mmkv by lazy { MMKV.mmkvWithID("profiles", MMKV.MULTI_PROCESS_MODE) }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun saveImported(list: List<Imported>) {
        val jsonString = json.encodeToString(ListSerializer(Imported.serializer()), list)
        mmkv.encode("imported", jsonString)
    }

    fun loadImported(): List<Imported> {
        val jsonString = mmkv.decodeString(IMPORTED_KEY) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Imported.serializer()), jsonString)
        } catch (error: Exception) {
            emptyList()
        }
    }

    fun saveSelections(list: List<Selection>) {
        val jsonString = json.encodeToString(ListSerializer(Selection.serializer()), list)
        mmkv.encode(SELECTIONS_KEY, jsonString)
    }

    fun loadSelections(): List<Selection> {
        val jsonString = mmkv.decodeString(SELECTIONS_KEY) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Selection.serializer()), jsonString)
        } catch (error: Exception) {
            emptyList()
        }
    }

    fun removeAllSelectionScopeKeys() {
        mmkv.allKeys()
            ?.filter { it.startsWith(SELECTION_SCOPE_KEY_PREFIX) }
            ?.forEach(mmkv::removeValueForKey)
    }

    fun migrateLegacySelectionMemoryIfNeeded() {
        val currentVersion = mmkv.decodeInt(SELECTION_MEMORY_MIGRATION_VERSION_KEY, 0)
        if (currentVersion >= SELECTION_MEMORY_MIGRATION_VERSION) {
            return
        }

        val migratedSelections = loadSelections()
            .asSequence()
            .mapNotNull { selection ->
                val groupName = selection.proxy.trim()
                val selectedProxy = selection.selected.trim()
                if (groupName.isEmpty() || selectedProxy.isEmpty()) {
                    null
                } else {
                    selection.copy(
                        proxy = groupName,
                        selected = selectedProxy,
                    )
                }
            }
            .groupBy { it.uuid to it.proxy }
            .values
            .mapNotNull { items -> items.maxByOrNull(Selection::updatedAt) ?: items.lastOrNull() }

        saveSelections(migratedSelections)
        removeAllSelectionScopeKeys()
        mmkv.encode(SELECTION_MEMORY_MIGRATION_VERSION_KEY, SELECTION_MEMORY_MIGRATION_VERSION)
    }

    fun saveProfileOrder(order: List<UUID>) {
        val jsonString = json.encodeToString(ListSerializer(UUIDSerializer()), order)
        mmkv.encode("profile_order", jsonString)
    }

    fun loadProfileOrder(): List<UUID> {
        val jsonString = mmkv.decodeString(PROFILE_ORDER_KEY) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(UUIDSerializer()), jsonString)
        } catch (error: Exception) {
            emptyList()
        }
    }

    fun countStoredKeys(): Int {
        var count = 0
        if (mmkv.decodeString(IMPORTED_KEY) != null) count++
        if (mmkv.decodeString(SELECTIONS_KEY) != null) count++
        if (mmkv.decodeString(PROFILE_ORDER_KEY) != null) count++
        return count
    }

    fun clear() {
        mmkv.clearAll()
    }
}
