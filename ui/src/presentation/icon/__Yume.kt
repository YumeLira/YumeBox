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

package com.github.yumelira.yumebox.presentation.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yumelira.yumebox.presentation.icon.yume.Github
import kotlin.collections.List as ____KtList
import kotlin.collections.Map as ____KtMap

object Yume

private var __AllIcons: ____KtList<ImageVector>? = null

val Yume.AllIcons: ____KtList<ImageVector>
    get() {
        if (__AllIcons != null) {
            return __AllIcons!!
        }
        __AllIcons = listOf(Github)
        return __AllIcons!!
    }

private var __AllIconsNamed: ____KtMap<String, ImageVector>? = null

val Yume.AllIconsNamed: ____KtMap<String, ImageVector>
    get() {
        if (__AllIconsNamed != null) {
            return __AllIconsNamed!!
        }
        __AllIconsNamed = mapOf("github" to Github)
        return __AllIconsNamed!!
    }
