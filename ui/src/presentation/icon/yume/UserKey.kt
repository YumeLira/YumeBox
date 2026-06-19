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

val Yume.UserKey: ImageVector
    get() {
        if (_UserKey != null) {
            return _UserKey!!
        }
        _UserKey =
            ImageVector.Builder(
                    name = "UserKey",
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
                        moveTo(20f, 11f)
                        verticalLineToRelative(6f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(20f, 13f)
                        horizontalLineToRelative(2f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(3f, 21f)
                        verticalLineToRelative(-2f)
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            4f,
                            -4f,
                        )
                        horizontalLineToRelative(6f)
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            2.072f,
                            0.578f,
                        )
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(10f, 7f)
                        moveToRelative(-4f, 0f)
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            8f,
                            0f,
                        )
                        arcToRelative(
                            4f,
                            4f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            -8f,
                            0f,
                        )
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(20f, 19f)
                        moveToRelative(-2f, 0f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            4f,
                            0f,
                        )
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            -4f,
                            0f,
                        )
                    }
                }
                .build()

        return _UserKey!!
    }

@Suppress("ObjectPropertyName") private var _UserKey: ImageVector? = null
