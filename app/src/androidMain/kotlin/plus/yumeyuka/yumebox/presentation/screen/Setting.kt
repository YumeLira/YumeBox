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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.generated.destinations.*
import kotlinx.coroutines.flow.collect
import org.koin.androidx.compose.koinViewModel
import com.github.yumelira.yumebox.BuildConfig
import com.github.yumelira.yumebox.substore.SubStoreService
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.viewmodel.SettingEvent
import com.github.yumelira.yumebox.presentation.viewmodel.SettingViewModel
import com.github.yumelira.yumebox.presentation.webview.WebViewActivity
import com.github.yumelira.yumebox.common.util.DeviceUtil.is32BitDevice
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.common.util.WebViewUtils.getLocalBaseUrl
import com.github.yumelira.yumebox.common.util.WebViewUtils.getPanelUrl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.other.GitHub
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.oom_wg.purejoy.mlang.MLang

@SuppressLint("LocalContextResourcesRead")
@Composable
fun SettingPager(mainInnerPadding: PaddingValues) {
    val viewModel = koinViewModel<SettingViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    val versionInfo = remember { BuildConfig.VERSION_NAME }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingEvent.OpenWebView -> {
                    runCatching {
                        WebViewActivity.start(context, event.url)
                    }.getOrElse { throwable ->
                        context.toast(MLang.Settings.Error.WebviewFailed.format(throwable.message))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = MLang.Settings.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
            enableBottomBarAutoHide = true,
        ) {

            item {
                SmallTitle(MLang.Settings.Section.UiSettings)
                Card {
                    SuperArrow(
                        title = MLang.Settings.UiSettings.App,
                        onClick = { navigator.navigate(AppSettingsScreenDestination) { launchSingleTop = true } },
                        leftAction = {
                            Icon(
                                imageVector = Yume.`Settings-2`,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.UiSettings.Network,
                        onClick = { navigator.navigate(NetworkSettingsScreenDestination) { launchSingleTop = true } },
                        leftAction = {
                            Icon(
                                imageVector = Yume.`Wifi-cog`,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.UiSettings.Override,
                        onClick = { navigator.navigate(OverrideScreenDestination) { launchSingleTop = true } },
                        leftAction = {
                            Icon(
                                imageVector = Yume.`Redo-dot`,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.UiSettings.MetaFeatures,
                        onClick = {
                            navigator.navigate(MetaFeatureScreenDestination) {
                                launchSingleTop = true
                            }
                        },
                        leftAction = {
                            Icon(
                                imageVector = Yume.Meta,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )
                }
            }
            item {
                SmallTitle(MLang.Settings.Section.Function)
                Card {
                    SuperArrow(
                        title = MLang.Settings.Function.SubStore,
                        onClick = { viewModel.onSubStoreCardClicked(isAllowed = SubStoreService.isRunning) },
                        enabled = !is32BitDevice() && SubStoreService.isRunning,
                        leftAction = {
                            Icon(
                                imageVector = Yume.Substore,
                                tint = MiuixTheme.colorScheme.onBackground,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .graphicsLayer(
                                        scaleX = 1.3f,
                                        scaleY = 1.3f,
                                        transformOrigin = TransformOrigin.Center,
                                    ),
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.Function.FeatureManagement,
                        onClick = {
                            navigator.navigate(FeatureScreenDestination) { launchSingleTop = true }
                        },
                        leftAction = {
                            Icon(
                                imageVector = Yume.Rocket,
                                tint = MiuixTheme.colorScheme.onBackground,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )

                }
            }
            item {
                SmallTitle(MLang.Settings.Section.More)

                Card {
                    SuperArrow(
                        title = MLang.Settings.More.TrafficStatistics,
                        onClick = { navigator.navigate(TrafficStatisticsScreenDestination) { launchSingleTop = true } },
                        leftAction = {
                            Icon(
                                imageVector = Yume.`Arrow-down-up`,
                                tint = MiuixTheme.colorScheme.onBackground,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.More.Logs,
                        onClick = { navigator.navigate(LogScreenDestination) { launchSingleTop = true } },
                        leftAction = {
                            Icon(
                                imageVector = Yume.`Scroll-text`,
                                tint = MiuixTheme.colorScheme.onBackground,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.More.About,
                        onClick = { navigator.navigate(AboutScreenDestination) { launchSingleTop = true } },
                        leftAction = {
                            Icon(
                                imageVector = MiuixIcons.Other.GitHub,
                                tint = MiuixTheme.colorScheme.onBackground,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                        rightActions = {
                            Text(versionInfo,
                                modifier = Modifier.padding(end = 16.dp),
                                style = MiuixTheme.textStyles.body2,
                            )
                        },
                    )
                }
            }
        }
    }
}
