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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.runtime.Composable
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

@Composable
fun NullableBooleanSelector(
    title: String,
    summary: String? = null,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit,
) {
    val items =
        listOf(
            MLang.Component.Selector.NotModify,
            MLang.Component.Selector.Enable,
            MLang.Component.Selector.Disable,
        )
    val selectedIndex =
        when (value) {
            null -> 0
            true -> 1
            false -> 2
        }

    WindowDropdownPreference(
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

    WindowDropdownPreference(
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

enum class ListMergeStrategy {

    None,
    Replace,
    Start,
    End,
}

enum class MapMergeStrategy {

    None,
    Replace,
    Merge,
}

@Composable
fun ListMergeStrategySelector(
    title: String,
    summary: String? = null,
    value: ListMergeStrategy,
    onValueChange: (ListMergeStrategy) -> Unit,
) {
    val items =
        listOf(
            MLang.Component.Selector.NotModify,
            MLang.Component.Selector.Replace,
            MLang.Component.Selector.Prepend,
            MLang.Component.Selector.Append,
        )
    val values =
        listOf(
            ListMergeStrategy.None,
            ListMergeStrategy.Replace,
            ListMergeStrategy.Start,
            ListMergeStrategy.End,
        )

    NullableEnumSelector(
        title = title,
        summary = summary,
        value = value,
        items = items,
        values = values,
        onValueChange = { onValueChange(it ?: ListMergeStrategy.None) },
    )
}

@Composable
fun MapMergeStrategySelector(
    title: String,
    summary: String? = null,
    value: MapMergeStrategy,
    onValueChange: (MapMergeStrategy) -> Unit,
) {
    val items =
        listOf(
            MLang.Component.Selector.NotModify,
            MLang.Component.Selector.Replace,
            MLang.Component.Selector.Merge,
        )
    val values = listOf(MapMergeStrategy.None, MapMergeStrategy.Replace, MapMergeStrategy.Merge)

    NullableEnumSelector(
        title = title,
        summary = summary,
        value = value,
        items = items,
        values = values,
        onValueChange = { onValueChange(it ?: MapMergeStrategy.None) },
    )
}
