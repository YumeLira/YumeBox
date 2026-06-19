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

val Yume.Palette: ImageVector
    get() {
        if (_Palette != null) {
            return _Palette!!
        }
        _Palette =
            ImageVector.Builder(
                    name = "Palette",
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
                        moveTo(12f, 22f)
                        arcToRelative(
                            1f,
                            1f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            0f,
                            -20f,
                        )
                        arcToRelative(
                            10f,
                            9f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            10f,
                            9f,
                        )
                        arcToRelative(
                            5f,
                            5f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -5f,
                            5f,
                        )
                        horizontalLineToRelative(-2.25f)
                        arcToRelative(
                            1.75f,
                            1.75f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            -1.4f,
                            2.8f,
                        )
                        lineToRelative(0.3f, 0.4f)
                        arcToRelative(
                            1.75f,
                            1.75f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -1.4f,
                            2.8f,
                        )
                        close()
                    }
                    path(
                        fill = SolidColor(Color.Black),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(13.5f, 6.5f)
                        moveToRelative(-0.5f, 0f)
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            1f,
                            0f,
                        )
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            -1f,
                            0f,
                        )
                    }
                    path(
                        fill = SolidColor(Color.Black),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(17.5f, 10.5f)
                        moveToRelative(-0.5f, 0f)
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            1f,
                            0f,
                        )
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            -1f,
                            0f,
                        )
                    }
                    path(
                        fill = SolidColor(Color.Black),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(6.5f, 12.5f)
                        moveToRelative(-0.5f, 0f)
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            1f,
                            0f,
                        )
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            -1f,
                            0f,
                        )
                    }
                    path(
                        fill = SolidColor(Color.Black),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(8.5f, 7.5f)
                        moveToRelative(-0.5f, 0f)
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            1f,
                            0f,
                        )
                        arcToRelative(
                            0.5f,
                            0.5f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            -1f,
                            0f,
                        )
                    }
                }
                .build()

        return _Palette!!
    }

@Suppress("ObjectPropertyName") private var _Palette: ImageVector? = null
