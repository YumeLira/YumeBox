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

package com.github.yumelira.yumebox.service.runtime.session

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import java.security.SecureRandom

class LocalHttpTransport(context: Context) : RuntimeTransport {
    private val appContext = context.appContextOrSelf
    private val random = SecureRandom()
    private val startupLogStore =
        RuntimeStartupLogStore(appContext, RuntimeStartupLogStore.Scope.LOCAL_HTTP)

    override fun start(spec: RuntimeSpec) {
        startupLogStore.append("LOCAL_HTTP transport start: begin")
        val address =
            Clash.startHttp(randomLoopbackListenAddress())
                ?: error("startHttp returned null listen address")
        startupLogStore.append("LOCAL_HTTP transport start: done listen=$address")
    }

    override fun stop() {
        Clash.stopHttp()
    }

    private fun randomLoopbackListenAddress(): String {
        val part = { 1 + random.nextInt(199) }
        return "127.${part()}.${part()}.${part()}:0"
    }
}
