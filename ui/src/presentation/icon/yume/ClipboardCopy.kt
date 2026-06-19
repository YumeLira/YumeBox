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

val Yume.ClipboardCopy: ImageVector
    get() {
        if (_ClipboardCopy != null) {
            return _ClipboardCopy!!
        }
        _ClipboardCopy =
            ImageVector.Builder(
                    name = "ClipboardCopy",
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
                        moveTo(9f, 2f)
                        lineTo(15f, 2f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 3f)
                        lineTo(16f, 5f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 6f)
                        lineTo(9f, 6f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8f, 5f)
                        lineTo(8f, 3f)
                        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 2f)
                        close()
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(8f, 4f)
                        horizontalLineTo(6f)
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
                        horizontalLineToRelative(12f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            2f,
                            -2f,
                        )
                        verticalLineToRelative(-2f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(16f, 4f)
                        horizontalLineToRelative(2f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            2f,
                            2f,
                        )
                        verticalLineToRelative(4f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(21f, 14f)
                        horizontalLineTo(11f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveToRelative(15f, 10f)
                        lineToRelative(-4f, 4f)
                        lineToRelative(4f, 4f)
                    }
                }
                .build()

        return _ClipboardCopy!!
    }

@Suppress("ObjectPropertyName") private var _ClipboardCopy: ImageVector? = null
