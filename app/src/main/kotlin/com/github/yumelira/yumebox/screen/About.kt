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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.BuildConfig
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.core.bridge.Bridge
import com.github.yumelira.yumebox.presentation.screen.AboutContent
import com.github.yumelira.yumebox.update.EmasUpdateManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.OpenSourceLicensesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang

@Composable
@Destination<RootGraph>
fun AboutScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var coreVersion by remember { mutableStateOf(MLang.About.App.VersionLoading) }

    LaunchedEffect(Unit) {
        coreVersion = try {
            Bridge.nativeCoreVersion()
        } catch (_: Exception) {
            MLang.About.App.VersionFailed
        }
    }

    AboutContent(
        appIconResId = R.drawable.yume,
        appVersionLabel = "${BuildConfig.VERSION_NAME} ($coreVersion)",
        onCheckUpdate = { EmasUpdateManager.startManualUpdate(async = true) },
        onOpenLicenses = { navigator.navigate(OpenSourceLicensesScreenDestination) },
        onOpenUrl = { url -> openUrl(context, url) },
    )
}
