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



package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.data.controller.AccessControlController
import com.github.yumelira.yumebox.data.controller.AppSettingsController
import com.github.yumelira.yumebox.data.controller.NetworkSettingsController
import com.github.yumelira.yumebox.data.controller.RuntimeOverrideController
import com.github.yumelira.yumebox.data.controller.ActiveProfileOverrideReloader
import com.github.yumelira.yumebox.data.controller.AppIdentityResolver
import com.github.yumelira.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumelira.yumebox.data.store.LogStore
import com.github.yumelira.yumebox.data.gateway.LogRecordGateway
import com.github.yumelira.yumebox.data.gateway.NetworkInfoService
import com.github.yumelira.yumebox.data.store.OverrideConfigProvider
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import com.github.yumelira.yumebox.data.controller.OverrideResolver
import com.github.yumelira.yumebox.data.controller.OverrideService
import com.github.yumelira.yumebox.data.store.ProfileBindingProvider
import com.github.yumelira.yumebox.data.store.ProfileBindingStore
import com.github.yumelira.yumebox.data.controller.ProvidersController
import com.github.yumelira.yumebox.data.store.AppSettingsStore
import com.github.yumelira.yumebox.data.store.FeatureStore
import com.github.yumelira.yumebox.data.store.MMKVProvider
import com.github.yumelira.yumebox.data.store.NetworkSettingsStore
import com.github.yumelira.yumebox.data.store.ProfileLinksStore
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumelira.yumebox.runtime.client.root.RootTunReloadScheduler
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val APPLICATION_SCOPE_NAME = "applicationScope"

val appFoundationModule = module {
    single<CoroutineScope>(named(APPLICATION_SCOPE_NAME)) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single { MMKVProvider() }
    single<MMKV>(named("profiles")) { get<MMKVProvider>().getMMKV("profiles") }
    single<MMKV>(named("settings")) { get<MMKVProvider>().getMMKV("settings") }
    single<MMKV>(named("network_settings")) { get<MMKVProvider>().getMMKV("network_settings") }
    single<MMKV>(named("substore")) { get<MMKVProvider>().getMMKV("substore") }
    single<MMKV>(named("proxy_display")) { get<MMKVProvider>().getMMKV("proxy_display") }
    single<MMKV>(named("traffic_statistics")) { get<MMKVProvider>().getMMKV("traffic_statistics") }
    single<MMKV>(named("profile_links")) { get<MMKVProvider>().getMMKV("profile_links") }
    single<MMKV>(named("service_cache")) { get<MMKVProvider>().getMMKV("service_cache") }
    single<MMKV>(named("override_bindings")) { get<MMKVProvider>().getMMKV("override_bindings") }

    single { AppSettingsStore(get<MMKV>(named("settings"))) }
    single { NetworkSettingsStore(get(named("network_settings"))) }
    single { ProfileLinksStore(get(named("profile_links"))) }
    single { FeatureStore(get(named("substore"))) }
    single { ProxyDisplaySettingsStore(get(named("proxy_display"))) }
    single { TrafficStatisticsStore(get(named("traffic_statistics"))) }
}

val appDataRuntimeModule = module {
    single { AppSettingsController(get(), applyLanguage = AppLanguageManager::apply) }
    single {
        val proxyFacade = get<ProxyFacade>()
        NetworkSettingsController(
            store = get(),
            isRunning = { RuntimeStateMapper.isActuallyRunning(proxyFacade.runtimeSnapshot.value) },
            restartProxy = { mode -> proxyFacade.startProxy(mode) },
        )
    }
    single {
        val proxyFacade = get<ProxyFacade>()
        AccessControlController(
            store = get(),
            isRunning = { proxyFacade.isRunning.value },
            resolveActiveMode = { RuntimeStateMapper.modeForOwner(proxyFacade.runtimeSnapshot.value.owner) },
            restartProxy = { mode -> proxyFacade.startProxy(mode) },
        )
    }
    single { LogStore(androidApplication(), get()) }
    single { NetworkInfoService() }
    single {
        val profilesRepository = get<ProfilesRepository>()
        RuntimeOverrideController(
            configStore = get(),
            queryActiveProfile = { profilesRepository.queryActiveProfile() },
        )
    }
    single {
        val appContext = androidContext()
        ProvidersController(appContext) {
            com.github.yumelira.yumebox.remote.ServiceClient.connect(appContext)
            com.github.yumelira.yumebox.remote.ServiceClient.clash().queryProviders()
        }
    }

    single { ProfileBindingStore(androidContext()) }
    single<ProfileBindingProvider> { get<ProfileBindingStore>() }

    single { OverrideConfigStore(androidContext(), get()) }
    single<OverrideConfigProvider> { get<OverrideConfigStore>() }

    single { OverrideResolver(get(), get()) }
    single {
        val appContext = androidContext()
        OverrideService(appContext, get()) {
            RootTunReloadScheduler.schedule(
                appContext,
                RootTunReloadScheduler.Reason.PROFILE_OVERRIDE_CHANGED,
            )
        }
    }
    single {
        val profilesRepository = get<ProfilesRepository>()
        ActiveProfileOverrideReloader(
            queryActiveProfile = { profilesRepository.queryActiveProfile() },
            bindingProvider = get(),
            overrideService = get(),
        )
    }

    single { ProxyFacade(androidContext()) }
    single { AppIdentityResolver(androidContext()) }
    single { ProfilesRepository(androidContext()) }
    single {
        val proxyFacade = get<ProxyFacade>()
        AppTrafficStatisticsCollector(
            isRunningFlow = proxyFacade.isRunning,
            currentProfileId = { proxyFacade.currentProfile.value?.uuid?.toString() },
            trafficStatisticsStore = get(),
            appIdentityResolver = get(),
            queryTrafficTotal = {
                TrafficData.from(proxyFacade.queryTrafficTotal())
            },
            queryConnections = {
                proxyFacade.queryConnections()
            },
            queryActiveProfileId = {
                proxyFacade.refreshCurrentProfile()
                proxyFacade.currentProfile.value?.uuid?.toString()
            },
        )
    }
}

val coreDiModules: List<Module> = listOf(
    appFoundationModule,
    appDataRuntimeModule,
)
