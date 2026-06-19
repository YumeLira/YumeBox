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

package com.github.yumelira.yumebox.presentation.screen.node

import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.data.model.ProxySortMode
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.window.WindowListPopup

internal val NodeSortModes =
    listOf(ProxySortMode.DEFAULT, ProxySortMode.BY_NAME, ProxySortMode.BY_LATENCY)

@Composable
internal fun NodeSortPopup(
    show: Boolean,
    onDismiss: () -> Unit,
    sortMode: ProxySortMode,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    val selectedSortIndex = NodeSortModes.indexOf(sortMode).coerceAtLeast(0)
    WindowListPopup(
        show = show,
        popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
        alignment = alignment,
        onDismissRequest = onDismiss,
    ) {
        ListPopupColumn {
            NodeSortModes.forEachIndexed { index, mode ->
                DropdownImpl(
                    text = mode.displayName,
                    optionSize = NodeSortModes.size,
                    isSelected = selectedSortIndex == index,
                    onSelectedIndexChange = {
                        if (mode != sortMode) onSortSelected(mode)
                        onDismiss()
                    },
                    index = index,
                )
            }
        }
    }
}
