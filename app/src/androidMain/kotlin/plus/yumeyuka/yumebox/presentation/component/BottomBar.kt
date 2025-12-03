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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Arrow-down-up`
import com.github.yumelira.yumebox.presentation.icon.yume.Bolt
import com.github.yumelira.yumebox.presentation.icon.yume.House
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.icon.yume.Play
import com.github.yumelira.yumebox.presentation.icon.yume.Square
import com.github.yumelira.yumebox.presentation.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.oom_wg.purejoy.mlang.MLang

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("LocalHandlePageChange is not provided") }
val LocalNavigator = compositionLocalOf<DestinationsNavigator> { error("LocalNavigator is not provided") }

object BottomBarVisibility {
    private val _isVisible = MutableStateFlow(true)
    val isVisible: StateFlow<Boolean> = _isVisible
    
    fun show() { _isVisible.value = true }
    fun hide() { _isVisible.value = false }
    fun toggle(visible: Boolean) { _isVisible.value = visible }
}

@Composable
fun BottomBar(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
) {
    val page = LocalPagerState.current.targetPage
    val handlePageChange = LocalHandlePageChange.current
    val isVisible by BottomBarVisibility.isVisible.collectAsState()

    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = destination.label,
            icon = destination.icon,
        )
    }

    val onItemClick: (Int) -> Unit = { index ->
        handlePageChange(index)
    }


    val bottomOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 120.dp,
        animationSpec = tween(durationMillis = 300),
        label = "BottomBarOffset"
    )

    Box(
        modifier = Modifier.offset(y = bottomOffset)
    ) {
        NavigationBarWithCenterButton(
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            items = items,
            selected = page,
            onClick = onItemClick,
        )
    }
}

enum class BottomBarDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home(MLang.Component.BottomBar.Home, Yume.House),
    Proxy(MLang.Component.BottomBar.Proxy, Yume.`Arrow-down-up`),
    Config(MLang.Component.BottomBar.Config, Yume.`Package-check`),
    Setting(MLang.Component.BottomBar.Setting, Yume.Bolt),
}

@Composable
private fun NavigationBarWithCenterButton(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    items: List<NavigationItem>,
    selected: Int,
    onClick: (Int) -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val displayRunning by homeViewModel.displayRunning.collectAsState()
    val isToggling by homeViewModel.isToggling.collectAsState()
    val profiles by homeViewModel.profiles.collectAsState()
    val recommendedProfile by homeViewModel.recommendedProfile.collectAsState()
    val hasEnabledProfile by homeViewModel.hasEnabledProfile.collectAsState(initial = false)

    val canToggle = !isToggling && (displayRunning || (profiles.isNotEmpty() && hasEnabledProfile))

    val leftItems = items.take(2)
    val rightItems = items.drop(2)

    val navBarHeight = 56.dp
    val buttonSize = 56.dp
    val buttonOverhang = 16.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
            .height(navBarHeight + buttonOverhang),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(navBarHeight)
                .clip(RoundedCornerShape(28.dp))
                .hazeEffect(hazeState) {
                    style = hazeStyle
                    blurRadius = 30.dp
                    noiseFactor = 0f
                }
                .background(MiuixTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftItems.forEachIndexed { index, item ->
                NavigationItemView(
                    item = item,
                    isSelected = selected == index,
                    onClick = { onClick(index) },
                    modifier = Modifier.weight(1f)
                )
            }


            Spacer(modifier = Modifier.width(buttonSize + 16.dp))

            rightItems.forEachIndexed { index, item ->
                val actualIndex = index + 2
                NavigationItemView(
                    item = item,
                    isSelected = selected == actualIndex,
                    onClick = { onClick(actualIndex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize)
                .clip(CircleShape)
                .background(
                    if (displayRunning) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.primaryVariant
                )
                .clickable(enabled = canToggle) {
                    if (displayRunning) {
                        homeViewModel.stopProxy()
                    } else {
                        recommendedProfile?.let { profile ->
                            homeViewModel.startProxy(profile.id)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (displayRunning) Yume.Square else Yume.Play,
                contentDescription = if (displayRunning) MLang.Home.Control.Stop else MLang.Home.Control.Start,
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun NavigationItemView(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (isSelected) MiuixTheme.colorScheme.primary
                   else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MiuixTheme.textStyles.footnote1,
            color = if (isSelected) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}
