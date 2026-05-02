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



package com.github.yumelira.yumebox.substore

import com.caoccao.javet.enums.V8AwaitMode
import com.caoccao.javet.interception.logging.JavetStandardConsoleInterceptor
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.options.NodeRuntimeOptions
import timber.log.Timber
import java.io.Closeable
import java.io.File

class CaseEngine(backendPort: Int, frontendPort: Int, allowLan: Boolean) : Closeable {
    val host = if (allowLan) "0.0.0.0" else "127.0.0.1"

    private var nodeRuntime: NodeRuntime? = null
    private lateinit var thread: Thread

    @Volatile
    private var shouldAwait = true

    @Volatile
    private var isRunning = false

    private val argv2EnvScript = """
        process.argv.slice(2).forEach(arg => {
          if (arg.startsWith('--SUB_STORE')) {
            const [key, value] = arg.slice(2).split('=');
            if (key && value !== undefined) {
              process.env[key] = value;
            }
          }
        });
    """.trimIndent()

    init {
        try {
            SubStorePaths.ensureStructure()
            val nodeRuntimeOptions = NodeRuntimeOptions()
            nodeRuntimeOptions.setConsoleArguments(
                arrayOf(
                    "--allow-fs-read",
                    "--allow-fs-write",
                    "--SUB_STORE_FRONTEND_HOST=$host",
                    "--SUB_STORE_FRONTEND_PORT=$frontendPort",
                    "--SUB_STORE_FRONTEND_PATH=${SubStorePaths.frontendDir.absolutePath}",
                    "--SUB_STORE_BACKEND_API_HOST=$host",
                    "--SUB_STORE_BACKEND_API_PORT=$backendPort",
                    "--SUB_STORE_DATA_BASE_PATH=${SubStorePaths.dataDir.absolutePath}",
                )
            )

            val runtime = V8Host.getNodeInstance().createV8Runtime<NodeRuntime>(nodeRuntimeOptions)
            nodeRuntime = runtime

            val javetStandardConsoleInterceptor = JavetStandardConsoleInterceptor(runtime)
            javetStandardConsoleInterceptor.register(runtime.globalObject)

            runtime.allowEval(true)
            runtime.getExecutor(argv2EnvScript).executeVoid()
        } catch (error: Exception) {
            Timber.e(error, "CaseEngine init failed")
        }
    }

    fun startServer() {
        val runtime = nodeRuntime ?: run {
            Timber.w("Skip starting CaseEngine: runtime is not initialized")
            return
        }
        if (isRunning) return
        isRunning = true
        shouldAwait = true

        val codeFile: File = SubStorePaths.backendBundle

        thread = Thread {
            try {
                val workingDir = SubStorePaths.workingDir.path
                runtime.getExecutor("process.chdir('$workingDir')").executeVoid()

                runtime.getExecutor(codeFile).executeVoid()
                while (shouldAwait) {
                    runtime.await(V8AwaitMode.RunNoWait)
                }
            } catch (_: InterruptedException) {
            } catch (error: Exception) {
                Timber.e(error, "CaseEngine run failed")
            } finally {
                cleanup()
            }
        }.apply {
            name = "CaseEngine-Worker"
            start()
        }
    }

    fun isInitialized(): Boolean = nodeRuntime != null

    override fun close() {
        stopServer()
    }

    fun stopServer() {
        if (!isRunning) return

        try {
            shouldAwait = false
            nodeRuntime?.terminateExecution()

            if (::thread.isInitialized && thread.isAlive) {
                thread.interrupt()
                thread.join(5000)
            }
        } catch (error: Exception) {
            Timber.e(error, "CaseEngine stop failed")
        }
    }

    private fun cleanup() {
        isRunning = false
        runCatching {
            nodeRuntime?.let {
                if (!it.isClosed) {
                    it.isStopping = true
                    it.close()
                }
            }
            nodeRuntime = null
        }.onFailure { error ->
            Timber.e(error, "CaseEngine cleanup failed")
        }
    }
}
