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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.github.yumelira.yumebox.presentation.component.AgeSecretKeyField
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_QR
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_URL
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DownloadProgressContent(
    downloadProgress: DownloadProgress?,
    stableSheetHeightPx: Int,
    stableSheetHeight: Dp,
    downloadSheetContentHeight: Dp,
    downloadCompleteSheetContentHeight: Dp,
) {
    val isCompleted = downloadProgress?.isCompleted == true
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .height(
                    if (isCompleted) {
                        downloadCompleteSheetContentHeight
                    } else if (stableSheetHeightPx > 0) {
                        stableSheetHeight
                    } else {
                        downloadSheetContentHeight
                    }
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16, Alignment.CenterVertically),
    ) {
        AnimatedContent(
            targetState = isCompleted,
            modifier = Modifier.size(UiDp.dp48),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ProgressIcon",
        ) { complete ->
            if (complete) {
                Icon(
                    imageVector = Yume.`Package-check`,
                    contentDescription = "Complete",
                    tint = MiuixTheme.colorScheme.onPrimary,
                    modifier =
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(UiDp.dp16))
                            .background(MiuixTheme.colorScheme.primary)
                            .padding(UiDp.dp10),
                )
            } else {
                InfiniteProgressIndicator(modifier = Modifier.size(UiDp.dp32))
            }
        }

        downloadProgress?.message?.let { message ->
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ProfileFormContent(
    selectedTypeIndex: Int,
    profileLocked: Boolean,
    nameTextFieldValue: TextFieldValue,
    urlTextFieldValue: TextFieldValue,
    fileNameTextFieldValue: TextFieldValue,
    ageSecretKeyTextFieldValue: TextFieldValue,
    error: String,
    hasCameraPermission: Boolean,
    showCameraPreview: Boolean,
    onContainerMeasured: (IntSize) -> Unit,
    onTypeSelected: (Int) -> Unit,
    onNameChange: (TextFieldValue) -> Unit,
    onUrlChange: (TextFieldValue) -> Unit,
    onAgeSecretKeyChange: (TextFieldValue) -> Unit,
    onPickFile: () -> Unit,
    onSelectQrImage: () -> Unit,
    onQrScanned: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().onSizeChanged(onContainerMeasured),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        ProfileTypeSelectorCard(
            selectedTypeIndex = selectedTypeIndex,
            profileLocked = profileLocked,
            onTypeSelected = onTypeSelected,
        )

        Crossfade(
            targetState = selectedTypeIndex,
            animationSpec = tween(200),
            label = "ProfileTypeContent",
        ) { typeIndex ->
            when (typeIndex) {
                PROFILE_IMPORT_TYPE_QR ->
                    QrScannerContent(
                        hasCameraPermission = hasCameraPermission,
                        showCameraPreview = showCameraPreview,
                        onSelectQrImage = onSelectQrImage,
                        onQrScanned = onQrScanned,
                    )

                else ->
                    ManualProfileContent(
                        typeIndex = typeIndex,
                        profileLocked = profileLocked,
                        nameTextFieldValue = nameTextFieldValue,
                        urlTextFieldValue = urlTextFieldValue,
                        fileNameTextFieldValue = fileNameTextFieldValue,
                        ageSecretKeyTextFieldValue = ageSecretKeyTextFieldValue,
                        error = error,
                        onNameChange = onNameChange,
                        onUrlChange = onUrlChange,
                        onAgeSecretKeyChange = onAgeSecretKeyChange,
                        onPickFile = onPickFile,
                    )
            }
        }
    }
}

@Composable
private fun ProfileTypeSelectorCard(
    selectedTypeIndex: Int,
    profileLocked: Boolean,
    onTypeSelected: (Int) -> Unit,
) {
    top.yukonga.miuix.kmp.basic.Card {
        Box(
            modifier =
                Modifier.alpha(if (profileLocked) 0.5f else 1f)
                    .clickable(
                        enabled = profileLocked,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
        ) {
            WindowSpinnerPreference(
                title = MLang.ProfilesPage.Type.Title,
                items =
                    listOf(
                        SpinnerEntry(title = MLang.ProfilesPage.Type.Subscription),
                        SpinnerEntry(title = MLang.ProfilesPage.Type.LocalFile),
                        SpinnerEntry(title = MLang.ProfilesPage.Type.QrScan),
                    ),
                selectedIndex = selectedTypeIndex,
                onSelectedIndexChange = onTypeSelected,
            )
        }
    }
}

@Composable
private fun QrScannerContent(
    hasCameraPermission: Boolean,
    showCameraPreview: Boolean,
    onSelectQrImage: () -> Unit,
    onQrScanned: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .height(UiDp.dp200)
                    .clip(RoundedCornerShape(UiDp.dp12))
                    .background(MiuixTheme.colorScheme.surfaceVariant),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showCameraPreview) {
                key("qr_scanner_stable") { StableQrScanner(onScanned = onQrScanned) }
            } else if (!hasCameraPermission) {
                Text(MLang.ProfilesPage.QrScanner.NeedPermission)
            } else {
                CircularProgressIndicator(modifier = Modifier.size(UiDp.dp32))
            }
        }

        TextButton(
            text = MLang.ProfilesPage.QrScanner.SelectFromAlbum,
            onClick = onSelectQrImage,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ManualProfileContent(
    typeIndex: Int,
    profileLocked: Boolean,
    nameTextFieldValue: TextFieldValue,
    urlTextFieldValue: TextFieldValue,
    fileNameTextFieldValue: TextFieldValue,
    ageSecretKeyTextFieldValue: TextFieldValue,
    error: String,
    onNameChange: (TextFieldValue) -> Unit,
    onUrlChange: (TextFieldValue) -> Unit,
    onAgeSecretKeyChange: (TextFieldValue) -> Unit,
    onPickFile: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        TextField(
            value = nameTextFieldValue,
            onValueChange = onNameChange,
            label = MLang.ProfilesPage.Input.ProfileName,
            useLabelAsPlaceholder = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (typeIndex == PROFILE_IMPORT_TYPE_URL) {
            TextField(
                value = urlTextFieldValue,
                onValueChange = onUrlChange,
                label = MLang.ProfilesPage.Input.SubscriptionUrl,
                useLabelAsPlaceholder = true,
                maxLines = 2,
                readOnly = profileLocked,
                enabled = !profileLocked,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TextField(
                value = fileNameTextFieldValue,
                onValueChange = {},
                label = MLang.ProfilesPage.Input.SelectFile,
                useLabelAsPlaceholder = true,
                readOnly = true,
                enabled = false,
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onPickFile,
                        ),
            )
        }

        AgeSecretKeyField(
            value = ageSecretKeyTextFieldValue,
            onValueChange = onAgeSecretKeyChange,
            label = MLang.ProfilesPage.Input.AgeSecretKey,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}
