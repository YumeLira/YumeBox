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

package com.github.yumelira.yumebox.presentation.viewmodel

import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.data.model.ProxySortMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class ProxyGroupSorter {
    private data class SortedGroupCacheEntry(
        val sourceGroup: ProxyGroupInfo,
        val sortMode: ProxySortMode,
        val originalOrder: List<String>,
        val result: ProxyGroupInfo,
    )

    private val groupOriginalOrder = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    private var sortedGroupCache: Map<String, SortedGroupCacheEntry> = emptyMap()

    fun track(groups: List<ProxyGroupInfo>) {
        groupOriginalOrder.update { current -> updateGroupOrderCache(current, groups) }
    }

    fun bind(
        scope: CoroutineScope,
        proxyGroups: StateFlow<List<ProxyGroupInfo>>,
        sortMode: StateFlow<ProxySortMode>,
    ): StateFlow<List<ProxyGroupInfo>> =
        combine(proxyGroups, sortMode, groupOriginalOrder) { groups, mode, originalOrderCache ->
                buildSortedProxyGroups(groups, mode, originalOrderCache)
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    private fun sortProxies(
        proxies: List<Proxy>,
        sortMode: ProxySortMode,
        originalOrder: List<String>,
    ): List<Proxy> =
        when (sortMode) {
            ProxySortMode.DEFAULT -> reorderByNameSequence(proxies, originalOrder)
            ProxySortMode.BY_NAME -> {
                val originalIndex =
                    originalOrder.withIndex().associate { (index, name) -> name to index }
                proxies
                    .sortedWith(
                        compareBy<Proxy>(
                            { it.name.lowercase() },
                            { originalIndex[it.name] ?: Int.MAX_VALUE },
                        )
                    )
                    .takeUnless { it.hasSameProxyOrderAs(proxies) } ?: proxies
            }
            ProxySortMode.BY_LATENCY -> {
                val originalIndex =
                    originalOrder.withIndex().associate { (index, name) -> name to index }
                proxies
                    .sortedWith(
                        compareBy<Proxy>(
                            { proxy ->
                                when {
                                    proxy.delay > 0 -> 0
                                    proxy.delay < 0 -> 2
                                    else -> 1
                                }
                            },
                            { proxy ->
                                when {
                                    proxy.delay > 0 -> proxy.delay
                                    proxy.delay < 0 -> Int.MAX_VALUE - 1
                                    else -> Int.MAX_VALUE
                                }
                            },
                            { proxy -> originalIndex[proxy.name] ?: Int.MAX_VALUE },
                        )
                    )
                    .takeUnless { it.hasSameProxyOrderAs(proxies) } ?: proxies
            }
        }

    private fun reorderByNameSequence(
        proxies: List<Proxy>,
        orderedNames: List<String>,
    ): List<Proxy> {
        if (proxies.isEmpty() || orderedNames.isEmpty()) return proxies

        val proxyByName = proxies.associateBy { it.name }
        val consumed = HashSet<String>(proxies.size)
        val reordered = ArrayList<Proxy>(proxies.size)

        orderedNames.forEach { name ->
            val proxy = proxyByName[name] ?: return@forEach
            reordered += proxy
            consumed += name
        }
        proxies.forEach { proxy -> if (consumed.add(proxy.name)) reordered += proxy }
        return reordered.takeUnless { it.hasSameProxyOrderAs(proxies) } ?: proxies
    }

    private fun buildSortedProxyGroups(
        groups: List<ProxyGroupInfo>,
        mode: ProxySortMode,
        originalOrderCache: Map<String, List<String>>,
    ): List<ProxyGroupInfo> {
        val previousCache = sortedGroupCache
        val nextCache = HashMap<String, SortedGroupCacheEntry>(groups.size)
        val results = groups.map { group ->
            val originalOrder = originalOrderCache[group.name].orEmpty()
            val cached = previousCache[group.name]
            if (
                cached != null &&
                    cached.sourceGroup == group &&
                    cached.sortMode == mode &&
                    cached.originalOrder == originalOrder
            ) {
                nextCache[group.name] = cached
                cached.result
            } else {
                val sortedProxies =
                    sortProxies(
                        proxies = group.proxies,
                        sortMode = mode,
                        originalOrder = originalOrder,
                    )
                val result =
                    if (sortedProxies === group.proxies) group
                    else group.copy(proxies = sortedProxies)
                SortedGroupCacheEntry(
                        sourceGroup = group,
                        sortMode = mode,
                        originalOrder = originalOrder,
                        result = result,
                    )
                    .also { nextCache[group.name] = it }
                    .result
            }
        }
        sortedGroupCache = nextCache
        return results
    }

    private fun updateGroupOrderCache(
        current: Map<String, List<String>>,
        groups: List<ProxyGroupInfo>,
    ): Map<String, List<String>> {
        val next = current.toMutableMap()
        var changed = false
        val activeGroupNames = groups.mapTo(HashSet(groups.size)) { it.name }

        if (next.keys.removeAll { it !in activeGroupNames }) {
            changed = true
        }

        groups.forEach { group ->
            val latestNames = group.proxies.map { it.name }
            val previous = next[group.name]
            val merged =
                if (previous == null) {
                    latestNames
                } else {
                    mergeStableOrder(previous, latestNames)
                }
            if (previous != merged) {
                next[group.name] = merged
                changed = true
            }
        }

        return if (changed) next else current
    }

    private fun mergeStableOrder(
        previousOrder: List<String>,
        latestNames: List<String>,
    ): List<String> {
        if (previousOrder.isEmpty()) return latestNames
        if (latestNames.isEmpty()) return emptyList()

        val latestSet = latestNames.toHashSet()
        val merged = ArrayList<String>(latestNames.size)
        previousOrder.forEach { name -> if (name in latestSet) merged += name }
        latestNames.forEach { name -> if (name !in merged) merged += name }
        return merged
    }

    private fun List<Proxy>.hasSameProxyOrderAs(other: List<Proxy>): Boolean {
        if (size != other.size) return false
        return indices.all { index -> this[index] == other[index] }
    }
}
