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



package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.OverrideConfig
import kotlinx.coroutines.flow.Flow

interface OverrideConfigProvider {

    suspend fun getAll(): List<OverrideConfig>

    fun getAllFlow(): Flow<List<OverrideConfig>>

    suspend fun getById(id: String): OverrideConfig?

    suspend fun getUserConfigs(): List<OverrideConfig>

    fun getUserConfigsFlow(): Flow<List<OverrideConfig>>

    suspend fun save(config: OverrideConfig)

    suspend fun delete(id: String): Boolean

    suspend fun duplicate(id: String): OverrideConfig?

    suspend fun exists(id: String): Boolean
}
