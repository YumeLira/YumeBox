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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperSwitch

@Composable
@Destination<RootGraph>
fun AppSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()


    val themeMode = viewModel.themeMode.state.collectAsState().value
    val colorTheme = viewModel.colorTheme.state.collectAsState().value

    val automaticRestart = viewModel.automaticRestart.state.collectAsState().value
    val hideAppIcon = viewModel.hideAppIcon.state.collectAsState().value
    val showTrafficNotification = viewModel.showTrafficNotification.state.collectAsState().value
    val bottomBarFloating = viewModel.bottomBarFloating.state.collectAsState().value
    val showDivider = viewModel.showDivider.state.collectAsState().value
    val bottomBarAutoHide = viewModel.bottomBarAutoHide.state.collectAsState().value

    val oneWord = viewModel.oneWord.state.collectAsState().value
    val oneWordAuthor = viewModel.oneWordAuthor.state.collectAsState().value
    val customUserAgent = viewModel.customUserAgent.state.collectAsState().value

    val showHideIconDialog = remember { mutableStateOf(false) }
    val showEditOneWordDialog = remember { mutableStateOf(false) }
    val showEditOneWordAuthorDialog = remember { mutableStateOf(false) }
    val showEditCustomUserAgentDialog = remember { mutableStateOf(false) }

    val oneWordTextFieldState = remember { mutableStateOf(TextFieldValue(oneWord)) }
    val oneWordAuthorTextFieldState = remember { mutableStateOf(TextFieldValue(oneWordAuthor)) }
    val customUserAgentTextFieldState = remember { mutableStateOf(TextFieldValue(customUserAgent)) }

    Scaffold(
        topBar = {
            TopBar(title = "应用设置", scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                SmallTitle("行为")
                Card {
                    SuperSwitch(
                        title = "自动启动",
                        summary = "应用启动和开机时自动启动代理服务",
                        checked = automaticRestart,
                        onCheckedChange = { viewModel.onAutomaticRestartChange(it) },
                    )
                    if (com.github.yumelira.yumebox.common.util.LocaleUtil.isChineseLocale()) {
                        SuperSwitch(
                            title = "坚持一个中国原则",
                            summary = "自动将港澳台地区旗帜及区域码显示为中国",
                            checked = true,
                            onCheckedChange = { },
                            enabled = false,
                        )
                    }
                }
                SmallTitle("首页")
                Card {
                    BasicComponent(
                        title = "一言",
                        summary = viewModel.oneWord.value,
                        onClick = {
                            oneWordTextFieldState.value = TextFieldValue(viewModel.oneWord.value)
                            showEditOneWordDialog.value = true
                        }
                    )
                    BasicComponent(
                        title = "作者",
                        summary = viewModel.oneWordAuthor.value,
                        onClick = {
                            oneWordAuthorTextFieldState.value = TextFieldValue(viewModel.oneWordAuthor.value)
                            showEditOneWordAuthorDialog.value = true
                        }
                    )
                }
                SmallTitle("界面")
                Card {
                    EnumSelector(
                        title = "主题模式",
                        summary = "选择应用的主题样式",
                        currentValue = themeMode,
                        items = listOf("跟随系统", "浅色", "深色"),
                        values = ThemeMode.entries,
                        onValueChange = { viewModel.onThemeModeChange(it) },
                    )
                    EnumSelector(
                        title = "配色方案",
                        summary = "选择品牌色风格，默认为极简黑白",
                        currentValue = colorTheme,
                        items = listOf(
                            "极简黑白",
                            "柏码经典",
                            "海洋之歌",
                            "清新晨露",
                            "小小公主",
                            "神秘世界",
                            "金色时光",
                        ),
                        values = AppColorTheme.entries,
                        onValueChange = { viewModel.onColorThemeChange(it) },
                    )
                    SuperSwitch(
                        title = "浮动导航栏",
                        summary = "使用浮动样式的底部导航栏",
                        checked = bottomBarFloating,
                        onCheckedChange = { viewModel.onBottomBarFloatingChange(it) },
                    )
                    SuperSwitch(
                        title = "滑动隐藏底栏",
                        summary = "向下滑动时自动隐藏底栏，向上滑动时显示",
                        checked = bottomBarAutoHide,
                        onCheckedChange = { viewModel.onBottomBarAutoHideChange(it) },
                    )
                    SuperSwitch(
                        title = "显示分割线",
                        summary = "在列表项之间显示分割线",
                        checked = showDivider,
                        onCheckedChange = { viewModel.onShowDividerChange(it) },
                    )
                    SuperSwitch(
                        title = "隐藏应用图标",
                        summary = "隐藏后可通过拨号盘 *#*#0721#*#* 打开",
                        checked = hideAppIcon,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showHideIconDialog.value = true
                            } else {
                                viewModel.onHideAppIconChange(false)
                                AppIconHelper.toggleIcon(context, false)
                            }
                        },
                    )
                }
                SmallTitle("服务")
                Card {
                    SuperSwitch(
                        title = "显示流量通知",
                        summary = "在通知栏中显示流量使用情况",
                        checked = showTrafficNotification,
                        onCheckedChange = { viewModel.onShowTrafficNotificationChange(it) },
                    )
                }
                SmallTitle("网络")
                Card {
                    BasicComponent(
                        title = "自定义 User-Agent",
                        summary = if (customUserAgent.isEmpty()) "未设置，使用默认值" else customUserAgent,
                        onClick = {
                            customUserAgentTextFieldState.value = TextFieldValue(customUserAgent)
                            showEditCustomUserAgentDialog.value = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    WarningBottomSheet(
        show = showHideIconDialog,
        title = "警告",
        messages = listOf(
            "请在隐藏之前确认你能够访问本应用的设置界面！",
            "对于 HyperOS, 请开启 自启动 和 后台弹出界面 权限,以接受拨号界面代码！"
        ),
        onConfirm = {
            viewModel.onHideAppIconChange(true)
            AppIconHelper.toggleIcon(context, true)
        },
    )

    TextEditBottomSheet(
        show = showEditOneWordDialog,
        title = "编辑一言",
        textFieldValue = oneWordTextFieldState,
        onConfirm = { viewModel.onOneWordChange(it) },
    )

    TextEditBottomSheet(
        show = showEditOneWordAuthorDialog,
        title = "编辑作者",
        textFieldValue = oneWordAuthorTextFieldState,
        onConfirm = { viewModel.onOneWordAuthorChange(it) },
    )

    TextEditBottomSheet(
        show = showEditCustomUserAgentDialog,
        title = "编辑 User-Agent",
        textFieldValue = customUserAgentTextFieldState,
        onConfirm = {
            viewModel.onCustomUserAgentChange(it)
            Clash.setCustomUserAgent(it)
        },
    )
}
