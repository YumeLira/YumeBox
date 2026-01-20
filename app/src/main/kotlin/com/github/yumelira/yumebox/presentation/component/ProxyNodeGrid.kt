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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode

@Composable
fun ProxyNodeGrid(
    proxies: List<Proxy>,
    selectedProxyName: String,
    displayMode: ProxyDisplayMode,
    onProxyClick: ((String) -> Unit)? = null,
    onProxyDelayClick: ((String) -> Unit)? = null,
    isDelayTesting: Boolean = false,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val columns = if (displayMode.isSingleColumn) 1 else 2
    val maxAnimatedPills = if (proxies.size <= 40) {
        proxies.size
    } else {
        12
    }
    val contentType = when {
        displayMode.isSingleColumn -> "single"
        displayMode.showDetail -> "detail"
        else -> "compact"
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = proxies,
            key = { _, proxy -> proxy.name },
            contentType = { _, _ -> contentType },
        ) { index, proxy ->
            val proxyName = proxy.name
            val onClick = onProxyClick?.let { handler ->
                remember(proxyName, handler) { { handler(proxyName) } }
            }
            val onDelayClick = onProxyDelayClick?.let { handler ->
                remember(proxyName, handler) { { handler(proxyName) } }
            }
            val isLoading = isDelayTesting && index < maxAnimatedPills
            ProxyNodeCard(
                name = proxyName,
                typeName = proxy.type.name,
                delay = proxy.delay,
                isSelected = proxy.name == selectedProxyName,
                onClick = onClick,
                isSingleColumn = displayMode.isSingleColumn,
                showDetail = displayMode.showDetail,
                onDelayClick = onDelayClick,
                isDelayTesting = isLoading,
                enableLoadingAnimation = isLoading,
            )
        }
    }
}
