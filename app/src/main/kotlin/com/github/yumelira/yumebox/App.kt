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

package com.github.yumelira.yumebox

import android.app.Application
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.PlatformIdentifier
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.data.repository.TrafficStatisticsCollector
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.di.appModule
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.substore.util.AppUtil
import com.github.yumelira.yumebox.update.EmasUpdateManager
import com.tencent.mmkv.MMKV
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.tukaani.xz.XZInputStream
import timber.log.Timber
import java.io.File
import java.io.IOException

class App : Application() {

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

        StartupGate.verify(this)

        Global.init(this)
        MMKV.initialize(this)

        val koinApp = startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        extractGeoFiles()

        val featureStore: FeatureStore = koinApp.koin.get()
        koinApp.koin.get<TrafficStatisticsCollector>()
        koinApp.koin.get<ProxyFacade>().warmUpProxyGroups()

        if (featureStore.isFirstTimeOpen()) {
            AppUtil.initFirstOpen()
            featureStore.markFirstOpenHandled()
        }

        PlatformIdentifier.getPlatformIdentifier()

        EmasUpdateManager.init(
            application = this,
            appKey = BuildConfig.EMAS_APP_KEY,
            appSecret = BuildConfig.EMAS_APP_SECRET,
            channelId = BuildConfig.EMAS_CHANNEL_ID,
            enableCustomDialog = true,
        )
    }

    private fun extractGeoFiles() {
        val clashDir = File(filesDir, "clash").apply { mkdirs() }
        val geoFiles = listOf("geoip.metadb", "geosite.dat", "ASN.mmdb")
        val failedFiles = mutableListOf<String>()

        geoFiles.forEach { filename ->
            val targetFile = File(clashDir, filename)
            if (!targetFile.exists()) {
                try {
                    if (!extractCompressedAssetIfExists("$filename.xz", targetFile)) {
                        assets.open(filename).use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } catch (_: IOException) {
                    failedFiles += filename
                }
            }
        }

        if (failedFiles.isNotEmpty()) {
            Timber.w("Failed to extract geo files: ${failedFiles.joinToString()}")
        }
    }

    private fun extractCompressedAssetIfExists(assetName: String, targetFile: File): Boolean {
        return try {
            assets.open(assetName).use { input ->
                XZInputStream(input.buffered()).use { xzInput ->
                    targetFile.outputStream().buffered().use { output ->
                        xzInput.copyTo(output)
                    }
                }
            }
            true
        } catch (_: IOException) {
            false
        }
    }
}
