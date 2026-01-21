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

fun extractFlaggedName(rawName: String): FlaggedName {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return FlaggedName(countryCode = null, displayName = rawName)

    val first = trimmed.codePointAt(0)
    val firstChars = Character.charCount(first)
    if (!isRegionalIndicator(first) || trimmed.length <= firstChars) {
        return FlaggedName(countryCode = null, displayName = trimmed)
    }

    val secondIndex = firstChars
    val second = trimmed.codePointAt(secondIndex)
    if (!isRegionalIndicator(second)) {
        return FlaggedName(countryCode = null, displayName = trimmed)
    }

    val countryCode = buildString(2) {
        append(('A'.code + (first - REGIONAL_INDICATOR_BASE)).toChar())
        append(('A'.code + (second - REGIONAL_INDICATOR_BASE)).toChar())
    }

    val afterSecond = secondIndex + Character.charCount(second)
    val rest = trimmed.substring(afterSecond).trimStart { it.isNameSeparator() }
    return FlaggedName(countryCode = countryCode, displayName = rest.ifEmpty { trimmed })
}

