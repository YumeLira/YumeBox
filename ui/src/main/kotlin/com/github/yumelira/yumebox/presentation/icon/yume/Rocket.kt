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
 * Copyright (c)  YumeLira 2026.
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

public val Yume.Rocket: ImageVector
    get() {
        if (_Rocket != null) {
            return _Rocket!!
        }
        _Rocket = ImageVector.Builder(
            name = "Rocket",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 16.5f)
                curveToRelative(-1.5f, 1.26f, -2f, 5f, -2f, 5f)
                reflectiveCurveToRelative(3.74f, -0.5f, 5f, -2f)
                curveToRelative(0.71f, -0.84f, 0.7f, -2.13f, -0.09f, -2.91f)
                arcToRelative(2.18f, 2.18f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2.91f, -0.09f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveToRelative(12f, 15f)
                lineToRelative(-3f, -3f)
                arcToRelative(22f, 22f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -3.95f)
                arcTo(12.88f, 12.88f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22f, 2f)
                curveToRelative(0f, 2.72f, -0.78f, 7.5f, -6f, 11f)
                arcToRelative(22.35f, 22.35f, 0f, isMoreThanHalf = false, isPositiveArc = true, -4f, 2f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 12f)
                horizontalLineTo(4f)
                reflectiveCurveToRelative(0.55f, -3.03f, 2f, -4f)
                curveToRelative(1.62f, -1.08f, 5f, 0f, 5f, 0f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                verticalLineToRelative(5f)
                reflectiveCurveToRelative(3.03f, -0.55f, 4f, -2f)
                curveToRelative(1.08f, -1.62f, 0f, -5f, 0f, -5f)
            }
        }.build()

        return _Rocket!!
    }

@Suppress("ObjectPropertyName")
private var _Rocket: ImageVector? = null
