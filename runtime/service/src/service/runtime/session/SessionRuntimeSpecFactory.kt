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
import com.github.yumelira.yumebox.core.model.OverrideSpec
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.root.RootTunConfigFactory
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import com.github.yumelira.yumebox.service.runtime.util.directoryLastModified
import com.github.yumelira.yumebox.service.runtime.util.importedDir
import java.io.File
import java.security.MessageDigest

class SessionRuntimeSpecFactory(
    context: Context,
    private val store: ServiceStore = ServiceStore(),
) {
    private val context: Context = context.appContextOrSelf
    private val compiledConfigPipeline = CompiledConfigPipeline(this.context)

    fun createTunSpec(): RuntimeSpec {
        return createLocalSpec(RuntimeOwner.LocalTun)
    }

    fun createHttpSpec(): RuntimeSpec {
        return createLocalSpec(RuntimeOwner.LocalHttp)
    }

    private fun createLocalSpec(owner: RuntimeOwner): RuntimeSpec {
        val profile = requireActiveProfile()
        val profileDir = context.importedDir.resolve(profile.uuid.toString())
        val overrideSpecs = compiledConfigPipeline.resolveOverrideSpecs(profile.uuid.toString())
        val ageSecretKey = normalizeAgeSecretKey(profile.ageSecretKey)
        return RuntimeSpec(
            owner = owner,
            profileUuid = profile.uuid.toString(),
            profileName = profile.name,
            profileDir = profileDir.absolutePath,
            runtimeConfigPath = profileDir.resolve("runtime.yaml").absolutePath,
            ageSecretKey = ageSecretKey,
            overrideSpecs = overrideSpecs,
            effectiveFingerprint =
                buildEffectiveFingerprint(profile.uuid.toString(), overrideSpecs, ageSecretKey),
            profileFingerprint = buildProfileFingerprint(profile.uuid.toString()),
        )
    }

    fun createRootTunSpec(): RuntimeSpec {
        val rootResult = RootTunConfigFactory(context).create()
        val profile =
            ImportedDao.queryByUUID(rootResult.profileUuid)
                ?: error("Root tun profile metadata not found: ${rootResult.profileUuid}")
        val overrideSpecs =
            compiledConfigPipeline.resolveOverrideSpecs(rootResult.profileUuid.toString())
        val ageSecretKey = normalizeAgeSecretKey(profile.ageSecretKey)
        return RuntimeSpec(
            owner = RuntimeOwner.RootTun,
            profileUuid = rootResult.profileUuid.toString(),
            profileName = rootResult.profileName,
            profileDir = rootResult.profileDir.absolutePath,
            runtimeConfigPath = rootResult.profileDir.resolve("runtime.yaml").absolutePath,
            ageSecretKey = ageSecretKey,
            overrideSpecs = overrideSpecs,
            rootTunConfig = rootResult.config,
            staticPlanFingerprint = rootResult.staticPlan.fingerprint,
            transportFingerprint = rootResult.dynamicOverrides.transportFingerprint,
            effectiveFingerprint =
                buildEffectiveFingerprint(
                    rootResult.profileUuid.toString(),
                    overrideSpecs,
                    ageSecretKey,
                ),
            profileFingerprint = rootResult.dynamicOverrides.profileFingerprint,
        )
    }

    private fun requireActiveProfile():
        com.github.yumelira.yumebox.service.runtime.entity.Imported {
        val profileId = store.activeProfile ?: error("No active profile selected")
        return ImportedDao.queryByUUID(profileId)
            ?: error("Active profile metadata not found: $profileId")
    }

    private fun buildProfileFingerprint(profileUuid: String): String {
        val dir = context.importedDir.resolve(profileUuid)
        return sha256 {
            update(profileUuid.toByteArray())
            updateFile(dir.resolve("config.yaml"))
            update((dir.directoryLastModified ?: -1L).toString().toByteArray())
        }
    }

    private fun buildEffectiveFingerprint(
        profileUuid: String,
        overrideSpecs: List<OverrideSpec>,
        ageSecretKey: String?,
    ): String {
        val profileDir = context.importedDir.resolve(profileUuid)
        val metadataFile = context.filesDir.resolve("overrides/metadata.yaml")
        return sha256 {
            update(profileUuid.toByteArray())
            updateAgeSecretKeyDigest(ageSecretKey)
            updateFile(profileDir.resolve("config.yaml"))
            updateFile(metadataFile)
            overrideSpecs.forEach { overrideSpec ->
                update(overrideSpec.path.toByteArray())
                update(overrideSpec.ext.toByteArray())
                updateFile(File(overrideSpec.path))
            }
        }
    }

    private fun MessageDigest.updateAgeSecretKeyDigest(ageSecretKey: String?) {
        update("age-secret-key:".toByteArray())
        update((ageSecretKey?.let(::sha256String) ?: "none").toByteArray())
    }

    private inline fun sha256(block: MessageDigest.() -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.block()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun MessageDigest.updateFile(file: File) {
        if (!file.exists()) {
            update("missing:${file.absolutePath}".toByteArray())
            return
        }
        update(file.absolutePath.toByteArray())
        update(file.readBytes())
    }

    private fun normalizeAgeSecretKey(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun sha256String(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
