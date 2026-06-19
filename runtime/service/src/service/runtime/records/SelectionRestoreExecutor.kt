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
        // Restore must be NON-DESTRUCTIVE. An empty runtimeGroups means the fetch wasn't
        // ready (core not up / providers not loaded), NOT that the user's groups are gone.
        // Bailing out here protects node memory from being wiped on a transient empty fetch.
        if (runtimeGroups.isEmpty()) {
            Log.w(
                "$tag restore skipped: runtime groups empty (fetch not ready) profile=$profileUuid"
            )
            return
        }

        val selectorGroups = runtimeGroups.associateBy { it.name }
        selections.forEach { selection ->
            val group =
                selectorGroups[selection.proxy]
                    ?: run {
                        skipSelection(profileUuid, selection, tag, "group missing")
                        return@forEach
                    }
            if (!group.isSelectable) {
                skipSelection(profileUuid, selection, tag, "group not selector")
                return@forEach
            }

            val currentNodes =
                group.proxies.mapNotNull { proxy -> proxy.name.trim().takeIf { it.isNotEmpty() } }
            val savedNode = selection.selected.trim()
            if (savedNode.isEmpty()) {
                skipSelection(profileUuid, selection, tag, "node missing")
                return@forEach
            }

            // Exact match first; the saved name is authoritative when it still exists verbatim.
            val targetNode =
                if (savedNode in currentNodes) {
                    savedNode
                } else {
                    // Conservative fuzzy fallback: a provider may cosmetically rename a node
                    // (e.g. "JP 01" -> "JP-01"). Match on a normalized form that strips ASCII
                    // separators/whitespace but KEEPS non-ASCII chars (emoji/CJK) so flags and
                    // region names still distinguish nodes. Only apply when EXACTLY ONE node
                    // matches; ambiguity (zero or many) must never silently pick a node.
                    val normalizedTarget = normalizeNodeName(savedNode)
                    val matches = currentNodes.filter { normalizeNodeName(it) == normalizedTarget }
                    val resolved = matches.singleOrNull()
                    if (resolved == null) {
                        skipSelection(profileUuid, selection, tag, "node missing")
                        return@forEach
                    }
                    Log.i(
                        "$tag restore selector fuzzy-matched: profile=$profileUuid " +
                            "group=${selection.proxy} saved=$savedNode resolved=$resolved"
                    )
                    resolved
                }

            if (!patchSelectorWithRetry(selection.proxy, targetNode)) {
                Log.w(
                    "$tag restore selector patch failed: profile=$profileUuid group=${selection.proxy} node=$targetNode"
                )
            }
        }
    }

    /**
     * Normalize a node name for cosmetic-rename matching: lowercase, then drop ASCII spaces and
     * the separators `-`, `_`, `.`. Non-ASCII characters (emoji flags, CJK region names) are
     * deliberately kept so that two genuinely different nodes are not collapsed together.
     */
    private fun normalizeNodeName(name: String): String {
        return name.lowercase(Locale.ROOT).filterNot { ch ->
            ch == ' ' || ch == '-' || ch == '_' || ch == '.'
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

    /**
     * Skip (but DO NOT delete) a selection that cannot be applied this cycle. Records are only
     * pruned by explicit user re-selection (upsert) or profile deletion (clear); restore never
     * removes a record, so a transiently-missing group/node simply re-applies once it reappears.
     */
    private fun skipSelection(
        profileUuid: UUID,
        selection: Selection,
        tag: String,
        reason: String,
    ) {
        Log.w(
            "$tag skip selector memory (kept): profile=$profileUuid group=${selection.proxy} " +
                "node=${selection.selected} reason=$reason"
        )
    }
}
