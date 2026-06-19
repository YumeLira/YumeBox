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

package com.github.yumelira.yumebox.presentation.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.screenPadding(): Modifier {
    val s = AppTheme.spacing
    return this.padding(
        start = s.screenHorizontal,
        end = s.screenHorizontal,
        top = s.screenVertical,
        bottom = s.screenVertical,
    )
}

@SuppressLint("SuspiciousModifierThen")
@Composable
fun Modifier.sectionVSpacing(top: Boolean = true, bottom: Boolean = true): Modifier {
    val s = AppTheme.spacing
    return this.then(
        Modifier.padding(
            top = if (top) s.space16 else UiDp.dp0,
            bottom = if (bottom) s.space16 else UiDp.dp0,
        )
    )
}

@SuppressLint("SuspiciousModifierThen")
@Composable
fun Modifier.sectionHPadding(start: Boolean = true, end: Boolean = true): Modifier {
    val s = AppTheme.spacing
    return this.then(
        Modifier.padding(
            start = if (start) s.contentHorizontal else UiDp.dp0,
            end = if (end) s.contentHorizontal else UiDp.dp0,
        )
    )
}

@Composable
fun Modifier.topPadding(amount: Dp = AppTheme.spacing.space8): Modifier {
    return this.padding(top = amount)
}

@Composable
fun Modifier.horizontalPadding(
    left: Dp = AppTheme.spacing.screenHorizontal,
    right: Dp = AppTheme.spacing.screenHorizontal,
): Modifier {
    return this.padding(start = left, end = right)
}
