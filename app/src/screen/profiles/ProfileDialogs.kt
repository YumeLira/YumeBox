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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.screen.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.data.model.OverrideConfig
import com.github.yumelira.yumebox.data.model.ProfileBinding
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumelira.yumebox.presentation.component.AppDialog
import com.github.yumelira.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumelira.yumebox.presentation.component.DialogButtonRow
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val PROFILE_SETTINGS_MIN_HEIGHT_FRACTION = 0.5f
private const val PROFILE_SETTINGS_MAX_HEIGHT_FRACTION = 0.7f

@Composable
internal fun EditProfileNameDialog(
    show: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editName by remember { mutableStateOf(currentName) }

    AppTextFieldDialog(
        show = show,
        title = MLang.ProfilesPage.EditDialog.Title,
        value = editName,
        onValueChange = { editName = it },
        onDismissRequest = onDismiss,
        onConfirm = { onConfirm(editName) },
        label = MLang.ProfilesPage.Input.ProfileName,
        singleLine = true,
    )
}

@Composable
internal fun DeleteConfirmDialog(
    show: Boolean,
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
) {
    AppDialog(
        show = show,
        modifier = Modifier,
        title = MLang.ProfilesPage.DeleteDialog.Title,
        titleColor = DialogDefaults.titleColor(),
        summary = MLang.ProfilesPage.DeleteDialog.Message.format(profileName),
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                cancelText = MLang.ProfilesPage.Button.Cancel,
                confirmText = MLang.ProfilesPage.DeleteDialog.Confirm
            )
        })
}

@Composable
internal fun ShareOptionsDialog(
    show: Boolean,
    profile: Profile,
    onDismiss: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    onShareFile: (Profile) -> Unit,
    onShareLink: (Profile) -> Unit
) {
    val spacing = AppTheme.spacing

    AppDialog(
        show = show,
        modifier = Modifier,
        title = MLang.ProfilesPage.ShareDialog.Title,
        titleColor = DialogDefaults.titleColor(),
        summary = null,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space12)
            ) {
                if (profile.type == Profile.Type.Url) {
                    Button(
                        onClick = { onShareLink(profile) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            MLang.ProfilesPage.ShareDialog.ShareLink,
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
                Button(
                    onClick = { onShareFile(profile) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(MLang.ProfilesPage.ShareDialog.ShareFile)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(MLang.ProfilesPage.Button.Cancel)
                }
            }
        })
}

@Composable
internal fun ProfileSettingsDialog(
    show: Boolean,
    profile: Profile,
    userConfigs: List<OverrideConfig>,
    binding: ProfileBinding?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSaveProfileMeta: (String, String) -> Unit,
    onSaveOverrideSettings: (List<String>) -> Unit,
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    val initialCustomRoutingEnabled = binding?.overrideIds
        ?.contains(OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID) == true

    val initialOverrideIds = binding
        ?.overrideIds
        .orEmpty()
        .filter { it != OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID }
    var editName by remember { mutableStateOf(profile.name) }
    var editSource by remember { mutableStateOf("") }
    var customRoutingSelected by remember { mutableStateOf(initialCustomRoutingEnabled) }
    var pendingSelectedUserOverrideIds by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(show, profile.uuid, profile.name, binding?.overrideIds) {
        if (show) {
            editName = profile.name
            editSource = ""
            customRoutingSelected = initialCustomRoutingEnabled
            pendingSelectedUserOverrideIds = initialOverrideIds
        }
    }

    val toggleUserOverrideSelection: (String, Boolean) -> Unit = { overrideId, isSelected ->
        pendingSelectedUserOverrideIds =
            toggleOverrideIdSelection(pendingSelectedUserOverrideIds, overrideId, isSelected)
    }
    val saveSettings = {
        val trimmedName = editName.trim()
        val trimmedSource = editSource.trim()
        val targetSource = if (profile.type == Profile.Type.Url && trimmedSource.isNotEmpty()) {
            trimmedSource
        } else {
            profile.source
        }
        if (trimmedName.isNotEmpty() && targetSource.isNotEmpty() &&
            (trimmedName != profile.name || targetSource != profile.source)
        ) {
            onSaveProfileMeta(trimmedName, targetSource)
        }

        val basicFinalIds = buildFinalOverrideIds(pendingSelectedUserOverrideIds)
        val finalSelectedOverrideIds = if (customRoutingSelected) {
            basicFinalIds + OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
        } else {
            basicFinalIds - OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
        }
        onSaveOverrideSettings(finalSelectedOverrideIds)
        onDismiss()
    }

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = MLang.ProfilesPage.SettingsDialog.Title,
        startAction = {
            AppBottomSheetCloseAction(
                onClick = onDismiss,
                contentDescription = MLang.ProfilesPage.Button.Cancel,
            )
        },
        endAction = {
            AppBottomSheetConfirmAction(
                onClick = saveSettings,
                contentDescription = MLang.ProfilesPage.Button.Confirm,
            )
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        enableNestedScroll = true,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val minimumSheetHeight = maxHeight * PROFILE_SETTINGS_MIN_HEIGHT_FRACTION
            val maximumSheetHeight = maxHeight * PROFILE_SETTINGS_MAX_HEIGHT_FRACTION

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minimumSheetHeight, max = maximumSheetHeight)
                    .padding(bottom = spacing.space16),
                verticalArrangement = Arrangement.spacedBy(spacing.space16),
            ) {
                TextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = MLang.ProfilesPage.Input.ProfileName,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (profile.type == Profile.Type.Url) {
                    TextField(
                        value = editSource,
                        onValueChange = { editSource = it },
                        label = MLang.ProfilesPage.SettingsDialog.ChangeLink,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }

                Card {
                    Column {
                        SwitchPreference(
                            title = MLang.ProfilesPage.SettingsDialog.CustomRouting,
                            summary = MLang.ProfilesPage.SettingsDialog.CustomRoutingSummary,
                            checked = customRoutingSelected,
                            onCheckedChange = { customRoutingSelected = it },
                        )
                    }
                }

                if (userConfigs.isNotEmpty()) {
                    Card {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = componentSizes.profileSettingsListMaxHeight),
                        ) {
                            itemsIndexed(userConfigs, key = { _, config -> config.id }) { index, config ->
                                val isSelected = config.id in pendingSelectedUserOverrideIds
                                BasicComponent(
                                    title = config.name,
                                    summary = config.description?.takeIf { it.isNotBlank() } ?: MLang.ProfilesPage.SettingsDialog.NoDescription,
                                    endActions = {
                                        Checkbox(
                                            state = ToggleableState(isSelected),
                                            onClick = {
                                                toggleUserOverrideSelection(config.id, isSelected)
                                            },
                                        )
                                    },
                                    onClick = {
                                        toggleUserOverrideSelection(config.id, isSelected)
                                    },
                                )
                                if (index < userConfigs.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = spacing.space16),
                                        thickness = componentSizes.thinDividerThickness,
                                        color = MiuixTheme.colorScheme.outline.copy(alpha = opacity.outline),
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

private fun toggleOverrideIdSelection(
    selectedOverrideIds: List<String>,
    overrideId: String,
    isSelected: Boolean,
): List<String> {
    return if (isSelected) {
        selectedOverrideIds - overrideId
    } else {
        (selectedOverrideIds + overrideId).distinct()
    }
}

private fun buildFinalOverrideIds(
    selectedUserOverrideIds: List<String>,
): List<String> {
    return selectedUserOverrideIds.distinct()
}
