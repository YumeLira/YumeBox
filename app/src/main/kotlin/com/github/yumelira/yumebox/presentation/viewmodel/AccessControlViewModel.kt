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

package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AccessControlViewModel(
    application: Application,
    private val storage: NetworkSettingsStorage,
) : AndroidViewModel(application) {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable?,
        val isSystemApp: Boolean,
        val isSelected: Boolean,
        val installTime: Long = 0L,
        val updateTime: Long = 0L,
    )

    enum class SortMode {
        PACKAGE_NAME,
        LABEL,
        INSTALL_TIME,
        UPDATE_TIME;

        val displayName: String
            get() = when (this) {
                PACKAGE_NAME -> "包名"
                LABEL -> "标签"
                INSTALL_TIME -> "安装时间"
                UPDATE_TIME -> "更新时间"
            }
    }

    data class UiState(
        val isLoading: Boolean = true,
        val apps: List<AppInfo> = emptyList(),
        val filteredApps: List<AppInfo> = emptyList(),
        val selectedPackages: Set<String> = emptySet(),
        val searchQuery: String = "",
        val showSystemApps: Boolean = false,
        val sortMode: SortMode = SortMode.LABEL,
        val descending: Boolean = false,
        val selectedFirst: Boolean = true,
        val needsMiuiPermission: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkAndLoad()
    }

    private fun checkAndLoad() {
        val context = getApplication<Application>()
        val permission = "com.android.permission.GET_INSTALLED_APPS"

        val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            loadApps()
        } else {
            val isMiui = runCatching {
                val permissionInfo = context.packageManager.getPermissionInfo(permission, 0)
                permissionInfo.packageName == "com.lbe.security.miui"
            }.getOrElse { false }

            if (isMiui) {
                _uiState.update { it.copy(needsMiuiPermission = true, isLoading = false) }
            } else {
                loadApps()
            }
        }
    }

    fun onPermissionResult() {
        _uiState.update { it.copy(needsMiuiPermission = false) }
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val selectedPackages = storage.accessControlPackages.value
            val apps = withContext(Dispatchers.IO) {
                loadInstalledApps(selectedPackages)
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    apps = apps,
                    selectedPackages = selectedPackages,
                    filteredApps = filterApps(
                        apps,
                        state.searchQuery,
                        state.showSystemApps,
                        state.sortMode,
                        state.descending,
                        state.selectedFirst
                    )
                )
            }
        }
    }

    private fun loadInstalledApps(selectedPackages: Set<String>): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        val selfPackageName = getApplication<Application>().packageName

        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages.filter { it.packageName != selfPackageName }.map { appInfo ->
            val pkgInfo = runCatching { pm.getPackageInfo(appInfo.packageName, 0) }.getOrNull()
            AppInfo(
                packageName = appInfo.packageName,
                label = appInfo.loadLabel(pm).toString(),
                icon = runCatching { appInfo.loadIcon(pm) }.getOrNull(),
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isSelected = selectedPackages.contains(appInfo.packageName),
                installTime = pkgInfo?.firstInstallTime ?: 0L,
                updateTime = pkgInfo?.lastUpdateTime ?: 0L
            )
        }
    }

    private fun filterApps(
        apps: List<AppInfo>,
        query: String,
        showSystemApps: Boolean,
        sortMode: SortMode = SortMode.LABEL,
        descending: Boolean = false,
        selectedFirst: Boolean = true
    ): List<AppInfo> {
        val filtered = apps.filter { app ->
            val matchesQuery = query.isEmpty() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            val matchesSystemFilter = showSystemApps || !app.isSystemApp
            matchesQuery && matchesSystemFilter
        }
        val comparator = when (sortMode) {
            SortMode.PACKAGE_NAME -> compareBy<AppInfo> { it.packageName.lowercase() }
            SortMode.LABEL -> compareBy { it.label.lowercase() }
            SortMode.INSTALL_TIME -> compareBy { it.installTime }
            SortMode.UPDATE_TIME -> compareBy { it.updateTime }
        }
        val sorted = if (descending) filtered.sortedWith(comparator.reversed()) else filtered.sortedWith(comparator)
        return if (selectedFirst) {
            sorted.sortedByDescending { it.isSelected }
        } else {
            sorted
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredApps = filterApps(
                    state.apps,
                    query,
                    state.showSystemApps,
                    state.sortMode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }
    }

    fun onSortModeChange(mode: SortMode) {
        _uiState.update { state ->
            state.copy(
                sortMode = mode,
                filteredApps = filterApps(
                    state.apps,
                    state.searchQuery,
                    state.showSystemApps,
                    mode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }
    }

    fun onDescendingChange(desc: Boolean) {
        _uiState.update { state ->
            state.copy(
                descending = desc,
                filteredApps = filterApps(
                    state.apps,
                    state.searchQuery,
                    state.showSystemApps,
                    state.sortMode,
                    desc,
                    state.selectedFirst
                )
            )
        }
    }

    fun onSelectedFirstChange(selectedFirst: Boolean) {
        _uiState.update { state ->
            state.copy(
                selectedFirst = selectedFirst,
                filteredApps = filterApps(
                    state.apps,
                    state.searchQuery,
                    state.showSystemApps,
                    state.sortMode,
                    state.descending,
                    selectedFirst
                )
            )
        }
    }

    fun onShowSystemAppsChange(show: Boolean) {
        _uiState.update { state ->
            state.copy(
                showSystemApps = show,
                filteredApps = filterApps(
                    state.apps,
                    state.searchQuery,
                    show,
                    state.sortMode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }
    }

    fun onAppSelectionChange(packageName: String, selected: Boolean) {
        _uiState.update { state ->
            val newSelectedPackages = if (selected) {
                state.selectedPackages + packageName
            } else {
                state.selectedPackages - packageName
            }

            val newApps = state.apps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isSelected = selected)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(
                    newApps,
                    state.searchQuery,
                    state.showSystemApps,
                    state.sortMode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }


        val packagesToSave = _uiState.value.selectedPackages
        storage.accessControlPackages.set(packagesToSave)
    }

    fun selectAll() {
        _uiState.update { state ->
            val allPackages = state.filteredApps.map { it.packageName }.toSet()
            val newSelectedPackages = state.selectedPackages + allPackages

            val newApps = state.apps.map { app ->
                if (allPackages.contains(app.packageName)) {
                    app.copy(isSelected = true)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(
                    newApps,
                    state.searchQuery,
                    state.showSystemApps,
                    state.sortMode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }

        storage.accessControlPackages.set(_uiState.value.selectedPackages)
    }

    fun deselectAll() {
        _uiState.update { state ->
            val allPackages = state.filteredApps.map { it.packageName }.toSet()
            val newSelectedPackages = state.selectedPackages - allPackages

            val newApps = state.apps.map { app ->
                if (allPackages.contains(app.packageName)) {
                    app.copy(isSelected = false)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(
                    newApps,
                    state.searchQuery,
                    state.showSystemApps,
                    state.sortMode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }

        storage.accessControlPackages.set(_uiState.value.selectedPackages)
    }

    fun invertSelection() {
        _uiState.update { state ->
            val allPackages = state.filteredApps.map { it.packageName }.toSet()
            val newSelectedPackages = state.selectedPackages.toMutableSet()
            allPackages.forEach { pkg ->
                if (newSelectedPackages.contains(pkg)) newSelectedPackages.remove(pkg) else newSelectedPackages.add(pkg)
            }
            val newApps = state.apps.map { app ->
                if (allPackages.contains(app.packageName)) {
                    app.copy(isSelected = !app.isSelected)
                } else {
                    app
                }
            }
            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(
                    newApps,
                    state.searchQuery,
                    state.showSystemApps,
                    state.sortMode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }
        storage.accessControlPackages.set(_uiState.value.selectedPackages)
    }

    fun exportPackages(): String {
        return _uiState.value.selectedPackages.joinToString("\n")
    }

    fun importPackages(text: String): Int {
        val packages = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        _uiState.update { state ->
            val validPackages = packages.intersect(state.apps.map { it.packageName }.toSet())
            val newSelectedPackages = state.selectedPackages + validPackages

            val newApps = state.apps.map { app ->
                if (validPackages.contains(app.packageName)) {
                    app.copy(isSelected = true)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(
                    newApps,
                    state.searchQuery,
                    state.showSystemApps,
                    state.sortMode,
                    state.descending,
                    state.selectedFirst
                )
            )
        }

        storage.accessControlPackages.set(_uiState.value.selectedPackages)
        return packages.intersect(_uiState.value.apps.map { it.packageName }.toSet()).size
    }
}
