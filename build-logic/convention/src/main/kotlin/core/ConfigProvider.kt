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
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package core

import org.gradle.api.Project
import java.util.*

class ConfigProvider(private val project: Project) {
    private val catalog by lazy { project.rootProject.extensions.findByName("libs") }
    private val externalProperties by lazy { loadExternalProperties() }
    private val gropifyAccessor by lazy { GropifyAccessor(project) }

    private fun loadExternalProperties(): Properties {
        val file = project.rootProject.file("kernel.properties")
        val props = Properties()
        if (file.isFile) {
            runCatching { file.inputStream().use(props::load) }
        }
        return props
    }

    private fun fromExternalConfig(key: String): String? {
        return externalProperties.getProperty(key)?.takeIf { it.isNotBlank() }
    }

    private fun fromGropify(key: String): String? {
        return gropifyAccessor.getPropertyValue(key)
    }

    private fun fromGradleProperties(key: String): String? {
        return project.findProperty(key)?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun fromCatalog(key: String): String? {
        val libs = catalog ?: return null
        val versionObj = ReflectionInvoker.invoke(libs, "findVersion", key) ?: return null
        val reqVersion = ReflectionInvoker.invoke(versionObj, "get") ?: return null
        val requiredVersion = ReflectionInvoker.invoke(reqVersion, "getRequiredVersion") as? String
        return requiredVersion?.takeIf { it.isNotBlank() }
    }

    fun getString(key: String, fallback: String): String {
        return fromExternalConfig(key)
            ?: fromGropify(key)
            ?: fromGradleProperties(key)
            ?: fromCatalog(key)
            ?: fallback
    }

    fun getInt(key: String, fallback: Int): Int {
        return getString(key, fallback.toString()).toIntOrNull() ?: fallback
    }

    fun getCsv(key: String, fallback: String): List<String> {
        return getString(key, fallback).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}

fun Project.gropifyString(path: String, fallback: String): String {
    val value = GropifyAccessor(this).getPropertyValue(path)
    return if (value.isNullOrBlank()) fallback else value
}

fun Project.mmkvDependencyNotation(): String = "com.tencent:mmkv:${mmkvVersionForCurrentAbi()}"

fun Project.mmkvVersionForCurrentAbi(): String {
    val targetAbi = findProperty("android.injected.build.abi") as String?
    return when (targetAbi) {
        "arm64-v8a", "x86_64" -> "2.2.4"
        else -> "1.3.14"
    }
}

private class GropifyAccessor(project: Project) {
    private val extension = project.extensions.findByName("gropify")
    private val getPropertyValueMethod = extension
        ?.javaClass
        ?.methods
        ?.firstOrNull { method ->
            method.name == "getPropertyValue" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == String::class.java
        }

    fun getPropertyValue(key: String): String? {
        val method = getPropertyValueMethod ?: return null
        val target = extension ?: return null
        return (runCatching { method.invoke(target, key) }.getOrNull() as? String)
            ?.takeIf { it.isNotBlank() }
    }
}

private object ReflectionInvoker {
    fun invoke(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = target.javaClass.methods.firstOrNull { method ->
            method.name == methodName && method.parameterTypes.size == args.size
        } ?: return null
        return runCatching { method.invoke(target, *args) }.getOrNull()
    }
}
