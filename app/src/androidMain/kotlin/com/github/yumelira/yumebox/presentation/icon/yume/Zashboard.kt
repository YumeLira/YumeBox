package com.github.yumelira.yumebox.presentation.icon.yume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume

public val Yume.Zashboard: ImageVector
    get() {
        if (_zashboard != null) {
            return _zashboard!!
        }
        _zashboard = Builder(name = "Zashboard", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
                viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 0.5f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Companion.Round, strokeLineMiter = 4.0f, pathFillType = EvenOdd) {
                moveTo(11.622f, 1.602f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.756f, 0.0f)
                lineToRelative(2.25f, 1.313f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -0.756f, 1.295f)
                lineTo(12.0f, 3.118f)
                lineTo(10.128f, 4.21f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, -0.756f, -1.295f)
                lineToRelative(2.25f, -1.313f)
                close()
                moveTo(5.898f, 5.81f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -0.27f, 1.025f)
                lineToRelative(-1.14f, 0.665f)
                lineToRelative(1.14f, 0.665f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, -0.756f, 1.295f)
                lineTo(3.75f, 8.806f)
                verticalLineToRelative(0.944f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -1.5f, 0.0f)
                lineTo(2.25f, 7.5f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.372f, -0.648f)
                lineToRelative(2.25f, -1.312f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 1.026f, 0.27f)
                close()
                moveTo(18.102f, 5.81f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 1.026f, -0.27f)
                lineToRelative(2.25f, 1.312f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.372f, 0.648f)
                verticalLineToRelative(2.25f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -1.5f, 0.0f)
                verticalLineToRelative(-0.944f)
                lineToRelative(-1.122f, 0.654f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, -0.756f, -1.295f)
                lineToRelative(1.14f, -0.665f)
                lineToRelative(-1.14f, -0.665f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -0.27f, -1.025f)
                close()
                moveTo(9.102f, 11.06f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 1.026f, -0.27f)
                lineTo(12.0f, 11.882f)
                lineToRelative(1.872f, -1.092f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, 0.756f, 1.295f)
                lineToRelative(-1.878f, 1.096f)
                lineTo(12.75f, 15.0f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -1.5f, 0.0f)
                verticalLineToRelative(-1.82f)
                lineToRelative(-1.878f, -1.095f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -0.27f, -1.025f)
                close()
                moveTo(3.0f, 13.5f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.75f, 0.75f)
                verticalLineToRelative(1.82f)
                lineToRelative(1.878f, 1.095f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, -0.756f, 1.295f)
                lineToRelative(-2.25f, -1.312f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -0.372f, -0.648f)
                verticalLineToRelative(-2.25f)
                arcTo(0.75f, 0.75f, 0.0f, false, true, 3.0f, 13.5f)
                close()
                moveTo(21.0f, 13.5f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.75f, 0.75f)
                verticalLineToRelative(2.25f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -0.372f, 0.648f)
                lineToRelative(-2.25f, 1.312f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, -0.756f, -1.295f)
                lineToRelative(1.878f, -1.096f)
                lineTo(20.25f, 14.25f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.75f, -0.75f)
                close()
                moveTo(12.0f, 18.75f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.75f, 0.75f)
                verticalLineToRelative(0.944f)
                lineToRelative(1.122f, -0.654f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, 0.756f, 1.295f)
                lineToRelative(-2.25f, 1.313f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, -0.756f, 0.0f)
                lineToRelative(-2.25f, -1.313f)
                arcToRelative(0.75f, 0.75f, 0.0f, true, true, 0.756f, -1.295f)
                lineToRelative(1.122f, 0.654f)
                lineTo(11.25f, 19.5f)
                arcToRelative(0.75f, 0.75f, 0.0f, false, true, 0.75f, -0.75f)
                close()
            }
        }
        .build()
        return _zashboard!!
    }

private var _zashboard: ImageVector? = null
