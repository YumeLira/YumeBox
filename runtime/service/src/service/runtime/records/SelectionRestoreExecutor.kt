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

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.isSelectable
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.runtime.entity.Selection
import java.util.*
import kotlinx.coroutines.runBlocking

internal object SelectionRestoreExecutor {
    private const val queryRetryCount = 3
    private const val queryRetryDelayMs = 150L

    fun restore(
        profileUuid: UUID,
        selections: List<Selection>,
        runtimeGroups: List<ProxyGroup>,
        tag: String,
    ) {
        val selectorGroups = runtimeGroups.associateBy { it.name }
        selections.forEach { selection ->
            val group =
                selectorGroups[selection.proxy]
                    ?: run {
                        removeSelection(profileUuid, selection, tag, "group missing")
                        return@forEach
                    }
            if (!group.isSelectable) {
                removeSelection(profileUuid, selection, tag, "group not selector")
                return@forEach
            }

            val currentNodes =
                group.proxies.mapNotNull { proxy -> proxy.name.trim().takeIf { it.isNotEmpty() } }
            val targetNode = selection.selected.trim()
            if (targetNode.isEmpty() || targetNode !in currentNodes) {
                removeSelection(profileUuid, selection, tag, "node missing")
                return@forEach
            }

            if (!patchSelectorWithRetry(selection.proxy, targetNode)) {
                Log.w(
                    "$tag restore selector patch failed: profile=$profileUuid group=${selection.proxy} node=$targetNode"
                )
            }
        }
    }

    private fun patchSelectorWithRetry(group: String, node: String): Boolean {
        repeat(queryRetryCount) { attempt ->
            if (Clash.patchSelector(group, node)) {
                return true
            }
            if (attempt < queryRetryCount - 1) {
                runBlocking {
                    PollingTimers.awaitTick(
                        PollingTimerSpecs.dynamic(
                            name = "selection_restore_patch_retry",
                            intervalMillis = queryRetryDelayMs,
                            initialDelayMillis = queryRetryDelayMs,
                        )
                    )
                }
            }
        }
        return false
    }

    private fun removeSelection(
        profileUuid: UUID,
        selection: Selection,
        tag: String,
        reason: String,
    ) {
        Log.w(
            "$tag remove invalid selector memory: profile=$profileUuid group=${selection.proxy} " +
                "node=${selection.selected} reason=$reason"
        )
        SelectionDao.remove(profileUuid, selection.proxy)
    }
}
