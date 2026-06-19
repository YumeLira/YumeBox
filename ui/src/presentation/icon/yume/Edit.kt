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

val Yume.Edit: ImageVector
    get() {
        if (_SquarePen != null) {
            return _SquarePen!!
        }
        _SquarePen =
            ImageVector.Builder(
                    name = "SquarePen",
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
                        moveTo(12f, 3f)
                        horizontalLineTo(5f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            -2f,
                            2f,
                        )
                        verticalLineToRelative(14f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            2f,
                            2f,
                        )
                        horizontalLineToRelative(14f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            2f,
                            -2f,
                        )
                        verticalLineToRelative(-7f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(18.375f, 2.625f)
                        arcToRelative(
                            1f,
                            1f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            3f,
                            3f,
                        )
                        lineToRelative(-9.013f, 9.014f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -0.853f,
                            0.505f,
                        )
                        lineToRelative(-2.873f, 0.84f)
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -0.62f,
                            -0.62f,
                        )
                        lineToRelative(0.84f, -2.873f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            0.506f,
                            -0.852f,
                        )
                        close()
                    }
                }
                .build()

        return _SquarePen!!
    }

@Suppress("ObjectPropertyName") private var _SquarePen: ImageVector? = null
