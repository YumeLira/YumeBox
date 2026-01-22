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

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Github
import com.github.yumelira.yumebox.presentation.icon.yume.Message
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val TOTAL_PAGES = 4
private const val PAGE_WELCOME = 0
private const val PAGE_PERMISSION = 1
private const val PAGE_PRIVACY = 2
private const val PAGE_PROJECT = 3

private const val PAGE_SCROLL_DURATION_MS = 420
private val PAGE_SCROLL_ANIMATION_SPEC = tween<Float>(
    durationMillis = PAGE_SCROLL_DURATION_MS,
    easing = FastOutSlowInEasing,
)

@Composable
internal fun OnboardingWizard(
    state: WizardState,
    onPrivacySheetRequest: () -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(state.paddingValues)
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = state.pagerState,
            beyondViewportPageCount = 1,
            userScrollEnabled = false,
            flingBehavior = state.flingBehavior as TargetedFlingBehavior,
        ) { page ->
            when (page) {
                PAGE_WELCOME -> WelcomePage()
                PAGE_PERMISSION -> PermissionPage(state.permissionState)
                PAGE_PRIVACY -> PrivacyPage(
                    accepted = state.privacyAccepted,
                    onRead = onPrivacySheetRequest,
                    onAcceptedChange = state.onPrivacyAcceptedChange,
                )

                PAGE_PROJECT -> ProjectPage()
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageIndicator(currentPage = state.currentPage, totalPages = TOTAL_PAGES)
            NavigationButton(
                currentPage = state.currentPage,
                privacyAccepted = state.privacyAccepted,
                pagerState = state.pagerState,
                scope = scope,
                onComplete = onComplete,
                onError = { context.toast(it) },
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    OnboardingPage(
        imageRes = R.drawable.i_1,
        title = MLang.Onboarding.Welcome.Title,
        subtitle = MLang.Onboarding.Welcome.Subtitle,
    )
}

@Composable
private fun PermissionPage(state: PermissionState) {
    OnboardingPage(
        imageRes = R.drawable.i_2,
        title = MLang.Onboarding.Permission.Title,
        subtitle = MLang.Onboarding.Permission.Subtitle,
    ) {
        PermissionList(state = state)
    }
}

@Composable
private fun PrivacyPage(
    accepted: Boolean,
    onRead: () -> Unit,
    onAcceptedChange: (Boolean) -> Unit,
) {
    OnboardingPage(
        imageRes = R.drawable.i_3,
        title = MLang.Onboarding.Privacy.Title,
        subtitle = MLang.Onboarding.Privacy.Subtitle,
    ) {
        PrivacyContent(
            accepted = accepted,
            onRead = onRead,
            onAcceptedChange = onAcceptedChange,
        )
    }
}

@Composable
private fun ProjectPage() {
    val context = LocalContext.current

    OnboardingPage(
        imageRes = R.drawable.i_4,
        title = MLang.Onboarding.Project.Title,
        subtitle = MLang.Onboarding.Project.Subtitle,
    ) {
        ProjectLinks(context = context)
    }
}

@Composable
private fun PrivacyContent(
    accepted: Boolean,
    onRead: () -> Unit,
    onAcceptedChange: (Boolean) -> Unit,
) {
    Card {
        SuperArrow(
            title = MLang.Onboarding.Privacy.Policy.Title,
            summary = MLang.Onboarding.Privacy.Policy.Summary,
            onClick = onRead,
        )
        SuperSwitch(
            title = MLang.Onboarding.Privacy.Accept.Title,
            checked = accepted,
            onCheckedChange = onAcceptedChange,
        )
    }
}

@Composable
private fun ProjectLinks(context: Context) {
    Card {
        BasicComponent(
            title = MLang.Onboarding.Project.Github.Title,
            summary = MLang.Onboarding.Project.Github.Summary,
            onClick = { openUrl(context, "https://github.com/YumeLira/YumeBox") },
            startAction = {
                Icon(
                    imageVector = Yume.Github,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        BasicComponent(
            title = MLang.Onboarding.Project.Community.Title,
            summary = MLang.Onboarding.Project.Community.Summary,
            onClick = { openUrl(context, "https://t.me/YumeBox") },
            startAction = {
                Icon(
                    imageVector = Yume.Message,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(totalPages) { index ->
                val isActive = index == currentPage
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (isActive) 20.dp else 8.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                        .background(
                            if (isActive) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.35f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun NavigationButton(
    currentPage: Int,
    privacyAccepted: Boolean,
    pagerState: PagerState,
    scope: CoroutineScope,
    onComplete: () -> Unit,
    onError: (String) -> Unit,
) {
    val isEnabled = when (currentPage) {
        PAGE_PRIVACY -> privacyAccepted
        else -> true
    }

    val buttonText = if (currentPage == PAGE_PROJECT) {
        MLang.Onboarding.Navigation.Start
    } else {
        MLang.Onboarding.Navigation.Next
    }

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = isEnabled,
        onClick = {
            handleNavigation(
                currentPage = currentPage,
                privacyAccepted = privacyAccepted,
                scope = scope,
                pagerState = pagerState,
                onComplete = onComplete,
                onError = onError,
            )
        },
        colors = ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(text = buttonText, color = MiuixTheme.colorScheme.onPrimary)
    }
}

private fun handleNavigation(
    currentPage: Int,
    privacyAccepted: Boolean,
    scope: CoroutineScope,
    pagerState: PagerState,
    onComplete: () -> Unit,
    onError: (String) -> Unit,
) {
    when (currentPage) {
        PAGE_WELCOME, PAGE_PERMISSION -> scope.launch {
            pagerState.animateScrollToPage(
                page = currentPage + 1,
                animationSpec = PAGE_SCROLL_ANIMATION_SPEC,
            )
        }
        PAGE_PRIVACY -> {
            if (privacyAccepted) {
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = PAGE_PROJECT,
                        animationSpec = PAGE_SCROLL_ANIMATION_SPEC,
                    )
                }
            }
            else onError(MLang.Onboarding.Privacy.Error.NotAccepted)
        }

        PAGE_PROJECT -> onComplete()
    }
}
