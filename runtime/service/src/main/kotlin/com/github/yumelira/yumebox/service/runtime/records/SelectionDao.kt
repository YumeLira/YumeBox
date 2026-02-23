package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Selection
import java.util.*

/**
 * DAO for Selection operations using MMKV
 */
object SelectionDao {
    
    fun queryAll(): List<Selection> {
        return ProfileStore.loadSelections()
    }

    fun querySelections(profileUUID: UUID): List<Selection> {
        return ProfileStore.loadSelections().filter { it.uuid == profileUUID }
    }

    fun setSelected(selection: Selection) {
        val list = ProfileStore.loadSelections().toMutableList()
        val index = list.indexOfFirst { 
            it.uuid == selection.uuid && it.proxy == selection.proxy 
        }
        if (index >= 0) {
            list[index] = selection
        } else {
            list.add(selection)
        }
        ProfileStore.saveSelections(list)
    }

    fun clear(profileUUID: UUID) {
        val list = ProfileStore.loadSelections().toMutableList()
        list.removeAll { it.uuid == profileUUID }
        ProfileStore.saveSelections(list)
    }

    fun clearAll() {
        ProfileStore.saveSelections(emptyList())
    }

    fun remove(profileUUID: UUID, proxy: String) {
        val list = ProfileStore.loadSelections().toMutableList()
        list.removeAll { it.uuid == profileUUID && it.proxy == proxy }
        ProfileStore.saveSelections(list)
    }

    fun removeSelections(profileUUID: UUID, proxies: List<String>) {
        val list = ProfileStore.loadSelections().toMutableList()
        list.removeAll { it.uuid == profileUUID && it.proxy in proxies }
        ProfileStore.saveSelections(list)
    }
}
