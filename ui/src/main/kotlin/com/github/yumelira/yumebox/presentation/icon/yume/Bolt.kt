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

public val Yume.Bolt: ImageVector
    get() {
        if (_bolt != null) {
            return _bolt!!
        }
        _bolt = Builder(name = "Bolt", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
                viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Companion.Round, strokeLineMiter = 4.0f, pathFillType = NonZero) {
                moveTo(21.0f, 16.0f)
                verticalLineTo(8.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, -1.0f, -1.73f)
                lineToRelative(-7.0f, -4.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, -2.0f, 0.0f)
                lineToRelative(-7.0f, 4.0f)
                arcTo(2.0f, 2.0f, 0.0f, false, false, 3.0f, 8.0f)
                verticalLineToRelative(8.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.0f, 1.73f)
                lineToRelative(7.0f, 4.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 2.0f, 0.0f)
                lineToRelative(7.0f, -4.0f)
                arcTo(2.0f, 2.0f, 0.0f, false, false, 21.0f, 16.0f)
                close()
            }
            path(fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Companion.Round, strokeLineMiter = 4.0f, pathFillType = NonZero) {
                moveTo(12.0f, 12.0f)
                moveToRelative(-4.0f, 0.0f)
                arcToRelative(4.0f, 4.0f, 0.0f, true, true, 8.0f, 0.0f)
                arcToRelative(4.0f, 4.0f, 0.0f, true, true, -8.0f, 0.0f)
            }
        }
        .build()
        return _bolt!!
    }

private var _bolt: ImageVector? = null
