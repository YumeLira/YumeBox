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

package com.github.yumelira.yumebox.presentation.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import com.github.yumelira.yumebox.presentation.viewmodel.HomeViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProfilesViewModel
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SpinnerEntry
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperSpinner
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.New
import top.yukonga.miuix.kmp.icon.icons.useful.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.github.yumelira.yumebox.MainActivity

@Composable
fun ProfilesPager(mainInnerPadding: PaddingValues) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()

    val showAddBottomSheet = remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Profile?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var pendingProfileId by remember { mutableStateOf<String?>(null) }
    var profileToEdit by remember { mutableStateOf<Profile?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }

    var importUrlFromScheme by remember { mutableStateOf<String?>(null) }
    val pendingImportUrl by MainActivity.pendingImportUrl.collectAsState()

    var scannedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingImportUrl) {
        if (pendingImportUrl != null) {
            importUrlFromScheme = pendingImportUrl
            profileToEdit = null
            showAddBottomSheet.value = true
            MainActivity.clearPendingImportUrl()
        }
    }

    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingProfileId?.let { profileId ->
                homeViewModel.startProxy(profileId, useTunMode = true)
            }
        }
        pendingProfileId = null
    }

    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.ProfilesPage.Title, scrollBehavior = scrollBehavior, actions = {
                    IconButton(
                        onClick = {
                            if (!isDownloading) {
                                isDownloading = true
                                scope.launch {
                                    profiles.filter { it.type == ProfileType.URL }.forEach { p ->
                                        try {
                                            profilesViewModel.downloadProfile(p)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    isDownloading = false
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            MiuixIcons.Useful.Refresh,
                            contentDescription = MLang.ProfilesPage.Action.UpdateAll
                        )
                    }

                    IconButton(
                        onClick = {
                            profileToEdit = null
                            showAddBottomSheet.value = true
                        },
                        modifier = Modifier.padding(end = LocalSpacing.current.xxl)
                    ) {
                        Icon(
                            MiuixIcons.Useful.New,
                            contentDescription = MLang.ProfilesPage.Action.AddProfile
                        )
                    }
                })
        },
    ) { innerPadding ->
        if (profiles.isEmpty()) {

            CenteredText(
                firstLine = MLang.ProfilesPage.Empty.NoProfiles,
                secondLine = MLang.ProfilesPage.Empty.Hint
            )
        } else {

            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
                topPadding = 20.dp,
                enableBottomBarAutoHide = true,
            ) {
                items(profiles.size) { index ->
                    val profile = profiles[index]


                    ProfileCard(
                        profile = profile,
                        isDownloading = isDownloading,
                        onExport = { profile ->
                            if (!isDownloading) {
                                val context = com.github.yumelira.yumebox.App.instance
                                val importedDir = File(context.filesDir, "imported")
                                val profileDir = File(importedDir, profile.id)
                                val file = File(profileDir, "config.yaml")
                                if (file.exists()) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                MLang.ProfilesPage.Action.ExportConfig
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            })
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        onUpdate = { profile ->
                            if (!isDownloading) {
                                isDownloading = true
                                scope.launch {
                                    profilesViewModel.downloadProfile(profile)
                                    isDownloading = false
                                }
                            }
                        },
                        onDelete = { profile -> if (!isDownloading) showDeleteDialog = profile },
                        onEdit = { profile ->
                            if (!isDownloading) {
                                profileToEdit = profile
                                editName = profile.name
                                showEditDialog = true
                            }
                        },
                        onToggleEnabled = { updatedProfile ->
                            if (!isDownloading) {
                                scope.launch {
                                    profilesViewModel.toggleProfileEnabled(
                                        profile = updatedProfile,
                                        enabled = updatedProfile.enabled,
                                        onProfileEnabled = { enabledProfile ->
                                            if (isRunning) {
                                                scope.launch {
                                                    homeViewModel.reloadProfile(enabledProfile.id)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    LocalContext.current
    AddProfileSheet(
        show = showAddBottomSheet,
        profileToEdit = profileToEdit,
        importUrl = importUrlFromScheme ?: scannedUrl,
        onAddProfile = { profile ->

            profilesViewModel.addProfile(profile)
        },
        onUpdateProfile = { profile ->
            profilesViewModel.updateProfile(profile)
        },
        onDownloadComplete = {
            showAddBottomSheet.value = false
            profilesViewModel.clearDownloadProgress()
            importUrlFromScheme = null
            scannedUrl = null
        },
        profilesViewModel = profilesViewModel
    )

    showDeleteDialog?.let { profile ->
        DeleteConfirmDialog(profileName = profile.name, onConfirm = {
            profilesViewModel.removeProfile(profile.id)
            showDeleteDialog = null
        }, onDismiss = { showDeleteDialog = null })
    }

    val currentProfileToEdit = profileToEdit
    if (showEditDialog && currentProfileToEdit != null) {
        EditProfileNameDialog(
            show = remember { mutableStateOf(true) },
            currentName = currentProfileToEdit.name,
            onDismiss = {
                showEditDialog = false
                profileToEdit = null
            },
            onConfirm = { newName ->
                if (newName.isNotBlank()) {
                    val updatedProfile = currentProfileToEdit.copy(
                        name = newName,
                        updatedAt = System.currentTimeMillis()
                    )
                    profilesViewModel.updateProfile(updatedProfile)
                    showEditDialog = false
                    profileToEdit = null
                }
            }
        )
    }
}


@Composable
private fun EditProfileNameDialog(
    show: MutableState<Boolean>,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editName by remember { mutableStateOf(currentName) }

    SuperDialog(
        title = MLang.ProfilesPage.EditDialog.Title,
        show = show,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = editName,
                onValueChange = { editName = it },
                label = MLang.ProfilesPage.Input.ProfileName,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(MLang.ProfilesPage.Button.Cancel)
                }
                Button(
                    onClick = { onConfirm(editName) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        MLang.ProfilesPage.Button.Confirm,
                        color = MiuixTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
private fun AddProfileSheet(
    show: MutableState<Boolean>,
    profileToEdit: Profile? = null,
    importUrl: String? = null,
    onAddProfile: (Profile) -> Unit,
    onUpdateProfile: (Profile) -> Unit = {},
    onDownloadComplete: () -> Unit,
    profilesViewModel: ProfilesViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStartTime by remember { mutableStateOf(0L) }

    val downloadProgress by profilesViewModel.downloadProgress.collectAsState()
    val uiState by profilesViewModel.uiState.collectAsState()
    var displayedProgress by remember { mutableStateOf(0) }
    var lastProgress by remember { mutableStateOf(0) }

    val urlPattern = remember {
        Regex(
            pattern = "^https?://\\S+$",
            options = setOf(RegexOption.IGNORE_CASE)
        )
    }

    val readClipboardAndCheckUrl: () -> String? = {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val clipText = item?.text?.toString()?.trim() ?: ""
                if (clipText.isNotEmpty() && urlPattern.matches(clipText)) {
                    clipText
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val clearAllState = {
        name = ""
        url = ""
        filePath = ""
        fileName = ""
        error = ""
        isDownloading = false
        displayedProgress = 0
        lastProgress = 0
    }


    val clearCurrentTypeState = {
        when (selectedTypeIndex) {
            0 -> url = ""
            1 -> {
                filePath = ""
                fileName = ""
            }

            2 -> {
            }
        }
        error = ""
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, MLang.ProfilesPage.QrScanner.NeedCamera, Toast.LENGTH_LONG)
                .show()
            selectedTypeIndex = 0
        }
    }

    LaunchedEffect(selectedTypeIndex) {
        if (selectedTypeIndex == 2 && !hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    DisposableEffect(show.value, profileToEdit, importUrl) {
        if (show.value) {
            clearAllState()
            if (profileToEdit != null) {
                name = profileToEdit.name
                if (profileToEdit.type == ProfileType.URL) {
                    selectedTypeIndex = 0
                    url = profileToEdit.remoteUrl ?: ""
                } else {
                    selectedTypeIndex = 1
                    filePath = profileToEdit.config
                    fileName = File(profileToEdit.config).name
                }
            } else if (!importUrl.isNullOrBlank()) {
                selectedTypeIndex = 0
                url = importUrl
                Toast.makeText(context, MLang.ProfilesPage.Message.UrlImported, Toast.LENGTH_SHORT)
                    .show()
            } else {
                selectedTypeIndex = 0
                try {
                    val clipboardUrl = readClipboardAndCheckUrl()
                    if (clipboardUrl != null) {
                        url = clipboardUrl
                        Toast.makeText(
                            context,
                            MLang.ProfilesPage.Message.ClipboardRead,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        onDispose { }
    }


    LaunchedEffect(downloadProgress) {
        val progress = downloadProgress
        if (progress != null) {
            val currentProgress = progress.progress


            val actualProgress = when {
                currentProgress == 10 -> {

                    val cycleTime = 2000L
                    val elapsed = (System.currentTimeMillis() % cycleTime)
                    val cycleProgress = (elapsed.toFloat() / cycleTime * 15).toInt() + 10
                    cycleProgress
                }

                currentProgress >= 10 && currentProgress <= 50 -> {

                    currentProgress
                }

                currentProgress >= 55 && currentProgress <= 95 -> {

                    currentProgress
                }

                currentProgress == 100 -> 100
                else -> lastProgress
            }


            if (actualProgress >= displayedProgress) {

                if (actualProgress - displayedProgress > 5) {

                    val steps = (actualProgress - displayedProgress) / 5
                    for (i in 1..steps) {
                        val stepProgress =
                            kotlin.math.min(displayedProgress + (i * 5), actualProgress)
                        displayedProgress = stepProgress
                        delay(100)
                    }
                }
                displayedProgress = actualProgress
            }


            if (progress != null && progress.progress == 100) {

                val elapsedTime = System.currentTimeMillis() - downloadStartTime
                val minDisplayTime = 1500L


                if (elapsedTime < minDisplayTime) {
                    delay(minDisplayTime - elapsedTime)
                }


                delay(500)
                onDownloadComplete()
            }
        }
    }


    LaunchedEffect(uiState.error) {
        if (uiState.error != null && isDownloading) {
            isDownloading = false
            error = uiState.error ?: MLang.Component.Message.Error
            profilesViewModel.clearError()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val actualFileName =
                context.contentResolver.query(
                    it,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )
                    ?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: MLang.ProfilesPage.Message.UnknownFile

            val extension = actualFileName.substringAfterLast(".", "")
            if (!extension.equals("yaml", ignoreCase = true) && !extension.equals(
                    "yml",
                    ignoreCase = true
                )
            ) {
                error = MLang.ProfilesPage.Validation.YamlOnly
                return@let
            }

            filePath = it.toString()
            error = ""
            fileName = actualFileName

            val fileNameWithoutExt = actualFileName.substringBeforeLast(".")
            if (name.isBlank() || name == actualFileName) {
                name =
                    if (fileNameWithoutExt.isNotBlank()) fileNameWithoutExt else MLang.ProfilesPage.Input.NewProfile
            }
        }
    }

    val qrImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch {
                    try {
                        val result = readQrFromImage(context, it)
                        if (result != null) {
                            url = result
                            selectedTypeIndex = 0
                            Toast.makeText(
                                context,
                                MLang.ProfilesPage.QrScanner.RecognizeSuccess,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                MLang.ProfilesPage.QrScanner.RecognizeFailed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            MLang.ProfilesPage.QrScanner.RecognizeError.format(e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    val showCameraPreview = remember { mutableStateOf(false) }

    LaunchedEffect(selectedTypeIndex, show.value, isDownloading, hasCameraPermission) {
        showCameraPreview.value =
            show.value && selectedTypeIndex == 2 && !isDownloading && hasCameraPermission
    }

    SuperBottomSheet(
        show = show,
        title = if (profileToEdit != null) MLang.ProfilesPage.Sheet.EditTitle else MLang.ProfilesPage.Sheet.AddTitle,
        backgroundColor = MiuixTheme.colorScheme.surface,
        dragHandleColor = MiuixTheme.colorScheme.onSurfaceVariantActions,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = {
            if (!isDownloading) {
                showCameraPreview.value = false
                show.value = false
                profilesViewModel.clearDownloadProgress()
            }
        }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = isDownloading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterVertically
                        )
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = displayedProgress == 100,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                                        animationSpec = tween(300)
                                    )
                                },
                                label = "ProgressIcon"
                            ) { complete ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (complete) {
                                        Icon(
                                            imageVector = Yume.`Package-check`,
                                            contentDescription = MLang.Log.Detail.Done,
                                            tint = MiuixTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = downloadProgress?.message
                                ?: MLang.ProfilesPage.Progress.Downloading,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "$displayedProgress%",
                            style = MiuixTheme.textStyles.body2,
                            color = if (displayedProgress >= 100) {
                                androidx.compose.ui.graphics.Color.Transparent
                            } else {
                                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isDownloading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    top.yukonga.miuix.kmp.basic.Card {
                        Box(modifier = Modifier.alpha(if (profileToEdit != null) 0.5f else 1f)) {
                            SuperSpinner(
                                title = MLang.ProfilesPage.Type.Title, items = listOf(
                                    SpinnerEntry(title = MLang.ProfilesPage.Type.Subscription),
                                    SpinnerEntry(title = MLang.ProfilesPage.Type.LocalFile),
                                    SpinnerEntry(title = MLang.ProfilesPage.Type.QrScan)
                                ), selectedIndex = selectedTypeIndex, onSelectedIndexChange = {
                                    if (profileToEdit == null) {
                                        selectedTypeIndex = it
                                        clearCurrentTypeState()
                                    }
                                })

                            if (profileToEdit != null) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {}
                                        )
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = selectedTypeIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(150))
                        },
                        label = "ProfileTypeContent"
                    ) { typeIndex ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (typeIndex) {
                                2 -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MiuixTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (showCameraPreview.value) {
                                            key("qr_scanner_stable") {
                                                StableQrScanner(
                                                    onScanned = { scannedUrl ->
                                                        showCameraPreview.value = false
                                                        url = scannedUrl
                                                        selectedTypeIndex = 0
                                                        Toast.makeText(
                                                            context,
                                                            MLang.ProfilesPage.QrScanner.ScanSuccess,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                )
                                            }
                                        } else if (!hasCameraPermission) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(MLang.ProfilesPage.QrScanner.NeedPermission)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(
                                                    text = MLang.ProfilesPage.QrScanner.GrantPermission,
                                                    onClick = {
                                                        cameraPermissionLauncher.launch(
                                                            Manifest.permission.CAMERA
                                                        )
                                                    }
                                                )
                                            }
                                        } else {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        }
                                    }

                                    TextButton(
                                        text = MLang.ProfilesPage.QrScanner.SelectFromAlbum,
                                        onClick = { qrImageLauncher.launch("image/*") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            showCameraPreview.value = false
                                            show.value = false
                                            profilesViewModel.clearDownloadProgress()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(MLang.ProfilesPage.Button.Cancel) }
                                }

                                else -> {
                                    TextField(
                                        value = name,
                                        onValueChange = { name = it; error = "" },
                                        label = MLang.ProfilesPage.Input.ProfileName,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (typeIndex == 0) {
                                        TextField(
                                            value = url,
                                            onValueChange = { url = it; error = "" },
                                            label = MLang.ProfilesPage.Input.SubscriptionUrl,
                                            maxLines = 2,
                                            readOnly = profileToEdit != null,
                                            enabled = profileToEdit == null,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        TextField(
                                            value = fileName.ifEmpty { "" },
                                            onValueChange = { },
                                            label = MLang.ProfilesPage.Input.SelectFile,
                                            readOnly = true,
                                            enabled = false,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }) {
                                                    launcher.launch(
                                                        "*/*"
                                                    )
                                                })
                                    }

                                    if (error.isNotEmpty()) {
                                        Text(
                                            text = error,
                                            color = MiuixTheme.colorScheme.error,
                                            style = MiuixTheme.textStyles.body2
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                show.value = false
                                                profilesViewModel.clearDownloadProgress()
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) { Text(MLang.ProfilesPage.Button.Cancel) }
                                        Button(
                                            onClick = {
                                                if (typeIndex == 0 && url.isBlank()) {
                                                    error =
                                                        MLang.ProfilesPage.Validation.EnterUrl; return@Button
                                                }
                                                if (typeIndex == 1 && filePath.isBlank()) {
                                                    error =
                                                        MLang.ProfilesPage.Validation.SelectFile; return@Button
                                                }

                                                keyboardController?.hide()
                                                profilesViewModel.clearError()
                                                downloadStartTime = System.currentTimeMillis()
                                                isDownloading = true

                                                if (typeIndex == 0) {
                                                    if (profileToEdit != null) {
                                                        val updatedProfile = profileToEdit.copy(
                                                            name = name,
                                                            remoteUrl = url,
                                                            updatedAt = System.currentTimeMillis()
                                                        )
                                                        onUpdateProfile(updatedProfile)
                                                        show.value = false
                                                    } else {
                                                        val profile = Profile(
                                                            id = UUID.randomUUID().toString(),
                                                            name = if (name.isNotBlank()) name else MLang.ProfilesPage.Input.NewProfile,
                                                            config = "",
                                                            remoteUrl = url,
                                                            type = ProfileType.URL,
                                                            createdAt = System.currentTimeMillis(),
                                                            updatedAt = System.currentTimeMillis()
                                                        )

                                                        onAddProfile(profile)

                                                        scope.launch {
                                                            val downloadedProfile =
                                                                profilesViewModel.downloadProfile(
                                                                    profile,
                                                                    saveToDb = true
                                                                )
                                                            if (downloadedProfile != null && !downloadedProfile.config.isNullOrBlank()) {
                                                                show.value = false
                                                                profilesViewModel.clearDownloadProgress()
                                                            } else {
                                                                isDownloading = false
                                                                profilesViewModel.clearDownloadProgress()
                                                                profilesViewModel.showError(MLang.ProfilesPage.Validation.DownloadFailed)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (profileToEdit != null) {
                                                        val updatedProfile = profileToEdit.copy(
                                                            name = name,
                                                            updatedAt = System.currentTimeMillis()
                                                        )
                                                        onUpdateProfile(updatedProfile)
                                                        show.value = false
                                                    } else {
                                                        scope.launch {
                                                            val importedProfile =
                                                                profilesViewModel.importProfileFromFile(
                                                                    Uri.parse(filePath),
                                                                    name.ifBlank({ MLang.ProfilesPage.Input.NewProfile }),
                                                                    saveToDb = false
                                                                )
                                                            isDownloading = false
                                                            if (importedProfile != null) {
                                                                onAddProfile(importedProfile)
                                                                show.value = false
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColorsPrimary()
                                        ) {
                                            Text(
                                                MLang.ProfilesPage.Button.Confirm,
                                                color = MiuixTheme.colorScheme.background
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StableQrScanner(
    onScanned: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnScanned by rememberUpdatedState(onScanned)

    val hasScanned = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        factory = { context ->
            val previewView = PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }

            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )

            val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

            val previewUseCase = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processStableQrImage(barcodeScanner, imageProxy) { text ->
                            if (hasScanned.compareAndSet(false, true)) {
                                currentOnScanned(text)
                            }
                        }
                    }
                }

            coroutineScope.launch {
                try {
                    val cameraProvider = context.getStableCameraProvider()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        previewUseCase,
                        imageAnalysisUseCase,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            previewView
        },
        onRelease = { previewView ->
            try {
                val context = previewView.context
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )
}

@android.annotation.SuppressLint("UnsafeOptInUsageError")
private fun processStableQrImage(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onScanned: (String) -> Unit,
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees,
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodeList ->
                barcodeList.firstOrNull()?.rawValue?.let { text ->
                    onScanned(text)
                }
            }
            .addOnCompleteListener {
                imageProxy.image?.close()
                imageProxy.close()
            }
    } ?: imageProxy.close()
}

private suspend fun Context.getStableCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener(
                { continuation.resume(future.get()) },
                ContextCompat.getMainExecutor(this),
            )
        }
    }

private suspend fun readQrFromImage(context: Context, uri: Uri): String? =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.getOrNull(0)
                    continuation.resume(barcode?.rawValue)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(null)
        }
    }

@Composable
private fun DeleteConfirmDialog(
    profileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    SuperDialog(
        title = MLang.ProfilesPage.DeleteDialog.Title,
        summary = MLang.ProfilesPage.DeleteDialog.Message.format(profileName),
        show = remember { mutableStateOf(true) },
        onDismissRequest = onDismiss
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text(MLang.ProfilesPage.Button.Cancel) }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(
                    MLang.ProfilesPage.DeleteDialog.Confirm,
                    color = MiuixTheme.colorScheme.background
                )
            }
        }
    }
}