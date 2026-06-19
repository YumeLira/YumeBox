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

package com.github.yumelira.yumebox.service.runtime.session

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.service.runtime.util.mergeProxyGroupNames
import java.io.File

class RuntimeProxyGroupResolver(
    private val compiledConfigPipeline: CompiledConfigPipeline,
) {
    private val expectedNameCacheLock = Any()
    private var expectedNameCache: ExpectedGroupCache? = null

    suspend fun expectedGroupNames(
        spec: RuntimeSpec,
        excludeNotSelectable: Boolean,
    ): List<String> {
        val cacheKey = ExpectedGroupKey(
            profileUuid = spec.profileUuid,
            effectiveFingerprint = spec.effectiveFingerprint,
            excludeNotSelectable = excludeNotSelectable,
        )
        synchronized(expectedNameCacheLock) {
            expectedNameCache?.takeIf { it.key == cacheKey }?.let { return it.names }
        }

        val names = if (spec.ageSecretKey != null) {
            compiledConfigPipeline.previewGroupNames(spec, excludeNotSelectable)
        } else {
            readRuntimeFileGroupNames(spec, excludeNotSelectable)
        }

        synchronized(expectedNameCacheLock) {
            expectedNameCache = ExpectedGroupCache(cacheKey, names)
        }
        return names
    }

    private fun readRuntimeFileGroupNames(
        spec: RuntimeSpec,
        excludeNotSelectable: Boolean,
    ): List<String> {
        val runtimeFile = File(spec.runtimeConfigPath)
        if (!runtimeFile.isFile) {
            return emptyList()
        }

        val yamlText = runtimeFile.readText()
        if (yamlText.isBlank()) {
            return emptyList()
        }

        return Clash.inspectCompiledGroups(
                yamlText,
                File(spec.profileDir),
                excludeNotSelectable,
            )
            .map(ProxyGroup::name)
            .filter(String::isNotBlank)
    }

    fun runtimeGroupNames(excludeNotSelectable: Boolean): List<String> {
        return Clash.queryGroupNames(excludeNotSelectable)
    }

    suspend fun resolvedGroupNames(
        spec: RuntimeSpec?,
        excludeNotSelectable: Boolean,
    ): List<String> {
        val runtimeNames = runtimeGroupNames(excludeNotSelectable)
        val expectedNames =
            spec?.let {
                runCatching { expectedGroupNames(it, excludeNotSelectable) }.getOrDefault(emptyList())
            } ?: emptyList()
        if (expectedNames.isEmpty()) {
            return runtimeNames
        }

        val selectableNames = if (excludeNotSelectable) runtimeNames.toSet() else null
        return mergeProxyGroupNames(expectedNames, runtimeNames) { groupName ->
            selectableNames == null || groupName in selectableNames
        }
    }

    suspend fun resolvedGroups(
        spec: RuntimeSpec?,
        excludeNotSelectable: Boolean,
    ): List<ProxyGroup> {
        return resolvedGroupNames(spec, excludeNotSelectable)
            .mapNotNull(::queryUsableGroup)
    }

    fun queryUsableGroup(name: String): ProxyGroup? {
        return Clash.queryGroup(name, ProxySort.Default).takeIf(::isUsable)
    }

    fun isUsable(group: ProxyGroup): Boolean {
        if (group.name.isBlank()) {
            return false
        }
        return group.type != Proxy.Type.Unknown ||
            group.proxies.isNotEmpty() ||
            group.now.isNotBlank() ||
            !group.icon.isNullOrBlank()
    }

    private data class ExpectedGroupKey(
        val profileUuid: String,
        val effectiveFingerprint: String,
        val excludeNotSelectable: Boolean,
    )

    private data class ExpectedGroupCache(
        val key: ExpectedGroupKey,
        val names: List<String>,
    )
}
