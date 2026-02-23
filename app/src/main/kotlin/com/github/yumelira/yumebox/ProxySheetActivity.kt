package com.github.yumelira.yumebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import org.koin.androidx.compose.koinViewModel
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.viewmodel.AppSettingsViewModel

class ProxySheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value
            val themeSeedColorArgb = appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    colorTheme = colorTheme,
                    themeSeedColorArgb = themeSeedColorArgb,
                ) {
                    ProxySheetContent(
                        onDismiss = {
                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        },
                    )
                }
            }
        }
    }
}
