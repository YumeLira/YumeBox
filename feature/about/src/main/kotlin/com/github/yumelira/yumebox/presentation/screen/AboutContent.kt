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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutContent(
    @DrawableRes appIconResId: Int,
    appVersionLabel: String,
    onCheckUpdate: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(title = MLang.About.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
            ),
            overscrollEffect = null,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Icon(
                        painter = painterResource(id = appIconResId),
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
                        text = appVersionLabel,
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
                top.yukonga.miuix.kmp.basic.SmallTitle(
                    modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp),
                    text = MLang.About.Section.ProjectLinks,
                )

                Card {
                    AboutLinkItem("YumeBox", "https://github.com/YumeLira/YumeBox", onOpenUrl, showArrow = false)
                    AboutLinkItem("Mihomo", "https://github.com/MetaCubeX/mihomo", onOpenUrl, showArrow = false)
                }
                top.yukonga.miuix.kmp.basic.SmallTitle(
                    modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp),
                    text = MLang.About.Section.More,
                )

                Card {
                    SuperArrow(
                        title = MLang.About.License.CheckUpdate,
                        summary = MLang.About.License.CheckUpdateSummary,
                        onClick = onCheckUpdate,
                    )
                    AboutLinkItem(MLang.About.Link.TelegramGroup, "https://t.me/OOM_Group", onOpenUrl, showArrow = true)
                    AboutLinkItem(MLang.About.Link.TelegramChannel, "https://t.me/YumeLira", onOpenUrl, showArrow = true)
                }
                top.yukonga.miuix.kmp.basic.SmallTitle(
                    modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp),
                    text = MLang.About.Section.License,
                )

                Card {
                    SuperArrow(
                        title = MLang.About.License.Libraries,
                        summary = MLang.About.License.LibrariesSummary,
                        onClick = onOpenLicenses,
                    )
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

@Composable
private fun AboutLinkItem(
    title: String,
    url: String,
    onOpenUrl: (String) -> Unit,
    showArrow: Boolean,
) {
    if (showArrow) {
        SuperArrow(
            title = title,
            summary = url,
            onClick = { onOpenUrl(url) },
        )
    } else {
        BasicComponent(
            title = title,
            summary = url,
            onClick = { onOpenUrl(url) },
        )
    }
}
