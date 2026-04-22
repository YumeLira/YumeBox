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

package com.github.yumelira.yumebox.core.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

object YamlCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    private val yaml = Yaml(
        DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
            width = 160
            splitLines = false
        },
    )

    fun <T> encode(
        serializer: KSerializer<T>,
        value: T,
    ): String {
        val element = json.encodeToJsonElement(serializer, value)
        val tree = toYamlNode(element)
        return dumpValue(tree)
    }

    fun <T> decode(
        serializer: KSerializer<T>,
        content: String,
    ): T {
        val loaded = loadValue(content)
        val element = toJsonElement(loaded)
        return json.decodeFromJsonElement(serializer, element)
    }

    fun dumpMap(value: Map<String, Any?>): String {
        return dumpValue(value)
    }

    @Suppress("UNCHECKED_CAST")
    fun loadMap(content: String): Map<String, Any?> {
        val loaded = loadValue(content)
        return loaded as? Map<String, Any?> ?: emptyMap()
    }

    fun dumpValue(value: Any?): String {
        return yaml.dump(normalizeYamlValue(value))
    }

    fun loadValue(content: String): Any? {
        return normalizeYamlValue(yaml.load(content))
    }

    fun validate(content: String) {
        if (content.isBlank()) return
        yaml.load(content)
    }

    private fun toYamlNode(element: JsonElement): Any? {
        return when (element) {
            JsonNull -> null
            is JsonObject -> LinkedHashMap<String, Any?>().apply {
                element.forEach { (key, value) -> put(key, toYamlNode(value)) }
            }

            is JsonArray -> element.map(::toYamlNode)
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.booleanOrNull
                element.intOrNull != null -> element.intOrNull
                element.longOrNull != null -> element.longOrNull
                element.doubleOrNull != null -> element.doubleOrNull
                else -> element.content
            }
        }
    }

    private fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is Map<*, *> -> JsonObject(
                LinkedHashMap<String, JsonElement>().apply {
                    value.forEach { (key, child) ->
                        put(key.toString(), toJsonElement(child))
                    }
                },
            )

            is List<*> -> JsonArray(value.map(::toJsonElement))
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value.toDouble())
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun normalizeYamlValue(value: Any?): Any? {
        return when (value) {
            null -> null
            is Map<*, *> -> LinkedHashMap<String, Any?>().apply {
                value.forEach { (key, childValue) ->
                    put(key.toString(), normalizeYamlValue(childValue))
                }
            }

            is Iterable<*> -> value.map(::normalizeYamlValue)
            is Array<*> -> value.map(::normalizeYamlValue)
            else -> value
        }
    }
}
