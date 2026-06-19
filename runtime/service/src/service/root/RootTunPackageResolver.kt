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

package com.github.yumelira.yumebox.service.root

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.github.yumelira.yumebox.service.runtime.config.AccessControlMode
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore

internal class RootTunPackageResolver(
    private val context: Context,
    private val store: ServiceStore = ServiceStore(),
) {
    data class Result(
        val includeUid: List<Int>,
        val excludeUid: List<Int>,
        val missingPackages: Set<String>,
    )

    private data class PackageUidLookup(
        val packageUidMap: Map<String, Int>,
        val hasFullPackageAccess: Boolean,
    )

    fun resolve(): Result {
        val selectedPackages = store.accessControlPackages
        val packageUidLookup = resolvePackageUidLookup(selectedPackages)
        val packageUidMap = packageUidLookup.packageUidMap
        val allAppUid =
            packageUidMap.values
                .asSequence()
                .filter { it >= APP_UID_MIN }
                .filterNot { it == context.applicationInfo.uid }
                .toSortedSet()

        val selectedUid =
            selectedPackages
                .asSequence()
                .mapNotNull(packageUidMap::get)
                .filter { it >= APP_UID_MIN }
                .filterNot { it == context.applicationInfo.uid }
                .toSortedSet()

        val missingPackages = selectedPackages.filterNot(packageUidMap::containsKey).toSortedSet()

        val includeUid =
            when (store.accessControlMode) {
                AccessControlMode.AcceptAll -> allAppUid
                AccessControlMode.AcceptSelected -> selectedUid
                AccessControlMode.RejectAll -> emptySet()
                AccessControlMode.RejectSelected -> {
                    check(packageUidLookup.hasFullPackageAccess) {
                        "App list permission is required to compute all application UIDs"
                    }
                    allAppUid - selectedUid
                }
            }.toList()

        if (store.accessControlMode == AccessControlMode.AcceptAll) {
            check(packageUidLookup.hasFullPackageAccess) {
                "App list permission is required to compute all application UIDs"
            }
        }

        return Result(
            includeUid = includeUid,
            excludeUid = listOf(context.applicationInfo.uid).distinct().sorted(),
            missingPackages = missingPackages,
        )
    }

    private fun resolvePackageUidLookup(selectedPackages: Set<String>): PackageUidLookup {
        return when (store.accessControlMode) {
            AccessControlMode.AcceptAll,
            AccessControlMode.RejectSelected -> resolveFullPackageUidLookup(selectedPackages)

            AccessControlMode.AcceptSelected,
            AccessControlMode.RejectAll ->
                PackageUidLookup(
                    packageUidMap = resolveSelectedPackageUidMap(selectedPackages),
                    hasFullPackageAccess = false,
                )
        }
    }

    private fun resolveFullPackageUidLookup(selectedPackages: Set<String>): PackageUidLookup {
        RootPackageShell.queryPackageUidMap()
            ?.takeIf { it.isNotEmpty() }
            ?.let { packageUidMap ->
                return PackageUidLookup(packageUidMap = packageUidMap, hasFullPackageAccess = true)
            }

        val fallback =
            PackageUidLookup(
                packageUidMap = resolveSelectedPackageUidMap(selectedPackages),
                hasFullPackageAccess = false,
            )

        if (!hasInstalledAppsPermission()) {
            return fallback
        }

        return runCatching {
                PackageUidLookup(
                    packageUidMap = installedPackageUidMap(),
                    hasFullPackageAccess = true,
                )
            }
            .getOrElse { error ->
                if (error is SecurityException) {
                    fallback
                } else {
                    throw error
                }
            }
    }

    private fun resolveSelectedPackageUidMap(packages: Set<String>): Map<String, Int> {
        if (packages.isEmpty()) return emptyMap()

        val resolved = linkedMapOf<String, Int>()
        packages.forEach { packageName ->
            resolvePackageUid(packageName)?.let { uid -> resolved[packageName] = uid }
        }

        val unresolved = packages - resolved.keys
        if (unresolved.isNotEmpty()) {
            RootPackageShell.queryPackageUidMap(unresolved)?.forEach { (packageName, uid) ->
                resolved[packageName] = uid
            }
        }

        return resolved
    }

    private fun resolvePackageUid(packageName: String): Int? {
        val packageInfo = runCatching { queryPackageInfo(packageName) }.getOrNull() ?: return null
        return packageInfo.applicationInfo?.uid
    }

    private fun queryPackageInfo(packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun installedPackageUidMap(): Map<String, Int> {
        return installedPackages()
            .asSequence()
            .mapNotNull { packageInfo ->
                packageInfo.applicationInfo?.uid?.let { uid -> packageInfo.packageName to uid }
            }
            .toMap()
    }

    private fun hasInstalledAppsPermission(): Boolean {
        val permission = MIUI_GET_INSTALLED_APPS_PERMISSION
        val permissionExists =
            runCatching {
                    context.packageManager.getPermissionInfo(permission, 0)
                    true
                }
                .getOrDefault(false)

        if (!permissionExists) {
            return true
        }

        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun installedPackages(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") context.packageManager.getInstalledPackages(0)
        }
    }

    private companion object {
        const val APP_UID_MIN = 10_000
        const val MIUI_GET_INSTALLED_APPS_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"
    }
}
