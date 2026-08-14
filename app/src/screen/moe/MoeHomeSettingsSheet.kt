/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import tf.gal.yumebox.locale.YumeTxt

@Composable
internal fun MoeHomeSettingsSheet(
    show: Boolean,
    quote: String,
    classicHomeEnabled: Boolean,
    sidebarExpanded: Boolean,
    useSystemWallpaper: Boolean,
    onQuoteChange: (String) -> Unit,
    onClassicHomeEnabledChange: (Boolean) -> Unit,
    onSidebarExpandedChange: (Boolean) -> Unit,
    onUseSystemWallpaperChange: (Boolean) -> Unit,
    onChangeWallpaper: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = AppTheme.spacing
    var draftQuote by remember(show, quote) { mutableStateOf(quote) }
    var draftClassicHomeEnabled by
    remember(show, classicHomeEnabled) {
        mutableStateOf(classicHomeEnabled)
    }
    var draftSidebarExpanded by remember(show, sidebarExpanded) { mutableStateOf(sidebarExpanded) }
    val save = {
        onQuoteChange(draftQuote)
        onClassicHomeEnabledChange(draftClassicHomeEnabled)
        onSidebarExpandedChange(draftSidebarExpanded)
        onDismiss()
    }

    AppActionBottomSheet(
        show = show,
        title = YumeTxt.Home.Settings.Title,
        startAction = { AppBottomSheetCloseAction(onClick = onDismiss) },
        endAction = { AppBottomSheetConfirmAction(onClick = save) },
        onDismissRequest = onDismiss,
        enableNestedScroll = true,
    ) {
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.space16)) {
            item {
                OemTextField(
                    value = draftQuote,
                    onValueChange = { draftQuote = it },
                    label = YumeTxt.Home.Settings.Quote,
                    useLabelAsPlaceholder = true,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.space12),
                )
            }
            item {
                AppCard(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.space12),
                    applyHorizontalPadding = false,
                ) {
                    Column {
                        PreferenceSwitchItem(
                            title = YumeTxt.Home.Settings.ClassicHome,
                            checked = draftClassicHomeEnabled,
                            onCheckedChange = { draftClassicHomeEnabled = it },
                        )
                        PreferenceSwitchItem(
                            title = YumeTxt.Home.Settings.ExpandSidebar,
                            checked = draftSidebarExpanded,
                            onCheckedChange = { draftSidebarExpanded = it },
                        )
                        SystemWallpaperPreferenceItem(
                            checked = useSystemWallpaper,
                            onCheckedChange = onUseSystemWallpaperChange,
                        )
                        PreferenceArrowItem(
                            title = YumeTxt.Home.Settings.ChangeWallpaper,
                            onClick = onChangeWallpaper,
                        )
                    }
                }
            }
        }
    }
}
