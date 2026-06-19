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

import android.content.pm.ApplicationInfo
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

object PredictiveBackCompat {
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @Volatile private var hiddenApiExemptionApplied = false

    fun apply(applicationInfo: ApplicationInfo, enabled: Boolean) {
        if (!isSupported) {
            return
        }
        runCatching {
                ensureHiddenApiExemption()
                val setEnableOnBackInvokedCallbackMethod =
                    ApplicationInfo::class
                        .java
                        .getDeclaredMethod(
                            "setEnableOnBackInvokedCallback",
                            Boolean::class.javaPrimitiveType,
                        )
                setEnableOnBackInvokedCallbackMethod.isAccessible = true
                setEnableOnBackInvokedCallbackMethod.invoke(applicationInfo, enabled)
            }
            .onFailure { throwable ->
                Timber.w(throwable, "Failed to apply predictive back callback state")
            }
    }

    private fun ensureHiddenApiExemption() {
        if (hiddenApiExemptionApplied) {
            return
        }
        synchronized(this) {
            if (hiddenApiExemptionApplied) {
                return
            }
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
            )
            hiddenApiExemptionApplied = true
        }
    }
}
