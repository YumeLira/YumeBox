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

val Yume.Folders: ImageVector
    get() {
        if (_Folders != null) {
            return _Folders!!
        }
        _Folders =
            ImageVector.Builder(
                    name = "Folders",
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
                        moveTo(20f, 5f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            2f,
                            2f,
                        )
                        verticalLineToRelative(7f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -2f,
                            2f,
                        )
                        horizontalLineTo(9f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -2f,
                            -2f,
                        )
                        verticalLineTo(5f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            2f,
                            -2f,
                        )
                        horizontalLineToRelative(2.5f)
                        arcToRelative(
                            1.5f,
                            1.5f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            1.2f,
                            0.6f,
                        )
                        lineToRelative(0.6f, 0.8f)
                        arcToRelative(
                            1.5f,
                            1.5f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            1.2f,
                            0.6f,
                        )
                        close()
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(3f, 8.268f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            -1f,
                            1.738f,
                        )
                        verticalLineTo(19f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            2f,
                            2f,
                        )
                        horizontalLineToRelative(11f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            1.732f,
                            -1f,
                        )
                    }
                }
                .build()

        return _Folders!!
    }

@Suppress("ObjectPropertyName") private var _Folders: ImageVector? = null
