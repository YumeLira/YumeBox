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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.data.controller

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import timber.log.Timber

class OverrideService(
    context: Context,
    private val resolver: OverrideResolver,
    private val onRuntimeOverrideChanged: () -> Unit = {},
) {
    private val appContext = context.appContextOrSelf

    suspend fun applyOverride(profileId: String): Boolean {
        return try {
            val overrideIds = resolver.resolveIds(profileId)
            val resolvedSpecs = resolver.resolveSpecs(overrideIds)
            val missingOverrideCount = overrideIds.size - resolvedSpecs.size

            Timber.i(
                "Apply override chain: profile=%s ids=%s specs=%s resolved=%d missing=%d",
                profileId,
                overrideIds.joinToString(","),
                resolvedSpecs.joinToString(",") { spec -> "${spec.ext}:${spec.path}" },
                resolvedSpecs.size,
                missingOverrideCount,
            )

            if (missingOverrideCount > 0) {
                Timber.w(
                    "Override chain contains missing configs: profile=%s ids=%s",
                    profileId,
                    overrideIds.joinToString(","),
                )
                return false
            }

            notifyRuntimeOverrideChanged()

            true
        } catch (error: Exception) {
            Timber.e(error, "Failed to apply override for profile: %s", profileId)
            false
        }
    }

    private fun notifyRuntimeOverrideChanged() {
        appContext.sendBroadcast(
            Intent(Intents.actionOverrideChanged(appContext.packageName))
                .setPackage(appContext.packageName),
        )
        onRuntimeOverrideChanged()
    }
}
