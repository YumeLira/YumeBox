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

package com.github.yumelira.yumebox.service.common.util

import com.github.yumelira.yumebox.service.runtime.entity.Profile

object AutoStartUpdatePolicy {
    enum class Decision {
        Proceed,
        AutoUpdateDisabled,
        SkipPostUpdateColdStart,
        SkipColdStartReason,
        NoActiveProfile,
        UnsupportedProfileType,
    }

    fun decide(
        autoUpdateEnabled: Boolean,
        activeProfile: Profile?,
        skipForPostUpdateColdStart: Boolean,
        startupReason: String? = null,
        coldStartReasons: Set<String> = emptySet(),
    ): Decision {
        if (!autoUpdateEnabled) return Decision.AutoUpdateDisabled
        if (skipForPostUpdateColdStart) return Decision.SkipPostUpdateColdStart
        if (!startupReason.isNullOrBlank() && startupReason in coldStartReasons) {
            return Decision.SkipColdStartReason
        }
        if (activeProfile == null) return Decision.NoActiveProfile
        if (activeProfile.type != Profile.Type.Url) return Decision.UnsupportedProfileType
        return Decision.Proceed
    }
}
