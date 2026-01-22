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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActivationWizardScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppStartScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MainScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>(start = true)
fun AppStartScreen(navigator: DestinationsNavigator) {
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val onboardingCompleted by appSettingsViewModel.onboardingCompleted.state.collectAsState()

    LaunchedEffect(onboardingCompleted) {
        val destination = if (onboardingCompleted) {
            MainScreenDestination(initialPage = 0)
        } else {
            ActivationWizardScreenDestination
        }

        navigator.navigate(destination) {
            popUpTo(AppStartScreenDestination) { inclusive = true }
            launchSingleTop = true
        }
    }

    Surface(color = MiuixTheme.colorScheme.surface) {}
}

