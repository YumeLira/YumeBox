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
                        "不支持的文件格式，请选择 ${validExtensions.joinToString("/")}",
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

                Toast.makeText(context, "已导入: $fileName", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Meta 功能",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 24.dp), onClick = { showResetDialog.value = true }) {
                        Icon(MiuixIcons.Reset, contentDescription = "刷新")
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
                SmallTitle("核心设置")
                Card {
                    NullableBooleanSelector(
                        title = "统一延迟",
                        summary = "使用统一的延迟测试方式", value = configuration.unifiedDelay,
                        onValueChange = { viewModel.setUnifiedDelay(it) },
                    )
                    NullableBooleanSelector(
                        title = "Geodata 模式",
                        summary = "使用 dat 格式的 GeoIP/GeoSite", value = configuration.geodataMode,
                        onValueChange = { viewModel.setGeodataMode(it) },
                    )
                    NullableBooleanSelector(
                        title = "TCP 并发",
                        summary = "启用 TCP 并发连接", value = configuration.tcpConcurrent,
                        onValueChange = { viewModel.setTcpConcurrent(it) },
                    )
                    NullableEnumSelector(
                        title = "进程匹配模式",
                        value = configuration.findProcessMode,
                        items = listOf(
                            "不修改",
                            "关闭",
                            "严格",
                            "始终"
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
                SmallTitle("嗅探器")
                Card {
                    NullableEnumSelector(
                        title = "嗅探策略",
                        value = configuration.sniffer.enable,
                        items = listOf(
                            "不修改",
                            "启用",
                            "禁用"
                        ),
                        values = listOf(null, true, false),
                        onValueChange = { viewModel.setSnifferEnable(it) },
                    )
                    if (configuration.sniffer.enable != false) {
                        StringListInput(
                            title = "HTTP 嗅探端口",
                            value = configuration.sniffer.sniff.http.ports,
                            placeholder = "例如: 80, 8080-8880",
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferHttpPorts(it) },
                        )
                        NullableBooleanSelector(
                            title = "HTTP 覆盖目标",
                            value = configuration.sniffer.sniff.http.overrideDestination,
                            onValueChange = { viewModel.setSnifferHttpOverride(it) },
                        )
                        StringListInput(
                            title = "TLS 嗅探端口",
                            value = configuration.sniffer.sniff.tls.ports,
                            placeholder = "例如: 443, 8443",
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferTlsPorts(it) },
                        )
                        NullableBooleanSelector(
                            title = "TLS 覆盖目标",
                            value = configuration.sniffer.sniff.tls.overrideDestination,
                            onValueChange = { viewModel.setSnifferTlsOverride(it) },
                        )
                        StringListInput(
                            title = "QUIC 嗅探端口",
                            value = configuration.sniffer.sniff.quic.ports,
                            placeholder = "例如: 443",
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferQuicPorts(it) },
                        )
                        NullableBooleanSelector(
                            title = "QUIC 覆盖目标",
                            value = configuration.sniffer.sniff.quic.overrideDestination,
                            onValueChange = { viewModel.setSnifferQuicOverride(it) },
                        )
                        NullableBooleanSelector(
                            title = "强制 DNS 映射",
                            value = configuration.sniffer.forceDnsMapping,
                            onValueChange = { viewModel.setSnifferForceDnsMapping(it) },
                        )
                        NullableBooleanSelector(
                            title = "解析纯 IP",
                            value = configuration.sniffer.parsePureIp,
                            onValueChange = { viewModel.setSnifferParsePureIp(it) },
                        )
                        NullableBooleanSelector(
                            title = "覆盖目标地址",
                            value = configuration.sniffer.overrideDestination,
                            onValueChange = { viewModel.setSnifferOverrideDestination(it) },
                        )
                        StringListInput(
                            title = "强制嗅探域名",
                            value = configuration.sniffer.forceDomain,
                            placeholder = "例如: +.google.com",
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferForceDomain(it) },
                        )
                        StringListInput(
                            title = "跳过嗅探域名",
                            value = configuration.sniffer.skipDomain,
                            placeholder = "例如: +.baidu.com",
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferSkipDomain(it) },
                        )
                        StringListInput(
                            title = "跳过源地址",
                            value = configuration.sniffer.skipSrcAddress,
                            placeholder = "例如: 192.168.0.0/16",
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferSkipSrcAddress(it) },
                        )
                        StringListInput(
                            title = "跳过目标地址",
                            value = configuration.sniffer.skipDstAddress,
                            placeholder = "例如: 10.0.0.0/8",
                            navigator = navigator,
                            onValueChange = { viewModel.setSnifferSkipDstAddress(it) },
                        )
                    }
                }
            }

            item {
                SmallTitle("GeoX 文件")
                Card {
                    SuperArrow(
                        title = "导入 GeoIP 文件",
                        summary = "导入自定义 GeoIP 数据库",
                        onClick = {
                            pendingGeoFileType = GeoFileType.GeoIP
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = "导入 GeoSite 文件",
                        summary = "导入自定义 GeoSite 数据库",
                        onClick = {
                            pendingGeoFileType = GeoFileType.GeoSite
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = "导入 Country 文件",
                        summary = "导入自定义 Country.mmdb 数据库",
                        onClick = {
                            pendingGeoFileType = GeoFileType.Country
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = "导入 ASN 文件",
                        summary = "导入自定义 ASN 数据库",
                        onClick = {
                            pendingGeoFileType = GeoFileType.ASN
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = "导入 Model 文件",
                        summary = "导入自定义 Model 数据库",
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
        title = "重置 Meta 功能设置",
        message = "所有的 Meta 功能覆写设置将会被擦除，确定要继续吗？",
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
