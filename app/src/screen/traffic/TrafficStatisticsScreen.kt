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

package com.github.yumelira.yumebox.screen.traffic

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.graphics.drawable.toBitmap
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.AppTrafficUsage
import com.github.yumelira.yumebox.data.model.StatisticsTimeRange
import com.github.yumelira.yumebox.data.model.TrafficStatisticsBuckets
import com.github.yumelira.yumebox.feature.meta.presentation.component.TabRowWithContour
import com.github.yumelira.yumebox.feature.meta.presentation.viewmodel.TrafficStatisticsViewModel
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.TrafficDonutChart
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun TrafficStatisticsScreen() {
    val viewModel = koinViewModel<TrafficStatisticsViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    val uiState by viewModel.uiState.collectAsState()
    val timeRanges = StatisticsTimeRange.entries
    val selectedTabIndex = timeRanges.indexOf(uiState.selectedTimeRange).coerceAtLeast(0)
    val activeSummary = uiState.summary

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.TrafficStatistics.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.clearAllStatistics()
                            context.toast(MLang.TrafficStatistics.Action.ClearSuccess)
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = MLang.TrafficStatistics.Action.Clear,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                TabRowWithContour(
                    tabs = timeRanges.map { it.label },
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        timeRanges.getOrNull(index)?.let(viewModel::setTimeRange)
                    },
                    modifier = Modifier.padding(horizontal = spacing.space16),
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space16)) {
                    Column(
                        modifier =
                            Modifier.padding(
                                horizontal = spacing.space18,
                                vertical = spacing.space18,
                            ),
                        verticalArrangement = Arrangement.spacedBy(spacing.space16),
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth().height(componentSizes.trafficChartHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            TrafficDonutChart(
                                total = activeSummary.total,
                                slices = uiState.donutSlices,
                                selectedKey = null,
                                onSliceClick = {},
                                modifier = Modifier.size(componentSizes.trafficDonutDiameter),
                                animationKey = uiState.selectedTimeRange,
                                strokeWidth = componentSizes.trafficDonutStrokeWidth,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement =
                                        Arrangement.spacedBy(componentSizes.textLineCompactSpacing),
                                ) {
                                    Text(
                                        text = formatBytes(activeSummary.total),
                                        style = MiuixTheme.textStyles.title4,
                                        color = MiuixTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text =
                                            if (
                                                uiState.selectedTimeRange ==
                                                    StatisticsTimeRange.TODAY
                                            ) {
                                                MLang.TrafficStatistics.Summary.TodayTraffic
                                            } else {
                                                MLang.TrafficStatistics.Summary.WeekTraffic
                                            },
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Title(MLang.TrafficStatistics.Section.Traffic)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space16),
                    verticalArrangement = Arrangement.spacedBy(spacing.space10),
                ) {
                    TrafficMetricCard(
                        downloadValue = formatBytes(activeSummary.totalDownload),
                        uploadValue = formatBytes(activeSummary.totalUpload),
                    )
                }
            }

            item { Title(MLang.TrafficStatistics.Section.TopApps) }

            if (uiState.topApps.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space16)) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(
                                        horizontal = spacing.space14,
                                        vertical = spacing.space18,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = MLang.TrafficStatistics.Section.EmptyApps,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            } else {
                items(items = uiState.topApps, key = AppTrafficUsage::appKey) { usage ->
                    Box(
                        modifier =
                            Modifier.padding(
                                horizontal = spacing.space16,
                                vertical = componentSizes.listItemVerticalMinimal,
                            )
                    ) {
                        AppTrafficRow(context = context, usage = usage, total = activeSummary.total)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficMetricCard(downloadValue: String, uploadValue: String) {
    val spacing = AppTheme.spacing
    val semanticColors = AppTheme.colors

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = spacing.space14, vertical = spacing.space12),
            verticalArrangement = Arrangement.spacedBy(spacing.space18),
        ) {
            TrafficMetricLine(
                label = MLang.TrafficStatistics.Metric.Download,
                value = downloadValue,
                valueColor = semanticColors.traffic.download,
            )
            TrafficMetricLine(
                label = MLang.TrafficStatistics.Metric.Upload,
                value = uploadValue,
                valueColor = semanticColors.traffic.upload,
            )
        }
    }
}

@Composable
private fun TrafficMetricLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AppTrafficRow(context: Context, usage: AppTrafficUsage, total: Long) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    val share = if (total > 0L) usage.totalBytes.toDouble() / total.toDouble() else 0.0
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = spacing.space14, vertical = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBadge(
                context = context,
                appKey = usage.appKey,
                packageName = usage.packageName,
                appName = usage.appName,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(componentSizes.textLineCompactSpacing),
            ) {
                Text(
                    text = usage.appName,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text =
                        MLang.TrafficStatistics.Metric.UsageLine.format(
                            formatBytes(usage.totalDownload),
                            formatBytes(usage.totalUpload),
                        ),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(componentSizes.textLineCompactSpacing),
            ) {
                Text(
                    text = formatBytes(usage.totalBytes),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "%.1f%%".format(share * 100),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AppIconBadge(context: Context, appKey: String, packageName: String?, appName: String) {
    val componentSizes = AppTheme.sizes
    val semanticColors = AppTheme.colors
    val opacity = AppTheme.opacity
    val radii = AppTheme.radii

    if (appKey == TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY) {
        Box(
            modifier =
                Modifier.size(componentSizes.iconBadgeMedium)
                    .clip(RoundedCornerShape(radii.radius12))
                    .background(
                        semanticColors.traffic.unattributed.copy(alpha = opacity.softOverlay)
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "?",
                style = MiuixTheme.textStyles.body1,
                color = semanticColors.traffic.unattributed,
                fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    val iconBitmap by
        produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
            value =
                withContext(Dispatchers.IO) {
                    packageName
                        ?.takeIf { it.isNotBlank() }
                        ?.let { target ->
                            runCatching {
                                    context.packageManager
                                        .getApplicationIcon(target)
                                        .toBitmap(width = 84, height = 84)
                                        .asImageBitmap()
                                }
                                .getOrNull()
                        }
                }
        }

    val bitmap = iconBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appName,
            modifier =
                Modifier.size(componentSizes.iconBadgeMedium)
                    .clip(RoundedCornerShape(radii.radius12)),
        )
        return
    }

    Box(
        modifier =
            Modifier.size(componentSizes.iconBadgeMedium)
                .clip(RoundedCornerShape(radii.radius12))
                .background(MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtleStrong)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = appName.take(1).ifBlank { "?" },
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}
