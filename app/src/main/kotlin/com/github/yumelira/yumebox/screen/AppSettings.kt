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

package com.github.yumelira.yumebox.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.theme.colorFromArgb
import com.github.yumelira.yumebox.presentation.theme.colorToArgbLong
import com.github.yumelira.yumebox.presentation.theme.isDefaultThemeSeedArgb
import com.github.yumelira.yumebox.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.text.toString

@Composable
@Destination<RootGraph>
fun AppSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()


    val themeMode = viewModel.themeMode.state.collectAsState().value
    val themeSeedColorArgb = viewModel.themeSeedColorArgb.state.collectAsState().value

    val automaticRestart = viewModel.automaticRestart.state.collectAsState().value
    val hideAppIcon = viewModel.hideAppIcon.state.collectAsState().value
    val excludeFromRecents = viewModel.excludeFromRecents.state.collectAsState().value
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
    val showThemeColorPicker = remember { mutableStateOf(false) }

    var editingThemeSeedColor by remember(themeSeedColorArgb) {
        mutableStateOf(runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White))
    }
    var editingThemeSeedHex by remember(themeSeedColorArgb) {
        mutableStateOf(toHexColor(themeSeedColorArgb))
    }

    val oneWordTextFieldState = remember { mutableStateOf(TextFieldValue(oneWord)) }
    val oneWordAuthorTextFieldState = remember { mutableStateOf(TextFieldValue(oneWordAuthor)) }
    val customUserAgentTextFieldState = remember { mutableStateOf(TextFieldValue(customUserAgent)) }

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
                        summary = oneWord,
                        onClick = {
                            oneWordTextFieldState.value = TextFieldValue(oneWord)
                            showEditOneWordDialog.value = true
                        }
                    )
                    BasicComponent(
                        title = MLang.AppSettings.Home.OneWordAuthorTitle,
                        summary = oneWordAuthor,
                        onClick = {
                            oneWordAuthorTextFieldState.value = TextFieldValue(oneWordAuthor)
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
                        items = listOf(
                            MLang.AppSettings.Interface.ThemeModeSystem,
                            MLang.AppSettings.Interface.ThemeModeLight,
                            MLang.AppSettings.Interface.ThemeModeDark
                        ),
                        values = ThemeMode.entries,
                        onValueChange = { viewModel.onThemeModeChange(it) },
                    )
                    BasicComponent(
                        title = MLang.AppSettings.Interface.ColorThemeTitle,
                        summary = if (isDefaultThemeSeedArgb(themeSeedColorArgb)) {
                            MLang.AppSettings.Interface.ColorThemeDefaultSummary
                        } else {
                            MLang.AppSettings.Interface.ColorThemeCustomSummary.format(
                                toHexColor(themeSeedColorArgb)
                            )
                        },
                        onClick = {
                            editingThemeSeedColor = runCatching { colorFromArgb(themeSeedColorArgb) }
                                .getOrDefault(Color.White)
                            editingThemeSeedHex = toHexColor(themeSeedColorArgb)
                            showThemeColorPicker.value = true
                        },
                        endActions = {
                            val colorScheme = MiuixTheme.colorScheme
                            val previewBg = remember(colorScheme) {
                                colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            }
                            val previewColor = remember(themeSeedColorArgb) {
                                runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
                            }
                            Box() {

                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(26.dp)
                                        .background(
                                            color = previewColor,
                                            shape = RoundedCornerShape(50)
                                        )
                                )

                            }
                        }
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.FloatingNavbarTitle,
                        summary = MLang.AppSettings.Interface.FloatingNavbarSummary,
                        checked = bottomBarFloating,
                        onCheckedChange = { viewModel.onBottomBarFloatingChange(it) },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.AutoHideNavbarTitle,
                        summary = MLang.AppSettings.Interface.AutoHideNavbarSummary,
                        checked = bottomBarAutoHide,
                        onCheckedChange = { viewModel.onBottomBarAutoHideChange(it) },
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
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.HideFromRecentsTitle,
                        summary = MLang.AppSettings.Interface.HideFromRecentsSummary,
                        checked = excludeFromRecents,
                        onCheckedChange = { viewModel.onExcludeFromRecentsChange(it) },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.IconWithSelectedLabelTitle,
                        summary = MLang.AppSettings.Interface.IconWithSelectedLabelSummary,
                        checked = viewModel.iconWithSelectedLabel.state.collectAsState().value,
                        onCheckedChange = { viewModel.iconWithSelectedLabel.set(it) },
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
                SmallTitle(MLang.AppSettings.Section.Network)
                Card {
                    BasicComponent(
                        title = MLang.AppSettings.Network.CustomUserAgentTitle,
                        summary = if (customUserAgent.isEmpty()) {
                            MLang.AppSettings.Network.CustomUserAgentSummaryDefault
                        } else {
                            customUserAgent
                        },
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
        secondaryButtonText = MLang.AppSettings.Button.Restore,
        onSecondaryClick = {
            viewModel.resetOneWordToDefault()
            showEditOneWordDialog.value = false
        },
    )

    TextEditBottomSheet(
        show = showEditOneWordAuthorDialog,
        title = MLang.AppSettings.EditDialog.AuthorTitle,
        textFieldValue = oneWordAuthorTextFieldState,
        onConfirm = { viewModel.onOneWordAuthorChange(it) },
        secondaryButtonText = MLang.AppSettings.Button.Restore,
        onSecondaryClick = {
            viewModel.resetOneWordAuthorToDefault()
            showEditOneWordAuthorDialog.value = false
        },
    )

    TextEditBottomSheet(
        show = showEditCustomUserAgentDialog,
        title = MLang.AppSettings.EditDialog.UserAgentTitle,
        textFieldValue = customUserAgentTextFieldState,
        onConfirm = { viewModel.applyCustomUserAgent(it) },
    )

    WindowBottomSheet(
        show = showThemeColorPicker,
        title = MLang.AppSettings.Interface.ColorThemePickerTitle,
        onDismissRequest = { showThemeColorPicker.value = false },
        insideMargin = DpSize(24.dp, 16.dp),
    ) {
        ColorPicker(
            color = editingThemeSeedColor,
            onColorChanged = {
                editingThemeSeedColor = it
                editingThemeSeedHex = toHexColor(colorToArgbLong(it))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        TextField(
            value = editingThemeSeedHex,
            onValueChange = { raw ->
                val normalized = normalizeHexInput(raw)
                editingThemeSeedHex = normalized
                parseHexColorOrNull(normalized)?.let {
                    editingThemeSeedColor = it
                }
            },
            label = MLang.AppSettings.Interface.ColorThemeCodeLabel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.resetThemeSeedColor()
                    editingThemeSeedColor = Color.White
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(MLang.AppSettings.Interface.ColorThemeResetDefault)
            }
            Button(
                onClick = {
                    val argb = colorToArgbLong(editingThemeSeedColor)
                    if (isDefaultThemeSeedArgb(argb)) {
                        viewModel.resetThemeSeedColor()
                    } else {
                        viewModel.onThemeSeedColorChange(argb)
                    }
                    showThemeColorPicker.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(MLang.AppSettings.Button.Apply, color = MiuixTheme.colorScheme.background)
            }
        }
    }
}

private fun toHexColor(argb: Long): String {
    val rgb = (argb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')
    return "#$rgb"
}

private fun normalizeHexInput(input: String): String {
    val body = input
        .uppercase()
        .filter { it in '0'..'9' || it in 'A'..'F' }
        .take(6)
    return "#$body"
}

private fun parseHexColorOrNull(input: String): Color? {
    val body = input.removePrefix("#")
    if (body.length != 6) return null
    val rgb = body.toLongOrNull(16) ?: return null
    return colorFromArgb(0xFF000000L or rgb)
}
