package com.github.yumelira.yumebox.service.clash

import com.github.yumelira.yumebox.service.common.log.Log
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
                    Log.d("ClashRuntime: initialize")

                    try {
                        val modules = mutableListOf<Module<*>>()

                        Clash.reset()
                        Clash.clearOverride(Clash.OverrideSlot.Session)

                        val scope = object : ClashRuntimeScope {
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
                            Clash.clearOverride(Clash.OverrideSlot.Session)
                            Clash.forceGc()

                            Log.d("ClashRuntime: destroyed")
                        }
                    }
                }
            }
        }

        override fun requestGc(force: Boolean) {
            launch(Dispatchers.Default) {
                Clash.forceGc()
            }
        }
    }
}
