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

package com.github.yumelira.yumebox.service.clash.module

import android.app.Service
import com.github.yumelira.yumebox.core.Clash
import java.security.SecureRandom

class HttpModule(service: Service) : Module<Unit>(service) {

    private val random = SecureRandom()

    fun start(): String? {
        val r = { 1 + random.nextInt(199) }
        val listenAt = "127.${r()}.${r()}.${r()}:0"
        return Clash.startHttp(listenAt)
    }

    override suspend fun run() {}

    fun stop() {
        Clash.stopHttp()
    }

    companion object {
        fun requestStop() {
            Clash.stopHttp()
        }
    }
}
