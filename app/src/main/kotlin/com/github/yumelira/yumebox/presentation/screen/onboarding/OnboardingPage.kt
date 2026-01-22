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

package com.github.yumelira.yumebox.presentation.screen.onboarding

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.AppConstants
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val ICON_ASPECT_RATIO = 800f / 600f

private const val HERO_SECTION_RATIO = 0.58f
private val PAGE_HORIZONTAL_PADDING = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING
private val SPACING_SMALL = 20.dp
private val SPACING_ITEM = 12.dp
private val BOTTOM_CONTENT_PADDING = 96.dp
private val HERO_IMAGE_WIDTH = 340.dp

@Composable
internal fun OnboardingPage(
    imageRes: Int,
    title: String,
    subtitle: String,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val heroHeight = maxHeight * HERO_SECTION_RATIO

        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingHero(
                imageRes = imageRes,
                heroHeight = heroHeight,
            )

            OnboardingTitle(title = title, subtitle = subtitle)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PAGE_HORIZONTAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(SPACING_ITEM),
            ) {
                if (content != null) content()
                Spacer(modifier = Modifier.height(BOTTOM_CONTENT_PADDING))
            }
        }
    }
}

@Composable
private fun OnboardingHero(
    imageRes: Int,
    heroHeight: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .padding(horizontal = PAGE_HORIZONTAL_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(imageRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .width(HERO_IMAGE_WIDTH)
                .aspectRatio(ICON_ASPECT_RATIO),
        )
    }
}

@Composable
private fun OnboardingTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_HORIZONTAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(SPACING_SMALL))
    }
}

@Composable
internal fun PermissionList(state: PermissionState) {
    Card {
        val notificationSummary = when {
            state.notificationGranted -> MLang.Onboarding.Permission.Common.Granted
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> MLang.Onboarding.Permission.Notification.SummaryNeed
            else -> MLang.Onboarding.Permission.Notification.SummaryNotRequired
        }

        SuperArrow(
            title = MLang.Onboarding.Permission.Notification.Title,
            summary = notificationSummary,
            onClick = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@SuperArrow
                if (state.notificationGranted) return@SuperArrow
                state.onRequestNotification()
            },
        )

        SuperArrow(
            title = MLang.Onboarding.Permission.AppList.Title,
            summary = if (state.appListGranted) {
                MLang.Onboarding.Permission.Common.Granted
            } else {
                MLang.Onboarding.Permission.AppList.SummaryNeed
            },
            onClick = {
                if (state.appListGranted) return@SuperArrow
                state.onRequestAppList()
            },
        )
    }
}
