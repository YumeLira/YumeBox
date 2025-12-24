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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.BuildConfig
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.core.bridge.Bridge
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.LinkItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SmallTitle
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.OpenSourceLicensesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AboutScreen(navigator: DestinationsNavigator) {

    val scrollBehavior = MiuixScrollBehavior()
    var coreVersion by remember { mutableStateOf(MLang.About.App.VersionLoading) }
    LocalContext.current

    LaunchedEffect(Unit) {
        coreVersion = try {
            Bridge.nativeCoreVersion()
        } catch (_: Exception) {
            MLang.About.App.VersionFailed
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = MLang.About.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.yume),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        tint = Color.Unspecified
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "YumeBox", style = MiuixTheme.textStyles.title1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${BuildConfig.VERSION_NAME} ($coreVersion)",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
                Card {
                    BasicComponent(
                        title = "YumeBox", summary = MLang.About.App.Description
                    )
                }
                SmallTitle(MLang.About.Section.ProjectLinks)

                Card {
                    LinkItem(
                        title = "YumeBox", url = "https://github.com/YumeLira/YumeBox"
                    )
                    LinkItem(
                        title = "Mihomo", url = "https://github.com/MetaCubeX/mihomo"
                    )
                }
                SmallTitle(MLang.About.Section.More)

                Card {
                    LinkItem(
                        title = MLang.About.Link.TelegramGroup, url = "https://t.me/OOM_Group", showArrow = true
                    )
                    LinkItem(
                        title = MLang.About.Link.TelegramChannel, url = "https://t.me/YumeLira", showArrow = true
                    )
                }
                SmallTitle(MLang.About.Section.License)

                Card {
                    SuperArrow(
                        title = MLang.About.License.Libraries,
                        summary = MLang.About.License.LibrariesSummary,
                        onClick = { navigator.navigate(OpenSourceLicensesScreenDestination) })
                    BasicComponent(
                        title = MLang.About.License.AgplName,
                        summary = MLang.About.License.AgplDescription,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = MLang.About.Copyright,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}