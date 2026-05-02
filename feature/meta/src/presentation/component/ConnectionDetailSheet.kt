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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */


package com.github.yumelira.yumebox.feature.meta.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.core.model.ConnectionInfo
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val CONNECTION_LEADING_ICON_BITMAP_SIZE = 96

@Composable
fun ConnectionDetailSheet(
    show: Boolean,
    connectionInfo: ConnectionInfo?,
    canInterrupt: Boolean,
    onInterruptConnection: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit = {},
) {
    val spacing = AppTheme.spacing
    val scope = rememberCoroutineScope()
    var isInterrupting by remember(connectionInfo?.id, show) { mutableStateOf(false) }
    val detailState = remember(connectionInfo) {
        connectionInfo?.toDetailState()
    }

    AppActionBottomSheet(
        show = show,
        title = detailState?.displayHost.orEmpty(),
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        val info = connectionInfo
        val state = detailState
        if (info != null && state != null) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space16),
            ) {
                item {
                    ConnectionInfoSection(
                        state = state,
                        upload = info.upload,
                        download = info.download,
                        chains = info.chains,
                    )
                }

                if (info.rule.isNotEmpty()) {
                    item {
                        RuleInfoSection(
                            rule = info.rule,
                            rulePayload = info.rulePayload,
                        )
                    }
                }

                if (canInterrupt) {
                    item {
                        InterruptConnectionButton(
                            isInterrupting = isInterrupting,
                            onInterrupt = {
                                if (isInterrupting) return@InterruptConnectionButton
                                isInterrupting = true
                                scope.launch {
                                    val closed = runCatching {
                                        onInterruptConnection(info.id)
                                    }.getOrDefault(false)
                                    isInterrupting = false
                                    if (closed) {
                                        onDismiss()
                                    }
                                }
                            },
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(spacing.space16))
                }
            }
        }
    }
}

@Composable
private fun ConnectionInfoSection(
    state: ConnectionDetailState,
    upload: Long,
    download: Long,
    chains: List<String>,
) {
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val appColors = AppTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space12),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space12),
        ) {
            ConnectionLeadingIcon(
                metadata = state.metadata,
                network = state.network,
                size = sizes.connectionLeadingIconSize,
                bitmapSize = CONNECTION_LEADING_ICON_BITMAP_SIZE,
            )
            SectionTitle(MLang.Connection.Detail.Section.Info)
        }

        InfoRow(label = MLang.Connection.Detail.Label.Protocol, value = state.network.uppercase())
        if (state.process.isNotEmpty()) {
            InfoRow(label = MLang.Connection.Detail.Label.Process, value = state.process)
        }
        InfoRow(label = MLang.Connection.Detail.Label.SourceAddress, value = state.sourceAddress)
        if (state.destinationAddress.isNotEmpty()) {
            InfoRow(label = MLang.Connection.Detail.Label.DestinationAddress, value = state.destinationAddress)
        }
        InfoRow(label = MLang.Connection.Detail.Label.Duration, value = state.duration)

        InfoRow(
            label = MLang.Connection.Detail.Label.Upload,
            value = formatBytes(upload),
            valueColor = appColors.protocol.tcp,
        )
        InfoRow(
            label = MLang.Connection.Detail.Label.Download,
            value = formatBytes(download),
            valueColor = appColors.protocol.udp,
        )

        if (chains.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.space4))
            ProxyChainRow(chains = chains)
        }
    }
}

@Composable
private fun ProxyChainRow(chains: List<String>) {
    val spacing = AppTheme.spacing
    val appColors = AppTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        chains.forEachIndexed { index, chain ->
            val isLast = index == chains.lastIndex

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                ChainNode(
                    name = chain,
                    isActive = isLast,
                )

                if (!isLast) {
                    Text(
                        text = "→",
                        style = MiuixTheme.textStyles.footnote1,
                        color = appColors.connection.chainArrow,
                        modifier = Modifier.padding(horizontal = spacing.space2),
                    )
                }
            }
        }
    }
}

@Composable
private fun InterruptConnectionButton(
    isInterrupting: Boolean,
    onInterrupt: () -> Unit,
) {
    Button(
        onClick = onInterrupt,
        enabled = !isInterrupting,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(),
    ) {
        Text(
            text = if (isInterrupting) {
                MLang.Connection.Detail.Action.Interrupting
            } else {
                MLang.Connection.Detail.Action.Interrupt
            },
            color = MiuixTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ChainNode(
    name: String,
    isActive: Boolean,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity
    val sizes = AppTheme.sizes
    val appColors = AppTheme.colors
    val backgroundColor = if (isActive) {
        appColors.connection.chainActive.copy(alpha = opacity.subtleStrong)
    } else {
        MiuixTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isActive) {
        appColors.connection.chainActive
    } else {
        appColors.connection.chainInactiveText
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(radii.radius8))
            .background(backgroundColor)
            .padding(
                horizontal = sizes.nodeChainNodeHorizontalPadding,
                vertical = sizes.nodeChainNodeVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(sizes.nodeChainIndicatorSize)
                    .clip(CircleShape)
                    .background(appColors.connection.chainActive),
            )
        }

        Text(
            text = name,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp),
            color = textColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun RuleInfoSection(
    rule: String,
    rulePayload: String,
) {
    val spacing = AppTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space12),
    ) {
        SectionTitle(MLang.Connection.Detail.Section.Rule)

        InfoRow(label = MLang.Connection.Detail.Label.Type, value = rule)
        if (rulePayload.isNotEmpty()) {
            InfoRow(label = MLang.Connection.Detail.Label.Content, value = rulePayload)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.body2.copy(
            fontWeight = FontWeight.Medium,
        ),
        color = MiuixTheme.colorScheme.onSurface,
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MiuixTheme.colorScheme.onSurface,
) {
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space16),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.width(sizes.connectionDetailLabelWidth),
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1,
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun calculateDuration(start: String): String {
    if (start.isEmpty()) return "00:00:00"

    return try {
        val startTime = java.time.OffsetDateTime.parse(start).toInstant()
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(startTime, now)

        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } catch (error: Exception) {
        "00:00:00"
    }
}

private data class ConnectionDetailState(
    val metadata: JsonObject,
    val displayHost: String,
    val network: String,
    val process: String,
    val sourceAddress: String,
    val destinationAddress: String,
    val duration: String,
)

private fun ConnectionInfo.toDetailState(): ConnectionDetailState {
    val host = metadata.stringOrEmpty("host")
    val network = metadata.stringOrEmpty("network").ifEmpty { "TCP" }
    val process = metadata.stringOrEmpty("process")
    val destinationPort = metadata.stringOrEmpty("destinationPort")
    val sourceIP = metadata.stringOrEmpty("sourceIP")
    val sourcePort = metadata.stringOrEmpty("sourcePort")
    val destinationIP = metadata.stringOrEmpty("destinationIP")

    val displayHost = when {
        host.isNotEmpty() && destinationPort.isNotEmpty() -> "$host:$destinationPort"
        host.isNotEmpty() -> host
        sourceIP.isNotEmpty() -> "$sourceIP:$sourcePort"
        else -> ""
    }

    return ConnectionDetailState(
        metadata = metadata,
        displayHost = displayHost,
        network = network,
        process = process,
        sourceAddress = "$sourceIP:$sourcePort",
        destinationAddress = destinationIP.takeIf(String::isNotEmpty)
            ?.let { "$it:$destinationPort" }
            .orEmpty(),
        duration = start.takeIf(String::isNotEmpty)?.let(::calculateDuration) ?: "00:00:00",
    )
}

private fun JsonObject.stringOrEmpty(key: String): String {
    return get(key)?.jsonPrimitive?.content.orEmpty()
}
