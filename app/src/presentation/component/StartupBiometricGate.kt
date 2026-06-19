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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.fragment.app.FragmentActivity
import com.github.yumelira.yumebox.common.util.BiometricHelper
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Stable
data class StartupBiometricGateState(
    val isAuthenticated: Boolean,
    val isAuthenticating: Boolean,
    val biometricErrorMessage: String?,
    val retryAuthentication: () -> Unit,
)

@Composable
fun rememberStartupBiometricGateState(
    activity: FragmentActivity,
    biometricUnlockEnabled: Boolean,
): StartupBiometricGateState {
    var isAuthenticated by
        remember(biometricUnlockEnabled) { mutableStateOf(!biometricUnlockEnabled) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableStateOf(0) }

    LaunchedEffect(biometricUnlockEnabled) {
        if (!biometricUnlockEnabled) {
            isAuthenticated = true
            isAuthenticating = false
            biometricErrorMessage = null
        } else {
            isAuthenticated = false
            biometricErrorMessage = null
            retryNonce += 1
        }
    }

    LaunchedEffect(biometricUnlockEnabled, retryNonce, isAuthenticated, isAuthenticating) {
        if (!biometricUnlockEnabled || isAuthenticated || isAuthenticating) {
            return@LaunchedEffect
        }

        if (!BiometricHelper.canAuthenticate(activity)) {
            biometricErrorMessage = BiometricHelper.getAuthenticationStatusMessage(activity)
            return@LaunchedEffect
        }

        isAuthenticating = true
        biometricErrorMessage = null
        BiometricHelper.authenticate(
            activity = activity,
            title = MLang.AppSettings.Privacy.BiometricPromptTitle,
            negativeButtonText = MLang.AppSettings.Privacy.BiometricExitButton,
            onSuccess = {
                isAuthenticated = true
                isAuthenticating = false
                biometricErrorMessage = null
            },
            onError = { errorCode, errString ->
                isAuthenticating = false
                if (BiometricHelper.shouldCloseAfterError(errorCode)) {
                    activity.finishAndRemoveTask()
                } else {
                    biometricErrorMessage = errString.ifBlank {
                        MLang.AppSettings.Privacy.BiometricUnavailableMessage
                    }
                }
            },
        )
    }

    return remember(isAuthenticated, isAuthenticating, biometricErrorMessage) {
        StartupBiometricGateState(
            isAuthenticated = isAuthenticated,
            isAuthenticating = isAuthenticating,
            biometricErrorMessage = biometricErrorMessage,
            retryAuthentication = {
                biometricErrorMessage = null
                retryNonce += 1
            },
        )
    }
}

@Composable
fun StartupBiometricContent(
    isAuthenticating: Boolean,
    biometricErrorMessage: String?,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = UiDp.dp24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            Text(
                text = MLang.AppSettings.Privacy.BiometricPromptTitle,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.title3,
                textAlign = TextAlign.Center,
            )
            Text(
                text = biometricErrorMessage ?: MLang.AppSettings.Privacy.BiometricPromptMessage,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                textAlign = TextAlign.Center,
            )
            if (!isAuthenticating && biometricErrorMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
                ) {
                    Button(modifier = Modifier.weight(1f), onClick = onRetry) {
                        Text(MLang.AppSettings.Privacy.BiometricRetryButton)
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(
                            text = MLang.AppSettings.Privacy.BiometricExitButton,
                            color = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
