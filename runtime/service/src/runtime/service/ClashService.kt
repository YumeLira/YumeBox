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

package com.github.yumelira.yumebox.runtime.service

import android.content.Intent
import android.os.IBinder
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.runtime.api.appContextOrSelf
import com.github.yumelira.yumebox.runtime.service.notification.ServiceNotificationManager
import com.github.yumelira.yumebox.runtime.service.session.LocalHttpTransport
import com.github.yumelira.yumebox.runtime.service.session.RuntimeStartupLogStore
import com.github.yumelira.yumebox.runtime.service.session.SessionRuntimeSpecFactory

class ClashService : BaseService() {
    private val controller =
        RuntimeForegroundController(
            service = this,
            scope = this,
            mode = ProxyMode.Http,
            label = "HTTP",
            notificationConfig = ServiceNotificationManager.httpConfig,
            logScope = RuntimeStartupLogStore.Scope.LOCAL_HTTP,
            createTransport = { LocalHttpTransport(this) },
            createSpec = { SessionRuntimeSpecFactory(appContextOrSelf).createHttpSpec() },
        )

    override fun onCreate() {
        super.onCreate()
        controller.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        controller.onStartCommand(startId)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        controller.onTrimMemory()
    }
}
