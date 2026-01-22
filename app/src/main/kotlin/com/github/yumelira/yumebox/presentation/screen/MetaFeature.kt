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

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.ConfirmDialog
import com.github.yumelira.yumebox.presentation.component.NullableBooleanSelector
import com.github.yumelira.yumebox.presentation.component.NullableEnumSelector
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SmallTitle
import com.github.yumelira.yumebox.presentation.component.StringListInput
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import java.io.File
import java.io.FileOutputStream

@Composable
@Destination<RootGraph>
fun MetaFeatureScreen(navigator: DestinationsNavigator) {
    val viewModel: OverrideViewModel = koinViewModel()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val configuration by viewModel.configuration.collectAsState()
    val showResetDialog = remember { mutableStateOf(false) }

    var pendingGeoFileType by remember { mutableStateOf<GeoFileType?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val fileType = pendingGeoFileType ?: return@rememberLauncherForActivityResult
        pendingGeoFileType = null

        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                var fileName = "unknown"
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = it.getString(nameIndex)
                        }
                    }
                }

                val ext = "." + fileName.substringAfterLast(".")
                val validExtensions = listOf(".metadb", ".db", ".dat", ".mmdb", ".bin")

                if (ext !in validExtensions) {
                    Toast.makeText(
                        context,
                        MLang.MetaFeature.Message.UnsupportedFormat.format(validExtensions.joinToString("/")),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val outputFileName = when (fileType) {
                    GeoFileType.GeoIP -> "geoip$ext"
                    GeoFileType.GeoSite -> "geosite$ext"
                    GeoFileType.Country -> "country$ext"
                    GeoFileType.ASN -> "ASN$ext"
                    GeoFileType.Model -> "Model.bin"
                }

                withContext(Dispatchers.IO) {
                    val clashDir = context.filesDir.resolve("clash")
                    clashDir.mkdirs()
                    val outputFile = File(clashDir, outputFileName)

                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                Toast.makeText(
                    context,
                    MLang.MetaFeature.Message.Imported.format(fileName),
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    MLang.MetaFeature.Message.ImportFailed.format(e.message ?: MLang.Util.Error.UnknownError),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.MetaFeature.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 24.dp), onClick = { showResetDialog.value = true }) {
                        Icon(MiuixIcons.Reset, contentDescription = MLang.Component.Navigation.Refresh)
                    }
                },
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                SmallTitle(MLang.MetaFeature.Section.CoreSettings)
                Card {
                    NullableBooleanSelector(
                        title = MLang.MetaFeature.Core.UnifiedDelayTitle,
                        summary = MLang.MetaFeature.Core.UnifiedDelaySummary,
                        value = configuration.unifiedDelay,
                        onValueChange = { viewModel.setUnifiedDelay(it) },
                    )
                    NullableBooleanSelector(
                        title = MLang.MetaFeature.Core.GeodataModeTitle,
                        summary = MLang.MetaFeature.Core.GeodataModeSummary,
                        value = configuration.geodataMode,
                        onValueChange = { viewModel.setGeodataMode(it) },
                    )
                    NullableBooleanSelector(
                        title = MLang.MetaFeature.Core.TcpConcurrentTitle,
                        summary = MLang.MetaFeature.Core.TcpConcurrentSummary,
                        value = configuration.tcpConcurrent,
                        onValueChange = { viewModel.setTcpConcurrent(it) },
                    )
                    NullableEnumSelector(
                        title = MLang.MetaFeature.Core.FindProcessModeTitle,
                        value = configuration.findProcessMode,
                        items = listOf(
                            MLang.MetaFeature.Core.FindProcessNotModify,
                            MLang.MetaFeature.Core.FindProcessOff,
                            MLang.MetaFeature.Core.FindProcessStrict,
                            MLang.MetaFeature.Core.FindProcessAlways
                        ),
                        values = listOf(
                            null,
                            ConfigurationOverride.FindProcessMode.Off,
                            ConfigurationOverride.FindProcessMode.Strict,
                            ConfigurationOverride.FindProcessMode.Always
                        ),
                        onValueChange = { viewModel.setFindProcessMode(it) },
                    )
                }
            }

            item {
                SmallTitle(MLang.MetaFeature.Section.Sniffer)
                Card {
                    NullableEnumSelector(
                        title = MLang.MetaFeature.Sniffer.EnableTitle,
                        value = configuration.sniffer.enable,
                        items = listOf(
                            MLang.MetaFeature.Sniffer.EnableNotModify,
                            MLang.MetaFeature.Sniffer.EnableOn,
                            MLang.MetaFeature.Sniffer.EnableOff
                        ),
                        values = listOf(null, true, false),
                        onValueChange = { viewModel.setSnifferEnable(it) },
                    )
                    if (configuration.sniffer.enable != false) {
                        StringListInput(
                            title = MLang.MetaFeature.Sniffer.HttpPorts,
                            value = configuration.sniffer.sniff.http.ports,
                            placeholder = MLang.MetaFeature.Sniffer.HttpPortsHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferHttpPorts(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.MetaFeature.Sniffer.HttpOverride,
                            value = configuration.sniffer.sniff.http.overrideDestination,
                            onValueChange = { viewModel.setSnifferHttpOverride(it) },
                        )
                        StringListInput(
                            title = MLang.MetaFeature.Sniffer.TlsPorts,
                            value = configuration.sniffer.sniff.tls.ports,
                            placeholder = MLang.MetaFeature.Sniffer.TlsPortsHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferTlsPorts(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.MetaFeature.Sniffer.TlsOverride,
                            value = configuration.sniffer.sniff.tls.overrideDestination,
                            onValueChange = { viewModel.setSnifferTlsOverride(it) },
                        )
                        StringListInput(
                            title = MLang.MetaFeature.Sniffer.QuicPorts,
                            value = configuration.sniffer.sniff.quic.ports,
                            placeholder = MLang.MetaFeature.Sniffer.QuicPortsHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferQuicPorts(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.MetaFeature.Sniffer.QuicOverride,
                            value = configuration.sniffer.sniff.quic.overrideDestination,
                            onValueChange = { viewModel.setSnifferQuicOverride(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.MetaFeature.Sniffer.ForceDnsMapping,
                            value = configuration.sniffer.forceDnsMapping,
                            onValueChange = { viewModel.setSnifferForceDnsMapping(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.MetaFeature.Sniffer.ParsePureIp,
                            value = configuration.sniffer.parsePureIp,
                            onValueChange = { viewModel.setSnifferParsePureIp(it) },
                        )
                        NullableBooleanSelector(
                            title = MLang.MetaFeature.Sniffer.OverrideDestination,
                            value = configuration.sniffer.overrideDestination,
                            onValueChange = { viewModel.setSnifferOverrideDestination(it) },
                        )
                        StringListInput(
                            title = MLang.MetaFeature.Sniffer.ForceDomain,
                            value = configuration.sniffer.forceDomain,
                            placeholder = MLang.MetaFeature.Sniffer.ForceDomainHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferForceDomain(it) },
                        )
                        StringListInput(
                            title = MLang.MetaFeature.Sniffer.SkipDomain,
                            value = configuration.sniffer.skipDomain,
                            placeholder = MLang.MetaFeature.Sniffer.SkipDomainHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferSkipDomain(it) },
                        )
                        StringListInput(
                            title = MLang.MetaFeature.Sniffer.SkipSrcAddress,
                            value = configuration.sniffer.skipSrcAddress,
                            placeholder = MLang.MetaFeature.Sniffer.SkipSrcAddressHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferSkipSrcAddress(it) },
                        )
                        StringListInput(
                            title = MLang.MetaFeature.Sniffer.SkipDstAddress,
                            value = configuration.sniffer.skipDstAddress,
                            placeholder = MLang.MetaFeature.Sniffer.SkipDstAddressHint,
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferSkipDstAddress(it) },
                        )
                    }
                }
            }

            item {
                SmallTitle(MLang.MetaFeature.Section.GeoXFiles)
                Card {
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportGeoipTitle,
                        summary = MLang.MetaFeature.GeoX.ImportGeoipSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.GeoIP
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportGeositeTitle,
                        summary = MLang.MetaFeature.GeoX.ImportGeositeSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.GeoSite
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportCountryTitle,
                        summary = MLang.MetaFeature.GeoX.ImportCountrySummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.Country
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportAsnTitle,
                        summary = MLang.MetaFeature.GeoX.ImportAsnSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.ASN
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportModelTitle,
                        summary = MLang.MetaFeature.GeoX.ImportModelSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.Model
                            filePickerLauncher.launch("*/*")
                        },
                    )
                }
            }
        }
    }

    ConfirmDialog(
        show = showResetDialog,
        title = MLang.MetaFeature.ResetDialog.Title,
        message = MLang.MetaFeature.ResetDialog.Message,
        onConfirm = {
            viewModel.resetConfiguration()
            showResetDialog.value = false
        },
        onDismiss = { showResetDialog.value = false },
    )
}


private enum class GeoFileType {
    GeoIP, GeoSite, Country, ASN, Model
}
