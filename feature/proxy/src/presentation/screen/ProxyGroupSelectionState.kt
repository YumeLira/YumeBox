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

package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo

data class ProxyGroupSelectionState(
    val selectedGroupName: String?,
    val selectedGroup: ProxyGroupInfo?,
    val displayGroup: ProxyGroupInfo?,
    val selectGroup: (ProxyGroupInfo) -> Unit,
    val clearSelection: () -> Unit,
)

@Composable
fun rememberProxyGroupSelectionState(
    proxyGroups: List<ProxyGroupInfo>,
    onRefreshGroup: (String) -> Unit,
    retainLastKnownGroup: Boolean,
): ProxyGroupSelectionState {
    val selectedGroupNameState = rememberSaveable { mutableStateOf<String?>(null) }
    val selectedGroupSnapshotState = remember { mutableStateOf<ProxyGroupInfo?>(null) }
    val selectGroup = remember {
        { group: ProxyGroupInfo -> selectedGroupNameState.value = group.name }
    }
    val clearSelection = remember { { selectedGroupNameState.value = null } }
    val selectedGroupName = selectedGroupNameState.value
    val selectedGroup =
        remember(selectedGroupName, proxyGroups) {
            selectedGroupName?.let { groupName ->
                proxyGroups.firstOrNull { group -> group.name == groupName }
            }
        }
    val displayGroup =
        remember(selectedGroup, selectedGroupSnapshotState.value, retainLastKnownGroup) {
            selectedGroup ?: selectedGroupSnapshotState.value.takeIf { retainLastKnownGroup }
        }

    LaunchedEffect(selectedGroup, retainLastKnownGroup) {
        if (retainLastKnownGroup) {
            selectedGroup?.let { selectedGroupSnapshotState.value = it }
        }
    }

    LaunchedEffect(selectedGroupName, selectedGroup, retainLastKnownGroup) {
        if (!retainLastKnownGroup && selectedGroupName != null && selectedGroup == null) {
            selectedGroupNameState.value = null
        }
    }

    LaunchedEffect(selectedGroupName) { selectedGroupName?.let(onRefreshGroup) }

    return remember(selectedGroupName, selectedGroup, displayGroup, selectGroup, clearSelection) {
        ProxyGroupSelectionState(
            selectedGroupName = selectedGroupName,
            selectedGroup = selectedGroup,
            displayGroup = displayGroup,
            selectGroup = selectGroup,
            clearSelection = clearSelection,
        )
    }
}
