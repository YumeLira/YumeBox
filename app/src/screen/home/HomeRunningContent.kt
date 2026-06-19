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

package com.github.yumelira.yumebox.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.gateway.IpMonitoringState
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.presentation.theme.UiDp

@Composable
fun HomeRunningContent(
    trafficNow: TrafficData,
    isRunning: Boolean,
    profileName: String?,
    tunnelMode: TunnelState.Mode?,
    serverName: String?,
    serverPing: Int?,
    ipMonitoringState: IpMonitoringState,
    speedHistory: List<Long>,
    onChartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = UiDp.dp24),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp32),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(UiDp.dp16)) {
            NodeInfoDisplay(serverName = serverName, serverPing = serverPing)

            IpInfoDisplay(state = ipMonitoringState)
        }

        Column(verticalArrangement = Arrangement.spacedBy(UiDp.dp12)) {
            SpeedChart(speedHistory = speedHistory, isRunning = isRunning, onClick = onChartClick)
        }
    }
}
