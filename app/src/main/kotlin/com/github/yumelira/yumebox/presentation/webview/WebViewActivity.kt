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

package com.github.yumelira.yumebox.presentation.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class WebViewActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_INITIAL_URL = "initial_url"

        fun start(
            context: Context,
            initialUrl: String = "file://${context.filesDir}/frontend/index.html",
        ) {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_URL, initialUrl)
            }
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    fun launchFilePicker(
        callback: ValueCallback<Array<Uri>>?,
        mimeTypes: Array<String>
    ) {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        val types = if (mimeTypes.isEmpty() || (mimeTypes.size == 1 && mimeTypes[0].isEmpty())) {
            arrayOf("*/*")
        } else {
            mimeTypes
        }
        fileChooserLauncher.launch(types)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        val initialUrl = intent.getStringExtra(EXTRA_INITIAL_URL) ?: "file://${filesDir}/frontend/index.html"


        setContent {
            WebViewScreen(initialUrl = initialUrl)
        }
    }
}
