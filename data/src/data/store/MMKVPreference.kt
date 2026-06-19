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

package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Base class for MMKV-based preference storage with process-local reactive StateFlow support.
 *
 * This class provides a type-safe property delegation pattern for storing and observing preferences
 * using MMKV (a fast key-value storage library).
 *
 * ## Features
 * - **Type-safe delegates**: `boolFlow`, `strFlow`, `intFlow`, `longFlow`, `floatFlow`, `enumFlow`,
 *   etc.
 * - **Reactive updates**: All `*Flow` delegates expose [Preference] with [StateFlow] for in-process
 *   observation
 * - **Multi-process storage compatibility**: Uses `MMKV.MULTI_PROCESS_MODE` so app and service can
 *   safely read/write the same store
 * - **JSON serialization**: Support for complex types via `jsonListFlow`
 *
 * ## Usage Example
 *
 * ### 1. Define Storage Class
 *
 * ```kotlin
 * class AppSettingsStore(mmkv: MMKV) : MMKVPreference(externalMmkv = mmkv) {
 *     // Boolean preference with default false
 *     val darkModeEnabled by boolFlow(false)
 *
 *     // String preference
 *     val userName by strFlow("")
 *
 *     // Enum preference
 *     val theme by enumFlow(ThemeMode.Auto)
 *
 *     // Integer preference
 *     val notificationCount by intFlow(0)
 *
 *     // Complex type (JSON serialization)
 *     val recentSearches by stringListFlow(emptyList())
 * }
 * ```
 *
 * ### 2. Expose from Store
 *
 * ```kotlin
 * class AppSettingsStore(private val storage: AppSettingsStore) {
 *     val darkModeEnabled: Preference<Boolean> = storage.darkModeEnabled
 *     val userName: Preference<String> = storage.userName
 * }
 * ```
 *
 * ### 3. Use in ViewModel
 *
 * ```kotlin
 * class SettingsViewModel(private val store: AppSettingsStore) : ViewModel() {
 *     // Collect as StateFlow
 *     val darkModeState: StateFlow<Boolean> = store.darkModeEnabled.state
 *
 *     fun toggleDarkMode() {
 *         // Update value
 *         store.darkModeEnabled.set(!store.darkModeEnabled.value)
 *     }
 * }
 * ```
 *
 * ### 4. Use in Composable
 *
 * ```kotlin
 * @Composable
 * fun SettingsScreen(viewModel: SettingsViewModel) {
 *     val darkMode by viewModel.darkModeState.collectAsState()
 *
 *     Switch(
 *         checked = darkMode,
 *         onCheckedChange = { viewModel.toggleDarkMode() }
 *     )
 * }
 * ```
 *
 * ## Property Delegates Available
 *
 * ### Simple (non-reactive):
 * - `bool(default)` → Boolean
 * - `str(default)` → String
 * - `int(default)` → Int
 * - `long(default)` → Long
 * - `float(default)` → Float
 * - `double(default)` → Double
 * - `enum<T>(default)` → T (where T : Enum<T>)
 * - `byteArray(default)` → ByteArray
 * - `stringSet(default)` → Set<String>
 *
 * ### Reactive (StateFlow):
 * - `boolFlow(default)` → Preference<Boolean>
 * - `strFlow(default)` → Preference<String>
 * - `intFlow(default)` → Preference<Int>
 * - `longFlow(default)` → Preference<Long>
 * - `floatFlow(default)` → Preference<Float>
 * - `doubleFlow(default)` → Preference<Double>
 * - `enumFlow<T>(default)` → Preference<T>
 * - `stringSetFlow(default)` → Preference<Set<String>>
 * - `stringListFlow(default)` → Preference<List<String>>
 * - `intListFlow(default)` → Preference<List<Int>>
 * - `jsonListFlow<T>(...)` → Preference<List<T>>
 *
 * ## Performance Notes
 * - MMKV is extremely fast (mmap-based, zero-copy)
 * - `MULTI_PROCESS_MODE` required for IPC between app and VPN service process
 * - StateFlow updates are efficient (only notifies on value change)
 * - JSON serialization has overhead but acceptable for small lists
 *
 * ## Cross-Process Behavior
 * `MULTI_PROCESS_MODE` keeps storage access compatible across processes, but existing
 * [Preference.state] instances do not automatically receive remote-process updates. Call
 * [Preference.refresh] / [Preference.invalidate] when you need to pull the latest persisted value
 * into the local flow cache.
 *
 * The storage mode ensures safe access when:
 * - Main app UI reads/writes preferences
 * - Background VPN service reads/writes preferences
 * - Both processes access same MMKV storage simultaneously
 *
 * @param mmkvID Optional MMKV instance ID (null = default)
 * @param externalMmkv Optional external MMKV instance (overrides mmkvID)
 */
abstract class MMKVPreference(mmkvID: String? = null, externalMmkv: MMKV? = null) {
    @PublishedApi
    internal val mmkv: MMKV =
        externalMmkv
            ?: mmkvID?.let { MMKV.mmkvWithID(it, MMKV.MULTI_PROCESS_MODE) }
            ?: MMKV.defaultMMKV()

    protected fun bool(default: Boolean = false) =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeBool(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun str(default: String = "") =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeString(key) ?: def },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun int(default: Int = 0) =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeInt(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun long(default: Long = 0L) =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeLong(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun float(default: Float = 0f) =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeFloat(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun double(default: Double = 0.0) =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeDouble(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected inline fun <reified T : Enum<T>> enum(default: T) =
        MMKVProperty(
            default = default,
            getter = { key, def ->
                runCatching {
                        val name = mmkv.decodeString(key) ?: def.name
                        java.lang.Enum.valueOf(T::class.java, name)
                    }
                    .getOrDefault(def)
            },
            setter = { key, value -> mmkv.encode(key, value.name) },
        )

    protected fun byteArray(default: ByteArray = ByteArray(0)) =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeBytes(key) ?: def },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun stringSet(default: Set<String> = emptySet()) =
        MMKVProperty(
            default = default,
            getter = { key, def -> mmkv.decodeStringSet(key) ?: def },
            setter = { key, value -> mmkv.encode(key, value) },
            skipEqualityCheck = true,
        )

    protected fun stringList(default: List<String> = emptyList()) =
        jsonList(
            default = default,
            decode = { str -> decodeFromString<List<String>>(str) },
            encode = { value -> encodeToString(value) },
        )

    protected fun intList(default: List<Int> = emptyList()) =
        jsonList(
            default = default,
            decode = { str -> decodeFromString<List<Int>>(str) },
            encode = { value -> encodeToString(value) },
        )

    private val json = Json { ignoreUnknownKeys = true }

    protected fun <T> jsonList(
        default: List<T> = emptyList(),
        decode: Json.(String) -> List<T>,
        encode: Json.(List<T>) -> String,
    ) =
        MMKVProperty(
            default = default,
            getter = { key, def ->
                runCatching { mmkv.decodeString(key)?.let { json.decode(it) } ?: def }
                    .getOrDefault(def)
            },
            setter = { key, value -> mmkv.encode(key, json.encode(value)) },
        )

    protected fun boolFlow(default: Boolean = false) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def -> mmkv.decodeBool(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun strFlow(default: String = "") =
        MMKVFlowProperty(
            default = default,
            getter = { key, def -> mmkv.decodeString(key) ?: def },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun intFlow(default: Int = 0) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def -> mmkv.decodeInt(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun longFlow(default: Long = 0L) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def -> mmkv.decodeLong(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun floatFlow(default: Float = 0f) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def -> mmkv.decodeFloat(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected fun doubleFlow(default: Double = 0.0) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def -> mmkv.decodeDouble(key, def) },
            setter = { key, value -> mmkv.encode(key, value) },
        )

    protected inline fun <reified T : Enum<T>> enumFlow(default: T) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def ->
                runCatching {
                        val name = mmkv.decodeString(key) ?: def.name
                        java.lang.Enum.valueOf(T::class.java, name)
                    }
                    .getOrDefault(def)
            },
            setter = { key, value -> mmkv.encode(key, value.name) },
        )

    protected fun stringSetFlow(default: Set<String> = emptySet()) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def -> mmkv.decodeStringSet(key) ?: def },
            setter = { key, value -> mmkv.encode(key, value) },
            skipEqualityCheck = true,
        )

    protected fun stringListFlow(default: List<String> = emptyList()) =
        jsonListFlow(
            default = default,
            decode = { str -> decodeFromString<List<String>>(str) },
            encode = { value -> encodeToString(value) },
        )

    protected fun intListFlow(default: List<Int> = emptyList()) =
        jsonListFlow(
            default = default,
            decode = { str -> decodeFromString<List<Int>>(str) },
            encode = { value -> encodeToString(value) },
        )

    protected fun <T> jsonListFlow(
        default: List<T> = emptyList(),
        decode: Json.(String) -> List<T>,
        encode: Json.(List<T>) -> String,
    ) =
        MMKVFlowProperty(
            default = default,
            getter = { key, def ->
                runCatching { mmkv.decodeString(key)?.let { json.decode(it) } ?: def }
                    .getOrDefault(def)
            },
            setter = { key, value -> mmkv.encode(key, json.encode(value)) },
            skipEqualityCheck = true,
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
            return cached
                ?: run {
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
                            get = { getter(key, default) },
                            refreshState = {
                                val latest = getter(key, default)
                                if (skipEqualityCheck || latest != flow.value) {
                                    flow.value = latest
                                }
                            },
                        )
                        .also { cached = it }
                }
        }
    }
}

data class Preference<T>(
    val state: StateFlow<T>,
    private val update: (T) -> Unit,
    private val get: () -> T,
    private val refreshState: () -> Unit = { update(get()) },
) {
    val value: T
        get() = get()

    fun set(value: T) = update(value)

    fun refresh() = refreshState()

    fun invalidate() = refresh()
}

fun Preference<Boolean>.toggle() = set(!value)

fun <T> Preference<List<T>>.add(item: T) = set(value + item)

fun <T> Preference<List<T>>.remove(predicate: (T) -> Boolean) = set(value.filterNot(predicate))

fun <T> Preference<List<T>>.update(predicate: (T) -> Boolean, transform: (T) -> T) =
    set(value.map { if (predicate(it)) transform(it) else it })
