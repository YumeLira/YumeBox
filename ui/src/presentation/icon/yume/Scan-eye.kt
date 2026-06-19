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
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume

val Yume.`Scan-eye`: ImageVector
    get() {
        if (`_scan-eye` != null) {
            return `_scan-eye`!!
        }
        `_scan-eye` =
            Builder(
                    name = "Scan-eye",
                    defaultWidth = 24.0.dp,
                    defaultHeight = 24.0.dp,
                    viewportWidth = 24.0f,
                    viewportHeight = 24.0f,
                )
                .apply {
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(3.0f, 7.0f)
                        verticalLineTo(5.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, false, true, 2.0f, -2.0f)
                        horizontalLineToRelative(2.0f)
                    }
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(17.0f, 3.0f)
                        horizontalLineToRelative(2.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, false, true, 2.0f, 2.0f)
                        verticalLineToRelative(2.0f)
                    }
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(21.0f, 17.0f)
                        verticalLineToRelative(2.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, false, true, -2.0f, 2.0f)
                        horizontalLineToRelative(-2.0f)
                    }
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(7.0f, 21.0f)
                        horizontalLineTo(5.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, false, true, -2.0f, -2.0f)
                        verticalLineToRelative(-2.0f)
                    }
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(12.0f, 12.0f)
                        moveToRelative(-1.0f, 0.0f)
                        arcToRelative(1.0f, 1.0f, 0.0f, true, true, 2.0f, 0.0f)
                        arcToRelative(1.0f, 1.0f, 0.0f, true, true, -2.0f, 0.0f)
                    }
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(18.944f, 12.33f)
                        arcToRelative(1.0f, 1.0f, 0.0f, false, false, 0.0f, -0.66f)
                        arcToRelative(7.5f, 7.5f, 0.0f, false, false, -13.888f, 0.0f)
                        arcToRelative(1.0f, 1.0f, 0.0f, false, false, 0.0f, 0.66f)
                        arcToRelative(7.5f, 7.5f, 0.0f, false, false, 13.888f, 0.0f)
                    }
                }
                .build()
        return `_scan-eye`!!
    }

private var `_scan-eye`: ImageVector? = null
