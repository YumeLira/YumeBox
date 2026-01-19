package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Stable
class BottomBarScrollBehavior {
    var isBottomBarVisible by mutableStateOf(true)
        private set

    var isAutoHideEnabled by mutableStateOf(true)

    private val scrollThreshold = 12f

    private var lastToggleTime = 0L
    private val toggleDelay = 150L

    private var accumulatedScroll = 0f

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!isAutoHideEnabled) return Offset.Zero
            if (source != NestedScrollSource.Drag) return Offset.Zero

            val delta = available.y

            if (kotlin.math.abs(delta) < 0.5f) return Offset.Zero

            accumulatedScroll += delta

            if (kotlin.math.abs(accumulatedScroll) >= scrollThreshold) {
                if (accumulatedScroll < 0) {
                    hideBottomBar()
                } else {
                    showBottomBar()
                }
                accumulatedScroll = 0f
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (!isAutoHideEnabled) return Offset.Zero
            if (source != NestedScrollSource.Drag) return Offset.Zero

            if (consumed.y != 0f) {
                val delta = consumed.y
                accumulatedScroll += delta

                if (kotlin.math.abs(accumulatedScroll) >= scrollThreshold) {
                    if (accumulatedScroll < 0) {
                        hideBottomBar()
                    } else {
                        showBottomBar()
                    }
                    accumulatedScroll = 0f
                }
            }
            return Offset.Zero
        }
    }

    fun showBottomBar() {
        val currentTime = System.currentTimeMillis()
        if (!isBottomBarVisible && currentTime - lastToggleTime >= toggleDelay) {
            isBottomBarVisible = true
            lastToggleTime = currentTime
        }
    }

    fun hideBottomBar() {
        val currentTime = System.currentTimeMillis()
        if (isBottomBarVisible && currentTime - lastToggleTime >= toggleDelay) {
            isBottomBarVisible = false
            lastToggleTime = currentTime
        }
    }
}

@Composable
fun rememberBottomBarScrollBehavior(
    autoHideEnabled: Boolean = true
): BottomBarScrollBehavior {
    return remember(autoHideEnabled) {
        BottomBarScrollBehavior().apply {
            isAutoHideEnabled = autoHideEnabled
        }
    }
}

@Composable
fun BottomBarScrollBehavior.withLazyListState(
    listState: LazyListState
): BottomBarScrollBehavior {
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            showBottomBar()
        }
    }

    val isScrollingUp by remember {
        derivedStateOf {
            if (listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.first()
                firstVisibleItem.index == 0 && firstVisibleItem.offset == 0
            } else false
        }
    }

    LaunchedEffect(isScrollingUp) {
        if (isScrollingUp) {
            showBottomBar()
        }
    }

    return this
}

val LocalBottomBarScrollBehavior = compositionLocalOf<BottomBarScrollBehavior?> {
    null
}
