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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.AppFormDialog
import com.github.yumelira.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumelira.yumebox.presentation.component.SectionCard
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LinkSettingsDialog(
    show: MutableState<Boolean>,
    links: List<ProfileLink>,
    linkOpenMode: LinkOpenMode,
    defaultLinkId: String,
    onOpenModeChange: (LinkOpenMode) -> Unit,
    onDefaultLinkChange: (String) -> Unit,
    onAddLink: () -> Unit,
    onDeleteLink: (String) -> Unit,
    onOpenLink: (ProfileLink) -> Unit,
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    val openModeOptions =
        listOf(
            MLang.ProfilesPage.LinkSettings.OpenModeInApp,
            MLang.ProfilesPage.LinkSettings.OpenModeExternal,
        )
    val openModeIndex =
        when (linkOpenMode) {
            LinkOpenMode.IN_APP -> 0
            LinkOpenMode.EXTERNAL_BROWSER -> 1
        }

    val defaultLinkIndex =
        if (defaultLinkId.isEmpty() || links.isEmpty()) {
            0
        } else {
            links.indexOfFirst { it.id == defaultLinkId }.let { if (it == -1) 0 else it }
        }

    AppActionBottomSheet(
        show = show.value,
        modifier = Modifier,
        title = MLang.ProfilesPage.LinkSettings.Title,
        onDismissRequest = { show.value = false },
        enableNestedScroll = true,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = spacing.space16),
                verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
            ) {
                SectionCard(title = MLang.ProfilesPage.LinkSettings.OpenMode) {
                    PreferenceEnumItem(
                        title = MLang.ProfilesPage.LinkSettings.OpenMode,
                        currentValue = linkOpenMode,
                        items = openModeOptions,
                        values = listOf(LinkOpenMode.IN_APP, LinkOpenMode.EXTERNAL_BROWSER),
                        onValueChange = onOpenModeChange,
                    )
                }

                if (links.isNotEmpty()) {
                    SectionCard(title = MLang.ProfilesPage.LinkSettings.DefaultLink) {
                        PreferenceEnumItem(
                            title = MLang.ProfilesPage.LinkSettings.DefaultLink,
                            summary = MLang.ProfilesPage.LinkSettings.DefaultLinkSummary,
                            currentValue = links.getOrNull(defaultLinkIndex)?.id ?: "",
                            items = links.map { it.name },
                            values = links.map { it.id },
                            onValueChange = onDefaultLinkChange,
                        )
                    }
                }

                if (links.isNotEmpty()) {
                    SectionCard(title = MLang.ProfilesPage.LinkSettings.Title) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            links.forEachIndexed { index, link ->
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clickable { onOpenLink(link) }
                                            .padding(
                                                horizontal = spacing.space16,
                                                vertical = spacing.space12,
                                            ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = link.name, style = MiuixTheme.textStyles.body1)
                                        Text(
                                            text = link.url,
                                            style = MiuixTheme.textStyles.body2,
                                            color =
                                                MiuixTheme.colorScheme.onSurface.copy(
                                                    alpha = opacity.secondaryText
                                                ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    IconButton(onClick = { onDeleteLink(link.id) }) {
                                        Icon(
                                            imageVector = MiuixIcons.Delete,
                                            contentDescription = "Delete",
                                            tint = MiuixTheme.colorScheme.error,
                                        )
                                    }
                                }

                                if (index < links.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = spacing.space16),
                                        thickness = componentSizes.thinDividerThickness,
                                        color =
                                            MiuixTheme.colorScheme.outline.copy(
                                                alpha = opacity.outline
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space12),
                ) {
                    TextButton(
                        text = MLang.ProfilesPage.LinkSettings.Close,
                        onClick = { show.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onAddLink,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(
                            MLang.ProfilesPage.LinkSettings.AddLink,
                            color = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        },
    )
}

@Composable
internal fun AddLinkDialog(
    show: MutableState<Boolean>,
    linkToEdit: ProfileLink?,
    linkName: String,
    onNameChange: (String) -> Unit,
    linkUrl: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var error by remember { mutableStateOf("") }
    var currentName by remember {
        mutableStateOf(TextFieldValue(linkName, TextRange(linkName.length)))
    }
    var currentUrl by remember {
        mutableStateOf(TextFieldValue(linkUrl, TextRange(linkUrl.length)))
    }

    LaunchedEffect(show.value, linkToEdit) {
        if (show.value) {
            if (linkToEdit != null) {
                currentName = TextFieldValue(linkToEdit.name, TextRange(linkToEdit.name.length))
                currentUrl = TextFieldValue(linkToEdit.url, TextRange(linkToEdit.url.length))
            } else {
                currentName = TextFieldValue()
                currentUrl = TextFieldValue()
            }
            error = ""
        }
    }

    AppFormDialog(
        show = show.value,
        title =
            if (linkToEdit != null) MLang.ProfilesPage.LinkSettings.EditLink
            else MLang.ProfilesPage.LinkSettings.AddLink,
        onDismissRequest = onDismiss,
        onConfirm = {
            error =
                when {
                    currentName.text.isBlank() ->
                        MLang.ProfilesPage.LinkSettings.Validation.EnterName
                    currentUrl.text.isBlank() -> MLang.ProfilesPage.LinkSettings.Validation.EnterUrl
                    !currentUrl.text.startsWith("http", ignoreCase = true) ->
                        MLang.ProfilesPage.LinkSettings.Validation.InvalidUrl
                    else -> ""
                }
            if (error.isEmpty()) {
                onNameChange(currentName.text)
                onUrlChange(currentUrl.text)
                onConfirm()
            }
        },
        error = error.ifBlank { null },
        cancelText = MLang.ProfilesPage.Button.Cancel,
        confirmText = MLang.ProfilesPage.Button.Confirm,
    ) {
        TextField(
            value = currentName,
            onValueChange = {
                currentName = it
                error = ""
            },
            label = MLang.ProfilesPage.LinkSettings.Name,
            useLabelAsPlaceholder = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TextField(
            value = currentUrl,
            onValueChange = {
                currentUrl = it
                error = ""
            },
            label = MLang.ProfilesPage.LinkSettings.Url,
            useLabelAsPlaceholder = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
