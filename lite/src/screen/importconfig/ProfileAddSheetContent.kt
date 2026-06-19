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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.AgeSecretKeyField
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_FILE
import com.github.yumelira.yumebox.presentation.util.PROFILE_IMPORT_TYPE_URL
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LiteDownloadProgressContent(
    downloadProgress: DownloadProgress?,
    stableSheetHeightPx: Int,
    stableSheetHeight: Dp,
    downloadSheetContentHeight: Dp,
    downloadCompleteSheetContentHeight: Dp,
) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(
                    if (downloadProgress?.isCompleted == true) {
                        downloadCompleteSheetContentHeight
                    } else if (stableSheetHeightPx > 0) {
                        stableSheetHeight
                    } else {
                        downloadSheetContentHeight
                    }
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            InfiniteProgressIndicator(modifier = Modifier.size(32.dp))
            Text(
                text = downloadProgress?.message ?: MLang.ProfilesVM.Progress.Preparing,
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
internal fun LiteProfileFormContent(
    selectedTypeIndex: Int,
    profileLocked: Boolean,
    name: String,
    url: String,
    fileName: String,
    ageSecretKeyTextFieldValue: TextFieldValue,
    error: String,
    onContainerMeasured: (height: Int) -> Unit,
    onTypeSelected: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onAgeSecretKeyChange: (TextFieldValue) -> Unit,
    onPickFile: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().onSizeChanged { onContainerMeasured(it.height) },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            WindowSpinnerPreference(
                title = MLang.ProfilesPage.Type.Title,
                items =
                    listOf(
                        SpinnerEntry(title = MLang.ProfilesPage.Type.Subscription),
                        SpinnerEntry(title = MLang.ProfilesPage.Type.LocalFile),
                    ),
                selectedIndex = selectedTypeIndex,
                onSelectedIndexChange = onTypeSelected,
            )
        }

        TextField(
            value = name,
            onValueChange = onNameChange,
            label = MLang.ProfilesPage.Input.ProfileName,
            modifier = Modifier.fillMaxWidth(),
        )

        if (selectedTypeIndex == PROFILE_IMPORT_TYPE_URL) {
            TextField(
                value = url,
                onValueChange = onUrlChange,
                label = MLang.ProfilesPage.Input.SubscriptionUrl,
                maxLines = 2,
                readOnly = profileLocked,
                enabled = !profileLocked,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TextField(
                value = fileName,
                onValueChange = {},
                label = MLang.ProfilesPage.Input.SelectFile,
                readOnly = true,
                modifier =
                    Modifier.fillMaxWidth().clickable(
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

internal fun shouldLaunchFilePicker(
    selectedTypeIndex: Int,
    profileLocked: Boolean,
    filePath: String,
): Boolean {
    return selectedTypeIndex == PROFILE_IMPORT_TYPE_FILE && !profileLocked && filePath.isBlank()
}
