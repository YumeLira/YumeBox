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

package com.github.yumelira.yumebox.service.root

import com.topjohnwu.superuser.Shell

object RootPackageShell {
    private data class CacheEntry<T>(val value: T, val cachedAt: Long) {
        fun isFresh(now: Long): Boolean = now - cachedAt <= CACHE_TTL_MS
    }

    private val packageUidRegex = Regex("""^package:([^\s]+).*(?:uid|userId):(\d+)\b""")
    private val packageNameRegex = Regex("""^package:([^\s]+)""")

    @Volatile private var packageUidCache: CacheEntry<Map<String, Int>>? = null

    @Volatile private var packageNameCache: CacheEntry<Set<String>>? = null

    fun hasRootAccess(): Boolean {
        return runCatching { Shell.getShell().isRoot }.getOrDefault(false)
    }

    fun queryPackageUidMap(packages: Set<String>? = null): Map<String, Int>? {
        if (!hasRootAccess()) return null

        val now = System.currentTimeMillis()
        val normalizedPackages =
            packages
                ?.asSequence()
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?.toSortedSet()
                ?.takeIf { it.isNotEmpty() }

        if (normalizedPackages == null) {
            packageUidCache
                ?.takeIf { it.isFresh(now) }
                ?.value
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    return it
                }
        } else {
            packageUidCache
                ?.takeIf { it.isFresh(now) }
                ?.value
                ?.takeIf { cached -> normalizedPackages.all(cached::containsKey) }
                ?.let { cached ->
                    return normalizedPackages.associateWith { cached.getValue(it) }
                }
        }

        val stdout = mutableListOf<String>()
        val result = Shell.cmd(buildUidQueryCommand(normalizedPackages)).to(stdout).exec()

        if (!result.isSuccess) return null

        val resolved =
            stdout
                .asSequence()
                .mapNotNull { line ->
                    val match = packageUidRegex.find(line.trim()) ?: return@mapNotNull null
                    val packageName = match.groupValues[1]
                    val uid = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    packageName to uid
                }
                .filter { (packageName, _) ->
                    normalizedPackages == null || packageName in normalizedPackages
                }
                .toMap()
                .ifEmpty { null }

        resolved?.let { value ->
            val cacheEntry = CacheEntry(value = value, cachedAt = now)
            if (normalizedPackages == null) {
                packageUidCache = cacheEntry
                packageNameCache = CacheEntry(value = value.keys, cachedAt = now)
            } else {
                val merged = (packageUidCache?.value.orEmpty() + value)
                packageUidCache = CacheEntry(value = merged, cachedAt = now)
            }
        }

        return resolved
    }

    fun queryInstalledPackageNames(): Set<String>? {
        if (!hasRootAccess()) return null

        val now = System.currentTimeMillis()
        packageUidCache
            ?.takeIf { it.isFresh(now) }
            ?.value
            ?.keys
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                return it
            }

        packageNameCache
            ?.takeIf { it.isFresh(now) }
            ?.value
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                return it
            }

        val stdout = mutableListOf<String>()
        val result = Shell.cmd("cmd package list packages || pm list packages").to(stdout).exec()

        if (!result.isSuccess) return null

        return stdout
            .asSequence()
            .mapNotNull { line -> packageNameRegex.find(line.trim())?.groupValues?.getOrNull(1) }
            .toSet()
            .ifEmpty { null }
            ?.also { packageNameCache = CacheEntry(it, now) }
    }

    fun invalidateCaches() {
        packageUidCache = null
        packageNameCache = null
    }

    private fun buildUidQueryCommand(packages: Set<String>?): String {
        if (packages.isNullOrEmpty()) {
            return "cmd package list packages -U || pm list packages -U"
        }

        if (packages.size == 1) {
            val filter = escapeShellArg(packages.first())
            return "cmd package list packages -U $filter || pm list packages -U $filter"
        }

        val filters = packages.joinToString(" ") { escapeShellArg(it) }
        return "for pkg in $filters; do cmd package list packages -U \"\$pkg\" || pm list packages -U \"\$pkg\"; done"
    }

    private fun escapeShellArg(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private const val CACHE_TTL_MS = 5 * 60 * 1000L
}
