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



package com.github.yumelira.yumebox.data.controller

import com.github.yumelira.yumebox.data.model.OverrideConfig
import com.github.yumelira.yumebox.data.model.OverrideContentType
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import java.util.UUID

class RuntimeOverrideController(
    private val configStore: OverrideConfigStore,
    private val queryActiveProfile: suspend () -> Profile?,
) {
    suspend fun updateProfile(
        transform: (String) -> String,
    ): Result<String> = updateInternal(transform)

    private suspend fun loadInternal(): Result<String> = runCatching {
        val activeProfile = queryActiveProfile() ?: return@runCatching ""
        configStore.getById(runtimeOverrideId(activeProfile.uuid))
            ?.content
            .orEmpty()
    }

    private suspend fun saveInternal(content: String): Result<Unit> = runCatching {
        if (content.isBlank()) {
            clearInternal().getOrThrow()
            return@runCatching
        }
        val activeProfile = requireActiveProfile()
        val configId = runtimeOverrideId(activeProfile.uuid)
        val existing = configStore.getById(configId)
        configStore.save(
            OverrideConfig(
                id = configId,
                name = INTERNAL_RUNTIME_NAME,
                description = "internal runtime override for ${activeProfile.uuid}",
                contentType = OverrideContentType.Yaml,
                content = content,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun clearInternal(): Result<Unit> = runCatching {
        val activeProfile = queryActiveProfile() ?: return@runCatching
        configStore.delete(runtimeOverrideId(activeProfile.uuid))
    }

    private suspend fun updateInternal(
        transform: (String) -> String,
    ): Result<String> {
        val current = loadInternal().getOrElse { return Result.failure(it) }
        val updated = transform(current)
        val saveResult = saveInternal(updated)
        if (saveResult.isFailure) {
            return Result.failure(saveResult.exceptionOrNull() ?: IllegalStateException("保存运行时覆写失败"))
        }
        return Result.success(updated)
    }

    private suspend fun requireActiveProfile(): Profile {
        return queryActiveProfile() ?: error("No active profile selected")
    }

    private fun runtimeOverrideId(profileUuid: UUID): String {
        return "${OverrideConfigStore.INTERNAL_RUNTIME_PREFIX}-profile-$profileUuid"
    }

    private companion object {
        private const val INTERNAL_RUNTIME_NAME = "运行时覆写"
    }
}
