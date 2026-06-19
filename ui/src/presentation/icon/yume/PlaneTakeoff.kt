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

val Yume.PlaneTakeoff: ImageVector
    get() {
        if (_PlaneTakeoff != null) {
            return _PlaneTakeoff!!
        }
        _PlaneTakeoff =
            ImageVector.Builder(
                    name = "PlaneTakeoff",
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
                        moveTo(2f, 22f)
                        horizontalLineToRelative(20f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(6.36f, 17.4f)
                        lineTo(4f, 17f)
                        lineToRelative(-2f, -4f)
                        lineToRelative(1.1f, -0.55f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            1.8f,
                            0f,
                        )
                        lineToRelative(0.17f, 0.1f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            1.8f,
                            0f,
                        )
                        lineTo(8f, 12f)
                        lineTo(5f, 6f)
                        lineToRelative(0.9f, -0.45f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            2.09f,
                            0.2f,
                        )
                        lineToRelative(4.02f, 3f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = false,
                            2.1f,
                            0.2f,
                        )
                        lineToRelative(4.19f, -2.06f)
                        arcToRelative(
                            2.41f,
                            2.41f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            1.73f,
                            -0.17f,
                        )
                        lineTo(21f, 7f)
                        arcToRelative(
                            1.4f,
                            1.4f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            0.87f,
                            1.99f,
                        )
                        lineToRelative(-0.38f, 0.76f)
                        curveToRelative(-0.23f, 0.46f, -0.6f, 0.84f, -1.07f, 1.08f)
                        lineTo(7.58f, 17.2f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -1.22f,
                            0.18f,
                        )
                        close()
                    }
                }
                .build()

        return _PlaneTakeoff!!
    }

@Suppress("ObjectPropertyName") private var _PlaneTakeoff: ImageVector? = null
