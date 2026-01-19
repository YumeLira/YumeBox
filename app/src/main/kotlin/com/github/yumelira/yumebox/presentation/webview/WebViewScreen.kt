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

package com.github.yumelira.yumebox.presentation.webview

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun WebViewScreen(
    initialUrl: String,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
    val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value

    BackHandler {
        onBack?.invoke() ?: activity?.finish()
    }

    ProvideAndroidPlatformTheme {
        YumeTheme(
            themeMode = themeMode,
            colorTheme = colorTheme
        ) {
            LocalWebView(initialUrl = initialUrl)
        }
    }
}
