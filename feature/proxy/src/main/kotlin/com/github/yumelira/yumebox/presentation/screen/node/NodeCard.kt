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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.presentation.component.CountryFlagCircle
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal object NodeCardDefaults {
    val CornerRadius = 12.dp
    val PaddingHorizontal = 12.dp
    val PaddingVertical = 16.dp
    val TextSpacing = 8.dp
}

private fun latencyDisplay(delay: Int, withUnit: Boolean): Pair<String, Color>? = when {
    delay < 0 -> "TIMEOUT" to Color(0xFF9E9E9E)
    delay in 1..800 -> "${delay}${if (withUnit) "ms" else ""}" to Color(0xFF4CAF50)
    delay in 801..5000 -> "${delay}${if (withUnit) "ms" else ""}" to Color(0xFFFFA726)
    else -> null
}

@Composable
internal fun NodeSelectableCard(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    paddingVertical: Dp = NodeCardDefaults.PaddingVertical,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(NodeCardDefaults.CornerRadius)
    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MiuixTheme.colorScheme.background
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, shape)
            .let {
                if (onClick != null) {
                    it.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    it
                }
            }
            .padding(
                horizontal = NodeCardDefaults.PaddingHorizontal,
                vertical = paddingVertical,
            ),
        content = content,
    )
}

@Composable
internal fun NodeCard(
    proxy: Proxy,
    isSelected: Boolean,
    onClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    isSingleColumn: Boolean = false,
    showDetail: Boolean = false,
    isDelayTesting: Boolean = false,
    onDelayTestClick: (() -> Unit)? = null,
    showCountryFlag: Boolean = true,
) {
    val onCardClick = remember(proxy.name, onClick) {
        onClick?.let { click -> { click(proxy.name) } }
    }
    NodeSelectableCard(
        isSelected = isSelected,
        onClick = onCardClick,
        modifier = modifier,
        paddingVertical = 12.dp,
    ) {
        val textColor = if (isSelected) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.onSurface
        }
        val flagged = remember(proxy.name) { extractFlaggedName(proxy.name) }
        val countryCode = flagged.countryCode
        val delayDisplay = remember(proxy.delay, isSingleColumn) {
            latencyDisplay(proxy.delay, withUnit = isSingleColumn)
        }

        if (isSingleColumn) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NodeTitleRow(
                    displayName = flagged.displayName,
                    countryCode = countryCode.takeIf { showCountryFlag },
                    textColor = textColor,
                    allowSoftWrap = true,
                )
                if (showDetail) {
                    Spacer(modifier = Modifier.height(6.dp))
                    NodeDetailRow(
                        typeName = proxy.type.name,
                        delayDisplay = delayDisplay,
                        isDelayTesting = isDelayTesting,
                        onDelayTestClick = onDelayTestClick,
                        allowSoftWrap = true,
                    )
                }
            }
        } else if (showDetail) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NodeTitleRow(
                    displayName = flagged.displayName,
                    countryCode = countryCode.takeIf { showCountryFlag },
                    textColor = textColor,
                    allowSoftWrap = false,
                )
                Spacer(modifier = Modifier.height(6.dp))
                NodeDetailRow(
                    typeName = proxy.type.name,
                    delayDisplay = delayDisplay,
                    isDelayTesting = isDelayTesting,
                    onDelayTestClick = onDelayTestClick,
                    allowSoftWrap = false,
                )
            }
        } else {
            NodeTitleRow(
                displayName = flagged.displayName,
                countryCode = countryCode.takeIf { showCountryFlag },
                textColor = textColor,
                allowSoftWrap = true,
            )
        }
    }
}

@Composable
private fun NodeTitleRow(
    displayName: String,
    countryCode: String?,
    textColor: Color,
    allowSoftWrap: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (countryCode != null) {
            CountryFlagCircle(countryCode = countryCode, size = 16.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = displayName,
            style = MiuixTheme.textStyles.body2,
            color = textColor,
            maxLines = 1,
            softWrap = allowSoftWrap,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NodeDetailRow(
    typeName: String,
    delayDisplay: Pair<String, Color>?,
    isDelayTesting: Boolean,
    onDelayTestClick: (() -> Unit)?,
    allowSoftWrap: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = typeName,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            softWrap = allowSoftWrap,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        NodeDelayIndicator(
            delayDisplay = delayDisplay,
            isDelayTesting = isDelayTesting,
            onDelayTestClick = onDelayTestClick,
        )
    }
}

@Composable
private fun NodeDelayIndicator(
    delayDisplay: Pair<String, Color>?,
    isDelayTesting: Boolean,
    onDelayTestClick: (() -> Unit)?,
) {
    val slotModifier = Modifier.width(56.dp)
    if (isDelayTesting) {
        Text(
            text = "...",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = slotModifier,
        )
        return
    }

    if (delayDisplay == null) return
    val (delayText, delayColor) = delayDisplay

    Text(
        text = delayText,
        style = MiuixTheme.textStyles.footnote1,
        color = delayColor,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.End,
        modifier = Modifier.let { base ->
            val slotted = base.then(slotModifier)
            if (onDelayTestClick != null) {
                slotted.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDelayTestClick,
                )
            } else {
                slotted
            }
        },
    )
}
