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

package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.component.CountryFlagCircle
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NodeInfoDisplay(
    serverName: String?,
    serverPing: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = MLang.Home.NodeInfo.Node,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(4.dp))
            val flagged = remember(serverName) {
                serverName?.let(::extractFlaggedName)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val countryCode = flagged?.countryCode
                if (countryCode != null) {
                    CountryFlagCircle(countryCode = countryCode, size = 18.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = flagged?.displayName ?: (serverName ?: "Not Selected"),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MLang.Home.NodeInfo.Delay,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(4.dp))
            PingValue(ping = serverPing)
        }
    }
}

@Composable
private fun PingValue(ping: Int?) {
    if (ping != null && ping <= 1000) {
        val color = if (ping < 500) {
            Color(0xFF4CAF50)
        } else {
            Color(0xFFFFA726)
        }
        Text(
            text = "${ping}ms",
            style = MiuixTheme.textStyles.body1,
            color = color
        )
    } else {
        Text(
            text = "--",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}
