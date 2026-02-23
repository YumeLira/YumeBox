package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.SettingViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureSubStoreViewModelModule = module {
    viewModel { SettingViewModel(get()) }
    viewModel { FeatureViewModel(get(), androidApplication()) }
}

val featureSubStoreModules: List<Module> = listOf(
    featureSubStoreViewModelModule,
)
