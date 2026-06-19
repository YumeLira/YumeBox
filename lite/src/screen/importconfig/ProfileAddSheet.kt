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
 * Copyright (c)  YumeLira & YumeRiMoe 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.importconfig

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_FILE
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_URL
import com.github.yumelira.yumebox.presentation.util.importTypeIndexFor
import com.github.yumelira.yumebox.presentation.util.isYamlConfigFileName
import com.github.yumelira.yumebox.presentation.util.profileNameFromConfigFileName
import com.github.yumelira.yumebox.presentation.util.readClipboardSubscriptionUrl
import com.github.yumelira.yumebox.presentation.util.readDisplayName
import com.github.yumelira.yumebox.presentation.util.sourceFileName
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import java.util.UUID
import kotlin.math.max

@Composable
internal fun AddProfileSheet(
    show: MutableState<Boolean>,
    profileToEdit: Profile? = null,
    importUrl: String? = null,
    initialType: Profile.Type? = null,
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
    onDismissFinished: () -> Unit,
    onDownloadComplete: () -> Unit,
    viewModel: ImportConfigViewModel,
) {
    val configuration = LocalConfiguration.current
    val downloadSheetContentHeight = configuration.screenHeightDp.dp * 0.3f
    val downloadCompleteSheetContentHeight = configuration.screenHeightDp.dp * 0.42f
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val profileLocked = profileToEdit != null

    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var ageSecretKeyTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var error by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var hasShownCompleteAnimation by remember { mutableStateOf(false) }
    var stableSheetHeightPx by remember { mutableIntStateOf(0) }
    var launcherLaunchPending by remember { mutableStateOf(false) }

    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    DisposableEffect(show.value, profileToEdit, importUrl, initialType) {
        if (show.value) {
            name = ""
            url = ""
            filePath = ""
            fileName = ""
            ageSecretKeyTextFieldValue = TextFieldValue()
            error = ""
            isDownloading = false
            hasShownCompleteAnimation = false

            when {
                profileToEdit != null -> {
                    name = profileToEdit.name
                    if (profileToEdit.type == Profile.Type.Url) {
                        selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                        url = profileToEdit.source
                    } else {
                        selectedTypeIndex = importTypeIndexFor(profileToEdit.type)
                        filePath = profileToEdit.source
                        fileName = sourceFileName(profileToEdit.source)
                    }
                }

                !importUrl.isNullOrBlank() -> {
                    selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                    url = importUrl
                }

                initialType == Profile.Type.File -> {
                    selectedTypeIndex = PROFILE_IMPORT_TYPE_FILE
                }

                else -> {
                    selectedTypeIndex = PROFILE_IMPORT_TYPE_URL
                    readClipboardSubscriptionUrl(context)?.let { url = it }
                }
            }
        }
        onDispose {}
    }

    LaunchedEffect(uiState.error) {
        val message = uiState.error
        if (message != null) {
            context.toast(message)
            if (isDownloading) {
                isDownloading = false
                error = message
            }
        }
    }

    LaunchedEffect(downloadProgress?.isCompleted, isDownloading) {
        if (isDownloading && downloadProgress?.isCompleted == true && !hasShownCompleteAnimation) {
            hasShownCompleteAnimation = true
            onDownloadComplete()
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
                fileName = actualFileName
                error = ""

                if (name.isBlank() || name == actualFileName) {
                    name =
                        profileNameFromConfigFileName(
                            actualFileName,
                            MLang.ProfilesPage.Input.NewProfile,
                        )
                }
            }
    }

    LaunchedEffect(show.value, selectedTypeIndex) {
        if (show.value && shouldLaunchFilePicker(selectedTypeIndex, profileLocked, filePath)) {
            launcherLaunchPending = true
        }
    }

    LaunchedEffect(launcherLaunchPending) {
        if (launcherLaunchPending) {
            launcherLaunchPending = false
            launcher.launch("*/*")
        }
    }

    val dismissSheet = {
        if (!isDownloading) {
            show.value = false
            viewModel.clearDownloadProgress()
        }
    }

    fun submitProfile() {
        if (isDownloading) return

        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_URL && url.isBlank()) {
            error = MLang.ProfilesPage.Validation.EnterUrl
            return
        }
        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_FILE && filePath.isBlank()) {
            error = MLang.ProfilesPage.Validation.SelectFile
            return
        }

        keyboardController?.hide()
        viewModel.clearError()
        hasShownCompleteAnimation = false
        isDownloading = true

        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_URL) {
            if (profileToEdit != null) {
                onUpdateProfile(
                    profileToEdit.uuid,
                    name.ifBlank { profileToEdit.name },
                    url,
                    profileToEdit.interval,
                )
            } else {
                onAddProfile(
                    name.ifBlank { MLang.ProfilesPage.Input.NewProfile },
                    url,
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
                    name.ifBlank { profileToEdit.name },
                    profileToEdit.source,
                    profileToEdit.interval,
                )
            } else {
                onAddProfile(
                    name.ifBlank { MLang.ProfilesPage.Input.NewProfile },
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
            if (profileLocked) MLang.ProfilesPage.Sheet.EditTitle
            else MLang.ProfilesPage.Sheet.AddTitle,
        startAction = {
            if (!isDownloading) {
                AppBottomSheetCloseAction(
                    contentDescription = MLang.Component.Button.Cancel,
                    onClick = dismissSheet,
                )
            }
        },
        endAction = {
            if (!isDownloading) {
                AppBottomSheetConfirmAction(
                    contentDescription = MLang.Component.Button.Confirm,
                    onClick = ::submitProfile,
                )
            }
        },
        onDismissRequest = dismissSheet,
        onDismissFinished = onDismissFinished,
    ) {
        val stableSheetHeight =
            remember(stableSheetHeightPx, density) {
                if (stableSheetHeightPx <= 0) 0.dp else with(density) { stableSheetHeightPx.toDp() }
            }

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                    .padding(bottom = 16.dp)
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
                label = "lite_profile_import_switch",
            ) { downloading ->
                if (downloading) {
                    LiteDownloadProgressContent(
                        downloadProgress = downloadProgress,
                        stableSheetHeightPx = stableSheetHeightPx,
                        stableSheetHeight = stableSheetHeight,
                        downloadSheetContentHeight = downloadSheetContentHeight,
                        downloadCompleteSheetContentHeight = downloadCompleteSheetContentHeight,
                    )
                } else {
                    LiteProfileFormContent(
                        selectedTypeIndex = selectedTypeIndex,
                        profileLocked = profileLocked,
                        name = name,
                        url = url,
                        fileName = fileName,
                        ageSecretKeyTextFieldValue = ageSecretKeyTextFieldValue,
                        error = error,
                        onContainerMeasured = {
                            stableSheetHeightPx = max(stableSheetHeightPx, it)
                        },
                        onTypeSelected = {
                            if (!profileLocked) {
                                selectedTypeIndex = it
                                error = ""
                                if (shouldLaunchFilePicker(it, profileLocked = false, filePath)) {
                                    launcherLaunchPending = true
                                }
                            }
                        },
                        onNameChange = {
                            name = it
                            error = ""
                        },
                        onUrlChange = {
                            url = it
                            error = ""
                        },
                        onAgeSecretKeyChange = {
                            ageSecretKeyTextFieldValue = it
                        },
                        onPickFile = { launcher.launch("*/*") },
                    )
                }
            }
        }
    }
}
