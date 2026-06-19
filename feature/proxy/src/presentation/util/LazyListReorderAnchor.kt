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

package com.github.yumelira.yumebox.presentation.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlin.math.abs

@Stable private data class LazyListTopAnchor(val key: String, val offset: Int)

@Composable
fun KeepLazyListTopAnchorOnReorder(
    listState: LazyListState,
    itemKeys: List<String>,
    enabled: Boolean,
    scrollToTopOnEnabled: Boolean = false,
) {
    var pendingAnchor by remember(listState) { mutableStateOf<LazyListTopAnchor?>(null) }
    var previousEnabled by remember(listState) { mutableStateOf(enabled) }

    LaunchedEffect(enabled, scrollToTopOnEnabled, itemKeys) {
        when {
            scrollToTopOnEnabled && enabled && !previousEnabled && itemKeys.isNotEmpty() -> {
                listState.scrollToItem(0)
            }

            !scrollToTopOnEnabled && enabled && pendingAnchor != null -> {
                val anchor = pendingAnchor ?: return@LaunchedEffect
                val targetIndex = itemKeys.indexOf(anchor.key)
                if (targetIndex >= 0) {
                    val topNow = listState.captureTopAnchor()
                    if (
                        topNow == null ||
                            topNow.key != anchor.key ||
                            abs(topNow.offset - anchor.offset) > 1
                    ) {
                        listState.scrollToItem(targetIndex, anchor.offset)
                    }
                }
                pendingAnchor = null
            }
        }
        previousEnabled = enabled
    }

    DisposableEffect(listState, itemKeys, enabled) {
        onDispose {
            if (enabled && itemKeys.isNotEmpty() && !scrollToTopOnEnabled) {
                pendingAnchor = listState.captureTopAnchor()
            }
        }
    }
}

private fun LazyListState.captureTopAnchor(): LazyListTopAnchor? {
    val layoutInfo = layoutInfo
    val topItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    val key = topItem.key as? String ?: return null
    val offset = (topItem.offset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
    return LazyListTopAnchor(key = key, offset = offset)
}
