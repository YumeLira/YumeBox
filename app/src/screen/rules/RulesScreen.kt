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

@file:Suppress("DuplicatedCode", "FunctionName")

package com.github.yumeyucca.yumebox.screen.rules


import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.RuntimeRule
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Runtime rules from `GET /rules`. Each card has an enable switch wired to `PATCH /rules/disable`.
 * This is **not** the custom-routing editor.
 *
 * Search chrome mirrors [com.github.yumeyucca.yumebox.screen.connection.ConnectionScreen] (collapsed
 * bar in TopBar + SearchPager overlay). Horizontal inset for cards is owned by shared [AppCard]
 * (`applyHorizontalPadding`), same as Log — not LazyColumn contentPadding.
 */
@Composable
fun RulesScreen(navigator: Navigator) {
    val density = LocalDensity.current
    val viewModel = koinViewModel<RulesViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val filteredRules by viewModel.filteredRules.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val spacing = AppTheme.spacing
    val mainLikePadding = rememberStandalonePageMainPadding()
    val mainListState = rememberLazyListState()

    var searchStatus by remember {
        mutableStateOf(SearchStatus(label = YumeTxt.Rules.SearchHint))
    }
    val dynamicTopPadding by remember {
        derivedStateOf { spacing.space12 * (1f - scrollBehavior.state.collapsedFraction) }
    }
    val listStartPadding = spacing.screenHorizontal
    val listEndPadding = spacing.screenHorizontal
    val currentSearchStatus =
        remember(searchStatus, filteredRules, uiState.searchQuery) {
            searchStatus.copy(
                resultStatus =
                    when {
                        searchStatus.searchText.isBlank() -> SearchStatus.ResultStatus.DEFAULT
                        filteredRules.isEmpty() -> SearchStatus.ResultStatus.EMPTY
                        else -> SearchStatus.ResultStatus.SHOW
                    }
            )
        }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(uiState.toggleError) {
        val error = uiState.toggleError ?: return@LaunchedEffect
        context.toast(YumeTxt.Rules.Message.ToggleFailed.replace("%s", error))
        viewModel.consumeToggleError()
    }
    LaunchedEffect(searchStatus.searchText) {
        if (searchStatus.searchText != uiState.searchQuery) {
            viewModel.setSearchQuery(searchStatus.searchText)
        }
    }
    LaunchedEffect(uiState.searchQuery) {
        if (searchStatus.searchText != uiState.searchQuery) {
            searchStatus = searchStatus.copy(searchText = uiState.searchQuery)
        }
    }

    // Same shell as ConnectionScreen / LogScreen: outer Box so SearchPager can cover the
    // scaffold (zIndex) while the collapsed bar still lives in TopBar.bottomContent.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                currentSearchStatus.TopAppBarAnim {
                    TopBar(
                        title = YumeTxt.Rules.Title,
                        scrollBehavior = scrollBehavior,
                        bottomContent = {
                            Box(
                                modifier =
                                    Modifier
                                        .alpha(
                                            if (currentSearchStatus.isCollapsed()) 1f else 0f
                                        )
                                        .onGloballyPositioned { coordinates ->
                                            with(density) {
                                                val offset = coordinates.positionInWindow().y.toDp()
                                                if (currentSearchStatus.offsetY != offset) {
                                                    searchStatus =
                                                        currentSearchStatus.copy(offsetY = offset)
                                                }
                                            }
                                        }
                                        .then(
                                            if (currentSearchStatus.isCollapsed()) {
                                                Modifier.pointerInput(currentSearchStatus.current) {
                                                    detectTapGestures {
                                                        searchStatus =
                                                            currentSearchStatus.copy(
                                                                current =
                                                                    SearchStatus.Status.EXPANDING
                                                            )
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                            ) {
                                CollapsedSearchBar(
                                    label = currentSearchStatus.label,
                                    topPadding = dynamicTopPadding,
                                    startPadding = listStartPadding,
                                    endPadding = listEndPadding,
                                )
                            }
                        },
                    )
                }
            }
        ) { innerPadding ->
            if (currentSearchStatus.shouldCollapse()) {
                when {
                    uiState.isLoading && uiState.rules.isEmpty() -> {
                        CenteredText(
                            firstLine = YumeTxt.Rules.Empty.Loading,
                            secondLine = YumeTxt.Rules.Empty.LoadingHint,
                        )
                    }

                    !uiState.isRunning && uiState.rules.isEmpty() -> {
                        CenteredText(
                            firstLine = YumeTxt.Rules.Empty.NotRunning,
                            secondLine = YumeTxt.Rules.Empty.NotRunningHint,
                        )
                    }

                    uiState.rules.isEmpty() -> {
                        CenteredText(
                            firstLine = YumeTxt.Rules.Empty.NoRules,
                            secondLine = YumeTxt.Rules.Empty.NoRulesHint,
                        )
                    }

                    else -> {
                        ScreenLazyColumn(
                            scrollBehavior = scrollBehavior,
                            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
                            lazyListState = mainListState,
                        ) {
                            items(
                                items = filteredRules,
                                key = { it.index },
                                contentType = { "rule" },
                            ) { rule ->
                                RuleCard(
                                    rule = rule,
                                    enabled = !rule.disabled,
                                    toggling = rule.index in uiState.togglingIndexes,
                                    onEnabledChange = { enabled ->
                                        viewModel.setRuleEnabled(rule.index, enabled)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Always composed — same as AccessControl. Do not gate with !isCollapsed().
        currentSearchStatus.SearchPager(
            onSearchStatusChange = { searchStatus = it },
            padding =
                SearchBarPadding(
                    top = dynamicTopPadding,
                    start = listStartPadding,
                    end = listEndPadding,
                ),
            emptyResult = {
                CenteredText(
                    firstLine = YumeTxt.Rules.Empty.NoResults,
                    secondLine = currentSearchStatus.searchText,
                )
            },
        ) {
            val searchListState = rememberLazyListState()
            LazyColumn(
                state = searchListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = spacing.space6,
                        bottom = mainLikePadding.calculateBottomPadding(),
                    ),
            ) {
                items(
                    items = filteredRules,
                    key = { it.index },
                    contentType = { "rule" },
                ) { rule ->
                    RuleCard(
                        rule = rule,
                        enabled = !rule.disabled,
                        toggling = rule.index in uiState.togglingIndexes,
                        onEnabledChange = { enabled ->
                            viewModel.setRuleEnabled(rule.index, enabled)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: RuntimeRule,
    enabled: Boolean,
    toggling: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val spacing = AppTheme.spacing
    val locale = LocalLocale.current.platformLocale
    val totalCount = rule.hitCount + rule.missCount
    val hitRate =
        if (totalCount > 0L) {
            String.format(locale, "%.1f%%", rule.hitCount * 100.0 / totalCount)
        } else {
            "-"
        }
    AppCard(modifier = Modifier.padding(vertical = spacing.space4)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space16, vertical = spacing.space12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            ) {
                Text(
                    text = rule.payload.ifBlank { "#" + rule.index },
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !toggling,
                )
            }
            Spacer(modifier = Modifier.size(spacing.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            ) {
                Text(
                    text = "${rule.type.ifBlank { "-" }}  ·  " + rule.proxy.ifBlank { "-" },
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = YumeTxt.Rules.Label.HitRate.replace("%s", hitRate),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }
    }
}
