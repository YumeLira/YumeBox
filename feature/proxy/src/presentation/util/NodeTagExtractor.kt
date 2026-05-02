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

private val NODE_KEYWORDS = listOf(
    "IEPL", "BGP", "APL", "IPLC", "CMI", "CN2", "GIA", "MPLS",
    "专线", "中转", "游戏", "企业", "NF", "Netflix", "Disney", "GPT",
)

private val MULTIPLIER_REGEX = Regex(
    """(?<![.\d])[xX×✕](\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)[xX×✕](?![.\d])"""
)

data class NodeTags(
    val keywords: List<String>,
    val multiplier: Float?,
)

fun extractNodeTags(name: String): NodeTags {
    val upperName = name.uppercase()
    val keywords = NODE_KEYWORDS.filter { keyword -> upperName.contains(keyword.uppercase()) }
    val multiplierMatch = MULTIPLIER_REGEX.find(name)
    val multiplier = multiplierMatch?.let { match ->
        val value = match.groupValues[1].ifEmpty { match.groupValues[2] }
        value.toFloatOrNull()
    }
    return NodeTags(keywords = keywords, multiplier = multiplier)
}
