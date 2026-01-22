package com.github.yumelira.yumebox.presentation.component

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.clash.isConfigSaved
import com.github.yumelira.yumebox.data.model.Profile
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File


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
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val colorScheme = MiuixTheme.colorScheme

    val isDark = isSystemInDarkTheme()
    val secondaryContainer = colorScheme.secondaryContainer.copy(alpha = 0.8f)
    val actionIconTint =
        remember(isDark) { colorScheme.onSurface.copy(alpha = if (isDark) 0.7f else 0.9f) }

    val isConfigSaved = remember(profile.id, profile.updatedAt) {
        profile.isConfigSaved(workDir)
    }

    val updateBg = remember(colorScheme) { colorScheme.tertiaryContainer.copy(alpha = 0.6f) }
    val updateTint = remember(colorScheme) { colorScheme.onTertiaryContainer.copy(alpha = 0.8f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {

                Text(
                    text = profile.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = profile.getDisplayProvider(),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = profile.enabled,
                enabled = !isDownloading,
                onCheckedChange = { newValue -> onToggleEnabled(profile.copy(enabled = newValue)) })
        }


        val infoText = remember(profile) {
            profile.getInfoText()
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {

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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = expireText,
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                lineHeight = 20.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            if (timeText.isNotEmpty()) {
                                Text(
                                    text = timeText,
                                    fontSize = 12.sp,
                                    color = colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 13.dp)
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
                            maxLines = 1
                        )
                    }
                }
            }
        }


        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 0.5.dp,
            color = colorScheme.outline.copy(alpha = 0.5f)
        )


        Row(verticalAlignment = Alignment.CenterVertically) {


            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                IconButton(
                    backgroundColor = secondaryContainer,
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    enabled = isConfigSaved && !isDownloading,
                    onClick = { if (isConfigSaved && !isDownloading) onExport(profile) }) {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(if (isConfigSaved) 1f else 0.4f),
                        imageVector = MiuixIcons.Share,
                        tint = actionIconTint.copy(alpha = if (isConfigSaved) 1f else 0.4f),
                        contentDescription = MLang.Component.ProfileCard.Export
                    )
                }


                IconButton(
                    backgroundColor = secondaryContainer,
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    enabled = !isDownloading,
                    onClick = { if (!isDownloading) onEdit(profile) }) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = MiuixIcons.Edit,
                        tint = actionIconTint,
                        contentDescription = MLang.Component.ProfileCard.Edit
                    )
                }
            }

            Spacer(Modifier.weight(1f))




            if (profile.shouldShowUpdateButton()) {
                IconButton(
                    modifier = Modifier.padding(end = 8.dp),
                    backgroundColor = updateBg,
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    enabled = !isDownloading,
                    onClick = { if (!isDownloading) onUpdate(profile) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = MiuixIcons.Refresh,
                            tint = updateTint,
                            contentDescription = MLang.Component.ProfileCard.Update,
                        )
                        Text(
                            modifier = Modifier.padding(end = 3.dp),
                            text = MLang.Component.ProfileCard.Update,
                            color = updateTint,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }


            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                enabled = !isDownloading,
                onClick = { if (!isDownloading) onDelete(profile) },
                backgroundColor = secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = MiuixIcons.Delete,
                        tint = actionIconTint,
                        contentDescription = MLang.Component.ProfileCard.Delete
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp, end = 3.dp),
                        text = MLang.Component.ProfileCard.Delete,
                        color = actionIconTint,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
