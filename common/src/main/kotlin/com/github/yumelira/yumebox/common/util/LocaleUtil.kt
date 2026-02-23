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

package com.github.yumelira.yumebox.common.util

import java.util.*

object LocaleUtil {

    private val CHINA_REGION_CODES = setOf("TW", "HK", "MO")

    fun isChineseLocale(): Boolean {
        val locale = Locale.getDefault()
        return locale.language == "zh"
    }

    fun normalizeRegionCode(countryCode: String?): String? {
        if (countryCode == null) return null
        if (!isChineseLocale()) return countryCode

        val upperCode = countryCode.uppercase()
        return if (upperCode in CHINA_REGION_CODES) "CN" else countryCode
    }

    fun normalizeFlagUrl(
        countryCode: String,
        baseUrl: String = "https://hatscripts.github.io/circle-flags/flags/"
    ): String {
        val normalizedCode = if (isChineseLocale()) {
            val upperCode = countryCode.uppercase()
            if (upperCode in CHINA_REGION_CODES) "cn" else countryCode.lowercase()
        } else {
            countryCode.lowercase()
        }
        return "${baseUrl}${normalizedCode}.svg"
    }
}
