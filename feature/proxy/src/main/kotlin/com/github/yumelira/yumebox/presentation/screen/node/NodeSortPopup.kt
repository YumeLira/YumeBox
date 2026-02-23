package com.github.yumelira.yumebox.presentation.screen.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.extra.WindowListPopup

internal val NodeSortModes = listOf(
    ProxySortMode.DEFAULT,
    ProxySortMode.BY_NAME,
    ProxySortMode.BY_LATENCY,
)

@Composable
internal fun NodeSortPopup(
    show: MutableState<Boolean>,
    onDismiss: () -> Unit,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    val selectedSortIndex = NodeSortModes.indexOf(sortMode).coerceAtLeast(0)
    WindowListPopup(
        show = show,
        popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
        alignment = PopupPositionProvider.Align.Start,
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
