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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

internal fun LazyListScope.nodeGridItems(
    proxies: List<Proxy>,
    selectedProxyName: String,
    onProxyClick: ((String) -> Unit)? = null,
    isDelayTesting: Boolean = false,
    testingProxyNames: Set<String> = emptySet(),
    onSingleNodeTestClick: ((String) -> Unit)? = null,
    outerHorizontalPadding: Dp = UiDp.dp0,
    itemVerticalPadding: Dp = UiDp.dp0,
    singleNodeTestEnabled: Boolean = true,
) {
    items(items = proxies, key = { it.name }, contentType = { "NodeCard1" }) { proxy ->
        NodeCard(
            proxy = proxy,
            isSelected = proxy.name == selectedProxyName,
            onClick = onProxyClick,
            isDelayTesting = isDelayTesting,
            isThisProxyTesting = proxy.name in testingProxyNames,
            onSingleNodeTestClick = onSingleNodeTestClick,
            showCountryFlag = true,
            singleNodeTestEnabled = singleNodeTestEnabled,
            modifier =
                Modifier.padding(
                    horizontal = outerHorizontalPadding,
                    vertical = itemVerticalPadding,
                ),
        )
    }
}

@Composable
internal fun NodeGrid(
    proxies: List<Proxy>,
    selectedProxyName: String,
    onProxyClick: ((String) -> Unit)? = null,
    isDelayTesting: Boolean = false,
    testingProxyNames: Set<String> = emptySet(),
    onSingleNodeTestClick: ((String) -> Unit)? = null,
    listStateKey: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(UiDp.dp0),
    singleNodeTestEnabled: Boolean = true,
) {
    val listState = rememberSaveable(listStateKey, saver = LazyListState.Saver) { LazyListState() }
    LazyColumn(
        modifier = modifier.scrollEndHaptic().overScrollVertical(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        overscrollEffect = null,
    ) {
        nodeGridItems(
            proxies = proxies,
            selectedProxyName = selectedProxyName,
            onProxyClick = onProxyClick,
            isDelayTesting = isDelayTesting,
            testingProxyNames = testingProxyNames,
            onSingleNodeTestClick = onSingleNodeTestClick,
            singleNodeTestEnabled = singleNodeTestEnabled,
        )
    }
}
