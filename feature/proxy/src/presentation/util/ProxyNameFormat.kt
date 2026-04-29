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



package com.github.yumelira.yumebox.presentation.util

data class FlaggedName(
    val countryCode: String?,
    val displayName: String,
)

private const val REGIONAL_INDICATOR_BASE = 0x1F1E6
private const val REGIONAL_INDICATOR_END = 0x1F1FF

private fun isRegionalIndicator(codePoint: Int): Boolean {
    return codePoint in REGIONAL_INDICATOR_BASE..REGIONAL_INDICATOR_END
}

private fun Char.isNameSeparator(): Boolean {
    return this.isWhitespace() || this == '-' || this == '|' || this == '·' || this == '•' || this == '—' || this == ':'
}

private val COUNTRY_NAME_TO_CODE = mapOf(

    "中国" to "CN", "香港" to "HK", "澳门" to "MO", "台湾" to "TW",
    "美国" to "US", "英国" to "GB", "法国" to "FR", "德国" to "DE",
    "日本" to "JP", "韩国" to "KR", "韩国" to "KR", "新加坡" to "SG",
    "澳大利亚" to "AU", "澳洲" to "AU", "加拿大" to "CA", "俄罗斯" to "RU",
    "印度" to "IN", "印尼" to "ID", "印度尼西亚" to "ID", "马来西亚" to "MY",
    "泰国" to "TH", "越南" to "VN", "菲律宾" to "PH", "柬埔寨" to "KH",
    "老挝" to "LA", "缅甸" to "MM", "孟加拉" to "BD", "巴基斯坦" to "PK",
    "斯里兰卡" to "LK", "尼泊尔" to "NP", "不丹" to "BT", "马尔代夫" to "MV",
    "哈萨克斯坦" to "KZ", "乌兹别克斯坦" to "UZ", "土库曼斯坦" to "TM", "塔吉克斯坦" to "TJ",
    "吉尔吉斯斯坦" to "KG", "伊朗" to "IR", "伊拉克" to "IQ", "沙特" to "SA",
    "阿联酋" to "AE", "卡塔尔" to "QA", "科威特" to "KW", "巴林" to "BH",
    "阿曼" to "OM", "也门" to "YE", "约旦" to "JO", "黎巴嫩" to "LB",
    "以色列" to "IL", "巴勒斯坦" to "PS", "土耳其" to "TR", "格鲁吉亚" to "GE",
    "阿塞拜疆" to "AZ", "亚美尼亚" to "AM", "乌克兰" to "UA", "白俄罗斯" to "BY",
    "波兰" to "PL", "捷克" to "CZ", "斯洛伐克" to "SK", "匈牙利" to "HU",
    "罗马尼亚" to "RO", "保加利亚" to "BG", "塞尔维亚" to "RS", "克罗地亚" to "HR",
    "斯洛文尼亚" to "SI", "波黑" to "BA", "黑山" to "ME", "北马其顿" to "MK",
    "阿尔巴尼亚" to "AL", "希腊" to "GR", "意大利" to "IT", "西班牙" to "ES",
    "葡萄牙" to "PT", "荷兰" to "NL", "比利时" to "BE", "卢森堡" to "LU",
    "瑞士" to "CH", "奥地利" to "AT", "瑞典" to "SE", "挪威" to "NO",
    "丹麦" to "DK", "芬兰" to "FI", "冰岛" to "IS", "爱尔兰" to "IE",
    "爱沙尼亚" to "EE", "拉脱维亚" to "LV", "立陶宛" to "LT",
    "墨西哥" to "MX", "巴西" to "BR", "阿根廷" to "AR", "智利" to "CL",
    "哥伦比亚" to "CO", "秘鲁" to "PE", "委内瑞拉" to "VE", "厄瓜多尔" to "EC",
    "乌拉圭" to "UY", "巴拉圭" to "PY", "玻利维亚" to "BO", "哥斯达黎加" to "CR",
    "巴拿马" to "PA", "古巴" to "CU", "牙买加" to "JM", "多米尼加" to "DO",
    "埃及" to "EG", "南非" to "ZA", "尼日利亚" to "NG", "肯尼亚" to "KE",
    "摩洛哥" to "MA", "突尼斯" to "TN", "阿尔及利亚" to "DZ", "利比亚" to "LY",
    "埃塞俄比亚" to "ET", "加纳" to "GH", "坦桑尼亚" to "TZ", "乌干达" to "UG",
    "卢旺达" to "RW", "津巴布韦" to "ZW", "博茨瓦纳" to "BW", "纳米比亚" to "NA",
    "新西兰" to "NZ", "斐济" to "FJ", "巴布亚新几内亚" to "PG",
    "朝鲜" to "KP", "蒙古" to "MN", "文莱" to "BN", "东帝汶" to "TL",

    "China" to "CN", "Hong Kong" to "HK", "Macau" to "MO", "Taiwan" to "TW",
    "United States" to "US", "USA" to "US", "America" to "US",
    "United Kingdom" to "GB", "UK" to "GB", "England" to "GB", "Britain" to "GB",
    "France" to "FR", "Germany" to "DE", "Deutschland" to "DE",
    "Japan" to "JP", "Korea" to "KR", "South Korea" to "KR",
    "Singapore" to "SG", "Australia" to "AU", "Canada" to "CA",
    "Russia" to "RU", "India" to "IN", "Indonesia" to "ID",
    "Malaysia" to "MY", "Thailand" to "TH", "Vietnam" to "VN",
    "Philippines" to "PH", "Cambodia" to "KH", "Laos" to "LA",
    "Myanmar" to "MM", "Burma" to "MM", "Bangladesh" to "BD",
    "Pakistan" to "PK", "Sri Lanka" to "LK", "Nepal" to "NP",
    "Bhutan" to "BT", "Maldives" to "MV",
    "Kazakhstan" to "KZ", "Uzbekistan" to "UZ", "Turkmenistan" to "TM",
    "Tajikistan" to "TJ", "Kyrgyzstan" to "KG",
    "Iran" to "IR", "Iraq" to "IQ", "Saudi Arabia" to "SA",
    "UAE" to "AE", "Qatar" to "QA", "Kuwait" to "KW", "Bahrain" to "BH",
    "Oman" to "OM", "Yemen" to "YE", "Jordan" to "JO", "Lebanon" to "LB",
    "Israel" to "IL", "Palestine" to "PS", "Turkey" to "TR",
    "Georgia" to "GE", "Azerbaijan" to "AZ", "Armenia" to "AM",
    "Ukraine" to "UA", "Belarus" to "BY", "Poland" to "PL",
    "Czech" to "CZ", "Slovakia" to "SK", "Hungary" to "HU",
    "Romania" to "RO", "Bulgaria" to "BG", "Serbia" to "RS",
    "Croatia" to "HR", "Slovenia" to "SI", "Bosnia" to "BA",
    "Montenegro" to "ME", "Macedonia" to "MK", "Albania" to "AL",
    "Greece" to "GR", "Italy" to "IT", "Spain" to "ES",
    "Portugal" to "PT", "Netherlands" to "NL", "Belgium" to "BE",
    "Luxembourg" to "LU", "Switzerland" to "CH", "Austria" to "AT",
    "Sweden" to "SE", "Norway" to "NO", "Denmark" to "DK",
    "Finland" to "FI", "Iceland" to "IS", "Ireland" to "IE",
    "Estonia" to "EE", "Latvia" to "LV", "Lithuania" to "LT",
    "Mexico" to "MX", "Brazil" to "BR", "Argentina" to "AR",
    "Chile" to "CL", "Colombia" to "CO", "Peru" to "PE",
    "Venezuela" to "VE", "Ecuador" to "EC", "Uruguay" to "UY",
    "Paraguay" to "PY", "Bolivia" to "BO", "Costa Rica" to "CR",
    "Panama" to "PA", "Cuba" to "CU", "Jamaica" to "JM",
    "Dominican" to "DO", "Egypt" to "EG", "South Africa" to "ZA",
    "Nigeria" to "NG", "Kenya" to "KE", "Morocco" to "MA",
    "Tunisia" to "TN", "Algeria" to "DZ", "Libya" to "LY",
    "Ethiopia" to "ET", "Ghana" to "GH", "Tanzania" to "TZ",
    "Uganda" to "UG", "Rwanda" to "RW", "Zimbabwe" to "ZW",
    "Botswana" to "BW", "Namibia" to "NA", "New Zealand" to "NZ",
    "Fiji" to "FJ", "Papua New Guinea" to "PG",
    "North Korea" to "KP", "Mongolia" to "MN", "Brunei" to "BN",
    "Timor" to "TL",
)

private val COUNTRY_NAME_REGEX: Regex = run {

    val sortedNames = COUNTRY_NAME_TO_CODE.keys.sortedByDescending { it.length }
    val pattern = sortedNames.joinToString("|") { Regex.escape(it) }
    Regex("(?:$pattern)", RegexOption.IGNORE_CASE)
}

private fun extractCountryCodeFromName(name: String): Pair<String?, String> {
    val match = COUNTRY_NAME_REGEX.find(name)
    match ?: return null to name

    val countryName = match.value
    val countryCode = COUNTRY_NAME_TO_CODE[countryName] ?: COUNTRY_NAME_TO_CODE.entries.find {
        it.key.equals(countryName, ignoreCase = true)
    }?.value

    if (countryCode == null) return null to name

    val displayName = buildString {
        append(name.substring(0, match.range.first))
        append(name.substring(match.range.last + 1))
    }
        .trim { it.isWhitespace() || it == '-' || it == '|' || it == '·' || it == '•' || it == '—' || it == ':' }

    return countryCode to displayName.ifEmpty { name }
}

private fun findFlagEmojiCountryCode(text: String): Pair<String, IntRange>? {
    var i = 0
    while (i < text.length - 1) {
        val first = text.codePointAt(i)
        val firstChars = Character.charCount(first)
        if (!isRegionalIndicator(first)) {
            i += firstChars
            continue
        }
        val secondIdx = i + firstChars
        if (secondIdx >= text.length) break
        val second = text.codePointAt(secondIdx)
        if (!isRegionalIndicator(second)) {
            i += firstChars
            continue
        }
        val countryCode = buildString(2) {
            append(('A'.code + (first - REGIONAL_INDICATOR_BASE)).toChar())
            append(('A'.code + (second - REGIONAL_INDICATOR_BASE)).toChar())
        }
        return countryCode to i..(secondIdx + Character.charCount(second) - 1)
    }
    return null
}

fun extractFlaggedName(rawName: String): FlaggedName {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return FlaggedName(countryCode = null, displayName = rawName)

    val flagResult = findFlagEmojiCountryCode(trimmed)
    if (flagResult != null) {
        val (countryCode, flagRange) = flagResult
        val before = trimmed.substring(0, flagRange.first)
            .trimEnd { it.isNameSeparator() }
        val after = trimmed.substring(flagRange.last + 1)
            .trimStart { it.isNameSeparator() }
        val displayName = when {
            before.isNotEmpty() && after.isNotEmpty() -> "$before $after"
            before.isNotEmpty() -> before
            else -> after
        }.ifEmpty { trimmed }
        return FlaggedName(countryCode = countryCode, displayName = displayName)
    }

    val (codeFromName, displayName) = extractCountryCodeFromName(trimmed)
    if (codeFromName != null) {
        return FlaggedName(countryCode = codeFromName, displayName = displayName)
    }

    return FlaggedName(countryCode = null, displayName = trimmed)
}
