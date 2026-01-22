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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable

internal data class WizardState(
    val pagerState: PagerState,
    val flingBehavior: androidx.compose.foundation.gestures.FlingBehavior,
    val paddingValues: PaddingValues,
    val privacyAccepted: Boolean,
    val onPrivacyAcceptedChange: (Boolean) -> Unit,
    val permissionState: PermissionState,
) {
    val currentPage: Int get() = pagerState.currentPage
}

internal data class PermissionState(
    val notificationGranted: Boolean,
    val appListGranted: Boolean,
    val miuiDynamicSupported: Boolean,
    val onRequestNotification: () -> Unit,
    val onRequestAppList: () -> Unit,
)

@Composable
internal fun rememberPagerAndFling(
    pageCount: Int,
): Pair<PagerState, androidx.compose.foundation.gestures.FlingBehavior> {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            dampingRatio = 0.7f,
        ),
    )
    return pagerState to flingBehavior
}

