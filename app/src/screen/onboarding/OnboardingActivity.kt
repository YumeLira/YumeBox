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

package com.github.yumelira.yumebox.screen.onboarding

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Palette
import com.github.yumelira.yumebox.presentation.icon.yume.ShieldCheck
import com.github.yumelira.yumebox.presentation.icon.yume.UserKey
import com.github.yumelira.yumebox.presentation.theme.colorFromArgb
import com.github.yumelira.yumebox.presentation.theme.colorToArgbLong
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerSheet
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

internal class OnboardingActivity : OnboardingBaseActivity() {

    private val appSettingsStorage: AppSettingsStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!previewMode && OnboardingLauncher.consumeResetPrivacy(intent)) {
            appSettingsStorage.privacyPolicyAccepted.set(false)
        }

        setOnboardingContent {
            OnboardingPagerScreen(
                activity = this,
                onFinish = {
                    if (!previewMode) {
                        appSettingsStorage.initialSetupCompleted.set(true)
                    }
                    finishOnboarding()
                },
                onGithubClick = { openUrl(this, "https://github.com/YumeYucca/YumeBox") },
                onCommunityClick = { openUrl(this, "https://t.me/YumeLira") },
            )
        }
    }
}

private enum class OnboardingStep {
    Startup,
    Permissions,
    Terms,
    Personalize,
    Finish,
}

@Composable
private fun OnboardingPagerScreen(
    activity: OnboardingActivity,
    onFinish: () -> Unit,
    onGithubClick: () -> Unit,
    onCommunityClick: () -> Unit,
) {
    val steps = remember { OnboardingStep.entries }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { steps.size })
    val coroutineScope = rememberCoroutineScope()
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState =
        rememberPermissionState(context = activity, lifecycleOwner = lifecycleOwner)
    val privacyState = rememberPrivacyAcceptedState(appSettingsViewModel)
    val themeState = rememberThemeCustomizationState(appSettingsViewModel)
    val showPrivacySheet = remember { mutableStateOf(false) }
    var showThemeColorPicker by remember { mutableStateOf(false) }
    var editingThemeSeedColor by
        remember(themeState.themeSeedColorArgb) {
            mutableStateOf(
                runCatching { colorFromArgb(themeState.themeSeedColorArgb) }
                    .getOrDefault(Color.White)
            )
        }
    var editingThemeSeedHex by
        remember(themeState.themeSeedColorArgb) {
            mutableStateOf(
                "#${(themeState.themeSeedColorArgb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
            )
        }

    fun navigateTo(page: Int) {
        if (page !in steps.indices || pagerState.isScrollInProgress) {
            return
        }
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = page,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            )
        }
    }

    BackHandler(
        enabled = pagerState.currentPage > 0 && !showPrivacySheet.value && !showThemeColorPicker
    ) {
        navigateTo(pagerState.currentPage - 1)
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        userScrollEnabled = false,
        beyondViewportPageCount = 0,
        overscrollEffect = null,
    ) { page ->
        when (steps[page]) {
            OnboardingStep.Startup -> {
                StartupHeroShell(
                    enabled = true,
                    onStart = { navigateTo(OnboardingStep.Permissions.ordinal) },
                )
            }

            OnboardingStep.Permissions -> {
                ProvisionDetailShell(
                    previewIcon = Yume.UserKey,
                    title = MLang.Onboarding.Permission.Title,
                    subtitle = MLang.Onboarding.Permission.Subtitle,
                    primaryText = MLang.Onboarding.Navigation.Next,
                    primaryEnabled = true,
                    onPrimaryClick = { navigateTo(OnboardingStep.Terms.ordinal) },
                    onBack = { navigateTo(OnboardingStep.Startup.ordinal) },
                ) {
                    PermissionContent(permissionState)
                }
            }

            OnboardingStep.Terms -> {
                ProvisionDetailShell(
                    previewIcon = Yume.ShieldCheck,
                    title = MLang.Onboarding.Privacy.Title,
                    subtitle = MLang.Onboarding.Privacy.Subtitle,
                    primaryText = MLang.Onboarding.Navigation.Next,
                    primaryEnabled = privacyState.accepted,
                    onPrimaryClick = {
                        if (privacyState.accepted) {
                            navigateTo(OnboardingStep.Personalize.ordinal)
                        }
                    },
                    onBack = { navigateTo(OnboardingStep.Permissions.ordinal) },
                ) {
                    TermsContent(
                        accepted = privacyState.accepted,
                        onAcceptedChange = privacyState.onAcceptedChange,
                        onPrivacySheetRequest = { showPrivacySheet.value = true },
                    )
                }
            }

            OnboardingStep.Personalize -> {
                ProvisionDetailShell(
                    previewIcon = Yume.Palette,
                    title = MLang.Onboarding.Personalize.Title,
                    subtitle = MLang.Onboarding.Personalize.Subtitle,
                    primaryText = MLang.Onboarding.Navigation.Next,
                    primaryEnabled = true,
                    onPrimaryClick = { navigateTo(OnboardingStep.Finish.ordinal) },
                    onBack = { navigateTo(OnboardingStep.Terms.ordinal) },
                ) {
                    PersonalizeContent(
                        themeMode = themeState.themeMode,
                        onThemeModeChange = themeState.onThemeModeChange,
                        themeSeedColorArgb = themeState.themeSeedColorArgb,
                        onShowThemeColorPickerChange = { show ->
                            if (show) {
                                editingThemeSeedColor =
                                    runCatching { colorFromArgb(themeState.themeSeedColorArgb) }
                                        .getOrDefault(Color.White)
                                editingThemeSeedHex =
                                    "#${(themeState.themeSeedColorArgb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
                            }
                            showThemeColorPicker = show
                        },
                    )
                }
            }

            OnboardingStep.Finish -> {
                FinishHeroShell(
                    enabled = true,
                    onPrimaryClick = onFinish,
                    onGithubClick = onGithubClick,
                    onCommunityClick = onCommunityClick,
                )
            }
        }
    }

    PrivacyPolicySheet(show = showPrivacySheet)

    ThemeColorPickerSheet(
        show = showThemeColorPicker,
        editingThemeSeedColor = editingThemeSeedColor,
        editingThemeSeedHex = editingThemeSeedHex,
        onDismissRequest = { showThemeColorPicker = false },
        onEditingThemeSeedColorChange = {
            editingThemeSeedColor = it
            editingThemeSeedHex =
                "#${(colorToArgbLong(it) and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')}"
        },
        onEditingThemeSeedHexChange = { raw ->
            val normalized =
                "#${raw.uppercase().filter { ch -> ch in '0'..'9' || ch in 'A'..'F' }.take(6)}"
            editingThemeSeedHex = normalized
            if (normalized.length == 7) {
                normalized.removePrefix("#").toLongOrNull(16)?.let {
                    editingThemeSeedColor = colorFromArgb(0xFF000000L or it)
                }
            }
        },
        onConfirm = {
            themeState.onThemeSeedColorChange(colorToArgbLong(editingThemeSeedColor))
            showThemeColorPicker = false
        },
    )
}
