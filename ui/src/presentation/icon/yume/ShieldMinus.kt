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

val Yume.ShieldMinus: ImageVector
    get() {
        if (_ShieldMinus != null) {
            return _ShieldMinus!!
        }
        _ShieldMinus =
            ImageVector.Builder(
                    name = "ShieldMinus",
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
                        moveTo(20f, 13f)
                        curveToRelative(0f, 5f, -3.5f, 7.5f, -7.66f, 8.95f)
                        arcToRelative(
                            1f,
                            1f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -0.67f,
                            -0.01f,
                        )
                        curveTo(7.5f, 20.5f, 4f, 18f, 4f, 13f)
                        verticalLineTo(6f)
                        arcToRelative(
                            1f,
                            1f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            1f,
                            -1f,
                        )
                        curveToRelative(2f, 0f, 4.5f, -1.2f, 6.24f, -2.72f)
                        arcToRelative(
                            1.17f,
                            1.17f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            1.52f,
                            0f,
                        )
                        curveTo(14.51f, 3.81f, 17f, 5f, 19f, 5f)
                        arcToRelative(
                            1f,
                            1f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            1f,
                            1f,
                        )
                        close()
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(9f, 12f)
                        horizontalLineToRelative(6f)
                    }
                }
                .build()

        return _ShieldMinus!!
    }

@Suppress("ObjectPropertyName") private var _ShieldMinus: ImageVector? = null
