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

val Yume.BadgeDollarSign: ImageVector
    get() {
        if (_BadgeDollarSign != null) {
            return _BadgeDollarSign!!
        }
        _BadgeDollarSign =
            ImageVector.Builder(
                    name = "BadgeDollarSign",
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
                        moveTo(3.85f, 8.62f)
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            4.78f,
                            -4.77f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            6.74f,
                            0f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            4.78f,
                            4.78f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            0f,
                            6.74f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -4.77f,
                            4.78f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -6.75f,
                            0f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -4.78f,
                            -4.77f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            0f,
                            -6.76f,
                        )
                        close()
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(16f, 8f)
                        horizontalLineToRelative(-6f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = false,
                            0f,
                            4f,
                        )
                        horizontalLineToRelative(4f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            0f,
                            4f,
                        )
                        horizontalLineTo(8f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(12f, 18f)
                        verticalLineTo(6f)
                    }
                }
                .build()

        return _BadgeDollarSign!!
    }

@Suppress("ObjectPropertyName") private var _BadgeDollarSign: ImageVector? = null
