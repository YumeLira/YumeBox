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

import android.app.Activity
import android.content.Context
import android.content.Intent

internal object OnboardingLauncher {

    private const val EXTRA_PREVIEW_MODE = "com.github.yumelira.yumebox.onboarding.preview_mode"
    private const val EXTRA_RESET_PRIVACY = "com.github.yumelira.yumebox.onboarding.reset_privacy"

    fun start(context: Context, previewMode: Boolean = false) {
        val intent = createIntent(context, previewMode, resetPrivacy = !previewMode)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun createIntent(
        context: Context,
        previewMode: Boolean,
        resetPrivacy: Boolean = false,
    ): Intent {
        return Intent(context, OnboardingActivity::class.java)
            .putExtra(EXTRA_PREVIEW_MODE, previewMode)
            .putExtra(EXTRA_RESET_PRIVACY, resetPrivacy)
    }

    fun isPreviewMode(intent: Intent?): Boolean {
        return intent?.getBooleanExtra(EXTRA_PREVIEW_MODE, false) ?: false
    }

    fun shouldResetPrivacy(intent: Intent?): Boolean {
        return intent?.getBooleanExtra(EXTRA_RESET_PRIVACY, false) ?: false
    }

    fun consumeResetPrivacy(intent: Intent?): Boolean {
        val shouldReset = shouldResetPrivacy(intent)
        intent?.removeExtra(EXTRA_RESET_PRIVACY)
        return shouldReset
    }
}
