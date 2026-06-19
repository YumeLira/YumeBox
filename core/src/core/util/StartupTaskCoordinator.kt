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

package com.github.yumelira.yumebox.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import timber.log.Timber

object StartupTaskCoordinator {
    private val runtimeWarmupFallback = CompletableDeferred(Unit)

    @Volatile private var runtimeWarmup: Deferred<Unit>? = null

    @Volatile private var backgroundWarmup: Deferred<Unit>? = null

    fun startRuntimeWarmup(scope: CoroutineScope, block: suspend () -> Unit) {
        if (runtimeWarmup != null) return
        synchronized(this) {
            if (runtimeWarmup != null) return
            runtimeWarmup = scope.async {
                try {
                    block()
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    Timber.e(error, "Runtime warmup failed")
                    throw error
                }
            }
        }
    }

    fun startBackgroundWarmup(scope: CoroutineScope, block: suspend () -> Unit) {
        if (backgroundWarmup != null) return
        synchronized(this) {
            if (backgroundWarmup != null) return
            backgroundWarmup = scope.async {
                try {
                    block()
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    Timber.e(error, "Background warmup failed")
                }
            }
        }
    }

    suspend fun awaitRuntimeWarmup() {
        (runtimeWarmup ?: runtimeWarmupFallback).await()
    }
}
