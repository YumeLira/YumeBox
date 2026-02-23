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

package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.data.repository.LogRecordGateway
import com.github.yumelira.yumebox.di.featureProxyModules
import com.github.yumelira.yumebox.di.featureSubStoreModules
import com.github.yumelira.yumebox.viewmodel.*
import com.github.yumelira.yumebox.service.LogRecordServiceGateway
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appIntegrationModule = module {
    single<LogRecordGateway> { LogRecordServiceGateway() }
}

val appViewModelModule = module {
    viewModel { AppSettingsViewModel(get()) }
    viewModel { HomeViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { ProfilesViewModel(androidApplication(), get(), get()) }
    viewModel { NetworkSettingsViewModel(androidApplication(), get(), get(), get()) }
    viewModel { AccessControlViewModel(androidApplication(), get()) }
    viewModel { TrafficStatisticsViewModel(androidApplication(), get()) }
    viewModel { LogViewModel(get()) }
}

val appModule: List<Module> = coreDiModules + listOf(
    appIntegrationModule,
    appViewModelModule,
) + featureSubStoreModules + featureProxyModules
