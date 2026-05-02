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

package com.github.yumelira.yumebox.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.zIndex
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.Sizes
import com.github.yumelira.yumebox.presentation.theme.Spacing
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SearchStatus.TopAppBarAnim(
    modifier: Modifier = Modifier,
    visible: Boolean = shouldCollapse(),
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 550 else 0, easing = FastOutSlowInEasing),
        label = "SearchTopBarAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .graphicsLayer { this.alpha = alpha },
    ) {
        content()
    }
}

@Composable
fun SearchStatus.SearchBox(
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp? = null,
    startPadding: Dp = Dp.Unspecified,
    endPadding: Dp = Dp.Unspecified,
    contentPadding: PaddingValues = PaddingValues(),
    collapseBar: (@Composable (SearchStatus, Dp, PaddingValues) -> Unit)? = null,
    content: @Composable (Dp) -> Unit,
) {
    val searchStatus = this
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val density = LocalDensity.current
    val isCollapsed = searchStatus.isCollapsed()
    val offsetY = remember { mutableIntStateOf(0) }
    val resolvedSearchBarTopPadding = searchBarTopPadding ?: componentSizes.searchBarTopPadding
    val resolvedStartPadding = startPadding.takeOrElse { spacing.space0 }
    val resolvedEndPadding = endPadding.takeOrElse { spacing.space0 }
    val boxHeight = remember { mutableStateOf(spacing.space0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .alpha(if (isCollapsed) 1f else 0f)
            .offset(y = contentPadding.calculateTopPadding())
            .pointerInput(searchStatus.current) {
                detectTapGestures {
                    onSearchStatusChange(searchStatus.copy(current = SearchStatus.Status.EXPANDING))
                }
            }
            .background(colorScheme.surface),
    ) {
        SearchBoxCollapsedBar(
            searchStatus = searchStatus,
            searchBarTopPadding = resolvedSearchBarTopPadding,
            startPadding = resolvedStartPadding,
            endPadding = resolvedEndPadding,
            contentPadding = contentPadding,
            collapseBar = collapseBar,
            onMeasured = { coordinates ->
                coordinates.positionInWindow().y.apply {
                    offsetY.intValue = (this * 0.9f).toInt()
                    with(density) {
                        val newOffsetY = this@apply.toDp()
                        val newBoxHeight = coordinates.size.height.toDp()
                        if (searchStatus.offsetY != newOffsetY) {
                            onSearchStatusChange(searchStatus.copy(offsetY = newOffsetY))
                        }
                        boxHeight.value = newBoxHeight
                    }
                }
            },
        )
        SearchBoxContentLayer(
            shouldCollapsed = searchStatus.shouldCollapse(),
            offsetY = offsetY.intValue,
            boxHeight = boxHeight.value,
            content = content,
        )
    }
}

@Composable
private fun SearchBoxCollapsedBar(
    searchStatus: SearchStatus,
    searchBarTopPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    contentPadding: PaddingValues,
    collapseBar: (@Composable (SearchStatus, Dp, PaddingValues) -> Unit)?,
    onMeasured: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit,
) {
    Box(
        modifier = Modifier.onGloballyPositioned(onMeasured),
    ) {
        val collapsedBar = collapseBar ?: { collapsedSearchStatus: SearchStatus, topPadding: Dp, innerPadding: PaddingValues ->
            SearchBarCollapsed(
                label = collapsedSearchStatus.label,
                searchBarTopPadding = topPadding,
                startPadding = startPadding,
                endPadding = endPadding,
                innerPadding = innerPadding,
            )
        }
        collapsedBar(searchStatus, searchBarTopPadding, contentPadding)
    }
}

@Composable
private fun SearchBoxContentLayer(
    shouldCollapsed: Boolean,
    offsetY: Int,
    boxHeight: Dp,
    content: @Composable (Dp) -> Unit,
) {
    AnimatedVisibility(
        visible = shouldCollapsed,
        enter = fadeIn(tween(300, easing = LinearOutSlowInEasing)) +
            slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { -offsetY },
        exit = fadeOut(tween(300, easing = LinearOutSlowInEasing)) +
            slideOutVertically(tween(300, easing = LinearOutSlowInEasing)) { -offsetY },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f),
        ) {
            content(boxHeight)
        }
    }
}

@Composable
fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    defaultResult: @Composable () -> Unit = {},
    emptyResult: @Composable () -> Unit = {},
    searchBarTopPadding: Dp? = null,
    startPadding: Dp = Dp.Unspecified,
    endPadding: Dp = Dp.Unspecified,
    result: @Composable () -> Unit,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val resolvedSearchBarTopPadding = searchBarTopPadding ?: componentSizes.searchBarTopPadding
    val resolvedStartPadding = startPadding.takeOrElse { spacing.space0 }
    val resolvedEndPadding = endPadding.takeOrElse { spacing.space0 }

    val searchStatus = this
    val isCollapsed = searchStatus.isCollapsed()
    val isExpanded = searchStatus.isExpanded()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topPadding by animateDpAsState(
        targetValue = if (searchStatus.shouldExpand()) {
            systemBarsPadding + componentSizes.listItemVerticalMinimal
        } else {
            max(searchStatus.offsetY, spacing.space0)
        },
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchTopPadding",
        finishedListener = {
            onSearchStatusChange(searchStatus.onAnimationComplete())
        },
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "SearchSurfaceAlpha",
    )

    BackHandler(enabled = !isCollapsed) {
        onSearchStatusChange(
            searchStatus.copy(
                searchText = "",
                current = SearchStatus.Status.COLLAPSING,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .background(colorScheme.surface.copy(alpha = surfaceAlpha))
            .then(
                if (!isCollapsed) {
                    Modifier.pointerInput(searchStatus.current) {}
                } else {
                    Modifier
                },
            ),
    ) {
        SearchPagerTopRow(
            searchStatus = searchStatus,
            onSearchStatusChange = onSearchStatusChange,
            topPadding = topPadding,
            searchBarTopPadding = resolvedSearchBarTopPadding,
            startPadding = resolvedStartPadding,
            endPadding = resolvedEndPadding,
        )
        SearchPagerResultsLayer(
            searchStatus = searchStatus,
            isExpanded = isExpanded,
            defaultResult = defaultResult,
            emptyResult = emptyResult,
            result = result,
        )
    }
}

@Composable
private fun SearchPagerTopRow(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    topPadding: Dp,
    searchBarTopPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
) {
    val isCollapsed = searchStatus.isCollapsed()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .then(
                if (!isCollapsed) {
                    Modifier.background(colorScheme.surface)
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isCollapsed) {
            SearchBar(
                searchStatus = searchStatus,
                onSearchStatusChange = onSearchStatusChange,
                searchBarTopPadding = searchBarTopPadding,
                startPadding = startPadding,
                endPadding = endPadding,
                modifier = Modifier.weight(1f),
            )
        }

        SearchPagerCancelButton(
            searchStatus = searchStatus,
            onSearchStatusChange = onSearchStatusChange,
            searchBarTopPadding = searchBarTopPadding,
        )
    }
}

@Composable
private fun SearchPagerCancelButton(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp,
) {
    val spacing = AppTheme.spacing
    val isExpanded = searchStatus.isExpanded() || searchStatus.isExpanding()
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
        exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
    ) {
        Text(
            text = MLang.Component.Button.Cancel,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            modifier = Modifier
                .padding(
                    start = spacing.space4,
                    end = spacing.space16,
                    top = searchBarTopPadding,
                    bottom = spacing.space6,
                )
                .clickable(
                    interactionSource = null,
                    indication = null,
                    enabled = searchStatus.isExpanded(),
                ) {
                    onSearchStatusChange(
                        searchStatus.copy(
                            searchText = "",
                            current = SearchStatus.Status.COLLAPSING,
                        ),
                    )
                },
        )
    }
}

@Composable
private fun SearchPagerResultsLayer(
    searchStatus: SearchStatus,
    isExpanded: Boolean,
    defaultResult: @Composable () -> Unit,
    emptyResult: @Composable () -> Unit,
    result: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = isExpanded,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        when (searchStatus.resultStatus) {
            SearchStatus.ResultStatus.DEFAULT -> defaultResult()
            SearchStatus.ResultStatus.EMPTY -> emptyResult()
            SearchStatus.ResultStatus.SHOW -> result()
        }
    }
}

@Composable
private fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchStatus.searchText)) }

    LaunchedEffect(searchStatus.searchText) {
        if (textFieldValue.text != searchStatus.searchText) {
            textFieldValue = TextFieldValue(searchStatus.searchText)
        }
    }

    LaunchedEffect(searchStatus.current) {
        if (searchStatus.isExpanding()) {
            focusRequester.requestFocus()
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onSearchStatusChange(searchStatus.copy(searchText = it.text))
        },
        singleLine = true,
        textStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .padding(top = searchBarTopPadding, bottom = componentSizes.searchBarBottomPadding)
            .heightIn(min = componentSizes.searchFieldMinHeight)
            .background(colorScheme.secondaryContainer, CircleShape)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBarLeadingIcon(componentSizes = componentSizes, spacing = spacing)
                Box(modifier = Modifier.weight(1f)) {
                    innerTextField()
                }
                SearchBarClearButton(
                    searchText = searchStatus.searchText,
                    onClear = {
                        textFieldValue = TextFieldValue("")
                        onSearchStatusChange(searchStatus.copy(searchText = ""))
                    },
                    componentSizes = componentSizes,
                    spacing = spacing,
                )
            }
        },
    )
}

@Composable
private fun SearchBarLeadingIcon(
    componentSizes: Sizes,
    spacing: Spacing,
) {
    Icon(
        imageVector = MiuixIcons.Basic.Search,
        contentDescription = MLang.Component.Editor.Action.Search,
        modifier = Modifier
            .size(componentSizes.searchIconTouchTarget)
            .padding(start = spacing.space16, end = spacing.space8),
        tint = colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun SearchBarClearButton(
    searchText: String,
    onClear: () -> Unit,
    componentSizes: Sizes,
    spacing: Spacing,
) {
    AnimatedVisibility(
        visible = searchText.isNotEmpty(),
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Icon(
            imageVector = MiuixIcons.Basic.SearchCleanup,
            contentDescription = MLang.Component.Button.Clear,
            tint = colorScheme.onSurface,
            modifier = Modifier
                .size(componentSizes.searchIconTouchTarget)
                .padding(start = spacing.space8, end = spacing.space16)
                .clickable(
                    interactionSource = null,
                    indication = null,
                ) {
                    onClear()
                },
        )
    }
}

@Composable
private fun SearchBarCollapsed(
    label: String,
    searchBarTopPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    innerPadding: PaddingValues = PaddingValues(),
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    val layoutDirection = LocalLayoutDirection.current
    InputField(
        query = "",
        onQueryChange = {},
        label = label,
        leadingIcon = {
            Icon(
                imageVector = MiuixIcons.Basic.Search,
                contentDescription = MLang.Component.Editor.Action.Search,
                modifier = Modifier
                    .size(componentSizes.searchIconTouchTarget)
                    .padding(start = spacing.space16, end = spacing.space8),
                tint = colorScheme.onSurfaceVariantSummary,
            )
        },
        modifier = Modifier
            .background(colorScheme.surface)
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            )
            .padding(top = searchBarTopPadding, bottom = componentSizes.searchBarBottomPadding),
        onSearch = {},
        enabled = false,
        expanded = false,
        onExpandedChange = {},
    )
}
