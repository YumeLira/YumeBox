@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Imported
import com.github.yumelira.yumebox.service.runtime.entity.Pending
import com.github.yumelira.yumebox.service.runtime.entity.Selection
import com.github.yumelira.yumebox.service.runtime.util.UUIDSerializer
import com.tencent.mmkv.MMKV
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * MMKV-based storage manager for profile data
 */
object ProfileStore {
    private val mmkv by lazy { MMKV.mmkvWithID("profiles", MMKV.MULTI_PROCESS_MODE) }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Imported operations
    fun saveImported(list: List<Imported>) {
        val jsonString = json.encodeToString(ListSerializer(Imported.serializer()), list)
        mmkv.encode("imported", jsonString)
    }

    fun loadImported(): List<Imported> {
        val jsonString = mmkv.decodeString("imported") ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Imported.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Pending operations
    fun savePending(list: List<Pending>) {
        val jsonString = json.encodeToString(ListSerializer(Pending.serializer()), list)
        mmkv.encode("pending", jsonString)
    }

    fun loadPending(): List<Pending> {
        val jsonString = mmkv.decodeString("pending") ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Pending.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Selection operations
    fun saveSelections(list: List<Selection>) {
        val jsonString = json.encodeToString(ListSerializer(Selection.serializer()), list)
        mmkv.encode("selections", jsonString)
    }

    fun loadSelections(): List<Selection> {
        val jsonString = mmkv.decodeString("selections") ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Selection.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Profile order operations
    fun saveProfileOrder(order: List<UUID>) {
        val jsonString = json.encodeToString(ListSerializer(UUIDSerializer()), order)
        mmkv.encode("profile_order", jsonString)
    }

    fun loadProfileOrder(): List<UUID> {
        val jsonString = mmkv.decodeString("profile_order") ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(UUIDSerializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear() {
        mmkv.clearAll()
    }
}
