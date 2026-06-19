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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeIdleContent(oneWord: String, author: String, modifier: Modifier = Modifier) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity

    val accentColor = MiuixTheme.colorScheme.primary
    val authorColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = componentSizes.homeIdleTopPadding,
                    bottom = componentSizes.homeIdleBottomPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.space16),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier.fillMaxWidth().padding(top = spacing.space18, start = spacing.space8)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space12),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier.padding(top = spacing.space12)
                                .width(componentSizes.homeIdleAccentLineWidth)
                                .height(componentSizes.homeIdleAccentLineHeight)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                accentColor.copy(alpha = opacity.accent),
                                                Color.Transparent,
                                            )
                                    )
                                )
                    )
                    Text(
                        text = oneWord,
                        style =
                            MiuixTheme.textStyles.headline1.copy(
                                fontSize = 28.sp,
                                lineHeight = 50.sp,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.width(componentSizes.homeIdleAuthorDividerWidth)
                        .height(componentSizes.homeIdleAuthorDividerHeight)
                        .background(authorColor.copy(alpha = opacity.muted))
            )
            Spacer(modifier = Modifier.width(spacing.space12))
            Text(
                text = author,
                style =
                    MiuixTheme.textStyles.title3.copy(
                        fontSize = 15.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                color = authorColor,
            )
        }
    }
}
