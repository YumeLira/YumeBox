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



package com.github.yumelira.yumebox.feature.editor.format

import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import org.json.JSONArray
import org.json.JSONObject

object CodeFormatter {

    fun format(content: String, language: LanguageScope): String? {
        return when (language) {
            LanguageScope.Json -> formatJson(content)
            LanguageScope.Yaml -> formatYaml(content)
            LanguageScope.JavaScript,
            LanguageScope.Text -> content
        }
    }

    fun validate(content: String, language: LanguageScope): Boolean {
        return when (language) {
            LanguageScope.Json -> validateJson(content)
            LanguageScope.Yaml -> true
            LanguageScope.JavaScript -> true
            LanguageScope.Text -> true
        }
    }

    private fun formatJson(content: String): String? {
        return try {
            val trimmed = content.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun validateJson(content: String): Boolean {
        return try {
            val trimmed = content.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun formatYaml(content: String): String? {
        return try {
            content.lines()
                .map { it.trimEnd() }
                .joinToString("\n")
                .replace(Regex("\n{3,}"), "\n\n")
        } catch (e: Exception) {
            null
        }
    }
}
