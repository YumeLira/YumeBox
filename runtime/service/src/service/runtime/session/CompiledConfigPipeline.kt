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

import android.content.Context
import android.util.Log
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.CompileRequest
import com.github.yumelira.yumebox.core.model.CompileRawSummary
import com.github.yumelira.yumebox.core.model.CompileResult
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.core.model.OverrideSpec
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.util.YamlCodec
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class CompiledConfigPipeline(private val context: Context) {

    fun resolveOverrideSpecs(profileUuid: String): List<OverrideSpec> {
        return resolveOverrideBundle(profileUuid, logger = null).overrides
    }

    fun resolveOverrideSpecs(profileUuid: String, logger: ((String) -> Unit)?): List<OverrideSpec> {
        return resolveOverrideBundle(profileUuid, logger).overrides
    }

    fun resolveOverrideBundle(profileUuid: String): ResolvedOverrideBundle {
        return resolveOverrideBundle(profileUuid, logger = null)
    }

    fun resolveOverrideBundle(
        profileUuid: String,
        logger: ((String) -> Unit)?,
    ): ResolvedOverrideBundle {
        val overridesDir = context.filesDir.resolve("overrides")
        val metadataFile = overridesDir.resolve("metadata.yaml")
        val metadata = loadMetadataIndex(overridesDir, metadataFile, logger)

        val binding = metadata.profileChains[profileUuid]
        logger?.invoke(
            "override resolve: profile=$profileUuid overrideIds=${binding?.overrideIds.orEmpty()}"
        )

        val userOverrides = mutableListOf<OverrideSpec>()
        val overrides = mutableListOf<OverrideSpec>()
        binding?.overrideIds.orEmpty().filterNot(::isReservedOverrideId).distinct().forEach {
            overrideId ->
            val file =
                resolveUserOverrideFile(overridesDir, overrideId, metadata)
                    ?: error("Override config not found for profile=$profileUuid id=$overrideId")
            val spec = file.toOverrideSpec()
            logger?.invoke(describeOverrideFile(file, overrideId))
            userOverrides += spec
            overrides += spec
        }

        val runtimeInternalOverride =
            resolveRuntimeInternalOverrideFile(overridesDir, profileUuid)
                ?.also { file ->
                    logger?.invoke(describeOverrideFile(file, INTERNAL_RUNTIME_PREFIX))
                }
                ?.toOverrideSpec()

        runtimeInternalOverride?.let(overrides::add)

        logger?.invoke(
            "override resolve: profile=$profileUuid resolved=${overrides.size} " +
                overrides.joinToString(prefix = "[", postfix = "]") { spec ->
                    "${spec.ext}:${spec.path.safeLogHash()}"
                }
        )

        return ResolvedOverrideBundle(
            profileUuid = profileUuid,
            userOverrides = userOverrides,
            runtimeInternalOverride = runtimeInternalOverride,
            overrides = overrides,
        )
    }

    suspend fun compileAndLoadNative(spec: RuntimeSpec, logger: ((String) -> Unit)?): Unit =
        withContext(Dispatchers.Default) {
            val request = buildRequest(spec)
            removeStaleRuntimeYaml(spec, logger)
            logger?.invoke(
                "runtime native: mode=compile-and-load ageKey=${spec.ageSecretKey != null}" +
                    " overrides=${spec.overrideSpecs.size}"
            )
            val load = CompletableDeferred<Unit>()
            val summary = Clash.compileAndLoadConfigSummary(request, load)
            logRawCompileWarnings(summary, logger)
            load.await()
        }

    /**
     * Loads the active profile into the live core. Every profile — encrypted and non-encrypted
     * alike — goes through the native in-memory compile-and-load path, so no plaintext runtime.yaml
     * is ever written to disk.
     */
    suspend fun compileAndLoad(spec: RuntimeSpec, logger: ((String) -> Unit)?): Unit {
        logger?.invoke(
            "runtime native: compile-and-load begin (ageKey=${spec.ageSecretKey != null})"
        )
        compileAndLoadNative(spec, logger)
        logger?.invoke("runtime native: compile-and-load done")
    }

    /**
     * Deletes any leftover runtime.yaml before loading a profile. runtime.yaml is no longer produced
     * by any code path, but historical builds may have left one on disk; clearing it for every
     * profile keeps the invariant "no runtime.yaml ever exists". A missing file is the normal case
     * and returns silently; only a failed delete of an existing file is treated as an error.
     */
    private fun removeStaleRuntimeYaml(spec: RuntimeSpec, logger: ((String) -> Unit)?) {
        val runtimeFile = File(spec.runtimeConfigPath)
        if (!runtimeFile.exists()) {
            return
        }
        if (!runtimeFile.delete()) {
            error("Stale runtime.yaml cleanup failed")
        }
        logger?.invoke("runtime native: removed stale runtime.yaml output=${runtimeFile.safeLogHash()}")
    }

    /**
     * Authoritative group list straight from the compiled rawConfig. Always goes through the native
     * in-memory compile (`compileAndInspectGroups`) for every profile — encrypted and non-encrypted
     * alike — so no plaintext finalYaml is ever returned to Kotlin and no runtime.yaml is written.
     */
    suspend fun previewGroups(spec: RuntimeSpec, excludeNotSelectable: Boolean): List<ProxyGroup> {
        return withContext(Dispatchers.Default) {
            Clash.compileAndInspectGroups(
                buildRequest(spec),
                File(spec.profileDir),
                excludeNotSelectable,
            )
        }
    }

    /**
     * Always inspects the tun `route-exclude-address` via the native in-memory compile for every
     * profile, so no plaintext finalYaml leaves native and no runtime.yaml is written.
     */
    suspend fun previewTunRouteExcludeAddress(spec: RuntimeSpec): List<String> {
        return withContext(Dispatchers.Default) {
            Clash.compileAndInspectTunRouteExcludeAddress(buildRequest(spec))
        }
    }

    /**
     * Returns the compiled YAML for non-encrypted profiles (the user-initiated "view compiled
     * config" export). This is the ONLY path that materialises plaintext finalYaml in Kotlin, and it
     * is intentionally retained: it is out of scope for the runtime.yaml elimination because it is an
     * explicit user export, not a runtime/load path. `Clash.compilePreview` returns the YAML in
     * memory and does NOT write `outputPath` to disk. Throws for encrypted profiles since full YAML
     * must not reach the Kotlin heap.
     */
    suspend fun previewCompiledYaml(
        profileUuid: String,
        profileDir: File,
        overrideSpecs: List<OverrideSpec> = resolveOverrideBundle(profileUuid).overrides,
        ageSecretKey: String? = null,
    ): CompileResult =
        withContext(Dispatchers.Default) {
            require(ageSecretKey == null) { "previewCompiledYaml is not supported for encrypted profiles" }
            val request =
                CompileRequest(
                    profileUuid = profileUuid,
                    profileDir = profileDir.absolutePath,
                    profilePath = profileDir.resolve("config.yaml").absolutePath,
                    overrides = overrideSpecs,
                    outputPath = profileDir.resolve("runtime.yaml").absolutePath,
                )
            val result = Clash.compilePreview(request)
            check(result.success) { result.error ?: "override preview failed" }
            validateCompiledProviderPaths(result.finalYaml, profileDir)
            result
        }

    private fun buildRequest(spec: RuntimeSpec): CompileRequest {
        val profileDir = File(spec.profileDir)
        return CompileRequest(
            profileUuid = spec.profileUuid,
            profileDir = profileDir.absolutePath,
            profilePath = profileDir.resolve("config.yaml").absolutePath,
            overrides = spec.overrideSpecs,
            outputPath =
                spec.runtimeConfigPath.ifBlank { profileDir.resolve("runtime.yaml").absolutePath },
            ageSecretKey = spec.ageSecretKey,
        )
    }

    private fun logRawCompileWarnings(summary: CompileRawSummary, logger: ((String) -> Unit)?) {
        if (logger == null) {
            return
        }
        if (!summary.success) {
            logger("runtime native: warning summary failed=${summary.error.safeNativeDiagnostic()}")
            return
        }
        summary.warnings.forEachIndexed { index, warning ->
            logger("runtime native: warning index=$index detail=${warning.safeNativeDiagnostic()}")
        }
    }

    private fun validateCompiledProviderPaths(finalYaml: String, profileDir: File) {
        val invalidPaths = mutableListOf<String>()
        val runtimeHomeDir = context.runtimeHomeDir
        val expectedRuleBase = profileDir.resolve("providers/rules").canonicalFile
        val expectedProxyBase = profileDir.resolve("providers/proxies").canonicalFile
        PATH_PATTERN.findAll(finalYaml).forEach { match ->
            val pathValue = match.groupValues[1].replace('\\', '/').trim()
            if (
                !pathValue.endsWith(".yaml") &&
                    !pathValue.endsWith(".yml") &&
                    !pathValue.endsWith(".mrs")
            ) {
                return@forEach
            }
            val isLegacyPath =
                pathValue.startsWith("./ruleset/") ||
                    pathValue.startsWith("ruleset/") ||
                    pathValue.contains("/clash/")
            val resolvedPath = runtimeHomeDir.resolve(pathValue).canonicalFile
            val inProfileProviders =
                resolvedPath.toPath().startsWith(expectedRuleBase.toPath()) ||
                    resolvedPath.toPath().startsWith(expectedProxyBase.toPath())
            if (isLegacyPath || File(pathValue).isAbsolute || !inProfileProviders) {
                invalidPaths += pathValue
            }
        }
        if (invalidPaths.isNotEmpty()) {
            invalidPaths.forEachIndexed { index, invalidPath ->
                Log.e(
                    TAG,
                    "Compiled provider path invalid index=$index path=${invalidPath.safeLogHash()}",
                )
            }
            error("Compiled provider path escaped profile scope")
        }
        if (PATH_PATTERN.containsMatchIn(finalYaml)) {
            Log.i(TAG, "Compiled provider paths validated")
        }
    }

    private fun loadMetadataIndex(
        overridesDir: File,
        metadataFile: File,
        logger: ((String) -> Unit)?,
    ): MetadataIndexPayload {
        val metadataRaw = metadataFile.takeIf(File::exists)?.readText().orEmpty()
        val metadata =
            if (metadataFile.exists()) {
                runCatching { YamlCodec.decode(MetadataIndexPayload.serializer(), metadataRaw) }
                    .getOrElse {
                        logger?.invoke(
                            "override resolve: metadata decode failed file=${metadataFile.safeLogHash()} " +
                                "size=${metadataRaw.length} sha=${metadataRaw.sha256Short()}"
                        )
                        MetadataIndexPayload()
                    }
            } else {
                MetadataIndexPayload()
            }
        val sanitized = sanitizeMetadataIndex(metadata)
        if (sanitized != metadata) {
            overridesDir.mkdirs()
            metadataFile.writeText(YamlCodec.encode(MetadataIndexPayload.serializer(), sanitized))
            logger?.invoke(
                "override resolve: metadata normalized file=${metadataFile.safeLogHash()}"
            )
        }
        return sanitized
    }

    private fun sanitizeMetadataIndex(metadata: MetadataIndexPayload): MetadataIndexPayload {
        val sanitizedConfigs = metadata.configs.filterKeys(::isUserOverrideId)
        return metadata.copy(
            configs = sanitizedConfigs,
            profileChains =
                metadata.profileChains.mapValues { (_, binding) ->
                    binding.copy(
                        overrideIds =
                            binding.overrideIds.filter { overrideId ->
                                !isLegacyPresetId(overrideId) &&
                                    (isReservedOverrideId(overrideId) ||
                                        isCustomRoutingOverrideId(overrideId) ||
                                        sanitizedConfigs.containsKey(overrideId))
                            }
                    )
                },
        )
    }

    private fun resolveUserOverrideFile(
        overridesDir: File,
        overrideId: String,
        metadataIndex: MetadataIndexPayload,
    ): File? {
        val expectedExtension =
            metadataIndex.configs[overrideId]?.contentType?.toOverrideExtension()
        if (expectedExtension != null) {
            val expectedFile = overridesDir.resolve("configs/$overrideId.$expectedExtension")
            if (expectedFile.exists()) {
                return expectedFile
            }
        }

        return USER_OVERRIDE_EXTENSIONS.asSequence()
            .map { extension -> overridesDir.resolve("configs/$overrideId.$extension") }
            .firstOrNull(File::exists)
    }

    private fun resolveRuntimeInternalOverrideFile(overridesDir: File, profileUuid: String): File? {
        val file =
            overridesDir.resolve("configs/${INTERNAL_RUNTIME_PREFIX}-profile-$profileUuid.yaml")
        if (!file.exists()) return null
        val content =
            runCatching { file.readText() }
                .getOrElse {
                    error(
                        "Runtime override file unreadable id=${profileUuid.safeLogHash()} reason=${it.message.safeNativeDiagnostic()}"
                    )
                }
        if (content.isBlank()) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    private fun isInternalRuntimeId(overrideId: String): Boolean {
        return overrideId.startsWith(INTERNAL_RUNTIME_PREFIX)
    }

    private fun isLegacyPresetId(overrideId: String): Boolean {
        return overrideId.startsWith(LEGACY_PRESET_PREFIX)
    }

    private fun isCustomRoutingOverrideId(overrideId: String): Boolean {
        return overrideId == OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
    }

    private fun isReservedOverrideId(overrideId: String): Boolean {
        return isInternalRuntimeId(overrideId) || isLegacyPresetId(overrideId)
    }

    private fun isUserOverrideId(overrideId: String): Boolean {
        return !isInternalRuntimeId(overrideId) && !isLegacyPresetId(overrideId)
    }

    private fun describeOverrideFile(file: File, overrideId: String): String {
        val content = file.takeIf(File::exists)?.readText().orEmpty()
        return buildString {
            append("override resolve: file id=")
            append(overrideId)
            append(" file=")
            append(file.safeLogHash())
            append(" exists=")
            append(file.exists())
            append(" size=")
            append(content.length)
            append(" sha=")
            append(content.sha256Short())
        }
    }

    suspend fun previewGroupNames(spec: RuntimeSpec, excludeNotSelectable: Boolean): List<String> {
        return previewGroups(spec, excludeNotSelectable)
            .map(ProxyGroup::name)
            .filter(String::isNotBlank)
    }

    private fun File.toOverrideSpec(): OverrideSpec {
        val extension =
            extension.lowercase().ifBlank {
                error("Override file missing extension")
            }
        return OverrideSpec(path = absolutePath, ext = extension)
    }

    private fun File.safeLogHash(): String = absolutePath.safeLogHash()

    private fun String.safeLogHash(): String = sha256Short()

    private fun String?.safeNativeDiagnostic(): String {
        val raw = this?.takeIf(String::isNotBlank) ?: return "unknown"
        return "len=${raw.length} sha=${raw.sha256Short()}"
    }

    private fun String.sha256Short(): String {
        if (isBlank()) return "empty"
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    data class ResolvedOverrideBundle(
        val profileUuid: String,
        val userOverrides: List<OverrideSpec>,
        val runtimeInternalOverride: OverrideSpec?,
        val overrides: List<OverrideSpec>,
    )

    @Serializable
    private data class MetadataIndexPayload(
        val configs: Map<String, ConfigMetadataPayload> = emptyMap(),
        val profileChains: Map<String, ProfileChainPayload> = emptyMap(),
    )

    @Serializable private data class ConfigMetadataPayload(val contentType: String = "yaml")

    @Serializable
    private data class ProfileChainPayload(val overrideIds: List<String> = emptyList())

    private companion object {
        private const val TAG = "CompiledConfigPipeline"
        private val PATH_PATTERN = Regex("""(?m)path:\s*["']?([^"'\n]+)["']?""")
        private val USER_OVERRIDE_EXTENSIONS = listOf("yaml", "yml", "js")
        const val INTERNAL_RUNTIME_PREFIX = "__runtime__"
        const val LEGACY_PRESET_PREFIX = "preset-"
    }
}

private fun String.toOverrideExtension(): String? {
    return when (lowercase()) {
        "yaml",
        "yml" -> "yaml"
        "js",
        "javascript" -> "js"
        else -> null
    }
}
