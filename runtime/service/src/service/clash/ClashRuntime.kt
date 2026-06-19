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

package com.github.yumelira.yumebox.service.clash

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.clash.module.Module
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val globalLock = Mutex()

interface ClashRuntimeScope {
    fun <E, T : Module<E>> install(module: T): T
}

interface ClashRuntime {
    fun launch()

    fun requestGc(force: Boolean = false)
}

fun CoroutineScope.clashRuntime(block: suspend ClashRuntimeScope.() -> Unit): ClashRuntime {
    return object : ClashRuntime {
        override fun launch() {
            launch(Dispatchers.IO) {
                globalLock.withLock {
                    try {
                        val modules = mutableListOf<Module<*>>()

                        Clash.reset()

                        val scope =
                            object : ClashRuntimeScope {
                                override fun <E, T : Module<E>> install(module: T): T {
                                    launch {
                                        modules.add(module)

                                        module.execute()
                                    }

                                    return module
                                }
                            }

                        scope.block()

                        cancel()
                    } finally {
                        withContext(NonCancellable) {
                            Clash.reset()
                            Clash.forceGc()
                        }
                    }
                }
            }
        }

        override fun requestGc(force: Boolean) {
            launch(Dispatchers.Default) { Clash.forceGc() }
        }
    }
}
