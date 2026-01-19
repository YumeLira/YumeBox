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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProxyGroupTabs(
    groups: List<ProxyGroupInfo>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val tabPositions = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }

    LaunchedEffect(selectedIndex, groups.size) {
        if (selectedIndex in groups.indices && tabPositions.containsKey(selectedIndex)) {
            val (tabPosition, tabWidth) = tabPositions[selectedIndex]!!
            val viewportWidth = scrollState.viewportSize.toFloat()

            val targetScroll = (tabPosition + tabWidth / 2 - viewportWidth / 2)
                .coerceIn(0f, scrollState.maxValue.toFloat())

            coroutineScope.launch {
                scrollState.animateScrollTo(targetScroll.toInt())
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .horizontalScroll(
                state = scrollState,
                flingBehavior = ScrollableDefaults.flingBehavior()
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        groups.forEachIndexed { index, group ->
            ProxyGroupTab(
                text = group.name,
                isSelected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    tabPositions[index] = Pair(
                        coordinates.positionInParent().x,
                        coordinates.size.width.toFloat()
                    )
                }
            )
        }
    }
}

@Composable
private fun ProxyGroupTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = textColor,
            maxLines = 1
        )
    }
}
