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



package com.github.yumelira.yumebox.service.runtime.util

internal fun mergeProxyGroupNames(
    expectedNames: List<String>,
    runtimeNames: List<String>,
    predicate: (String) -> Boolean = { true },
): List<String> {
    fun MutableList<String>.addNames(names: List<String>) {
        names.forEach { groupName ->
            if (groupName.isBlank()) return@forEach
            if (!predicate(groupName)) return@forEach
            if (groupName !in this) {
                add(groupName)
            }
        }
    }

    return buildList(expectedNames.size + runtimeNames.size) {
        addNames(expectedNames)
        addNames(runtimeNames)
    }
}
