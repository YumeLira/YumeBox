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

package com.github.yumelira.yumebox.common.util

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.oom_wg.purejoy.mlang.MLang

object BiometricHelper {
    private const val BIOMETRIC_ONLY_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    private const val BIOMETRIC_OR_DEVICE_CREDENTIAL_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun findFragmentActivity(context: Context): FragmentActivity? =
        context.findFragmentActivityOrNull()

    /** 检查设备是否支持生物识别 */
    fun canAuthenticate(activity: FragmentActivity): Boolean =
        getAuthenticationStatus(activity) == BiometricManager.BIOMETRIC_SUCCESS

    /** 获取生物识别状态描述 */
    fun getAuthenticationStatus(activity: FragmentActivity): Int {
        val biometricManager = BiometricManager.from(activity)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                biometricManager.canAuthenticate(BIOMETRIC_OR_DEVICE_CREDENTIAL_AUTHENTICATORS)

            biometricManager.canAuthenticate(BIOMETRIC_ONLY_AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.BIOMETRIC_SUCCESS

            hasDeviceCredential(activity) -> BiometricManager.BIOMETRIC_SUCCESS
            else -> biometricManager.canAuthenticate(BIOMETRIC_ONLY_AUTHENTICATORS)
        }
    }

    /** 获取当前状态对应的提示文案 */
    fun getAuthenticationStatusMessage(activity: FragmentActivity): String {
        val status = getAuthenticationStatus(activity)
        return when {
            status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED &&
                !hasDeviceCredential(activity) ->
                MLang.AppSettings.Privacy.BiometricUnavailableNoDeviceCredential

            status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                MLang.AppSettings.Privacy.BiometricUnavailableNoneEnrolled

            status == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                MLang.AppSettings.Privacy.BiometricUnavailableNoHardware

            status == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                MLang.AppSettings.Privacy.BiometricUnavailableHwUnavailable

            else -> MLang.AppSettings.Privacy.BiometricUnavailableMessage
        }
    }

    fun shouldCloseAfterError(errorCode: Int): Boolean {
        return errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
            errorCode == BiometricPrompt.ERROR_USER_CANCELED
    }

    /**
     * 启动生物识别验证
     *
     * @param activity FragmentActivity
     * @param title 提示标题
     * @param subtitle 提示副标题
     * @param onSuccess 验证成功回调
     * @param onFailure 验证失败回调
     * @param onError 验证错误回调
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String = MLang.Component.Button.Cancel,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {},
        onError: (Int, String) -> Unit = { _, _ -> },
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onFailure()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errorCode, errString.toString())
                    }
                },
            )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder().setTitle(title)

        subtitle?.takeIf { it.isNotBlank() }?.let(promptInfoBuilder::setSubtitle)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                promptInfoBuilder.setAllowedAuthenticators(
                    BIOMETRIC_OR_DEVICE_CREDENTIAL_AUTHENTICATORS
                )
            }

            hasDeviceCredential(activity) -> {
                @Suppress("DEPRECATION") promptInfoBuilder.setDeviceCredentialAllowed(true)
            }

            else -> {
                promptInfoBuilder
                    .setAllowedAuthenticators(BIOMETRIC_ONLY_AUTHENTICATORS)
                    .setNegativeButtonText(negativeButtonText)
            }
        }

        val promptInfo = promptInfoBuilder.build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun hasDeviceCredential(activity: FragmentActivity): Boolean {
        val keyguardManager = activity.getSystemService(KeyguardManager::class.java)
        return keyguardManager?.isDeviceSecure == true
    }

    private tailrec fun Context.findFragmentActivityOrNull(): FragmentActivity? {
        return when (this) {
            is FragmentActivity -> this
            is ContextWrapper -> baseContext.findFragmentActivityOrNull()
            else -> null
        }
    }
}
