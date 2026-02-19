package com.github.yumelira.yumebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.Speed
import com.github.yumelira.yumebox.presentation.screen.node.NodeGroupSheetContent
import com.github.yumelira.yumebox.presentation.screen.node.NodeSheetContent
import com.github.yumelira.yumebox.presentation.screen.node.NodeSortPopup
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.presentation.util.WindowBlurEffect
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

private const val POPUP_ANIMATION_DURATION_MS = 320

class ProxySheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value
            val themeSeedColorArgb = appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value

            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    colorTheme = colorTheme,
                    themeSeedColorArgb = themeSeedColorArgb,
                ) {
                    ProxySheet(
                        onDismiss = {
                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxySheet(
    onDismiss: () -> Unit,
    proxyViewModel: ProxyViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val sheetHeightFraction by proxyViewModel.sheetHeightFraction.collectAsState()

    val showSheet = rememberSaveable { mutableStateOf(true) }
    val showSortPopup = rememberSaveable { mutableStateOf(false) }
    var selectedGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    val blurRadius by animateIntAsState(
        targetValue = if (showSheet.value) 30 else 0,
        animationSpec = tween(durationMillis = POPUP_ANIMATION_DURATION_MS, easing = AnimationSpecs.Legacy),
        label = "notification_proxy_popup_blur",
    )

    val groupsByName = remember(proxyGroups) { proxyGroups.associateBy { it.name } }
    val selectedGroup by remember(selectedGroupName, groupsByName) {
        derivedStateOf {
            val name = selectedGroupName ?: return@derivedStateOf null
            groupsByName[name]
        }
    }

    DisposableEffect(Unit) {
        proxyViewModel.ensureCoreLoaded(true)
        onDispose {
            proxyViewModel.ensureCoreLoaded(false)
        }
    }

    LaunchedEffect(proxyGroups, selectedGroupName) {
        if (selectedGroupName != null && selectedGroup == null) {
            selectedGroupName = null
        }
    }

    val dismissSheet = remember(onDismiss) {
        {
            showSortPopup.value = false
            showSheet.value = false
            scope.launch {
                kotlinx.coroutines.delay(POPUP_ANIMATION_DURATION_MS.toLong())
                onDismiss()
            }
        }
    }

    WindowBlurEffect(useBlur = true, blurRadius = blurRadius)

    WindowBottomSheet(
        show = showSheet,
        title = selectedGroup?.name ?: MLang.Proxy.Title,
        startAction = {
            if (selectedGroup != null) {
                IconButton(onClick = { selectedGroupName = null }) {
                    Icon(MiuixIcons.Back, contentDescription = MLang.Component.Navigation.Back)
                }
            } else {
                IconButton(onClick = { showSortPopup.value = true }) {
                    Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Settings.SortMode)
                }

                NodeSortPopup(
                    show = showSortPopup,
                    onDismiss = { showSortPopup.value = false },
                    sortMode = sortMode,
                    onSortSelected = proxyViewModel::setSortMode,
                )
            }
        },
        endAction = {
            IconButton(
                onClick = {
                    val group = selectedGroup
                    if (group == null) {
                        proxyViewModel.testDelay()
                    } else {
                        proxyViewModel.testDelay(group.name)
                    }
                },
            ) {
                Icon(Yume.Speed, contentDescription = MLang.Proxy.Action.Test)
            }
        },
        onDismissRequest = {
            dismissSheet()
        },
        insideMargin = DpSize(16.dp, 16.dp),
    ) {
        AnimatedContent(
            targetState = selectedGroupName,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 260, easing = AnimationSpecs.Legacy),
                        initialOffsetX = { it },
                    ) + fadeIn(animationSpec = tween(durationMillis = 220))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = AnimationSpecs.Legacy),
                            targetOffsetX = { -it / 3 },
                        ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 220, easing = AnimationSpecs.Legacy),
                        initialOffsetX = { -it / 3 },
                    ) + fadeIn(animationSpec = tween(durationMillis = 200))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(durationMillis = 240, easing = AnimationSpecs.Legacy),
                            targetOffsetX = { it },
                        ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                }
            },
            label = "notification_node_sheet_content",
        ) { targetGroupName ->
            val group = targetGroupName?.let(groupsByName::get)
            if (group == null) {
                NodeGroupSheetContent(
                    groups = proxyGroups,
                    displayMode = displayMode,
                    onGroupClick = { group ->
                        selectedGroupName = group.name
                    },
                    onGroupDelayClick = { group ->
                        proxyViewModel.testDelay(group.name)
                    },
                    testingGroupNames = testingGroupNames,
                    sheetHeightFraction = sheetHeightFraction,
                )
            } else {
                NodeSheetContent(
                    group = group,
                    displayMode = displayMode,
                    isDelayTesting = testingGroupNames.contains(group.name),
                    onSelectProxy = { proxyName ->
                        if (group.type == Proxy.Type.Selector) {
                            proxyViewModel.selectProxy(group.name, proxyName)
                        } else {
                            proxyViewModel.testDelay(group.name)
                        }
                    },
                    onTestDelay = {
                        proxyViewModel.testDelay(group.name)
                    },
                    sheetHeightFraction = sheetHeightFraction,
                )
            }
        }
    }
}

