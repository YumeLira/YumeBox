package com.github.yumelira.yumebox.presentation.screen

import Rocket
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.BuildConfig
import com.github.yumelira.yumebox.common.util.DeviceUtil.is32BitDevice
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SmallTitle
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Atom
import com.github.yumelira.yumebox.presentation.icon.yume.Rocket
import com.github.yumelira.yumebox.presentation.icon.yume.`Chart-column`
import com.github.yumelira.yumebox.presentation.icon.yume.`Git-merge`
import com.github.yumelira.yumebox.presentation.icon.yume.Github
import com.github.yumelira.yumebox.presentation.icon.yume.Meta
import com.github.yumelira.yumebox.presentation.icon.yume.`Settings-2`
import com.github.yumelira.yumebox.presentation.icon.yume.`Wifi-cog`
import com.github.yumelira.yumebox.presentation.viewmodel.SettingEvent
import com.github.yumelira.yumebox.presentation.viewmodel.SettingViewModel
import com.github.yumelira.yumebox.presentation.webview.WebViewActivity
import com.github.yumelira.yumebox.substore.SubStoreService
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
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun CircularIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Float = 1f,
) {
    Box(
        modifier = modifier
            .padding(start = 4.dp, end = 16.dp)
            .requiredSize(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier
            .layout { measurable, _ ->
                val containerSize = 32.dp.roundToPx()
                val parentSize = 24.dp.roundToPx()
                val offset = (containerSize - parentSize) / 2

                val placeable = measurable.measure(
                    androidx.compose.ui.unit.Constraints.fixed(containerSize, containerSize)
                )
                layout(parentSize, parentSize) {
                    placeable.place(-offset, -offset)
                }
            }
            .size(32.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.primary),
            contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MiuixTheme.colorScheme.background,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(
                        scaleX = iconSize,
                        scaleY = iconSize,
                        transformOrigin = TransformOrigin.Center,
                    )
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
        ) {

            item {
                SmallTitle(MLang.Settings.Section.UiSettings)
                Card {
                    SuperArrow(
                        title = MLang.Settings.UiSettings.App,
                        onClick = { navigator.navigate(AppSettingsScreenDestination) { launchSingleTop = true } },
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.`Settings-2`, contentDescription = null
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.UiSettings.Network,
                        onClick = { navigator.navigate(NetworkSettingsScreenDestination) { launchSingleTop = true } },
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.`Wifi-cog`, contentDescription = null
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.UiSettings.Override,
                        onClick = { navigator.navigate(OverrideScreenDestination) { launchSingleTop = true } },
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.`Git-merge`, contentDescription = null
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
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.Meta, contentDescription = null
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
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.Atom, contentDescription = null
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.Function.FeatureManagement,
                        onClick = {
                            navigator.navigate(FeatureScreenDestination) { launchSingleTop = true }
                        },
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.Rocket, contentDescription = null
                            )
                        },
                    )

                }
            }
            item {
                SmallTitle(MLang.Settings.Section.More)

                Card {
                    SuperArrow(
                        title = MLang.Settings.More.Logs,
                        onClick = { navigator.navigate(LogScreenDestination) { launchSingleTop = true } },
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.`Chart-column`, contentDescription = null
                            )
                        },
                    )
                    SuperArrow(
                        title = MLang.Settings.More.About,
                        onClick = { navigator.navigate(AboutScreenDestination) { launchSingleTop = true } },
                        startAction = {
                            CircularIcon(
                                imageVector = Yume.Github, contentDescription = null
                            )
                        },
                        endActions = {
                            VersionBadge(versionInfo)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionBadge(
    versionInfo: String?
) {
    Surface(
        color = MiuixTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .height(22.dp)
            .padding(end = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = versionInfo ?: "Unknown", style = MiuixTheme.textStyles.footnote1.copy(
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                ), color = MiuixTheme.colorScheme.primary
            )
        }
    }
}
