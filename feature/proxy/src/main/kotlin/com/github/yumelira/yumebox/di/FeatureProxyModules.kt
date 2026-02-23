package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.presentation.viewmodel.OverrideViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProvidersViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureProxyViewModelModule = module {
    viewModel { ProxyViewModel(get(), get(), get()) }
    viewModel { ProvidersViewModel(get(), get()) }
    viewModel { OverrideViewModel(get()) }
}

val featureProxyModules = listOf(
    featureProxyViewModelModule,
)
