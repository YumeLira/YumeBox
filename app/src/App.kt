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

package com.github.yumelira.yumebox

import android.app.Application
import android.content.res.Configuration
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.common.util.PlatformIdentifier
import com.github.yumelira.yumebox.common.util.PredictiveBackCompat
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.core.util.StartupTaskCoordinator
import com.github.yumelira.yumebox.core.util.runtimeHomeDir
import com.github.yumelira.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.di.appModule
import com.github.yumelira.yumebox.feature.meta.presentation.util.CustomRoutingBootstrapper
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.screen.settings.AcgWallpaperImporter
import com.github.yumelira.yumebox.substore.util.AppUtil
import com.tencent.mmkv.MMKV
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.tukaani.xz.XZInputStream
import timber.log.Timber

class App : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        Global.init(this)
        MMKV.initialize(this)

        val koinApp = startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        val appSettingsStorage: AppSettingsStore = koinApp.koin.get()
        AppLanguageManager.apply(appSettingsStorage.appLanguage.value)
        PredictiveBackCompat.apply(applicationInfo, appSettingsStorage.predictiveBackEnabled.value)

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
        for (name in listOf("geoip.metadb", "geosite.dat", "ASN.mmdb")) {
            val target = File(dir, name)
            if (!target.exists()) {
                extractXzAsset("$name.xz", target) ?: copyAsset(name, target)
            }
        }
    }

    private fun copyAsset(name: String, target: File) {
        assets.open(name).use { it.copyTo(target.outputStream()) }
    }

    /**
     * Best-effort self-heal for the ACG wallpaper: if the persisted [AppSettingsStore.acgWallpaperUri]
     * points at a local file:// copy that no longer exists, but the original source is still recorded
     * and readable, re-import it so the home screen keeps rendering the user's choice after a cache or
     * data wipe. Silent no-op otherwise; the render path falls back to the bundled asset.
     */
    private suspend fun ensureAcgWallpaperLocalCopy(appSettings: AppSettingsStore) {
        val stored = appSettings.acgWallpaperUri.value
        if (!stored.startsWith("file://")) return
        val localPath = stored.removePrefix("file://")
        if (localPath.startsWith("/android_asset/")) return
        if (File(localPath).exists()) return
        val source = appSettings.acgWallpaperSourceUri.value
        if (source.isBlank()) return
        val reimported = AcgWallpaperImporter.importToLocal(this, source)
        if (reimported != null) {
            appSettings.acgWallpaperUri.set(reimported)
        }
    }

    private fun extractXzAsset(assetName: String, target: File): Unit? =
        runCatching {
                assets.open(assetName).use { input ->
                    XZInputStream(input.buffered()).use { xz ->
                        target.outputStream().buffered().use { xz.copyTo(it) }
                    }
                }
                Unit
            }
            .getOrNull()

    private fun scheduleDeferredStartupTasks(
        koin: Koin,
        featureStore: FeatureStore,
        appSettings: AppSettingsStore,
    ) {
        StartupTaskCoordinator.startRuntimeWarmup(startupScope) {
            runCatching { koin.get<CustomRoutingBootstrapper>().ensureDefaultContent() }
                .onFailure { Timber.e(it, "Failed to bootstrap custom routing default content") }
            runCatching { ensureAcgWallpaperLocalCopy(appSettings) }
                .onFailure { Timber.e(it, "Failed to ensure ACG wallpaper local copy") }
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
