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

package com.github.yumelira.yumebox.screen.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.BuildConfig
import com.github.yumelira.yumebox.WebViewActivity
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.viewmodel.SettingEvent
import com.github.yumelira.yumebox.presentation.viewmodel.SettingViewModel
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FeatureScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MetaFeatureScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NetworkSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OverrideScreenDestination
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun CircularIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Float = 1f,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val componentSizes = AppTheme.sizes

    Box(
        modifier =
            modifier
                .padding(start = spacing.space4, end = spacing.space16)
                .requiredSize(componentSizes.settingsIconSlotSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.layout { measurable, _ ->
                        val containerSize = componentSizes.settingsIconContainerSize.roundToPx()
                        val parentSize = componentSizes.settingsIconSlotSize.roundToPx()
                        val offset = (containerSize - parentSize) / 2

                        val placeable =
                            measurable.measure(
                                androidx.compose.ui.unit.Constraints.fixed(
                                    containerSize,
                                    containerSize,
                                )
                            )
                        layout(parentSize, parentSize) { placeable.place(-offset, -offset) }
                    }
                    .size(componentSizes.settingsIconContainerSize)
                    .clip(RoundedCornerShape(radii.radius16))
                    .background(MiuixTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier =
                    Modifier.size(componentSizes.settingsIconGlyphSize)
                        .graphicsLayer(
                            scaleX = iconSize,
                            scaleY = iconSize,
                            transformOrigin = TransformOrigin.Center,
                        ),
            )
        }
    }
}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun SettingPager(mainInnerPadding: PaddingValues) {
    val viewModel = koinViewModel<SettingViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    val versionInfo = BuildConfig.VERSION_NAME

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingEvent.OpenWebView -> {
                    runCatching { WebViewActivity.start(context, event.url) }
                        .getOrElse { throwable ->
                            context.toast(
                                MLang.Settings.Error.WebviewFailed.format(throwable.message)
                            )
                        }
                }
            }
        }
    }

    Scaffold(topBar = { TopBar(title = MLang.Settings.Title, scrollBehavior = scrollBehavior) }) {
        innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        ) {
            item {
                Title(MLang.Settings.Section.UiSettings)
                Card {
                    ArrowPreference(
                        title = MLang.Settings.UiSettings.App,
                        summary = MLang.Settings.UiSettings.AppSummary,
                        onClick = {
                            navigator.navigate(AppSettingsScreenDestination) {
                                launchSingleTop = true
                            }
                        },
                        startAction = {
                            CircularIcon(imageVector = Yume.`Settings-2`, contentDescription = null)
                        },
                    )
                    ArrowPreference(
                        title = MLang.Settings.UiSettings.Network,
                        summary = MLang.Settings.UiSettings.NetworkSummary,
                        onClick = {
                            navigator.navigate(NetworkSettingsScreenDestination) {
                                launchSingleTop = true
                            }
                        },
                        startAction = {
                            CircularIcon(imageVector = Yume.`Wifi-cog`, contentDescription = null)
                        },
                    )
                    ArrowPreference(
                        title = MLang.Settings.UiSettings.Override,
                        summary = MLang.Settings.UiSettings.OverrideSummary,
                        onClick = {
                            navigator.navigate(OverrideScreenDestination) { launchSingleTop = true }
                        },
                        startAction = {
                            CircularIcon(imageVector = Yume.`Git-merge`, contentDescription = null)
                        },
                    )
                    ArrowPreference(
                        title = MLang.Settings.UiSettings.MetaFeatures,
                        summary = MLang.Settings.UiSettings.MetaFeaturesSummary,
                        onClick = {
                            navigator.navigate(MetaFeatureScreenDestination) {
                                launchSingleTop = true
                            }
                        },
                        startAction = {
                            CircularIcon(imageVector = Yume.Meta, contentDescription = null)
                        },
                    )
                }
            }
            item {
                Title(MLang.Settings.Section.More)

                Card {
                    ArrowPreference(
                        title = MLang.Settings.More.Lab,
                        summary = MLang.Settings.More.LabSummary,
                        onClick = {
                            navigator.navigate(FeatureScreenDestination) { launchSingleTop = true }
                        },
                        startAction = {
                            CircularIcon(imageVector = Yume.FlaskConical, contentDescription = null)
                        },
                    )
                    ArrowPreference(
                        title = MLang.Settings.More.Logs,
                        summary = MLang.Settings.More.LogsSummary,
                        onClick = {
                            navigator.navigate(LogScreenDestination) { launchSingleTop = true }
                        },
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.`Chart-column`,
                                contentDescription = null,
                            )
                        },
                    )
                    ArrowPreference(
                        title = MLang.Settings.More.About,
                        summary = MLang.Settings.More.AboutSummary,
                        onClick = {
                            navigator.navigate(AboutScreenDestination) { launchSingleTop = true }
                        },
                        startAction = {
                            CircularIcon(imageVector = Yume.Github, contentDescription = null)
                        },
                        endActions = { VersionBadge(versionInfo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionBadge(versionInfo: String?) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity

    Surface(
        color = MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(componentSizes.versionBadgeHeight).padding(end = spacing.space12),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space8),
        ) {
            Text(
                text = versionInfo ?: "Unknown",
                style =
                    MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}
