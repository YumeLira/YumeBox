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

package com.github.yumelira.yumebox.data.util

import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.isProxyGroup
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import timber.log.Timber

class ProxyChainResolver {
    companion object {
        private const val TAG = "ProxyChainResolver"
    }

    fun resolveEndNode(startNodeName: String, groups: List<ProxyGroupInfo>): Proxy? {
        return resolveProxyChain(startNodeName, groups, mutableSetOf())
    }

    private fun resolveProxyChain(
        proxyName: String,
        groups: List<ProxyGroupInfo>,
        visitedNames: MutableSet<String>,
    ): Proxy? {
        if (proxyName in visitedNames) {
            Timber.tag(TAG).w("Proxy cycle detected: $proxyName")
            return null
        }
        visitedNames.add(proxyName)

        val asGroup = groups.find { it.name == proxyName }
        if (asGroup != null && asGroup.now.isNotBlank()) {
            return resolveProxyChain(asGroup.now, groups, visitedNames)
        }

        for (group in groups) {
            val proxy = group.proxies.find { it.name == proxyName }
            if (proxy != null) {
                if (proxy.isProxyGroup) {
                    val targetGroup = groups.find { it.name == proxyName }
                    if (targetGroup != null && targetGroup.now.isNotBlank()) {
                        return resolveProxyChain(targetGroup.now, groups, visitedNames)
                    }
                }
                return proxy
            }
        }

        Timber.tag(TAG).w("Proxy node unresolved: $proxyName")
        return null
    }

    fun buildChainPath(startNodeName: String, groups: List<ProxyGroupInfo>): List<String> {
        val path = mutableListOf<String>()
        buildChainPathRecursive(startNodeName, groups, mutableSetOf(), path)
        return path
    }

    private fun buildChainPathRecursive(
        proxyName: String,
        groups: List<ProxyGroupInfo>,
        visited: MutableSet<String>,
        path: MutableList<String>,
    ) {
        if (proxyName in visited) return
        visited.add(proxyName)
        path.add(proxyName)

        val asGroup = groups.find { it.name == proxyName }
        if (asGroup != null && asGroup.now.isNotBlank()) {
            buildChainPathRecursive(asGroup.now, groups, visited, path)
        }
    }
}
