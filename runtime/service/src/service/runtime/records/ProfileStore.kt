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
 * Copyright (c)  YumeLira & YumeRiMoe 2025 - Present
 *
 */

@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Imported
import com.github.yumelira.yumebox.service.runtime.entity.Selection
import com.github.yumelira.yumebox.service.runtime.util.UUIDSerializer
import com.tencent.mmkv.MMKV
import java.util.*
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object ProfileStore {
    private const val IMPORTED_KEY = "imported"
    private const val SELECTIONS_KEY = "selections"
    private const val PROFILE_ORDER_KEY = "profile_order"
    private const val SELECTION_SCOPE_KEY_PREFIX = "selection_scope_key:"
    // Per-key selection storage. Each Selection lives under its own MMKV key so that
    // cross-process writes to different selections never clobber each other (MMKV per-key
    // encode/decode is atomic and multi-process safe). The full Selection (group + node +
    // updatedAt + uuid) is serialized into the value, so groupName containing ':' is harmless:
    // the key is never parsed back into parts. The canonical 36-char UUID makes the
    // "sel:<uuid>:" prefix unambiguous for enumeration.
    private const val SELECTION_KEY_PREFIX = "sel:"
    private const val SELECTION_MEMORY_MIGRATION_VERSION_KEY = "selection_memory_migration_version"
    private const val SELECTION_MEMORY_MIGRATION_VERSION = 2

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

    private fun selectionKey(uuid: UUID, group: String): String =
        SELECTION_KEY_PREFIX + uuid.toString() + ":" + group

    private fun selectionScopePrefix(uuid: UUID): String =
        SELECTION_KEY_PREFIX + uuid.toString() + ":"

    private fun decodeSelection(key: String): Selection? {
        val jsonString = mmkv.decodeString(key) ?: return null
        return try {
            json.decodeFromString(Selection.serializer(), jsonString)
        } catch (error: Exception) {
            null
        }
    }

    /**
     * Compat helper (migration / bulk replace). Writes each selection as its own per-key entry.
     * Does NOT remove keys it doesn't mention — granular removal goes through [removeSelection] /
     * [clearSelections]. Callers that need replace-all semantics should clear first.
     */
    fun saveSelections(list: List<Selection>) {
        list.forEach(::putSelection)
    }

    fun putSelection(selection: Selection) {
        val jsonString = json.encodeToString(Selection.serializer(), selection)
        mmkv.encode(selectionKey(selection.uuid, selection.proxy), jsonString)
    }

    fun removeSelection(uuid: UUID, group: String) {
        mmkv.removeValueForKey(selectionKey(uuid, group))
    }

    fun loadSelections(): List<Selection> {
        return mmkv
            .allKeys()
            ?.asSequence()
            ?.filter { it.startsWith(SELECTION_KEY_PREFIX) }
            ?.mapNotNull(::decodeSelection)
            ?.toList()
            .orEmpty()
    }

    fun loadSelections(uuid: UUID): List<Selection> {
        val prefix = selectionScopePrefix(uuid)
        return mmkv
            .allKeys()
            ?.asSequence()
            ?.filter { it.startsWith(prefix) }
            ?.mapNotNull(::decodeSelection)
            ?.toList()
            .orEmpty()
    }

    fun clearSelections(uuid: UUID) {
        val prefix = selectionScopePrefix(uuid)
        mmkv
            .allKeys()
            ?.filter { it.startsWith(prefix) }
            ?.forEach(mmkv::removeValueForKey)
    }

    fun clearAllSelections() {
        mmkv
            .allKeys()
            ?.filter { it.startsWith(SELECTION_KEY_PREFIX) }
            ?.forEach(mmkv::removeValueForKey)
    }

    fun removeAllSelectionScopeKeys() {
        mmkv
            .allKeys()
            ?.filter { it.startsWith(SELECTION_SCOPE_KEY_PREFIX) }
            ?.forEach(mmkv::removeValueForKey)
    }

    private fun loadLegacySelectionBlob(): List<Selection> {
        val jsonString = mmkv.decodeString(SELECTIONS_KEY) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Selection.serializer()), jsonString)
        } catch (error: Exception) {
            emptyList()
        }
    }

    fun migrateLegacySelectionMemoryIfNeeded() {
        val currentVersion = mmkv.decodeInt(SELECTION_MEMORY_MIGRATION_VERSION_KEY, 0)
        if (currentVersion >= SELECTION_MEMORY_MIGRATION_VERSION) {
            return
        }

        // v0/v1 -> v2: the old single "selections" JSON blob (one whole-list write target,
        // the source of the cross-process RMW race) is converted into per-key entries.
        // Reads the legacy blob directly (loadSelections now enumerates per-key entries),
        // dedupes by (uuid, proxy) keeping max updatedAt, writes each as its own key, then
        // removes the old blob key. Idempotent: if the blob is already gone, nothing migrates.
        val migratedSelections =
            loadLegacySelectionBlob()
                .asSequence()
                .mapNotNull { selection ->
                    val groupName = selection.proxy.trim()
                    val selectedProxy = selection.selected.trim()
                    if (groupName.isEmpty() || selectedProxy.isEmpty()) {
                        null
                    } else {
                        selection.copy(proxy = groupName, selected = selectedProxy)
                    }
                }
                .groupBy { it.uuid to it.proxy }
                .values
                .mapNotNull { items ->
                    items.maxByOrNull(Selection::updatedAt) ?: items.lastOrNull()
                }

        migratedSelections.forEach(::putSelection)
        mmkv.removeValueForKey(SELECTIONS_KEY)
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
        if (mmkv.allKeys()?.any { it.startsWith(SELECTION_KEY_PREFIX) } == true) count++
        if (mmkv.decodeString(PROFILE_ORDER_KEY) != null) count++
        return count
    }

    fun clear() {
        mmkv.clearAll()
    }
}
