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

package com.github.yumelira.yumebox.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.repository.NetworkSettingsRepository
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AccessControlViewModel(
    application: Application,
    private val repository: NetworkSettingsRepository,
) : AndroidViewModel(application) {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val isSystemApp: Boolean,
        val isChinaApp: Boolean,
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
                PACKAGE_NAME -> MLang.AccessControl.SortMode.PackageName
                LABEL -> MLang.AccessControl.SortMode.Label
                INSTALL_TIME -> MLang.AccessControl.SortMode.InstallTime
                UPDATE_TIME -> MLang.AccessControl.SortMode.UpdateTime
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

            val selectedPackages = repository.accessControlPackages.value
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
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isChinaApp = isChinaPackage(appInfo.packageName),
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
        selectedFirst: Boolean = true,
        descending: Boolean = false,
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
                    state.selectedFirst
                )
            )
        }
    }

    fun onDescendingChange(desc: Boolean) {
        _uiState.update { state ->
            state.copy(
                filteredApps = filterApps(
                    apps = state.apps,
                    query = state.searchQuery,
                    showSystemApps = state.showSystemApps,
                    sortMode = state.sortMode,
                    selectedFirst = state.selectedFirst,
                    descending = desc,
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
                    state.selectedFirst
                )
            )
        }


        val packagesToSave = _uiState.value.selectedPackages
        repository.accessControlPackages.set(packagesToSave)
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
                    state.selectedFirst
                )
            )
        }

        repository.accessControlPackages.set(_uiState.value.selectedPackages)
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
                    state.selectedFirst
                )
            )
        }

        repository.accessControlPackages.set(_uiState.value.selectedPackages)
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
                    state.selectedFirst
                )
            )
        }
        repository.accessControlPackages.set(_uiState.value.selectedPackages)
    }

    fun selectChinaAppsInCurrentList(): Int {
        return applyRegionalSelectionInCurrentList(selectChina = true)
    }

    fun selectNonChinaAppsInCurrentList(): Int {
        return applyRegionalSelectionInCurrentList(selectChina = false)
    }

    private fun applyRegionalSelectionInCurrentList(selectChina: Boolean): Int {
        var selectedCount = 0
        _uiState.update { state ->
            val currentPackages = state.filteredApps.map { it.packageName }.toSet()
            val targetPackages = state.filteredApps
                .filter { it.isChinaApp == selectChina }
                .mapTo(mutableSetOf()) { it.packageName }
            selectedCount = targetPackages.size

            val newSelectedPackages = state.selectedPackages
                .minus(currentPackages)
                .plus(targetPackages)

            val newApps = state.apps.map { app ->
                if (app.packageName in currentPackages) {
                    app.copy(isSelected = app.packageName in targetPackages)
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
                    state.selectedFirst
                )
            )
        }
        repository.accessControlPackages.set(_uiState.value.selectedPackages)
        return selectedCount
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
                    state.selectedFirst
                )
            )
        }

        repository.accessControlPackages.set(_uiState.value.selectedPackages)
        return packages.intersect(_uiState.value.apps.map { it.packageName }.toSet()).size
    }

    private fun isChinaPackage(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        skipPrefixList.forEach {
            if (normalized == it || normalized.startsWith("$it.")) {
                return false
            }
        }
        if (normalized.startsWith("cn.") || normalized.contains(".cn.") || normalized.endsWith(".cn")) {
            return true
        }
        return normalized.matches(chinaAppRegex)
    }

    companion object {
        private val skipPrefixList = listOf(
            "com.google",
            "com.android.chrome",
            "com.android.vending",
            "com.microsoft",
            "com.apple",
            "com.zhiliaoapp.musically",
            "com.android.providers.downloads",
        )

        private val chinaAppPrefixList = listOf(
            "com.tencent",
            "com.tencent.mobileqq",
            "com.tencent.mm",
            "com.tencent.qqlive",
            "com.tencent.news",
            "com.tencent.wework",
            "com.tencent.weishi",
            "com.tencent.karaoke",
            "com.tencent.qqmusic",
            "com.alibaba",
            "com.alibaba.android",
            "com.alibaba.wireless",
            "com.alibaba.rimet",
            "com.umeng",
            "com.qihoo",
            "com.ali",
            "com.alipay",
            "com.amap",
            "com.sina",
            "com.weibo",
            "com.sankuai",
            "com.sankuai.meituan",
            "com.sankuai.meituan.takeoutnew",
            "com.dianping",
            "com.jingdong",
            "com.xunmeng",
            "com.xingin",
            "com.zhihu",
            "com.bilibili",
            "com.coolapk",
            "tv.danmaku",
            "com.kuaishou",
            "com.smile.gifmaker",
            "com.ss.android",
            "com.ss.android.ugc",
            "com.ss.android.article",
            "com.qiyi",
            "com.youku",
            "com.youku.phone",
            "com.sohu",
            "com.autonavi",
            "com.sogou",
            "com.sogou.inputmethod",
            "com.iflytek",
            "com.iflytek.inputmethod",
            "com.kingsoft",
            "com.qzone",
            "com.vivo",
            "com.xiaomi",
            "com.huawei",
            "com.taobao",
            "com.taobao.idlefish",
            "com.secneo",
            "s.h.e.l.l",
            "com.stub",
            "com.kiwisec",
            "com.secshell",
            "com.wrapper",
            "cn.securitystack",
            "com.mogosec",
            "com.secoen",
            "com.netease",
            "com.mx",
            "com.qq.e",
            "com.baidu",
            "com.bytedance",
            "com.bugly",
            "com.miui",
            "com.oppo",
            "com.coloros",
            "com.iqoo",
            "com.meizu",
            "com.gionee",
            "com.oplus",
            "andes.oplus",
            "com.unionpay",
        )

        private val chinaAppRegex by lazy {
            ("(" + chinaAppPrefixList.joinToString("|").replace(".", "\\.") + ").*").toRegex()
        }
    }
}
