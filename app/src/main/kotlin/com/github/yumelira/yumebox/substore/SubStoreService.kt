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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.substore

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.github.yumelira.yumebox.common.native.NativeLibraryManager
import timber.log.Timber

class SubStoreService : Service() {

    companion object {
        var caseEngine: CaseEngine? = null
        var isRunning: Boolean = false
            private set

        fun startService(frontendPort: Int = 8080, backendPort: Int = 8081, allowLan: Boolean = false) {
            val context = com.github.yumelira.yumebox.App.instance
            val intent = Intent(context, SubStoreService::class.java).apply {
                putExtra("frontendPort", frontendPort)
                putExtra("backendPort", backendPort)
                putExtra("allowLan", allowLan)
            }
            context.startService(intent)
        }

        fun stopService() {
            val context = com.github.yumelira.yumebox.App.instance
            val intent = Intent(context, SubStoreService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return runCatching {
            val frontendPort = intent?.getIntExtra("frontendPort", 8080) ?: 8080
            val backendPort = intent?.getIntExtra("backendPort", 8081) ?: 8081
            val allowLan = intent?.getBooleanExtra("allowLan", false) ?: false

            if (NetworkUtil.isPortInUse(frontendPort) || NetworkUtil.isPortInUse(backendPort)) {
                throw Exception("端口 $frontendPort 或 $backendPort 已被占用")
            }

            if (!ensureJavetLibraryLoaded()) {
                throw Exception("Javet native 库加载失败")
            }

            val engine = CaseEngine(
                backendPort = backendPort,
                frontendPort = frontendPort,
                allowLan = allowLan
            )
            caseEngine = engine

            if (!engine.isInitialized()) {
                throw Exception("CaseEngine 初始化失败")
            }

            engine.startServer()
            isRunning = true

            START_STICKY
        }.getOrElse { e ->
            Timber.e(e, "Sub-Store service start failed")
            isRunning = false
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            caseEngine?.stopServer()
            caseEngine = null
            isRunning = false
        }.onFailure { e ->
            Timber.e(e, "Failed to stop Sub-Store service")
        }
    }

    private fun ensureJavetLibraryLoaded(): Boolean = runCatching {
        NativeLibraryManager.initialize(applicationContext)
        val javetLibBaseName = "libjavet-node-android"

        if (!NativeLibraryManager.isLibraryAvailable(javetLibBaseName)) {
            val results = NativeLibraryManager.extractAllLibraries()
            if (results[javetLibBaseName] != true) {
                Timber.e("Javet 库提取失败")
                return false
            }
        }

        val loaded = NativeLibraryManager.loadJniLibrary(javetLibBaseName)
        if (!loaded) {
            Timber.e("Javet 库加载失败，库状态: ${NativeLibraryManager.getLibraryStatus(javetLibBaseName)}")
        }
        loaded
    }.getOrElse { e ->
        Timber.e(e, "Javet 库加载异常")
        false
    }
}
