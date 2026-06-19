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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.presentation.webview.WebViewScreen

class WebViewActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_INITIAL_URL = "initial_url"

        fun intent(context: Context, initialUrl: String): Intent =
            Intent(context, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_INITIAL_URL, initialUrl)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun start(
            context: Context,
            initialUrl: String = "file://${context.filesDir}/frontend/index.html",
        ) {
            context.startActivity(intent(context, initialUrl))
        }
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            filePathCallback?.onReceiveValue(uris.toTypedArray())
            filePathCallback = null
        }

    fun launchFilePicker(callback: ValueCallback<Array<Uri>>?, mimeTypes: Array<String>) {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        val types =
            if (mimeTypes.isEmpty() || (mimeTypes.size == 1 && mimeTypes[0].isEmpty())) {
                arrayOf("*/*")
            } else {
                mimeTypes
            }
        fileChooserLauncher.launch(types)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val useLightNavigationBarIcons = !isDarkMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (useLightNavigationBarIcons)
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                else 0,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            )
        } else {
            @Suppress("DEPRECATION")
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightNavigationBars = useLightNavigationBarIcons
        }

        val initialUrl =
            intent.getStringExtra(EXTRA_INITIAL_URL) ?: "file://${filesDir}/frontend/index.html"

        setContent { WebViewScreen(initialUrl = initialUrl) }
    }
}
