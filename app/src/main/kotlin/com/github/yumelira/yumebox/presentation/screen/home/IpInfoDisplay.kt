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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.yumelira.yumebox.common.util.LocaleUtil
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun IpInfoDisplay(
    state: IpMonitoringState,
    modifier: Modifier = Modifier
) {
    when (state) {
        is IpMonitoringState.Loading -> IpInfoRow(
            label = MLang.Home.IpInfo.ExitIp,
            value = MLang.Home.IpInfo.Loading,
            valueColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            countryCode = null,
            modifier = modifier
        )

        is IpMonitoringState.Error -> IpInfoRow(
            label = MLang.Home.IpInfo.ExitIp,
            value = MLang.Home.IpInfo.Failed,
            valueColor = MiuixTheme.colorScheme.error,
            countryCode = null,
            modifier = modifier
        )

        is IpMonitoringState.Success -> {
            val externalIp = state.externalIp
            if (externalIp != null) {
                IpInfoRow(
                    label = MLang.Home.IpInfo.ExitIp,
                    value = formatIp(externalIp.ip),
                    valueColor = MiuixTheme.colorScheme.onSurface,
                    countryCode = externalIp.countryCode,
                    modifier = modifier
                )
            } else {
                IpInfoRow(
                    label = MLang.Home.IpInfo.ExitIp,
                    value = MLang.Home.IpInfo.NotFetched,
                    valueColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    countryCode = null,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun IpInfoRow(
    label: String,
    value: String,
    valueColor: Color,
    countryCode: String?,
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
                text = label,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MiuixTheme.textStyles.body1,
                fontFamily = FontFamily.Monospace,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        CountryBadge(countryCode = countryCode)
    }
}

@Composable
private fun CountryBadge(countryCode: String?) {
    if (countryCode != null) {
        val displayCountryCode = LocaleUtil.normalizeRegionCode(countryCode) ?: countryCode
        val flagUrl = LocaleUtil.normalizeFlagUrl(countryCode)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = flagUrl,
                contentDescription = "$displayCountryCode flag",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                text = displayCountryCode,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.primary
            )
        }
    } else {
        Text(
            text = "--",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

private fun formatIp(ip: String): String {
    return if (ip.contains(":")) {
        ip.split(":").take(4).joinToString(":") + "..."
    } else {
        ip
    }
}
