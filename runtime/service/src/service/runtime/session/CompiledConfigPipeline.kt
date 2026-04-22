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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.service.runtime.session

import android.content.Context
import android.util.Log
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.CompileRequest
import com.github.yumelira.yumebox.core.model.CompileResult
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.core.model.OverrideSpec
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.util.YamlCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest

class CompiledConfigPipeline(
    private val context: Context,
) {
    private val overrideEnabled = !context.packageName.endsWith(".lite")

    fun resolveOverrideSpecs(profileUuid: String): List<OverrideSpec> {
        return resolveOverrideBundle(profileUuid, logger = null).overrides
    }

    fun resolveOverrideSpecs(
        profileUuid: String,
        logger: ((String) -> Unit)?,
    ): List<OverrideSpec> {
        return resolveOverrideBundle(profileUuid, logger).overrides
    }

    fun resolveOverrideBundle(profileUuid: String): ResolvedOverrideBundle {
        return resolveOverrideBundle(profileUuid, logger = null)
    }

    fun resolveOverrideBundle(
        profileUuid: String,
        logger: ((String) -> Unit)?,
    ): ResolvedOverrideBundle {
        if (!overrideEnabled) {
            logger?.invoke("override resolve skipped for lite package=${context.packageName} profile=$profileUuid")
            return ResolvedOverrideBundle(
                profileUuid = profileUuid,
                userOverrides = emptyList(),
                runtimeInternalOverride = null,
                overrides = emptyList(),
            )
        }

        val overridesDir = context.filesDir.resolve("overrides")
        val metadataFile = overridesDir.resolve("metadata.yaml")
        val metadata = loadMetadataIndex(overridesDir, metadataFile, logger)

        val binding = metadata.profileChains[profileUuid]
        logger?.invoke(
            "override resolve: profile=$profileUuid overrideIds=${binding?.overrideIds.orEmpty()}",
        )

        val userOverrides = mutableListOf<OverrideSpec>()
        val overrides = mutableListOf<OverrideSpec>()
        binding
            ?.overrideIds
            .orEmpty()
            .filterNot(::isReservedOverrideId)
            .distinct()
            .forEach { overrideId ->
                val file = resolveUserOverrideFile(overridesDir, overrideId, metadata)
                    ?: error("Override config not found for profile=$profileUuid id=$overrideId")
                val spec = file.toOverrideSpec()
                logger?.invoke(describeOverrideFile(file, overrideId))
                if (isCustomRoutingId(overrideId)) {
                    overrides += spec
                    return@forEach
                }
                userOverrides += spec
                overrides += spec
            }

        val runtimeInternalOverride = resolveRuntimeInternalOverrideFile(overridesDir, profileUuid)
            ?.also { file -> logger?.invoke(describeOverrideFile(file, INTERNAL_RUNTIME_PREFIX)) }
            ?.toOverrideSpec()

        runtimeInternalOverride?.let(overrides::add)

        logger?.invoke(
            "override resolve: profile=$profileUuid resolved=${overrides.size} " +
                overrides.joinToString(prefix = "[", postfix = "]") { spec ->
                    "${spec.ext}:${spec.path}"
                },
        )

        return ResolvedOverrideBundle(
            profileUuid = profileUuid,
            userOverrides = userOverrides,
            runtimeInternalOverride = runtimeInternalOverride,
            overrides = overrides,
        )
    }

    suspend fun applyOverrideToRuntimeFile(spec: RuntimeSpec): String = withContext(Dispatchers.Default) {
        applyOverrideToRuntimeFile(spec, logger = null)
    }

    suspend fun applyOverrideToRuntimeFile(
        spec: RuntimeSpec,
        logger: ((String) -> Unit)?,
    ): String = withContext(Dispatchers.Default) {
        val profileDir = File(spec.profileDir)
        val sourceFile = profileDir.resolve("config.yaml")
        val runtimeFile = File(spec.runtimeConfigPath.ifBlank { profileDir.resolve("runtime.yaml").absolutePath })

        if (spec.overrideSpecs.isEmpty()) {
            logger?.invoke(
                "runtime prepare: mode=copy-config source=${sourceFile.absolutePath} target=${runtimeFile.absolutePath}",
            )
            val fingerprint = copyProfileConfigToRuntimeFile(sourceFile, runtimeFile)
            logger?.invoke(
                "runtime prepare: copied sourceSha=${sourceFile.readText().sha256Short()} targetSha=${runtimeFile.readText().sha256Short()}",
            )
            return@withContext fingerprint
        }

        logger?.invoke(
            "runtime prepare: mode=compile-overrides count=${spec.overrideSpecs.size} " +
                spec.overrideSpecs.joinToString(prefix = "[", postfix = "]") { overrideSpec ->
                    "${overrideSpec.ext}:${overrideSpec.path}"
                },
        )

        val request = buildRequest(spec)
        val result = Clash.compileToFile(request)
        check(result.success) {
            val failureMessage = result.error ?: "apply override to runtime config failed"
            logger?.invoke("runtime prepare: compile failed reason=$failureMessage")
            failureMessage
        }
        validateCompiledProviderPaths(result.finalYaml, profileDir)
        logger?.invoke(
            "runtime prepare: compile done fingerprint=${result.fingerprint} runtimeSha=${result.finalYaml.sha256Short()}",
        )
        result.fingerprint
    }

    suspend fun previewGroups(spec: RuntimeSpec, excludeNotSelectable: Boolean): List<ProxyGroup> {
        val result = previewOverride(spec)
        if (!result.success || result.finalYaml.isBlank()) return emptyList()
        return withContext(Dispatchers.Default) {
            Clash.inspectCompiledGroups(result.finalYaml, File(spec.profileDir), excludeNotSelectable)
        }
    }

    suspend fun previewCompiledYaml(
        profileUuid: String,
        profileDir: File,
        overrideSpecs: List<OverrideSpec> = resolveOverrideBundle(profileUuid).overrides,
    ): CompileResult = withContext(Dispatchers.Default) {
        val request = CompileRequest(
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

    suspend fun previewOverride(spec: RuntimeSpec): CompileResult = withContext(Dispatchers.Default) {
        val result = Clash.compilePreview(buildRequest(spec))
        check(result.success) { result.error ?: "override preview failed" }
        validateCompiledProviderPaths(result.finalYaml, File(spec.profileDir))
        result
    }

    private fun buildRequest(spec: RuntimeSpec): CompileRequest {
        val profileDir = File(spec.profileDir)
        return CompileRequest(
            profileUuid = spec.profileUuid,
            profileDir = profileDir.absolutePath,
            profilePath = profileDir.resolve("config.yaml").absolutePath,
            overrides = spec.overrideSpecs,
            outputPath = spec.runtimeConfigPath.ifBlank { profileDir.resolve("runtime.yaml").absolutePath },
        )
    }

    private fun copyProfileConfigToRuntimeFile(sourceFile: File, runtimeFile: File): String {
        check(sourceFile.isFile) { "Profile config missing: ${sourceFile.absolutePath}" }
        runtimeFile.parentFile?.mkdirs()
        sourceFile.copyTo(runtimeFile, overwrite = true)
        return runtimeFile.readText().sha256Short()
    }

    private fun validateCompiledProviderPaths(finalYaml: String, profileDir: File) {
        val acceptedPrefixes = listOf(
            "./providers/rules/",
            "./providers/proxies/",
            "providers/rules/",
            "providers/proxies/",
        )
        val invalidPaths = mutableListOf<String>()
        PATH_PATTERN.findAll(finalYaml).forEach { match ->
            val pathValue = match.groupValues[1].replace('\\', '/').trim()
            if (!pathValue.endsWith(".yaml") && !pathValue.endsWith(".yml") && !pathValue.endsWith(".mrs")) {
                return@forEach
            }
            val isLegacyPath = pathValue.startsWith("./ruleset/") ||
                pathValue.startsWith("ruleset/") ||
                pathValue.contains("/clash/")
            val inProfileProviders = acceptedPrefixes.any(pathValue::startsWith)
            if (isLegacyPath || !inProfileProviders) {
                invalidPaths += pathValue
            }
        }
        if (invalidPaths.isNotEmpty()) {
            invalidPaths.forEach { invalidPath ->
                Log.e(TAG, "Compiled provider path invalid: $invalidPath")
            }
            error("Compiled provider path escaped profile scope: ${invalidPaths.first()}")
        }
        if (PATH_PATTERN.containsMatchIn(finalYaml)) {
            Log.i(
                TAG,
                "Compiled provider paths validated: profile=${profileDir.absolutePath} prefixes=$acceptedPrefixes",
            )
        }
    }

    private fun loadMetadataIndex(
        overridesDir: File,
        metadataFile: File,
        logger: ((String) -> Unit)?,
    ): MetadataIndexPayload {
        val metadataRaw = metadataFile.takeIf(File::exists)?.readText().orEmpty()
        val metadata = if (metadataFile.exists()) {
            runCatching {
                YamlCodec.decode(MetadataIndexPayload.serializer(), metadataRaw)
            }.getOrElse {
                logger?.invoke(
                    "override resolve: metadata decode failed path=${metadataFile.absolutePath} " +
                        "size=${metadataRaw.length} sha=${metadataRaw.sha256Short()}",
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
            logger?.invoke("override resolve: metadata normalized path=${metadataFile.absolutePath}")
        }
        return sanitized
    }

    private fun sanitizeMetadataIndex(metadata: MetadataIndexPayload): MetadataIndexPayload {
        val sanitizedConfigs = metadata.configs.filterKeys(::isUserOverrideId)
        return metadata.copy(
            configs = sanitizedConfigs,
            profileChains = metadata.profileChains.mapValues { (_, binding) ->
                binding.copy(
                    overrideIds = binding.overrideIds.filter { overrideId ->
                        !isLegacyPresetId(overrideId) &&
                            (isReservedOverrideId(overrideId) || sanitizedConfigs.containsKey(overrideId))
                    },
                )
            },
        )
    }

    private fun resolveUserOverrideFile(
        overridesDir: File,
        overrideId: String,
        metadataIndex: MetadataIndexPayload,
    ): File? {
        val expectedExtension = metadataIndex.configs[overrideId]?.contentType
            ?.toOverrideExtension()
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
        val file = overridesDir.resolve("configs/${INTERNAL_RUNTIME_PREFIX}-profile-$profileUuid.yaml")
        if (!file.exists()) return null
        val content = runCatching { file.readText() }.getOrElse {
            error("Runtime override file unreadable path=${file.absolutePath} reason=${it.message}")
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

    private fun isReservedOverrideId(overrideId: String): Boolean {
        return isInternalRuntimeId(overrideId) || isLegacyPresetId(overrideId)
    }

    private fun isCustomRoutingId(overrideId: String): Boolean {
        return overrideId == OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
    }

    private fun isUserOverrideId(overrideId: String): Boolean {
        return !isInternalRuntimeId(overrideId) && !isLegacyPresetId(overrideId)
    }

    private fun describeOverrideFile(file: File, overrideId: String): String {
        val content = file.takeIf(File::exists)?.readText().orEmpty()
        return buildString {
            append("override resolve: file id=")
            append(overrideId)
            append(" path=")
            append(file.absolutePath)
            append(" exists=")
            append(file.exists())
            append(" size=")
            append(content.length)
            append(" sha=")
            append(content.sha256Short())
            content.lineSequence()
                .map(String::trim)
                .firstOrNull { it.isNotEmpty() }
                ?.let {
                    append(" firstLine=")
                    append(it.take(160))
                }
        }
    }

    private fun File.toOverrideSpec(): OverrideSpec {
        val extension = extension.lowercase().ifBlank {
            error("Override file missing extension: $absolutePath")
        }
        return OverrideSpec(
            path = absolutePath,
            ext = extension,
        )
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

    @Serializable
    private data class ConfigMetadataPayload(
        val contentType: String = "yaml",
    )

    @Serializable
    private data class ProfileChainPayload(
        val overrideIds: List<String> = emptyList(),
    )

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
        "yaml", "yml" -> "yaml"
        "js", "javascript" -> "js"
        else -> null
    }
}
