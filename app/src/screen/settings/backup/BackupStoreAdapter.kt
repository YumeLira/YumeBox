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
 */

package com.github.yumeyucca.yumebox.screen.settings.backup

import com.github.yumeyucca.yumebox.data.store.*
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.profile.ProfileStore
import java.util.*

internal class BackupStoreAdapter(
    private val appSettings: AppSettingsStore,
    private val networkSettings: NetworkSettingsStore,
    private val featureSettings: FeatureStore,
    private val proxyDisplaySettings: ProxyDisplaySettingsStore,
    private val profileLinks: ProfileLinksStore,
    private val remoteController: RemoteControllerStore,
    private val proxyFacade: ProxyFacade,
    private val mmkvProvider: MMKVProvider,
) {
    fun collectPayload(): BackupPayload =
        BackupPayload(
            appSettings =
                AppSettingsBackup(
                    themeMode = appSettings.themeMode.value,
                    appLanguage = appSettings.appLanguage.value,
                    colorTheme = appSettings.colorTheme.value,
                    themeAccentColorArgb = appSettings.themeAccentColorArgb.value,
                    invertOnPrimaryColors = appSettings.invertOnPrimaryColors.value,
                    homePreviewGuideShown = appSettings.homePreviewGuideShown.value,
                    automaticRestart = appSettings.automaticRestart.value,
                    autoUpdateCurrentProfileOnStart =
                        appSettings.autoUpdateCurrentProfileOnStart.value,
                    excludeFromRecents = appSettings.excludeFromRecents.value,
                    showTrafficNotification = appSettings.showTrafficNotification.value,
                    bottomBarAutoHide = appSettings.bottomBarAutoHide.value,
                    topBarBlurEnabled = appSettings.topBarBlurEnabled.value,
                    classicHomeEnabled = appSettings.classicHomeEnabled.value,
                    useSystemWallpaper = appSettings.useSystemWallpaper.value,
                    moeWallpaperUri = appSettings.moeWallpaperUri.value,
                    moeWallpaperSourceUri = appSettings.moeWallpaperSourceUri.value,
                    moeWallpaperZoom = appSettings.moeWallpaperZoom.value,
                    moeWallpaperBiasX = appSettings.moeWallpaperBiasX.value,
                    moeWallpaperBiasY = appSettings.moeWallpaperBiasY.value,
                    moeHomeQuote = appSettings.moeHomeQuote.value,
                    moeSidebarExpanded = appSettings.moeSidebarExpanded.value,
                    pageScale = appSettings.pageScale.value,
                    singleNodeTest = appSettings.singleNodeTest.value,
                    customUserAgent = appSettings.customUserAgent.value,
                ),
            networkSettings =
                NetworkSettingsBackup(
                    runMode = networkSettings.runMode.value,
                    bypassPrivateNetwork = networkSettings.bypassPrivateNetwork.value,
                    dnsHijack = networkSettings.dnsHijack.value,
                    allowBypass = networkSettings.allowBypass.value,
                    enableIPv6 = networkSettings.enableIPv6.value,
                    systemProxy = networkSettings.systemProxy.value,
                    tunStack = networkSettings.tunStack.value,
                    tunRouteExcludeAddress = networkSettings.tunRouteExcludeAddress.value,
                    tunIfName = networkSettings.tunIfName.value,
                    tunMtu = networkSettings.tunMtu.value,
                    tunAutoRoute = networkSettings.tunAutoRoute.value,
                    tunStrictRoute = networkSettings.tunStrictRoute.value,
                    tunAutoRedirect = networkSettings.tunAutoRedirect.value,
                    tunIncludeAndroidUser = networkSettings.tunIncludeAndroidUser.value,
                    tunDnsMode = networkSettings.tunDnsMode.value,
                    tunFakeIpRange = networkSettings.tunFakeIpRange.value,
                    tunFakeIpRange6 = networkSettings.tunFakeIpRange6.value,
                    accessControlMode = networkSettings.accessControlMode.value,
                    accessControlPackages = networkSettings.accessControlPackages.value,
                    accessControlSelectedFirst = networkSettings.accessControlSelectedFirst.value,
                    accessControlShowSystemApps = networkSettings.accessControlShowSystemApps.value,
                    accessControlSortMode = networkSettings.accessControlSortMode.value,
                ),
            featureSettings =
                FeatureSettingsBackup(
                    allowLanAccess = featureSettings.allowLanAccess.value,
                    backendPort = featureSettings.backendPort.value,
                    frontendPort = featureSettings.frontendPort.value,
                    selectedPanelType = featureSettings.selectedPanelType.value,
                    panelOpenMode = featureSettings.panelOpenMode.value,
                    showWebControlInProxy = featureSettings.showWebControlInProxy.value,
                    exitUiWhenBackground = featureSettings.exitUiWhenBackground.value,
                    isFirstOpen = featureSettings.isFirstOpen,
                ),
            proxyDisplaySettings =
                ProxyDisplaySettingsBackup(
                    sortMode = proxyDisplaySettings.sortMode.value,
                    displayMode = proxyDisplaySettings.displayMode.value,
                    proxyMode = proxyDisplaySettings.proxyMode.value,
                    sheetHeightFraction = proxyDisplaySettings.sheetHeightFraction.value,
                ),
            profileLinks =
                ProfileLinksBackup(
                    linkOpenMode = profileLinks.linkOpenMode.value,
                    links = profileLinks.links.value,
                    defaultLinkId = profileLinks.defaultLinkId.value,
                ),
            remoteController =
                RemoteControllerBackup(
                    controllerEnabled = remoteController.controllerEnabled.value,
                    backends = remoteController.backends.value,
                    activeBackendId = remoteController.activeBackendId.value,
                ),
            profiles =
                ProfilesBackup(
                    imported = ProfileStore.loadImported().map(ImportedBackup::from),
                    profileOrder = ProfileStore.loadProfileOrder().map(UUID::toString),
                ),
            service = ServiceBackup(activeProfile = ServiceStore().activeProfile?.toString()),
        )

    fun clearAndApply(payload: BackupPayload) {
        clearConfigurationStores()
        applyAppSettings(payload.appSettings)
        applyNetworkSettings(payload.networkSettings)
        applyFeatureSettings(payload.featureSettings)
        applyProxyDisplaySettings(payload.proxyDisplaySettings)
        applyProfileLinks(payload.profileLinks)
        applyRemoteController(payload.remoteController)
        applyProfiles(payload.profiles)
        ServiceStore().activeProfile = payload.service.activeProfile?.let(UUID::fromString)
    }

    private fun clearConfigurationStores() {
        listOf(
            "settings",
            "network_settings",
            "substore",
            "proxy_display",
            "profile_links",
            "remote_controller",
            "profiles",
            "service",
            "override_bindings",
        )
            .forEach { id -> mmkvProvider.getMMKV(id).clearAll() }
        refreshPreferenceCachesAfterRawClear()
    }

    private fun refreshPreferenceCachesAfterRawClear() {
        refreshAfterRawStoreClear(
            appSettings.themeMode,
            appSettings.appLanguage,
            appSettings.colorTheme,
            appSettings.themeAccentColorArgb,
            appSettings.invertOnPrimaryColors,
            appSettings.homePreviewGuideShown,
            appSettings.automaticRestart,
            appSettings.autoUpdateCurrentProfileOnStart,
            appSettings.excludeFromRecents,
            appSettings.showTrafficNotification,
            appSettings.bottomBarAutoHide,
            appSettings.topBarBlurEnabled,
            appSettings.classicHomeEnabled,
            appSettings.useSystemWallpaper,
            appSettings.moeWallpaperUri,
            appSettings.moeWallpaperSourceUri,
            appSettings.moeWallpaperZoom,
            appSettings.moeWallpaperBiasX,
            appSettings.moeWallpaperBiasY,
            appSettings.moeHomeQuote,
            appSettings.moeSidebarExpanded,
            appSettings.pageScale,
            appSettings.singleNodeTest,
            appSettings.customUserAgent,
        )
        refreshAfterRawStoreClear(
            networkSettings.runMode,
            networkSettings.bypassPrivateNetwork,
            networkSettings.dnsHijack,
            networkSettings.allowBypass,
            networkSettings.enableIPv6,
            networkSettings.systemProxy,
            networkSettings.tunStack,
            networkSettings.tunRouteExcludeAddress,
            networkSettings.tunIfName,
            networkSettings.tunMtu,
            networkSettings.tunAutoRoute,
            networkSettings.tunStrictRoute,
            networkSettings.tunAutoRedirect,
            networkSettings.tunIncludeAndroidUser,
            networkSettings.tunDnsMode,
            networkSettings.tunFakeIpRange,
            networkSettings.tunFakeIpRange6,
            networkSettings.accessControlMode,
            networkSettings.accessControlPackages,
            networkSettings.accessControlSelectedFirst,
            networkSettings.accessControlShowSystemApps,
            networkSettings.accessControlSortMode,
        )
        refreshAfterRawStoreClear(
            featureSettings.allowLanAccess,
            featureSettings.backendPort,
            featureSettings.frontendPort,
            featureSettings.selectedPanelType,
            featureSettings.panelOpenMode,
            featureSettings.showWebControlInProxy,
            featureSettings.exitUiWhenBackground,
        )
        refreshAfterRawStoreClear(
            proxyDisplaySettings.sortMode,
            proxyDisplaySettings.displayMode,
            proxyDisplaySettings.proxyMode,
            proxyDisplaySettings.sheetHeightFraction,
        )
        refreshAfterRawStoreClear(
            profileLinks.linkOpenMode,
            profileLinks.links,
            profileLinks.defaultLinkId,
        )
        refreshAfterRawStoreClear(
            remoteController.controllerEnabled,
            remoteController.backends,
            remoteController.activeBackendId,
        )
    }

    private fun applyAppSettings(value: AppSettingsBackup) {
        appSettings.themeMode.set(value.themeMode)
        appSettings.appLanguage.set(value.appLanguage)
        appSettings.colorTheme.set(value.colorTheme)
        appSettings.themeAccentColorArgb.set(value.themeAccentColorArgb)
        appSettings.invertOnPrimaryColors.set(value.invertOnPrimaryColors)
        appSettings.homePreviewGuideShown.set(value.homePreviewGuideShown)
        appSettings.automaticRestart.set(value.automaticRestart)
        appSettings.autoUpdateCurrentProfileOnStart.set(value.autoUpdateCurrentProfileOnStart)
        appSettings.excludeFromRecents.set(value.excludeFromRecents)
        appSettings.showTrafficNotification.set(value.showTrafficNotification)
        appSettings.bottomBarAutoHide.set(value.bottomBarAutoHide)
        appSettings.topBarBlurEnabled.set(value.topBarBlurEnabled)
        appSettings.classicHomeEnabled.set(value.classicHomeEnabled)
        appSettings.useSystemWallpaper.set(value.useSystemWallpaper)
        appSettings.moeWallpaperUri.set(value.moeWallpaperUri)
        appSettings.moeWallpaperSourceUri.set(value.moeWallpaperSourceUri)
        appSettings.moeWallpaperZoom.set(value.moeWallpaperZoom)
        appSettings.moeWallpaperBiasX.set(value.moeWallpaperBiasX)
        appSettings.moeWallpaperBiasY.set(value.moeWallpaperBiasY)
        appSettings.moeHomeQuote.set(value.moeHomeQuote)
        appSettings.moeSidebarExpanded.set(value.moeSidebarExpanded)
        appSettings.pageScale.set(value.pageScale)
        appSettings.singleNodeTest.set(value.singleNodeTest)
        appSettings.customUserAgent.set(value.customUserAgent)
    }

    private fun applyNetworkSettings(value: NetworkSettingsBackup) {
        networkSettings.runMode.set(value.runMode)
        networkSettings.bypassPrivateNetwork.set(value.bypassPrivateNetwork)
        networkSettings.dnsHijack.set(value.dnsHijack)
        networkSettings.allowBypass.set(value.allowBypass)
        networkSettings.enableIPv6.set(value.enableIPv6)
        networkSettings.systemProxy.set(value.systemProxy)
        networkSettings.tunStack.set(value.tunStack)
        networkSettings.tunRouteExcludeAddress.set(value.tunRouteExcludeAddress)
        networkSettings.tunIfName.set(value.tunIfName)
        networkSettings.tunMtu.set(value.tunMtu)
        networkSettings.tunAutoRoute.set(value.tunAutoRoute)
        networkSettings.tunStrictRoute.set(value.tunStrictRoute)
        networkSettings.tunAutoRedirect.set(value.tunAutoRedirect)
        networkSettings.tunIncludeAndroidUser.set(value.tunIncludeAndroidUser)
        networkSettings.tunDnsMode.set(value.tunDnsMode)
        networkSettings.tunFakeIpRange.set(value.tunFakeIpRange)
        networkSettings.tunFakeIpRange6.set(value.tunFakeIpRange6)
        networkSettings.accessControlMode.set(value.accessControlMode)
        networkSettings.accessControlPackages.set(value.accessControlPackages)
        networkSettings.accessControlSelectedFirst.set(value.accessControlSelectedFirst)
        networkSettings.accessControlShowSystemApps.set(value.accessControlShowSystemApps)
        networkSettings.accessControlSortMode.set(value.accessControlSortMode)
    }

    private fun applyFeatureSettings(value: FeatureSettingsBackup) {
        featureSettings.allowLanAccess.set(value.allowLanAccess)
        featureSettings.backendPort.set(value.backendPort)
        featureSettings.frontendPort.set(value.frontendPort)
        featureSettings.selectedPanelType.set(value.selectedPanelType)
        featureSettings.panelOpenMode.set(value.panelOpenMode)
        featureSettings.showWebControlInProxy.set(value.showWebControlInProxy)
        featureSettings.exitUiWhenBackground.set(value.exitUiWhenBackground)
        featureSettings.isFirstOpen = value.isFirstOpen
    }

    private fun applyProxyDisplaySettings(value: ProxyDisplaySettingsBackup) {
        proxyDisplaySettings.sortMode.set(value.sortMode)
        proxyDisplaySettings.displayMode.set(value.displayMode)
        proxyDisplaySettings.proxyMode.set(value.proxyMode)
        proxyDisplaySettings.sheetHeightFraction.set(value.sheetHeightFraction)
    }

    private fun applyProfileLinks(value: ProfileLinksBackup) {
        profileLinks.linkOpenMode.set(value.linkOpenMode)
        profileLinks.links.set(value.links)
        profileLinks.defaultLinkId.set(value.defaultLinkId)
    }

    private fun applyRemoteController(value: RemoteControllerBackup) {
        remoteController.backends.set(value.backends)
        remoteController.activeBackendId.set(value.activeBackendId)
        remoteController.controllerEnabled.set(value.controllerEnabled)
        proxyFacade.applyRemoteControllerState()
    }

    private fun applyProfiles(value: ProfilesBackup) {
        ProfileStore.saveImported(value.imported.map(ImportedBackup::toImported))
        ProfileStore.saveProfileOrder(value.profileOrder.map(UUID::fromString))
    }
}

internal fun refreshAfterRawStoreClear(vararg preferences: Preference<*>) {
    preferences.forEach { it.refresh() }
}
