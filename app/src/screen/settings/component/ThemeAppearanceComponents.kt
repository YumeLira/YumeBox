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

package com.github.yumelira.yumebox.screen.settings.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumelira.yumebox.presentation.component.EnumSelector
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Palette
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.presentation.theme.colorFromArgb
import com.github.yumelira.yumebox.presentation.theme.colorToArgbLong
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*

@Composable
internal fun ThemeModeAndColorItems(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
) {
    ThemeModeSelectorItem(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
    ThemeColorPickerItem(
        themeSeedColorArgb = themeSeedColorArgb,
        onThemeSeedColorChange = onThemeSeedColorChange,
    )
}

@Composable
internal fun ThemeModeSelectorItem(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    EnumSelector(
        title = MLang.AppSettings.Interface.ThemeModeTitle,
        summary = MLang.AppSettings.Interface.ThemeModeSummary,
        currentValue = themeMode,
        items =
            listOf(
                MLang.AppSettings.Interface.ThemeModeSystem,
                MLang.AppSettings.Interface.ThemeModeLight,
                MLang.AppSettings.Interface.ThemeModeDark,
            ),
        values = ThemeMode.entries,
        onValueChange = onThemeModeChange,
    )
}

@Composable
internal fun ThemeColorPickerItem(
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
) {
    ThemeColorPickerItem(
        themeSeedColorArgb = themeSeedColorArgb,
        onThemeSeedColorChange = onThemeSeedColorChange,
        showBottomSheetInPlace = true,
    )
}

@Composable
internal fun ThemeColorPickerItem(
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
    showBottomSheetInPlace: Boolean,
    onOpenPickerRequest: (() -> Unit)? = null,
) {
    val showThemeColorPicker = remember { mutableStateOf(false) }
    val editingThemeSeedColor =
        remember(themeSeedColorArgb) {
            mutableStateOf(
                runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
            )
        }
    val editingThemeSeedHex =
        remember(themeSeedColorArgb) { mutableStateOf(formatThemeSeedHex(themeSeedColorArgb)) }

    BasicComponent(
        title = MLang.AppSettings.Interface.ColorThemeTitle,
        summary =
            MLang.AppSettings.Interface.ColorThemeCustomSummary.format(
                formatThemeSeedHex(themeSeedColorArgb)
            ),
        onClick = {
            editingThemeSeedColor.value =
                runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
            editingThemeSeedHex.value = formatThemeSeedHex(themeSeedColorArgb)
            if (showBottomSheetInPlace) {
                showThemeColorPicker.value = true
            } else {
                onOpenPickerRequest?.invoke()
            }
        },
        endActions = {
            val previewColor =
                remember(themeSeedColorArgb) {
                    runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
                }
            Icon(
                Yume.Palette,
                tint = previewColor,
                contentDescription = null,
                modifier = Modifier.padding(end = UiDp.dp12),
            )
        },
    )

    if (showBottomSheetInPlace) {
        ThemeColorPickerSheet(
            show = showThemeColorPicker.value,
            editingThemeSeedColor = editingThemeSeedColor.value,
            editingThemeSeedHex = editingThemeSeedHex.value,
            onDismissRequest = { showThemeColorPicker.value = false },
            onEditingThemeSeedColorChange = {
                editingThemeSeedColor.value = it
                editingThemeSeedHex.value = formatThemeSeedHex(colorToArgbLong(it))
            },
            onEditingThemeSeedHexChange = { raw ->
                // 不立即 normalize，让用户自由输入
                editingThemeSeedHex.value = raw.uppercase()
                // 尝试解析颜色，成功则更新预览
                parseThemeHexColorOrNull(raw)?.let { editingThemeSeedColor.value = it }
            },
            onConfirm = {
                val argb = colorToArgbLong(editingThemeSeedColor.value)
                onThemeSeedColorChange(argb)
                showThemeColorPicker.value = false
            },
        )
    }
}

@Composable
internal fun ThemeColorPickerSheet(
    show: Boolean,
    editingThemeSeedColor: Color,
    editingThemeSeedHex: String,
    onDismissRequest: () -> Unit,
    onEditingThemeSeedColorChange: (Color) -> Unit,
    onEditingThemeSeedHexChange: (String) -> Unit,
    onConfirm: () -> Unit,
    renderInRootScaffold: Boolean = true,
) {
    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = MLang.AppSettings.Interface.ColorThemePickerTitle,
        onDismissRequest = onDismissRequest,
        enableNestedScroll = true,
        renderInRootScaffold = renderInRootScaffold,
        defaultWindowInsetsPadding = false,
        startAction = { AppBottomSheetCloseAction(onClick = onDismissRequest) },
        endAction = { AppBottomSheetConfirmAction(onClick = onConfirm) },
        content = {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
                ColorPicker(
                    color = editingThemeSeedColor,
                    onColorChanged = onEditingThemeSeedColorChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = editingThemeSeedHex,
                    onValueChange = onEditingThemeSeedHexChange,
                    label = MLang.AppSettings.Interface.ColorThemeCodeLabel,
                    modifier = Modifier.fillMaxWidth().padding(top = UiDp.dp8),
                )
            }
        },
    )
}

private fun formatThemeSeedHex(argb: Long): String {
    val rgb = (argb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')
    return "#$rgb"
}

private fun parseThemeHexColorOrNull(input: String): Color? {
    val body = input.removePrefix("#").removePrefix("0x").uppercase()
    if (body.length != 6) return null
    val rgb = body.toLongOrNull(16) ?: return null
    return colorFromArgb(0xFF000000L or rgb)
}
