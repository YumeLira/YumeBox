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



package com.github.yumelira.yumebox.core.bridge

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import kotlinx.coroutines.CompletableDeferred
import java.io.File

@Keep
object Bridge {
    external fun nativeCompilePreview(requestJson: String): String
    external fun nativeCompileToFile(requestJson: String): String
    external fun nativeReset()
    external fun nativeForceGc()
    external fun nativeSuspend(suspend: Boolean)
    external fun nativeQueryTunnelState(): String
    external fun nativeQueryTrafficNow(): Long
    external fun nativeQueryTrafficTotal(): Long
    external fun nativeQueryConnections(): String
    external fun nativeCloseConnection(id: String): Boolean
    external fun nativeCloseAllConnections()
    external fun nativeNotifyDnsChanged(dnsList: String)
    external fun nativeNotifyTimeZoneChanged(name: String, offset: Int)
    external fun nativeStartTun(fd: Int, stack: String, gateway: String, portal: String, dns: String, cb: TunInterface)
    external fun nativeStopTun()
    external fun nativeStartRootTun(configYaml: String): String?
    external fun nativeStopRootTun()
    external fun nativeStartHttp(listenAt: String): String?
    external fun nativeStopHttp()
    external fun nativeQueryGroupNames(excludeNotSelectable: Boolean): String
    external fun nativeInspectCompiledGroups(yamlText: String, profileDir: String, excludeNotSelectable: Boolean): String?
    external fun nativeQueryGroup(name: String, sort: String): String?
    external fun nativeHealthCheck(completable: CompletableDeferred<Unit>, name: String)
    external fun nativeHealthCheckProxy(completable: CompletableDeferred<String>, proxyName: String)
    external fun nativeHealthCheckAll()
    external fun nativePatchSelector(selector: String, name: String): Boolean
    external fun nativeFetchAndValid(
        completable: FetchCallback,
        path: String,
        url: String,
        force: Boolean,
    )

    external fun nativeLoad(completable: CompletableDeferred<Unit>, path: String)
    external fun nativeLoadCompiledConfig(completable: CompletableDeferred<Unit>, path: String)
    external fun nativeQueryProviders(): String
    external fun nativeUpdateProvider(
        completable: CompletableDeferred<Unit>,
        type: String,
        name: String,
    )

    external fun nativeQueryConfiguration(): String
    external fun nativeSubscribeLogcat(callback: LogcatInterface)
    external fun nativeCoreVersion(): String
    external fun nativeSetCustomUserAgent(userAgent: String)

    private external fun nativeInit(home: String, versionName: String, sdkVersion: Int)

    init {
        System.loadLibrary("override")
        System.loadLibrary("bridge")

        val ctx = Global.application

        ParcelFileDescriptor.open(File(ctx.packageCodePath), ParcelFileDescriptor.MODE_READ_ONLY)
            .detachFd()

        val home = ctx.runtimeHomeDir.apply { mkdirs() }.absolutePath
        val versionName = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "unknown"
        val sdkVersion = Build.VERSION.SDK_INT

        nativeInit(home, versionName, sdkVersion)
    }
}
