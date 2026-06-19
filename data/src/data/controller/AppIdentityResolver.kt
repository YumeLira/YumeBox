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

package com.github.yumelira.yumebox.data.controller

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class AppIdentity(val appKey: String, val packageName: String? = null, val appName: String)

class AppIdentityResolver(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val packageCache = ConcurrentHashMap<String, AppIdentity>()
    private val labelCache = ConcurrentHashMap<String, String>()
    private val uidCache = ConcurrentHashMap<Int, String?>()

    @Volatile private var installedAppsCache: List<InstalledAppIdentity>? = null

    fun resolve(metadata: JsonObject): AppIdentity {
        val explicitPackageName =
            metadata["packageName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val processName = metadata["process"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val uid = metadata["uid"]?.jsonPrimitive?.intOrNull
        return resolve(
            explicitPackageName = explicitPackageName,
            processName = processName,
            uid = uid,
        )
    }

    fun resolve(explicitPackageName: String, processName: String, uid: Int?): AppIdentity {
        val cacheKey = buildString {
            append(explicitPackageName)
            append('|')
            append(processName)
            append('|')
            append(uid ?: "")
        }
        packageCache[cacheKey]?.let {
            return it
        }

        val packageName =
            findInstalledPackage(explicitPackageName)
                ?: resolveByUid(uid)
                ?: resolveByProcess(processName)

        val identity =
            when {
                packageName != null ->
                    AppIdentity(
                        appKey = "package:$packageName",
                        packageName = packageName,
                        appName = resolveLabel(packageName).ifBlank { packageName },
                    )

                uid != null && uid > 0 ->
                    AppIdentity(
                        appKey = "uid:$uid",
                        packageName = null,
                        appName = processName.ifBlank { "UID $uid" },
                    )

                processName.isNotBlank() ->
                    AppIdentity(
                        appKey = "process:$processName",
                        packageName = null,
                        appName = processName,
                    )

                else ->
                    AppIdentity(
                        appKey = UNKNOWN_APP_KEY,
                        packageName = null,
                        appName = UNKNOWN_APP_NAME,
                    )
            }

        packageCache[cacheKey] = identity
        return identity
    }

    private fun resolveByUid(uid: Int?): String? {
        if (uid == null || uid <= 0) return null
        if (uidCache.containsKey(uid)) return uidCache[uid]

        val packageName =
            packageManager.getPackagesForUid(uid)?.firstNotNullOfOrNull(::findInstalledPackage)
        uidCache[uid] = packageName
        return packageName
    }

    private fun resolveByProcess(processName: String): String? {
        if (processName.isBlank()) return null

        val candidates =
            buildList {
                    add(processName)
                    add(processName.substringBefore(':'))
                }
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()

        candidates.firstNotNullOfOrNull(::findInstalledPackage)?.let {
            return it
        }

        return installedApps()
            .firstOrNull { app ->
                candidates.any { candidate ->
                    candidate.equals(app.packageName, ignoreCase = true) ||
                        candidate.equals(app.processName, ignoreCase = true) ||
                        candidate.equals(app.label, ignoreCase = true) ||
                        candidate.startsWith("${app.packageName}:", ignoreCase = true) ||
                        candidate.startsWith("${app.processName}:", ignoreCase = true)
                }
            }
            ?.packageName
    }

    private fun installedApps(): List<InstalledAppIdentity> {
        installedAppsCache?.let {
            return it
        }
        val apps =
            runCatching { packageManager.getInstalledApplications(PackageManager.GET_META_DATA) }
                .getOrDefault(emptyList<ApplicationInfo>())
                .map { app ->
                    InstalledAppIdentity(
                        packageName = app.packageName,
                        processName = app.processName?.trim().orEmpty(),
                        label =
                            runCatching { app.loadLabel(packageManager).toString().trim() }
                                .getOrDefault(""),
                    )
                }
        installedAppsCache = apps
        return apps
    }

    private fun findInstalledPackage(packageName: String): String? {
        if (packageName.isBlank()) return null
        return runCatching {
                packageManager.getApplicationInfo(packageName, 0)
                packageName
            }
            .getOrNull()
    }

    private fun resolveLabel(packageName: String): String {
        labelCache[packageName]?.let {
            return it
        }
        val label =
            runCatching {
                    val info = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(info).toString().trim()
                }
                .getOrDefault(packageName)
        labelCache[packageName] = label
        return label
    }

    private data class InstalledAppIdentity(
        val packageName: String,
        val processName: String,
        val label: String,
    )

    companion object {
        const val UNKNOWN_APP_KEY = "unknown"
        const val UNKNOWN_APP_NAME = "未知应用"
    }
}
