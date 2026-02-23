package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Pending
import java.util.*

/**
 * DAO for Pending profile operations using MMKV
 */
object PendingDao {
    
    fun queryAll(): List<Pending> {
        return ProfileStore.loadPending()
    }

    fun queryByUUID(uuid: UUID): Pending? {
        return ProfileStore.loadPending().find { it.uuid == uuid }
    }

    fun insert(pending: Pending): Long {
        val list = ProfileStore.loadPending().toMutableList()
        list.add(pending)
        ProfileStore.savePending(list)
        return 1L
    }

    fun update(pending: Pending) {
        val list = ProfileStore.loadPending().toMutableList()
        val index = list.indexOfFirst { it.uuid == pending.uuid }
        if (index >= 0) {
            list[index] = pending
            ProfileStore.savePending(list)
        }
    }

    fun remove(uuid: UUID) {
        val list = ProfileStore.loadPending().toMutableList()
        list.removeAll { it.uuid == uuid }
        ProfileStore.savePending(list)
    }

    fun removeAll(uuids: Collection<UUID>) {
        val list = ProfileStore.loadPending().toMutableList()
        list.removeAll { it.uuid in uuids }
        ProfileStore.savePending(list)
    }

    fun exists(uuid: UUID): Boolean {
        return ProfileStore.loadPending().any { it.uuid == uuid }
    }

    fun queryAllUUIDs(): List<UUID> {
        return ProfileStore.loadPending().map { it.uuid }
    }
}
