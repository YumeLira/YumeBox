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

package com.github.yumelira.yumebox.presentation.screen.node

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.state.IntColorDrawableStateImage
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.presentation.component.LoadingDotsWave
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.panpf.sketch.AsyncImage as SketchAsyncImage

internal fun nodeLatencyLabel(delay: Int?): Pair<String, Color>? = when {
    delay == null -> null
    delay < 0 -> "TIMEOUT" to Color(0xFF9E9E9E)
    delay == 0 -> null
    delay in 1..800 -> "${delay}ms" to Color(0xFF4CAF50)
    delay in 801..5000 -> "${delay}ms" to Color(0xFFFFA726)
    else -> null
}

internal fun LazyListScope.nodeGroupItems(
    groups: List<ProxyGroupInfo>,
    displayMode: ProxyDisplayMode,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupDelayClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String> = emptySet(),
    onGroupBoundsChanged: ((String, Rect) -> Unit)? = null,
    itemVerticalPadding: Dp = 6.dp,
) {
    val showDetail = displayMode.showDetail

    items(
        items = groups,
        key = { group -> "${group.type.name}:${group.name}" },
        contentType = { "NodeGroupCard" },
    ) { group ->
        NodeGroupCard(
            group = group,
            showDetail = showDetail,
            isDelayTesting = testingGroupNames.contains(group.name),
            onClick = { onGroupClick(group) },
            onDelayClick = { onGroupDelayClick(group) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = itemVerticalPadding),
            onBoundsChanged = onGroupBoundsChanged?.let { callback ->
                { bounds -> callback(group.name, bounds) }
            },
        )
    }
}

@Composable
internal fun NodeGroupCard(
    group: ProxyGroupInfo,
    showDetail: Boolean,
    isDelayTesting: Boolean,
    onClick: () -> Unit,
    onDelayClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    textAlpha: Float = 1f,
) {
    val resolvedTextAlpha = textAlpha.coerceIn(0f, 1f)
    val currentNode = remember(group.now) { extractFlaggedName(group.now) }
    val summary = remember(currentNode.displayName) {
        currentNode.displayName.ifBlank { MLang.Proxy.Mode.Direct }
    }
    val iconUri = remember(group.icon) {
        group.icon
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(::normalizeNodeGroupIconUri)
    }
    val delay = remember(group.proxies, group.now) {
        group.proxies.firstOrNull { it.name == group.now }?.delay
    }

    NodeSelectableCard(
        isSelected = false,
        onClick = onClick,
        modifier = modifier.let { base ->
            if (onBoundsChanged == null) {
                base
            } else {
                base.onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInWindow())
                }
            }
        },
    ) {
        val testingColor = MiuixTheme.colorScheme.primary
        val delayLabel = remember(delay) {
            nodeLatencyLabel(delay)
        }
        val delaySlotModifier = Modifier.width(56.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconUri != null) {
                NodeGroupIcon(
                    iconUri = iconUri,
                    modifier = Modifier
                        .size(NodeIconDefaults.Size)
                        .clip(NodeIconDefaults.Shape),
                )
                Spacer(modifier = Modifier.width(NodeIconDefaults.Gap))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(resolvedTextAlpha),
                )

                if (showDetail) {
                    Spacer(modifier = Modifier.height(NodeCardDefaults.TextSpacing))
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(resolvedTextAlpha),
                    )
                }
            }

            if (isDelayTesting) {
                Box(
                    modifier = delaySlotModifier
                        .height(14.dp)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(modifier = Modifier.alpha(resolvedTextAlpha)) {
                        LoadingDotsWave(color = testingColor)
                    }
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
                        .alpha(resolvedTextAlpha)
                        .clickable(onClick = onDelayClick),
                )
            }
        }
    }
}

private object NodeIconDefaults {
    val Size = 36.dp
    val Gap = 16.dp
    val Shape = RoundedCornerShape(6.dp)
}

private fun normalizeNodeGroupIconUri(raw: String): String {
    if (raw.startsWith("//")) return "https:$raw"
    if (raw.startsWith("www.", ignoreCase = true)) return "https://$raw"
    return raw
}

@Composable
private fun NodeGroupIcon(
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
        request = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
