package com.github.yumelira.yumebox.screen

import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.presentation.screen.ProvidersContent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
@Destination<RootGraph>
fun ProvidersScreen(navigator: DestinationsNavigator) {
    ProvidersContent(navigator = navigator)
}
