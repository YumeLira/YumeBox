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

package com.github.yumelira.yumebox.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeIdleContent(
    oneWord: String,
    author: String,
    modifier: Modifier = Modifier
) {
    val accentColor = MiuixTheme.colorScheme.primary
    val authorColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 122.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .width(2.dp)
                            .height(36.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.58f),
                                        Color.Transparent,
                                    )
                                )
                            )
                    )
                    Text(
                        text = oneWord,
                        style = MiuixTheme.textStyles.headline1.copy(
                            fontSize = 28.sp,
                            lineHeight = 50.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(1.dp)
                    .background(authorColor.copy(alpha = 0.35f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = author,
                style = MiuixTheme.textStyles.title3.copy(
                    fontSize = 15.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = authorColor,
            )
        }
    }
}
