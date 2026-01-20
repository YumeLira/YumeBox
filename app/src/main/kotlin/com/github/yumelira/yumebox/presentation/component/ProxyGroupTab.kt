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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun LazyListScope.proxyGroupGridItems(
    groups: List<ProxyGroupInfo>,
    displayMode: ProxyDisplayMode,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupDelayClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String> = emptySet(),
) {
    val columns = if (displayMode.isSingleColumn) 1 else 2
    val showDetail = displayMode.showDetail

    if (columns == 1) {
        items(
            count = groups.size,
            key = { index -> "group_${groups[index].name}_$index" },
        ) { index ->
            val group = groups[index]
            ProxyGroupCard(
                group = group,
                showDetail = showDetail,
                isSingleColumn = true,
                isDelayTesting = testingGroupNames.contains(group.name),
                onClick = { onGroupClick(group) },
                onDelayClick = { onGroupDelayClick(group) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            )
        }
    } else {
        val rowCount = (groups.size + 1) / 2
        items(
            count = rowCount,
            key = { rowIndex ->
                val startIndex = rowIndex * 2
                val first = groups.getOrNull(startIndex)?.name.orEmpty()
                val second = groups.getOrNull(startIndex + 1)?.name.orEmpty()
                "group_${first}_${second}_$rowIndex"
            },
        ) { rowIndex ->
            val startIndex = rowIndex * 2
            val firstGroup = groups[startIndex]
            val secondGroup = groups.getOrNull(startIndex + 1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProxyGroupCard(
                        group = firstGroup,
                        showDetail = showDetail,
                        isSingleColumn = false,
                        isDelayTesting = testingGroupNames.contains(firstGroup.name),
                        onClick = { onGroupClick(firstGroup) },
                        onDelayClick = { onGroupDelayClick(firstGroup) },
                    )
                }

                if (secondGroup != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProxyGroupCard(
                            group = secondGroup,
                            showDetail = showDetail,
                            isSingleColumn = false,
                            isDelayTesting = testingGroupNames.contains(secondGroup.name),
                            onClick = { onGroupClick(secondGroup) },
                            onDelayClick = { onGroupDelayClick(secondGroup) },
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProxyGroupCard(
    group: ProxyGroupInfo,
    showDetail: Boolean,
    isSingleColumn: Boolean,
    onClick: () -> Unit,
    onDelayClick: () -> Unit,
    isDelayTesting: Boolean,
    modifier: Modifier = Modifier,
) {
    val summary = group.now.ifBlank { MLang.Proxy.Mode.Direct }
    val proxiesState = rememberUpdatedState(group.proxies)
    val nowState = rememberUpdatedState(group.now)
    val selectedDelay: Int? by remember {
        derivedStateOf { proxiesState.value.firstOrNull { it.name == nowState.value }?.delay }
    }
    val delay = selectedDelay

    ProxySelectableCard(
        isSelected = false,
        onClick = onClick,
        modifier = modifier,
    ) {
        if (isSingleColumn) {
            if (showDetail) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(ProxyCardDefaults.TextSpacing))
                        Text(
                            text = summary,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (delay != null) {
                            DelayPill(
                                delay = delay,
                                onClick = onDelayClick,
                                isLoading = isDelayTesting,
                                textStyle = MiuixTheme.textStyles.body2,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group.name,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (delay != null) DelayPill(delay = delay, onClick = null, textStyle = MiuixTheme.textStyles.body2)
                }
            }
        } else {
            if (showDetail) {
                Column(verticalArrangement = Arrangement.spacedBy(ProxyCardDefaults.TextSpacing)) {
                    Text(
                        text = group.name,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = summary,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (delay != null) {
                            DelayPill(
                                delay = delay,
                                onClick = onDelayClick,
                                isLoading = isDelayTesting,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group.name,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (delay != null) DelayPill(delay = delay, onClick = onDelayClick, isLoading = isDelayTesting)
                }
            }
        }
    }
}
