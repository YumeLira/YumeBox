package com.github.yumeyucca.yumebox.screen.moe

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.presentation.component.PreferenceSwitchItem
import tf.gal.yumebox.locale.YumeTxt

@Composable
internal fun SystemWallpaperPreferenceItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    var waitingForAccess by rememberSaveable { mutableStateOf(false) }
    val readPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            waitingForAccess = false
            if (granted && SystemWallpaperAccess.isGranted(context)) {
                currentOnCheckedChange(true)
            }
        }

    DisposableEffect(lifecycleOwner, waitingForAccess) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                waitingForAccess &&
                SystemWallpaperAccess.isGranted(context)
            ) {
                waitingForAccess = false
                currentOnCheckedChange(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PreferenceSwitchItem(
        title = YumeTxt.AppSettings.Interface.SystemWallpaperTitle,
        checked = checked,
        onCheckedChange = { enabled ->
            if (!enabled) {
                onCheckedChange(false)
            } else if (SystemWallpaperAccess.isGranted(context)) {
                onCheckedChange(true)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                waitingForAccess = true
                context.startActivity(SystemWallpaperAccess.settingsIntent(context))
            } else {
                waitingForAccess = true
                readPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        },
    )
}
