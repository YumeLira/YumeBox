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

package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class MMKVPreference(
    mmkvID: String? = null,
    externalMmkv: MMKV? = null,
) {
    @PublishedApi
    internal val mmkv: MMKV = externalMmkv ?: mmkvID?.let { MMKV.mmkvWithID(it) } ?: MMKV.defaultMMKV()


    protected fun bool(default: Boolean = false) = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeBool(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun str(default: String = "") = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeString(key) ?: def },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun int(default: Int = 0) = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeInt(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun long(default: Long = 0L) = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeLong(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun float(default: Float = 0f) = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeFloat(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun double(default: Double = 0.0) = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeDouble(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected inline fun <reified T : Enum<T>> enum(default: T) = MMKVProperty(
        default = default,
        getter = { key, def ->
            runCatching {
                val name = mmkv.decodeString(key) ?: def.name
                java.lang.Enum.valueOf(T::class.java, name)
            }.getOrDefault(def)
        },
        setter = { key, value -> mmkv.encode(key, value.name) },
    )

    protected fun byteArray(default: ByteArray = ByteArray(0)) = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeBytes(key) ?: def },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun stringSet(default: Set<String> = emptySet()) = MMKVProperty(
        default = default,
        getter = { key, def -> mmkv.decodeStringSet(key) ?: def },
        setter = { key, value ->
            mmkv.encode(key, value)
        },
        skipEqualityCheck = true
    )

    protected fun stringList(default: List<String> = emptyList()) = jsonList(
        default = default,
        decode = { str -> decodeFromString<List<String>>(str) },
        encode = { value -> encodeToString(value) },
    )

    protected fun intList(default: List<Int> = emptyList()) = jsonList(
        default = default,
        decode = { str -> decodeFromString<List<Int>>(str) },
        encode = { value -> encodeToString(value) },
    )

    private val json = Json { ignoreUnknownKeys = true }

    protected fun <T> jsonList(
        default: List<T> = emptyList(),
        decode: Json.(String) -> List<T>,
        encode: Json.(List<T>) -> String,
    ) = MMKVProperty(
        default = default,
        getter = { key, def ->
            runCatching {
                mmkv.decodeString(key)?.let { json.decode(it) } ?: def
            }.getOrDefault(def)
        },
        setter = { key, value ->
            mmkv.encode(key, json.encode(value))
        },
    )


    protected fun boolFlow(default: Boolean = false) = MMKVFlowProperty(
        default = default,
        getter = { key, def -> mmkv.decodeBool(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun strFlow(default: String = "") = MMKVFlowProperty(
        default = default,
        getter = { key, def -> mmkv.decodeString(key) ?: def },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun intFlow(default: Int = 0) = MMKVFlowProperty(
        default = default,
        getter = { key, def -> mmkv.decodeInt(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun longFlow(default: Long = 0L) = MMKVFlowProperty(
        default = default,
        getter = { key, def -> mmkv.decodeLong(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun floatFlow(default: Float = 0f) = MMKVFlowProperty(
        default = default,
        getter = { key, def -> mmkv.decodeFloat(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected fun doubleFlow(default: Double = 0.0) = MMKVFlowProperty(
        default = default,
        getter = { key, def -> mmkv.decodeDouble(key, def) },
        setter = { key, value -> mmkv.encode(key, value) },
    )

    protected inline fun <reified T : Enum<T>> enumFlow(default: T) = MMKVFlowProperty(
        default = default,
        getter = { key, def ->
            runCatching {
                val name = mmkv.decodeString(key) ?: def.name
                java.lang.Enum.valueOf(T::class.java, name)
            }.getOrDefault(def)
        },
        setter = { key, value -> mmkv.encode(key, value.name) },
    )

    protected fun stringSetFlow(default: Set<String> = emptySet()) = MMKVFlowProperty(
        default = default,
        getter = { key, def -> mmkv.decodeStringSet(key) ?: def },
        setter = { key, value -> mmkv.encode(key, value) },
        skipEqualityCheck = true
    )

    protected fun <T> jsonListFlow(
        default: List<T> = emptyList(),
        decode: Json.(String) -> List<T>,
        encode: Json.(List<T>) -> String,
    ) = MMKVFlowProperty(
        default = default,
        getter = { key, def ->
            runCatching {
                mmkv.decodeString(key)?.let { json.decode(it) } ?: def
            }.getOrDefault(def)
        },
        setter = { key, value ->
            mmkv.encode(key, json.encode(value))
        },
        skipEqualityCheck = true
    )


    protected class MMKVProperty<T>(
        private val default: T,
        private val getter: (key: String, default: T) -> T,
        private val setter: (key: String, value: T) -> Unit,
        private val skipEqualityCheck: Boolean = false,
    ) : ReadOnlyProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return getter(property.name, default)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            if (!skipEqualityCheck && value == getter(property.name, default)) return
            setter(property.name, value)
        }
    }

    protected class MMKVFlowProperty<T>(
        private val default: T,
        private val getter: (key: String, default: T) -> T,
        private val setter: (key: String, value: T) -> Unit,
        private val skipEqualityCheck: Boolean = false,
    ) : ReadOnlyProperty<Any?, Preference<T>> {
        private var cached: Preference<T>? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): Preference<T> {
            return cached ?: run {
                val key = property.name
                val initialValue = getter(key, default)
                val flow = MutableStateFlow(initialValue)
                Preference(
                    state = flow.asStateFlow(),
                    update = { value ->
                        if (skipEqualityCheck || value != flow.value) {
                            setter(key, value)
                            flow.value = value
                        }
                    },
                    get = { getter(key, default) }
                ).also { cached = it }
            }
        }
    }
}

data class Preference<T>(
    val state: StateFlow<T>,
    private val update: (T) -> Unit,
    private val get: () -> T,
) {
    val value: T get() = state.value

    fun set(value: T) = update(value)

    fun refresh() = update(get())
}

fun Preference<Boolean>.toggle() = set(!value)

fun <T> Preference<List<T>>.add(item: T) = set(value + item)
fun <T> Preference<List<T>>.remove(predicate: (T) -> Boolean) = set(value.filterNot(predicate))
fun <T> Preference<List<T>>.update(predicate: (T) -> Boolean, transform: (T) -> T) =
    set(value.map { if (predicate(it)) transform(it) else it })
