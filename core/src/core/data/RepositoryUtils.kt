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

package com.github.yumelira.yumebox.core.data

import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Utility functions for Repository layer to reduce boilerplate code.
 */
object RepositoryUtils {

    suspend fun <T> safeApiCall(
        tag: String,
        operation: String,
        block: suspend () -> T,
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Timber.tag(tag).e(error, "Failed to execute $operation")
            Result.failure(error)
        }
    }

    fun <T> safeCall(
        tag: String,
        operation: String,
        block: () -> T,
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Timber.tag(tag).e(error, "Failed to execute $operation")
            Result.failure(error)
        }
    }
}
