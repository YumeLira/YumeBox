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
import com.github.yumelira.yumebox.presentation.theme.UiDp
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class SearchStatus(
    val label: String,
    val searchText: String = "",
    val current: Status = Status.COLLAPSED,
    val offsetY: Dp = UiDp.dp0,
    val resultStatus: ResultStatus = ResultStatus.DEFAULT,
) {
    fun isExpanded() = current == Status.EXPANDED
    fun isCollapsed() = current == Status.COLLAPSED
    fun shouldExpand() = current == Status.EXPANDED || current == Status.EXPANDING
    fun shouldCollapse() = current == Status.COLLAPSED || current == Status.COLLAPSING
    fun isExpanding() = current == Status.EXPANDING

    fun onAnimationComplete(): SearchStatus = when (current) {
        Status.EXPANDING -> copy(current = Status.EXPANDED)
        Status.COLLAPSING -> copy(searchText = "", current = Status.COLLAPSED)
        else -> this
    }

    enum class Status {
        EXPANDED,
        EXPANDING,
        COLLAPSED,
        COLLAPSING,
    }

    enum class ResultStatus {
        DEFAULT,
        EMPTY,
        SHOW,
    }
}
