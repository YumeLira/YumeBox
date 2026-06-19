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

package com.github.yumelira.yumebox.presentation.component

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Circle-fading-arrow-up`
import com.github.yumelira.yumebox.presentation.icon.yume.Delete
import com.github.yumelira.yumebox.presentation.icon.yume.Edit
import com.github.yumelira.yumebox.presentation.icon.yume.Share
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.util.*
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProfileCard(
    profile: Profile,
    workDir: File,
    isDownloading: Boolean = false,
    onExport: (Profile) -> Unit,
    onUpdate: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onToggleEnabled: (Profile) -> Unit,
    onOverrideSettings: ((Profile) -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    val colorScheme = MiuixTheme.colorScheme

    val isDark = isSystemInDarkTheme()
    val secondaryContainer = colorScheme.secondaryContainer.copy(alpha = opacity.strong)
    val actionIconTint =
        remember(isDark, opacity) {
            colorScheme.onSurface.copy(
                alpha = if (isDark) opacity.subtleText else opacity.prominentText
            )
        }

    val isConfigSaved = remember(profile.uuid, profile.updatedAt) { profile.isConfigSaved(workDir) }

    val updateBg =
        remember(colorScheme, opacity) { colorScheme.primary.copy(alpha = opacity.subtle) }
    val updateTint = remember(colorScheme) { colorScheme.primary }

    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = spacing.space12),
        insideMargin = PaddingValues(spacing.space16),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = spacing.space4)) {
                Text(
                    text = profile.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = profile.getDisplayProvider(),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = spacing.space2),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Switch(
                checked = profile.enabled,
                enabled = !isDownloading,
                onCheckedChange = { onToggleEnabled(profile) },
            )
        }

        val infoText = remember(profile) { profile.getInfoText() }

        Column(modifier = Modifier.padding(top = spacing.space8)) {
            val lines = infoText.split('\n')

            lines.forEachIndexed { _, line ->
                when {
                    line.contains('|') -> {
                        val parts = line.split('|')
                        val expireText = parts.getOrNull(0) ?: ""
                        val timeText = parts.getOrNull(1) ?: ""

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = expireText,
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                lineHeight = 20.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )

                            if (timeText.isNotEmpty()) {
                                Text(
                                    text = timeText,
                                    fontSize = 12.sp,
                                    color =
                                        colorScheme.onTertiaryContainer.copy(
                                            alpha = opacity.strong
                                        ),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier =
                                        Modifier.padding(
                                            end = componentSizes.profileMetaTrailingInset
                                        ),
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = line,
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            lineHeight = 20.sp,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = spacing.space12),
            thickness = componentSizes.thinDividerThickness,
            color = colorScheme.outline.copy(alpha = opacity.medium),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                backgroundColor = secondaryContainer,
                minHeight = componentSizes.compactActionButtonSize,
                minWidth = componentSizes.compactActionButtonSize,
                enabled = isConfigSaved && !isDownloading,
                onClick = { if (isConfigSaved && !isDownloading) onExport(profile) },
            ) {
                Icon(
                    modifier =
                        Modifier.size(spacing.space20)
                            .alpha(if (isConfigSaved) 1f else opacity.disabledSecondary),
                    imageVector = Yume.Share,
                    tint =
                        actionIconTint.copy(
                            alpha = if (isConfigSaved) 1f else opacity.disabledSecondary
                        ),
                    contentDescription = "Export",
                )
            }

            Spacer(Modifier.width(spacing.space8))

            IconButton(
                backgroundColor = secondaryContainer,
                minHeight = componentSizes.compactActionButtonSize,
                minWidth = componentSizes.compactActionButtonSize,
                enabled = !isDownloading,
                onClick = { if (!isDownloading) onEdit(profile) },
            ) {
                Icon(
                    modifier = Modifier.size(spacing.space20),
                    imageVector = Yume.Edit,
                    tint = actionIconTint,
                    contentDescription = "Edit",
                )
            }

            Spacer(Modifier.weight(1f))

            if (profile.shouldShowUpdateButton()) {
                IconButton(
                    modifier = Modifier.padding(end = spacing.space8),
                    backgroundColor = updateBg,
                    minHeight = componentSizes.compactActionButtonSize,
                    minWidth = componentSizes.compactActionButtonSize,
                    enabled = !isDownloading,
                    onClick = { if (!isDownloading) onUpdate(profile) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.space10),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    ) {
                        Icon(
                            modifier = Modifier.size(spacing.space20),
                            imageVector = Yume.`Circle-fading-arrow-up`,
                            tint = updateTint,
                            contentDescription = "Update",
                        )
                        Text(
                            modifier =
                                Modifier.padding(end = componentSizes.textLineCompactSpacing),
                            text = MLang.Component.ProfileCard.Update,
                            color = updateTint,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            IconButton(
                minHeight = componentSizes.compactActionButtonSize,
                minWidth = componentSizes.compactActionButtonSize,
                enabled = !isDownloading,
                onClick = { if (!isDownloading) onDelete(profile) },
                backgroundColor = secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.space10),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(spacing.space20),
                        imageVector = Yume.Delete,
                        tint = actionIconTint,
                        contentDescription = "Delete",
                    )
                    Text(
                        modifier =
                            Modifier.padding(
                                start = spacing.space4,
                                end = componentSizes.textLineCompactSpacing,
                            ),
                        text = MLang.Component.ProfileCard.Delete,
                        color = actionIconTint,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
