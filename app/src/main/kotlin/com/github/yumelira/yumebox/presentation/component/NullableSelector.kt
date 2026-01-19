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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.runtime.Composable
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.extra.SuperDropdown

@Composable
fun NullableBooleanSelector(
    title: String,
    summary: String? = null,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit,
) {
    val items =
        listOf(MLang.Component.Selector.NotModify, MLang.Component.Selector.Enable, MLang.Component.Selector.Disable)
    val selectedIndex = when (value) {
        null -> 0
        true -> 1
        false -> 2
    }

    SuperDropdown(
        title = title,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            onValueChange(
                when (index) {
                    1 -> true
                    2 -> false
                    else -> null
                }
            )
        },
    )
}

@Composable
fun <T> NullableEnumSelector(
    title: String,
    summary: String? = null,
    value: T?,
    items: List<String>,
    values: List<T?>,
    onValueChange: (T?) -> Unit,
) {
    val selectedIndex = values.indexOf(value).coerceAtLeast(0)

    SuperDropdown(
        title = title,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            if (index >= 0 && index < values.size) {
                onValueChange(values[index])
            }
        },
    )
}
