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

package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Selection
import java.util.*

object SelectionDao {
    fun migrateLegacyIfNeeded() {
        ProfileStore.migrateLegacySelectionMemoryIfNeeded()
    }

    fun queryAll(): List<Selection> {
        migrateLegacyIfNeeded()
        return ProfileStore.loadSelections()
    }

    fun querySelections(profileUUID: UUID): List<Selection> {
        migrateLegacyIfNeeded()
        return ProfileStore.loadSelections(profileUUID)
    }

    fun queryRestorableSelections(profileUUID: UUID): List<Selection> {
        return querySelections(profileUUID)
    }

    fun upsertManualSelection(profileUUID: UUID, groupName: String, selectedProxy: String) {
        upsertManualSelection(
            Selection(
                uuid = profileUUID,
                proxy = groupName.trim(),
                selected = selectedProxy.trim(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    fun upsertManualSelection(selection: Selection) {
        migrateLegacyIfNeeded()
        val normalized =
            selection.copy(
                proxy = selection.proxy.trim(),
                selected = selection.selected.trim(),
                updatedAt =
                    if (selection.updatedAt > 0L) selection.updatedAt
                    else System.currentTimeMillis(),
            )
        if (normalized.proxy.isEmpty() || normalized.selected.isEmpty()) {
            return
        }
        // Single atomic per-key write — no whole-list read-modify-write, so concurrent
        // writes from the app process (ClashManager) and the root process (SessionRuntime)
        // to different selections never clobber one another.
        ProfileStore.putSelection(normalized)
    }

    fun setSelected(selection: Selection) {
        upsertManualSelection(selection)
    }

    fun clear(profileUUID: UUID) {
        migrateLegacyIfNeeded()
        ProfileStore.clearSelections(profileUUID)
    }

    fun clearAll() {
        migrateLegacyIfNeeded()
        ProfileStore.clearAllSelections()
        ProfileStore.removeAllSelectionScopeKeys()
    }

    fun remove(profileUUID: UUID, proxy: String) {
        migrateLegacyIfNeeded()
        ProfileStore.removeSelection(profileUUID, proxy.trim())
    }

    fun removeSelections(profileUUID: UUID, proxies: List<String>) {
        migrateLegacyIfNeeded()
        proxies.forEach { proxy -> ProfileStore.removeSelection(profileUUID, proxy.trim()) }
    }
}
