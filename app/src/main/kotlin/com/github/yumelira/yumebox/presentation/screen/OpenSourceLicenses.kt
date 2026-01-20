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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Destination<RootGraph>
fun OpenSourceLicensesScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val showLicenseSheet = remember { mutableStateOf(false) }
    val selectedLibrary = remember { mutableStateOf<Library?>(null) }

    val libraries by produceLibraries {
        Thread.currentThread().contextClassLoader?.getResourceAsStream("aboutlibraries.json")
            ?.bufferedReader()?.readText() ?: "{}"
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = MLang.OpenSourceLicenses.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            libraries?.libraries?.let { libs ->
                items(libs.size) { index ->
                    val library = libs[index]
                    LibraryItem(
                        library = library,
                        onClick = {
                            selectedLibrary.value = library
                            showLicenseSheet.value = true
                        }
                    )
                }
            }
        }
    }

    selectedLibrary.value?.let { library ->
        LicenseBottomSheet(
            show = showLicenseSheet,
            library = library,
            onDismiss = {
                showLicenseSheet.value = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryItem(
    library: Library,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = library.name,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            library.artifactVersion?.let { version ->
                Text(
                    text = version,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        library.developers.firstOrNull()?.name?.let { author ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = author,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        if (library.licenses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                library.licenses.forEach { license ->
                    LicenseChip(licenseName = license.name)
                }
            }
        }
    }
}

@Composable
private fun LicenseChip(licenseName: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = licenseName,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun LicenseBottomSheet(
    show: MutableState<Boolean>,
    library: Library,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val licenseContent = remember(library) { library.strippedLicenseContent.takeIf { it.isNotEmpty() } }

    WindowBottomSheet(
        show = show,
        title = library.name,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (licenseContent != null) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 450.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = licenseContent,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = MLang.OpenSourceLicenses.LicenseSheet.NoContent,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(MLang.OpenSourceLicenses.LicenseSheet.Confirm, color = MiuixTheme.colorScheme.onPrimary)
            }
        }
    }
}
