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

package com.github.yumelira.yumebox.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OverrideContentType(
    val extension: String,
) {
    @SerialName("yaml")
    Yaml("yaml"),

    @SerialName("js")
    JavaScript("js"),
    ;

    companion object {
        fun fromExtension(extension: String?): OverrideContentType? {
            return when (extension?.lowercase()?.removePrefix(".")) {
                "yaml", "yml" -> Yaml
                "js" -> JavaScript
                else -> null
            }
        }

        fun fromFileName(fileName: String?): OverrideContentType? {
            return fromExtension(fileName?.substringAfterLast('.', missingDelimiterValue = ""))
        }
    }
}
