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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.state.IntColorDrawableStateImage
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.panpf.sketch.AsyncImage as SketchAsyncImage

private fun delayLabel(delay: Int?): Pair<String, Color>? = when {
    delay == null -> null
    delay < 0 -> "TIMEOUT" to Color(0xFF9E9E9E)
    delay == 0 -> null
    delay in 1..800 -> "${delay}ms" to Color(0xFF4CAF50)
    delay in 801..5000 -> "${delay}ms" to Color(0xFFFFA726)
    else -> null
}

fun LazyListScope.proxyGroupGridItems(
    groups: List<ProxyGroupInfo>,
    displayMode: ProxyDisplayMode,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupDelayClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String> = emptySet(),
) {
    val showDetail = displayMode.showDetail

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
    val summary = remember(group.now) {
        extractFlaggedName(group.now).displayName.ifBlank { MLang.Proxy.Mode.Direct }
    }
    val iconUri = remember(group.icon) { group.icon?.trim()?.takeIf { it.isNotEmpty() } }
    val delay = remember(group.proxies, group.now) {
        group.proxies.firstOrNull { it.name == group.now }?.delay
    }

    ProxySelectableCard(
        isSelected = false,
        onClick = onClick,
        modifier = modifier,
    ) {
        val testingColor = MiuixTheme.colorScheme.primary
        val delayLabel = remember(delay) {
            delayLabel(delay)
        }
        val delaySlotModifier = Modifier.width(56.dp)
        if (isSingleColumn) {
            if (showDetail) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (iconUri != null) {
                        ProxyGroupIcon(
                            iconUri = iconUri,
                            modifier = Modifier
                                .size(ProxyIconDefaults.Size)
                                .clip(ProxyIconDefaults.Shape),
                        )
                        Spacer(modifier = Modifier.width(ProxyIconDefaults.Gap))
                    }
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
                    if (isDelayTesting) {
                        Box(
                            modifier = delaySlotModifier
                                .height(14.dp)
                                .padding(start = 8.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            LoadingDotsWave(color = testingColor)
                        }
                    } else if (delayLabel != null) {
                        val (delayText, delayColor) = delayLabel
                        Text(
                            text = delayText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = delayColor,
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .then(delaySlotModifier)
                                .clickable(onClick = onDelayClick),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (iconUri != null) {
                        ProxyGroupIcon(
                            iconUri = iconUri,
                            modifier = Modifier
                                .size(ProxyIconDefaults.Size)
                                .clip(ProxyIconDefaults.Shape),
                        )
                        Spacer(modifier = Modifier.width(ProxyIconDefaults.Gap))
                    }
                    Text(
                        text = group.name,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDelayTesting) {
                        Box(
                            modifier = delaySlotModifier
                                .height(14.dp)
                                .padding(start = 8.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            LoadingDotsWave(color = testingColor)
                        }
                    } else if (delayLabel != null) {
                        val (delayText, delayColor) = delayLabel
                        Text(
                            text = delayText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = delayColor,
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .then(delaySlotModifier)
                                .clickable(onClick = onDelayClick),
                        )
                    }
                }
            }
        } else {
            if (showDetail) {
                Column(verticalArrangement = Arrangement.spacedBy(ProxyCardDefaults.TextSpacing)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (iconUri != null) {
                            ProxyGroupIcon(
                                iconUri = iconUri,
                                modifier = Modifier
                                    .size(ProxyIconDefaults.Size)
                                    .clip(ProxyIconDefaults.Shape),
                            )
                            Spacer(modifier = Modifier.width(ProxyIconDefaults.Gap))
                        }
                        Text(
                            text = group.name,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                        if (isDelayTesting) {
                            Box(
                                modifier = delaySlotModifier
                                    .height(14.dp)
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                LoadingDotsWave(color = testingColor)
                            }
                        } else if (delayLabel != null) {
                            val (delayText, delayColor) = delayLabel
                            Text(
                                text = delayText,
                                style = MiuixTheme.textStyles.footnote1,
                                color = delayColor,
                                maxLines = 1,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .then(delaySlotModifier)
                                    .clickable(onClick = onDelayClick),
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
                    if (iconUri != null) {
                        ProxyGroupIcon(
                            iconUri = iconUri,
                            modifier = Modifier
                                .size(ProxyIconDefaults.Size)
                                .clip(ProxyIconDefaults.Shape),
                        )
                        Spacer(modifier = Modifier.width(ProxyIconDefaults.Gap))
                    }
                    Text(
                        text = group.name,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDelayTesting) {
                        Box(
                            modifier = delaySlotModifier
                                .height(14.dp)
                                .padding(start = 8.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            LoadingDotsWave(color = testingColor)
                        }
                    } else if (delayLabel != null) {
                        val (delayText, delayColor) = delayLabel
                        Text(
                            text = delayText,
                            style = MiuixTheme.textStyles.footnote1,
                            color = delayColor,
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .then(delaySlotModifier)
                                .clickable(onClick = onDelayClick),
                        )
                    }
                }
            }
        }
    }
}

private object ProxyIconDefaults {
    val Size = 36.dp
    val Gap = 16.dp
    val Shape = RoundedCornerShape(6.dp)
}

@Composable
private fun ProxyGroupIcon(
    iconUri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val placeholderColorInt = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f).toArgb()
    val request = remember(context, iconUri, placeholderColorInt) {
        ImageRequest(context, iconUri) {
            placeholder(IntColorDrawableStateImage(placeholderColorInt))
            error(IntColorDrawableStateImage(placeholderColorInt))
            crossfade(true)
        }
    }

    SketchAsyncImage(
        request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
