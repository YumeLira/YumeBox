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

package com.github.yumelira.yumebox.core.domain

import com.github.yumelira.yumebox.core.model.ConnectionInfo

object ConnectionHistoryManager {

    private const val MAX_SIZE = 100

    private val _closedConnections = mutableListOf<ConnectionInfo>()
    private var previousConnections: Map<String, ConnectionInfo> = emptyMap()
    private val lock = Any()

    fun updateConnections(currentConnections: List<ConnectionInfo>) {
        synchronized(lock) {
            val currentMap = currentConnections.associateBy { it.id }
            val currentIds = currentMap.keys

            previousConnections.keys.minus(currentIds).forEach { closedId ->
                previousConnections[closedId]?.let { conn -> _closedConnections.add(0, conn) }
            }

            while (_closedConnections.size > MAX_SIZE) {
                _closedConnections.removeAt(_closedConnections.lastIndex)
            }

            previousConnections = currentMap
        }
    }

    fun getClosedConnections(): List<ConnectionInfo> {
        synchronized(lock) {
            return _closedConnections.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            _closedConnections.clear()
            previousConnections = emptyMap()
        }
    }
}
