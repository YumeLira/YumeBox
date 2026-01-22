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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActivationWizardScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MainScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
@Destination<RootGraph>
fun ActivationWizardScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()

    val wizardState = rememberWizardState(
        context = context,
        lifecycleOwner = lifecycleOwner,
        appSettingsViewModel = appSettingsViewModel,
    )

    var showPrivacySheet by remember { mutableStateOf(false) }

    Scaffold {
        OnboardingWizard(
            state = wizardState,
            onPrivacySheetRequest = { showPrivacySheet = true },
            onComplete = {
                appSettingsViewModel.setOnboardingCompleted(true)
                navigator.navigate(MainScreenDestination(initialPage = 0)) {
                    popUpTo(ActivationWizardScreenDestination) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }

    if (showPrivacySheet) {
        PrivacyPolicySheet(onDismiss = { showPrivacySheet = false })
    }
}

