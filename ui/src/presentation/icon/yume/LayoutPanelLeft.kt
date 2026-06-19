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

package com.github.yumelira.yumebox.presentation.icon.yume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume

val Yume.LayoutPanelLeft: ImageVector
    get() {
        if (_LayoutPanelLeft != null) {
            return _LayoutPanelLeft!!
        }
        _LayoutPanelLeft =
            ImageVector.Builder(
                    name = "LayoutPanelLeft",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(4f, 3f)
                        lineTo(9f, 3f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10f, 4f)
                        lineTo(10f, 20f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 21f)
                        lineTo(4f, 21f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 20f)
                        lineTo(3f, 4f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 3f)
                        close()
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(15f, 3f)
                        lineTo(20f, 3f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 4f)
                        lineTo(21f, 9f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 10f)
                        lineTo(15f, 10f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14f, 9f)
                        lineTo(14f, 4f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 3f)
                        close()
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(15f, 14f)
                        lineTo(20f, 14f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 15f)
                        lineTo(21f, 20f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 21f)
                        lineTo(15f, 21f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14f, 20f)
                        lineTo(14f, 15f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 14f)
                        close()
                    }
                }
                .build()

        return _LayoutPanelLeft!!
    }

@Suppress("ObjectPropertyName") private var _LayoutPanelLeft: ImageVector? = null
