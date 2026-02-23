package com.github.yumelira.yumebox.presentation.icon.yume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume

public val Yume.`Badge-plus`: ImageVector
    get() {
        if (`_badge-plus` != null) {
            return `_badge-plus`!!
        }
        `_badge-plus` = Builder(name = "Badge-plus", defaultWidth = 24.0.dp, defaultHeight =
                24.0.dp, viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Companion.Round, strokeLineMiter = 4.0f, pathFillType = NonZero) {
                moveTo(3.85f, 8.62f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, 4.78f, -4.77f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, 6.74f, 0.0f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, 4.78f, 4.78f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, 0.0f, 6.74f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, -4.77f, 4.78f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, -6.75f, 0.0f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, -4.78f, -4.77f)
                arcToRelative(4.0f, 4.0f, 0.0f, false, true, 0.0f, -6.76f)
                close()
            }
            path(fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Companion.Round, strokeLineMiter = 4.0f, pathFillType = NonZero) {
                moveTo(12.0f, 8.0f)
                lineTo(12.0f, 16.0f)
            }
            path(fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Companion.Round, strokeLineMiter = 4.0f, pathFillType = NonZero) {
                moveTo(8.0f, 12.0f)
                lineTo(16.0f, 12.0f)
            }
        }
        .build()
        return `_badge-plus`!!
    }

private var `_badge-plus`: ImageVector? = null
