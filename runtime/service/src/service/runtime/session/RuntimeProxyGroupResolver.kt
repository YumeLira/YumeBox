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

package com.github.yumelira.yumebox.service.runtime.session

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import java.security.MessageDigest

/**
 * Resolves the proxy-group list shown in the UI.
 *
 * When a session is running the live core itself is the single source of truth for group
 * **order + membership + live state**: the mihomo fork preserves the `proxy-groups:` declaration
 * order (`config/config.go`), and native `QueryProxyGroupNames` walks the GLOBAL provider in that
 * exact declaration order. Since the rust override path now loads the rawConfig correctly, the
 * running core already yields the correct order with real-time `now` / `proxies`, so a canonical
 * recompile is redundant while running.
 *
 * [canonicalGroups] (compiled rawConfig, `proxy-groups:` declaration order) is kept only as the
 * preview source and as the fallback for the transient window before the core has loaded.
 */
class RuntimeProxyGroupResolver(
    private val compiledConfigPipeline: CompiledConfigPipeline,
) {
    private val expectedNameCacheLock = Any()
    private var expectedNameCache: ExpectedGroupCache? = null

    private val canonicalCacheLock = Any()
    private var canonicalCache: CanonicalGroupCache? = null

    /**
     * Authoritative ordered group list straight from the compiled rawConfig (`proxy-groups:`).
     * Always rebuilt from a fresh compile via [CompiledConfigPipeline.previewGroups] so it can never
     * read a stale on-disk runtime.yaml; cached by the [CanonicalGroupKey] so the recompile only
     * happens when the config/override set actually changes (fingerprint flip), not on every refresh.
     *
     * An empty result is NOT cached: a transient empty compile during the start window must not
     * poison the cache for the rest of the session.
     */
    suspend fun canonicalGroups(
        spec: RuntimeSpec,
        excludeNotSelectable: Boolean,
    ): List<ProxyGroup> {
        val cacheKey =
            CanonicalGroupKey(
                profileUuid = spec.profileUuid,
                effectiveFingerprint = spec.effectiveFingerprint,
                excludeNotSelectable = excludeNotSelectable,
                ageSecretKeyFingerprint = sha256Short(spec.ageSecretKey),
            )
        synchronized(canonicalCacheLock) {
            canonicalCache?.takeIf { it.key == cacheKey }?.let { return it.groups }
        }

        val groups =
            runCatching { compiledConfigPipeline.previewGroups(spec, excludeNotSelectable) }
                .getOrDefault(emptyList())
                .filter { it.name.isNotBlank() }

        if (groups.isNotEmpty()) {
            synchronized(canonicalCacheLock) {
                canonicalCache = CanonicalGroupCache(cacheKey, groups)
            }
        }
        return groups
    }

    suspend fun expectedGroupNames(
        spec: RuntimeSpec,
        excludeNotSelectable: Boolean,
    ): List<String> {
        val cacheKey = ExpectedGroupKey(
            profileUuid = spec.profileUuid,
            effectiveFingerprint = spec.effectiveFingerprint,
            excludeNotSelectable = excludeNotSelectable,
            ageSecretKeyFingerprint = sha256Short(spec.ageSecretKey),
        )
        synchronized(expectedNameCacheLock) {
            expectedNameCache?.takeIf { it.key == cacheKey }?.let { return it.names }
        }

        // Every profile (encrypted and non-encrypted) goes through the native in-memory compile, so
        // expected names never depend on an on-disk runtime.yaml.
        val names = compiledConfigPipeline.previewGroupNames(spec, excludeNotSelectable)

        // Never cache an empty result: a transient empty compile during the start window must not
        // pin the whole session to an empty expected-name set.
        if (names.isNotEmpty()) {
            synchronized(expectedNameCacheLock) {
                expectedNameCache = ExpectedGroupCache(cacheKey, names)
            }
        }
        return names
    }

    fun runtimeGroupNames(excludeNotSelectable: Boolean): List<String> {
        return Clash.queryGroupNames(excludeNotSelectable)
    }

    suspend fun resolvedGroupNames(
        spec: RuntimeSpec?,
        excludeNotSelectable: Boolean,
    ): List<String> {
        // Running core is the source of truth: GLOBAL provider order == proxy-groups declaration order.
        val coreNames = runtimeGroupNames(excludeNotSelectable).filter(String::isNotBlank)
        if (coreNames.isNotEmpty()) {
            return coreNames
        }
        // Core not loaded yet (preview / transient start window): fall back to the compiled rawConfig.
        return spec?.let { canonicalGroups(it, excludeNotSelectable) }.orEmpty()
            .map(ProxyGroup::name)
            .filter(String::isNotBlank)
    }

    /**
     * The ordered, complete group list.
     *
     * @param enrichLive when true (running session) the list is taken straight from the live core
     *   ([coreGroups]): it already carries the `proxy-groups:` declaration order plus real-time
     *   `now` / `proxies`, so no canonical recompile is needed. When the core is not loaded yet
     *   (transient start window) or when [enrichLive] is false (preview / not running), the compiled
     *   rawConfig ([canonicalGroups]) is used instead.
     */
    suspend fun resolvedGroups(
        spec: RuntimeSpec?,
        excludeNotSelectable: Boolean,
        enrichLive: Boolean = true,
    ): List<ProxyGroup> {
        if (enrichLive) {
            val coreGroups = coreGroups(excludeNotSelectable)
            if (coreGroups.isNotEmpty()) {
                return coreGroups
            }
        }
        // Preview / core not loaded yet: use the compiled rawConfig (no running core to read from).
        return spec?.let { canonicalGroups(it, excludeNotSelectable) }.orEmpty()
    }

    /**
     * Groups straight from the live core, in GLOBAL-provider order (== `proxy-groups:` declaration
     * order in the mihomo fork). Each name is resolved to a usable live group; names that cannot be
     * resolved are skipped (they are not part of the live runtime). Returns empty when no core is
     * loaded, which signals the caller to fall back to [canonicalGroups].
     */
    private fun coreGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        val coreNamesByTrimmed = buildCoreNamesByTrimmed(excludeNotSelectable)
        return runtimeGroupNames(excludeNotSelectable)
            .mapNotNull { name -> queryUsableGroup(name, coreNamesByTrimmed) }
    }

    private fun queryUsableGroup(
        name: String,
        coreNamesByTrimmed: Map<String, String>,
    ): ProxyGroup? {
        Clash.queryGroup(name, ProxySort.Default).takeIf(::isUsable)?.let { return it }

        // The expected name was trimmed in the native compile but the core registers the raw key.
        // Retry against the core's actual (untrimmed) key for this trimmed name.
        val actualKey = coreNamesByTrimmed[name.trim()]
        if (actualKey != null && actualKey != name) {
            Clash.queryGroup(actualKey, ProxySort.Default).takeIf(::isUsable)?.let { return it }
        }
        return null
    }

    private fun buildCoreNamesByTrimmed(excludeNotSelectable: Boolean): Map<String, String> {
        val coreNames = runtimeGroupNames(excludeNotSelectable)
        val lookup = HashMap<String, String>(coreNames.size)
        coreNames.forEach { coreName ->
            val key = coreName.trim()
            if (key.isEmpty()) return@forEach
            // Prefer an exact (already-trimmed) registration; only fill from an untrimmed twin when
            // no exact key has been recorded for this trimmed name.
            val existing = lookup[key]
            if (existing == null || (existing != key && coreName == key)) {
                lookup[key] = coreName
            }
        }
        return lookup
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

    /**
     * Short SHA-256 hash of the age secret key. The raw key is NEVER stored in the cache key;
     * only this hash is. Returns the literal "none" when the profile is not age-encrypted so a
     * null/non-null transition still invalidates the caches.
     */
    private fun sha256Short(value: String?): String {
        if (value == null) return "none"
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private data class ExpectedGroupKey(
        val profileUuid: String,
        val effectiveFingerprint: String,
        val excludeNotSelectable: Boolean,
        val ageSecretKeyFingerprint: String,
    )

    private data class ExpectedGroupCache(
        val key: ExpectedGroupKey,
        val names: List<String>,
    )

    private data class CanonicalGroupKey(
        val profileUuid: String,
        val effectiveFingerprint: String,
        val excludeNotSelectable: Boolean,
        val ageSecretKeyFingerprint: String,
    )

    private data class CanonicalGroupCache(
        val key: CanonicalGroupKey,
        val groups: List<ProxyGroup>,
    )
}
