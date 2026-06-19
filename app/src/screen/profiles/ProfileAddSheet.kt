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

package com.github.yumelira.yumebox.screen.profiles

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_FILE
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_QR
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_URL
import com.github.yumelira.yumebox.presentation.util.importTypeIndexFor
import com.github.yumelira.yumebox.presentation.util.isYamlConfigFileName
import com.github.yumelira.yumebox.presentation.util.profileNameFromConfigFileName
import com.github.yumelira.yumebox.presentation.util.readClipboardSubscriptionUrl
import com.github.yumelira.yumebox.presentation.util.readDisplayName
import com.github.yumelira.yumebox.presentation.util.sourceFileName
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import java.util.*
import kotlin.math.max
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*

@Composable
internal fun AddProfileSheet(
    show: MutableState<Boolean>,
    profileToEdit: Profile? = null,
    importUrl: String? = null,
    onAddProfile:
        (
            name: String,
            source: String,
            type: Profile.Type,
            interval: Long,
            fileUri: android.net.Uri?,
            ageSecretKey: String,
        ) -> Unit,
    onUpdateProfile: (uuid: UUID, name: String, source: String, interval: Long) -> Unit,
    onDownloadComplete: () -> Unit,
    profilesViewModel: ProfilesViewModel,
) {
    val configuration = LocalConfiguration.current
    val downloadSheetContentHeight = configuration.screenHeightDp.dp * 0.3f
    val downloadCompleteSheetContentHeight = configuration.screenHeightDp.dp * 0.42f
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    var nameTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var urlTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var filePath by remember { mutableStateOf("") }
    var fileNameTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var ageSecretKeyTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var error by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }

    val downloadProgress by profilesViewModel.downloadProgress.collectAsState()
    val uiState by profilesViewModel.uiState.collectAsState()
    var hasShownCompleteAnimation by remember { mutableStateOf(false) }
    var stableSheetHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(show.value) {
        if (!show.value) {
            hasShownCompleteAnimation = false
            isDownloading = false
        }
    }

    val applyNameText: (String) -> Unit = { updatedText ->
        nameTextFieldValue = textFieldValueAtEnd(updatedText)
    }
    val applyUrlText: (String) -> Unit = { updatedText ->
        urlTextFieldValue = textFieldValueAtEnd(updatedText)
    }
    val applyFileNameText: (String) -> Unit = { updatedText ->
        fileNameTextFieldValue = textFieldValueAtEnd(updatedText)
    }

    val clearAllState = {
        applyNameText("")
        applyUrlText("")
        filePath = ""
        applyFileNameText("")
        ageSecretKeyTextFieldValue = TextFieldValue()
        error = ""
        isDownloading = false
        hasShownCompleteAnimation = false
    }

    val clearCurrentTypeState = {
        when (selectedTypeIndex) {
            PROFILE_IMPORT_TYPE_URL -> applyUrlText("")
            PROFILE_IMPORT_TYPE_FILE -> {
                filePath = ""
                applyFileNameText("")
            }

            PROFILE_IMPORT_TYPE_QR -> {}
        }
        error = ""
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                context.toast(MLang.ProfilesPage.QrScanner.NeedCamera, Toast.LENGTH_LONG)
                selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
            }
        }

    LaunchedEffect(selectedTypeIndex) {
        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_QR && !hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val showCameraPreview by
        remember(show.value, selectedTypeIndex, isDownloading, hasCameraPermission) {
            derivedStateOf {
                show.value &&
                    selectedTypeIndex == PROFILE_IMPORT_TYPE_QR &&
                    !isDownloading &&
                    hasCameraPermission
            }
        }

    DisposableEffect(show.value, profileToEdit, importUrl) {
        if (show.value) {
            clearAllState()
            if (profileToEdit != null) {
                applyNameText(profileToEdit.name)
                if (profileToEdit.type == Profile.Type.Url) {
                    selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                    applyUrlText(profileToEdit.source)
                } else {
                    selectedTypeIndex = importTypeIndexFor(profileToEdit.type)
                    filePath = profileToEdit.source
                    applyFileNameText(sourceFileName(profileToEdit.source))
                }
            } else if (!importUrl.isNullOrBlank()) {
                selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                applyUrlText(importUrl)
            } else {
                selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                readClipboardSubscriptionUrl(context)?.let(applyUrlText)
            }
        }
        onDispose {}
    }
    LaunchedEffect(uiState.error) {
        val errorMessage = uiState.error
        if (errorMessage != null) {
            context.toast(errorMessage, Toast.LENGTH_LONG)
            if (isDownloading) {
                isDownloading = false
                error = errorMessage
            }
            profilesViewModel.clearError()
        }
    }

    LaunchedEffect(downloadProgress?.isCompleted, isDownloading) {
        if (isDownloading && downloadProgress?.isCompleted == true && !hasShownCompleteAnimation) {
            hasShownCompleteAnimation = true
            onDownloadComplete()
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null && isDownloading && !hasShownCompleteAnimation) {
            hasShownCompleteAnimation = true
            onDownloadComplete()
        }
        if (uiState.message != null) {
            profilesViewModel.clearMessage()
        }
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val actualFileName =
                    readDisplayName(context, it, MLang.ProfilesPage.Message.UnknownFile)

                if (!isYamlConfigFileName(actualFileName)) {
                    error = MLang.ProfilesPage.Validation.YamlOnly
                    return@let
                }

                filePath = it.toString()
                error = ""
                applyFileNameText(actualFileName)

                if (
                    nameTextFieldValue.text.isBlank() ||
                        nameTextFieldValue.text == actualFileName
                ) {
                    applyNameText(
                        profileNameFromConfigFileName(
                            actualFileName,
                            MLang.ProfilesPage.Input.NewProfile,
                        )
                    )
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
                            applyUrlText(result)
                            selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                            context.toast(MLang.ProfilesPage.QrScanner.RecognizeSuccess)
                        } else {
                            context.toast(MLang.ProfilesPage.QrScanner.RecognizeFailed)
                        }
                    } catch (error: Exception) {
                        context.toast(
                            MLang.ProfilesPage.QrScanner.RecognizeError.format(error.message ?: "")
                        )
                    }
                }
            }
        }

    val dismissSheet = {
        if (!isDownloading) {
            show.value = false
            profilesViewModel.clearDownloadProgress()
        }
    }

    fun submitProfile() {
        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_QR || isDownloading) {
            return
        }
        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_URL && urlTextFieldValue.text.isBlank()) {
            error = MLang.ProfilesPage.Validation.EnterUrl
            return
        }
        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_FILE && filePath.isBlank()) {
            error = MLang.ProfilesPage.Validation.SelectFile
            return
        }

        keyboardController?.hide()
        profilesViewModel.clearError()
        hasShownCompleteAnimation = false
        isDownloading = true

        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_URL) {
            if (profileToEdit != null) {
                onUpdateProfile(
                    profileToEdit.uuid,
                    nameTextFieldValue.text,
                    urlTextFieldValue.text,
                    profileToEdit.interval,
                )
            } else {
                onAddProfile(
                    nameTextFieldValue.text.ifBlank { MLang.ProfilesPage.Input.NewProfile },
                    urlTextFieldValue.text,
                    Profile.Type.Url,
                    0L,
                    null,
                    ageSecretKeyTextFieldValue.text.trim(),
                )
            }
        } else {
            if (profileToEdit != null) {
                onUpdateProfile(
                    profileToEdit.uuid,
                    nameTextFieldValue.text,
                    profileToEdit.source,
                    profileToEdit.interval,
                )
            } else {
                onAddProfile(
                    nameTextFieldValue.text.ifBlank { MLang.ProfilesPage.Input.NewProfile },
                    filePath,
                    Profile.Type.File,
                    0L,
                    filePath.toUri(),
                    ageSecretKeyTextFieldValue.text.trim(),
                )
            }
        }
    }

    AppActionBottomSheet(
        show = show.value,
        title =
            if (profileToEdit != null) MLang.ProfilesPage.Sheet.EditTitle
            else MLang.ProfilesPage.Sheet.AddTitle,
        startAction = {
            if (!isDownloading) {
                AppBottomSheetCloseAction(contentDescription = "Cancel", onClick = dismissSheet)
            }
        },
        endAction = {
            if (!isDownloading && selectedTypeIndex != PROFILE_IMPORT_TYPE_QR) {
                AppBottomSheetConfirmAction(
                    contentDescription = "Confirm",
                    onClick = { submitProfile() },
                )
            }
        },
        onDismissRequest = dismissSheet,
    ) {
        val stableSheetHeight =
            remember(stableSheetHeightPx, density) {
                if (stableSheetHeightPx <= 0) UiDp.dp0
                else with(density) { stableSheetHeightPx.toDp() }
            }
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                    .padding(bottom = UiDp.dp16)
        ) {
            AnimatedContent(
                targetState = isDownloading,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it }) +
                            fadeIn()) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(220),
                                targetOffsetX = { -it / 3 },
                            ) + fadeOut())
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(220),
                            initialOffsetX = { -it / 3 },
                        ) + fadeIn()) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(260),
                                targetOffsetX = { it },
                            ) + fadeOut())
                    }
                },
                label = "ProfileImportContentSwitch",
            ) { downloading ->
                if (downloading) {
                    DownloadProgressContent(
                        downloadProgress = downloadProgress,
                        stableSheetHeightPx = stableSheetHeightPx,
                        stableSheetHeight = stableSheetHeight,
                        downloadSheetContentHeight = downloadSheetContentHeight,
                        downloadCompleteSheetContentHeight = downloadCompleteSheetContentHeight,
                    )
                } else {
                    ProfileFormContent(
                        selectedTypeIndex = selectedTypeIndex,
                        profileLocked = profileToEdit != null,
                        nameTextFieldValue = nameTextFieldValue,
                        urlTextFieldValue = urlTextFieldValue,
                        fileNameTextFieldValue = fileNameTextFieldValue,
                        ageSecretKeyTextFieldValue = ageSecretKeyTextFieldValue,
                        error = error,
                        hasCameraPermission = hasCameraPermission,
                        showCameraPreview = showCameraPreview,
                        onContainerMeasured = {
                            stableSheetHeightPx = max(stableSheetHeightPx, it.height)
                        },
                        onTypeSelected = {
                            selectedTypeIndex = it
                            clearCurrentTypeState()
                        },
                        onNameChange = { updatedTextFieldValue ->
                            nameTextFieldValue = updatedTextFieldValue
                            error = ""
                        },
                        onUrlChange = { updatedTextFieldValue ->
                            urlTextFieldValue = updatedTextFieldValue
                            error = ""
                        },
                        onAgeSecretKeyChange = { updatedTextFieldValue ->
                            ageSecretKeyTextFieldValue = updatedTextFieldValue
                        },
                        onPickFile = { launcher.launch("*/*") },
                        onSelectQrImage = { qrImageLauncher.launch("image/*") },
                        onQrScanned = { scannedUrl ->
                            applyUrlText(scannedUrl)
                            selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                        },
                    )
                }
            }
        }
    }
}

private fun textFieldValueAtEnd(text: String): TextFieldValue {
    return TextFieldValue(text = text, selection = TextRange(text.length))
}
