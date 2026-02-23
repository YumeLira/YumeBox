package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.data.repository.*
import com.github.yumelira.yumebox.data.store.*
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
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

    single { AppSettingsStorage(get<MMKV>(named("settings"))) }
    single { NetworkSettingsStorage(get(named("network_settings"))) }
    single { ProfileLinksStorage(get(named("profile_links"))) }
    single { FeatureStore(get(named("substore"))) }
    single { ProxyDisplaySettingsStore(get(named("proxy_display"))) }
    single { TrafficStatisticsStore(get(named("traffic_statistics"))) }
}

val appDataRuntimeModule = module {
    single { AppSettingsRepository(get()) }
    single { NetworkSettingsRepository(get()) }
    single { FeatureSettingsRepository(get()) }
    single { ProxyDisplaySettingsRepository(get()) }
    single { ProfileLinksRepository(get()) }
    single { TrafficStatisticsRepository(get()) }
    single { LogRepository(androidApplication(), get()) }
    single { NetworkInfoService() }
    single { ProxyChainResolver() }
    single { OverrideRepository(androidContext()) }
    single { ProvidersRepository(androidContext()) }

    single { com.github.yumelira.yumebox.remote.ServiceClient }
    single { ProxyFacade(androidContext()) }
    single { ProfilesRepository(androidContext()) }
    single { TrafficStatisticsCollector(get(), get()) }
}

val coreDiModules: List<Module> = listOf(
    appFoundationModule,
    appDataRuntimeModule,
)
