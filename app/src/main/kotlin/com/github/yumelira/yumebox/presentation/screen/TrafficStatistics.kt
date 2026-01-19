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

package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.data.model.ProfileTrafficUsage
import com.github.yumelira.yumebox.data.model.StatisticsTimeRange
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.TrafficStatisticsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun TrafficStatisticsScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<TrafficStatisticsViewModel>()
    val scrollBehavior = MiuixScrollBehavior()

    val todaySummary by viewModel.todaySummary.collectAsState()
    val weekSummary by viewModel.weekSummary.collectAsState()
    val trafficDifference by viewModel.trafficDifference.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val chartItems by viewModel.chartItems.collectAsState()
    val profileUsages by viewModel.profileUsages.collectAsState()
    val selectedBarIndex by viewModel.selectedBarIndex.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.TrafficStatistics.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon(navigator = navigator)
                }
            )
        }
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            topPadding = 16.dp,
            bottomPadding = 32.dp
        ) {
            item {
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        TimeRangeSelector(
                            selectedRange = selectedTimeRange,
                            onRangeSelected = { viewModel.setTimeRange(it) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier.height(84.dp)
                        ) {
                            Text(
                                text = when (selectedTimeRange) {
                                    StatisticsTimeRange.TODAY -> MLang.TrafficStatistics.Summary.TodayTraffic
                                    StatisticsTimeRange.WEEK -> MLang.TrafficStatistics.Summary.WeekTraffic
                                },
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val displayTotal = when (selectedTimeRange) {
                                StatisticsTimeRange.TODAY -> todaySummary.total
                                StatisticsTimeRange.WEEK -> weekSummary
                            }

                            Text(
                                text = formatBytes(displayTotal),
                                style = MiuixTheme.textStyles.headline1.copy(fontSize = 32.sp),
                                color = MiuixTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val differenceText = when (selectedTimeRange) {
                                StatisticsTimeRange.TODAY -> when {
                                    trafficDifference > 0 -> MLang.TrafficStatistics.Compare.MoreThanYesterday.format(
                                        formatBytes(trafficDifference)
                                    )

                                    trafficDifference < 0 -> MLang.TrafficStatistics.Compare.LessThanYesterday.format(
                                        formatBytes(trafficDifference)
                                    )

                                    else -> MLang.TrafficStatistics.Compare.SameAsYesterday
                                }

                                StatisticsTimeRange.WEEK -> MLang.TrafficStatistics.Compare.WeekStats
                            }
                            Text(
                                text = differenceText,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            TrafficBarChart(
                                items = chartItems,
                                selectedIndex = selectedBarIndex,
                                onItemClick = { index ->
                                    viewModel.setSelectedBarIndex(
                                        if (selectedBarIndex == index) -1 else index
                                    )
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedBarIndex >= 0 && chartItems.isNotEmpty()) {
                                val selectedItem = chartItems.getOrNull(selectedBarIndex)
                                if (selectedItem != null && selectedItem.label.isNotEmpty()) {
                                    Text(
                                        text = "${selectedItem.label}: ${formatBytes(selectedItem.value)}",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }



            item {
                SmallTitle(MLang.TrafficStatistics.ProfileUsage.Section)
            }
            if (profileUsages.isEmpty()) {
                item {
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = MLang.TrafficStatistics.ProfileUsage.Empty,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            } else {
                item {
                    top.yukonga.miuix.kmp.basic.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            profileUsages.forEachIndexed { index, usage ->
                                ProfileUsageItem(
                                    usage = usage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                                if (index < profileUsages.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(0.5.dp)
                                            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedRange: StatisticsTimeRange,
    onRangeSelected: (StatisticsTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatisticsTimeRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRangeSelected(range) },
                color = if (isSelected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        style = MiuixTheme.textStyles.body2,
                        color = if (isSelected) {
                            MiuixTheme.colorScheme.onPrimary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileUsageItem(
    usage: ProfileTrafficUsage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = usage.profileName,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "↑ ${formatBytes(usage.totalUpload)}  ↓ ${formatBytes(usage.totalDownload)}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        Text(
            text = formatBytes(usage.totalBytes),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.primary
        )
    }
}
