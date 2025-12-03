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

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.data.model.ThemeMode

import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperSwitch
import dev.oom_wg.purejoy.mlang.MLang

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
    val showDivider = viewModel.showDivider.state.collectAsState().value

    val oneWord = viewModel.oneWord.state.collectAsState().value
    val oneWordAuthor = viewModel.oneWordAuthor.state.collectAsState().value

    val showHideIconDialog = remember { mutableStateOf(false) }
    val showEditOneWordDialog = remember { mutableStateOf(false) }
    val showEditOneWordAuthorDialog = remember { mutableStateOf(false) }

    val oneWordTextFieldState = remember { mutableStateOf(TextFieldValue(oneWord)) }
    val oneWordAuthorTextFieldState = remember { mutableStateOf(TextFieldValue(oneWordAuthor)) }

    Scaffold(
        topBar = {
            TopBar(title = MLang.AppSettings.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                SmallTitle(MLang.AppSettings.Section.Behavior)
                Card {
                    SuperSwitch(
                        title = MLang.AppSettings.Behavior.AutoStartTitle,
                        summary = MLang.AppSettings.Behavior.AutoStartSummary,
                        checked = automaticRestart,
                        onCheckedChange = { viewModel.onAutomaticRestartChange(it) },
                    )
                    if (com.github.yumelira.yumebox.common.util.LocaleUtil.isChineseLocale()) {
                        SuperSwitch(
                            title = MLang.AppSettings.Behavior.OneChinaTitle,
                            summary = MLang.AppSettings.Behavior.OneChinaSummary,
                            checked = true,
                            onCheckedChange = { },
                            enabled = false,
                        )
                    }
                }
                SmallTitle(MLang.AppSettings.Section.Home)
                Card {
                    BasicComponent(
                        title = MLang.AppSettings.Home.OneWordTitle,
                        summary = viewModel.oneWord.value,
                        onClick = {
                            oneWordTextFieldState.value = TextFieldValue(viewModel.oneWord.value)
                            showEditOneWordDialog.value = true
                        }
                    )
                    BasicComponent(
                        title = MLang.AppSettings.Home.OneWordAuthorTitle,
                        summary = viewModel.oneWordAuthor.value,
                        onClick = {
                            oneWordAuthorTextFieldState.value = TextFieldValue(viewModel.oneWordAuthor.value)
                            showEditOneWordAuthorDialog.value = true
                        }
                    )
                }
                SmallTitle(MLang.AppSettings.Section.Interface)
                Card {
EnumSelector(
                        title = MLang.AppSettings.Interface.ThemeModeTitle,
                        summary = MLang.AppSettings.Interface.ThemeModeSummary,
                        currentValue = themeMode,
                        items = listOf(MLang.AppSettings.Interface.ThemeModeSystem, MLang.AppSettings.Interface.ThemeModeLight, MLang.AppSettings.Interface.ThemeModeDark),
                        values = ThemeMode.entries,
                        onValueChange = { viewModel.onThemeModeChange(it) },
                    )
                    EnumSelector(
                        title = MLang.AppSettings.Interface.ColorThemeTitle,
                        summary = MLang.AppSettings.Interface.ColorThemeSummary,
                        currentValue = colorTheme,
                        items = listOf(
                            MLang.AppSettings.Interface.ColorMinimal,
                            MLang.AppSettings.Interface.ColorClassic,
                            MLang.AppSettings.Interface.ColorOcean,
                            MLang.AppSettings.Interface.ColorFresh,
                            MLang.AppSettings.Interface.ColorPrincess,
                            MLang.AppSettings.Interface.ColorMystery,
                            MLang.AppSettings.Interface.ColorGolden,
                        ),
                        values = AppColorTheme.entries,
                        onValueChange = { viewModel.onColorThemeChange(it) },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.ShowDividerTitle,
                        summary = MLang.AppSettings.Interface.ShowDividerSummary,
                        checked = showDivider,
                        onCheckedChange = { viewModel.onShowDividerChange(it) },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.HideIconTitle,
                        summary = MLang.AppSettings.Interface.HideIconSummary,
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
                SmallTitle(MLang.AppSettings.Section.Service)
                Card {
                    SuperSwitch(
                        title = MLang.AppSettings.ServiceSection.TrafficNotificationTitle,
                        summary = MLang.AppSettings.ServiceSection.TrafficNotificationSummary,
                        checked = showTrafficNotification,
                        onCheckedChange = { viewModel.onShowTrafficNotificationChange(it) },
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    WarningBottomSheet(
        show = showHideIconDialog,
        title = MLang.AppSettings.WarningDialog.Title,
        messages = listOf(
            MLang.AppSettings.WarningDialog.HideIconMsg1,
            MLang.AppSettings.WarningDialog.HideIconMsg2
        ),
        onConfirm = {
            viewModel.onHideAppIconChange(true)
            AppIconHelper.toggleIcon(context, true)
        },
    )

    TextEditBottomSheet(
        show = showEditOneWordDialog,
        title = MLang.AppSettings.EditDialog.OneWordTitle,
        textFieldValue = oneWordTextFieldState,
        onConfirm = { viewModel.onOneWordChange(it) },
    )

    TextEditBottomSheet(
        show = showEditOneWordAuthorDialog,
        title = MLang.AppSettings.EditDialog.AuthorTitle,
        textFieldValue = oneWordAuthorTextFieldState,
        onConfirm = { viewModel.onOneWordAuthorChange(it) },
    )
}
