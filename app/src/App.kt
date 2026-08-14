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

package com.github.yumeyucca.yumebox

import android.app.Application
import android.content.ComponentName
import android.content.res.Configuration
import com.github.yumeyucca.yumebox.common.util.AppLanguageManager
import com.github.yumeyucca.yumebox.common.util.PlatformIdentifier
import com.github.yumeyucca.yumebox.core.Global
import com.github.yumeyucca.yumebox.core.util.AppVisibilityTracker
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.di.appModule
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.CustomRoutingBootstrapper
import com.github.yumeyucca.yumebox.runtime.api.Components
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.screen.settings.MoeWallpaperImporter
import com.github.yumeyucca.yumebox.substore.util.AppUtil
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.tukaani.xz.XZInputStream
import timber.log.Timber
import java.io.File

class App : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        AppVisibilityTracker.register(this)
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        Global.init(this)
        MMKV.initialize(this)

        // Runtime notifications/tiles jump to these activities; the runtime layer must not
        // name app classes, so the entry points are injected here.
        Components.MAIN_ACTIVITY = ComponentName(this, MainActivity::class.java)
        Components.PROXY_SHEET_ACTIVITY = ComponentName(this, ProxySheetActivity::class.java)

        val koinApp = startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        val appSettingsStorage: AppSettingsStore = koinApp.koin.get()
        AppLanguageManager.apply(appSettingsStorage.appLanguage.value)

        // Keep first-run geo extraction ahead of the UI so starting the proxy never inherits it.
        extractGeoFiles()
        val featureStore: FeatureStore = koinApp.koin.get()
        featureStore.syncAppVersion(BuildConfig.VERSION_CODE)
        scheduleDeferredStartupTasks(koinApp.koin, featureStore, appSettingsStorage)

        PlatformIdentifier.getPlatformIdentifier()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguageManager.refreshSystemLanguage()
    }

    private fun extractGeoFiles() {
        val dir = runtimeHomeDir.apply { mkdirs() }
        for (name in listOf("geoip.metadb", "geosite.dat", "ASN.mmdb", "BundleMRS.7z")) {
            val target = File(dir, name)
            if (!target.exists()) {
                extractXzAsset("$name.xz", target) ?: copyAssetIfExists(name, target)
            }
        }
    }

    /**
     * External-geo builds omit these runtime assets. Remove partial writes so the startup guard
     * can require a Builtin APK to install a complete Geo data set.
     */
    private fun copyAssetIfExists(name: String, target: File) {
        runCatching {
            assets.open(name).use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }
            .onFailure {
                target.delete()
                Timber.i("Runtime asset %s not bundled; a Builtin APK must install it", name)
            }
    }

    /**
     * Best-effort self-heal for the Moe wallpaper: if the persisted
     * [AppSettingsStore.moeWallpaperUri] points at a local file:// copy that no longer exists, but
     * the original source is still recorded and readable, re-import it so the home screen keeps
     * rendering the user's choice after a cache or data wipe. Silent no-op otherwise; the render
     * path falls back to the bundled asset.
     */
    private suspend fun ensureMoeWallpaperLocalCopy(appSettings: AppSettingsStore) {
        val stored = appSettings.moeWallpaperUri.value
        if (!stored.startsWith("file://")) return
        val localPath = stored.removePrefix("file://")
        if (localPath.startsWith("/android_asset/")) return
        if (File(localPath).exists()) return
        val source = appSettings.moeWallpaperSourceUri.value
        if (source.isBlank()) return
        val reimported = MoeWallpaperImporter.importToLocal(this, source)
        if (reimported != null) {
            appSettings.moeWallpaperUri.set(reimported)
        }
    }

    private fun extractXzAsset(assetName: String, target: File): Unit? = runCatching {
        assets.open(assetName).use { input ->
            XZInputStream(input.buffered()).use { xz ->
                target.outputStream().buffered().use { xz.copyTo(it) }
            }
        }
        Unit
    }
        .onFailure { target.delete() }
        .getOrNull()

    private fun scheduleDeferredStartupTasks(
        koin: Koin,
        featureStore: FeatureStore,
        appSettings: AppSettingsStore,
    ) {
        startupScope.launch {
            runCatching { koin.get<CustomRoutingBootstrapper>().ensureDefaultContent() }
                .onFailure { Timber.e(it, "Failed to bootstrap custom routing default content") }
            runCatching { ensureMoeWallpaperLocalCopy(appSettings) }
                .onFailure { Timber.e(it, "Failed to ensure Moe wallpaper local copy") }
            runCatching { koin.get<AppTrafficStatisticsCollector>() }
            runCatching { koin.get<ProxyFacade>().awaitProxyGroupWarmUp() }

            if (featureStore.isFirstTimeOpen()) {
                withContext(Dispatchers.IO) {
                    AppUtil.initFirstOpen()
                    featureStore.markFirstOpenHandled()
                }
            }
        }
    }
}
