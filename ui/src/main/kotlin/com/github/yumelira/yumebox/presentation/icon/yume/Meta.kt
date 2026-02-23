package com.github.yumelira.yumebox.presentation.icon.yume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume

public val Yume.Meta: ImageVector
    get() {
        if (_meta != null) {
            return _meta!!
        }
        _meta = Builder(name = "Meta", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
                viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(4.44f, 2.19f)
                curveTo(4.54f, 2.18f, 4.65f, 2.18f, 4.74f, 2.24f)
                curveTo(4.88f, 2.33f, 4.99f, 2.46f, 5.11f, 2.57f)
                curveTo(6.9f, 4.32f, 8.7f, 6.06f, 10.49f, 7.81f)
                curveTo(10.61f, 7.93f, 10.72f, 8.06f, 10.87f, 8.13f)
                curveTo(10.99f, 8.19f, 11.12f, 8.16f, 11.24f, 8.14f)
                curveTo(11.57f, 8.08f, 11.9f, 8.08f, 12.23f, 8.08f)
                curveTo(12.55f, 8.08f, 12.87f, 8.1f, 13.19f, 8.15f)
                curveTo(13.32f, 8.17f, 13.47f, 8.21f, 13.56f, 8.09f)
                curveTo(15.53f, 6.16f, 17.51f, 4.24f, 19.49f, 2.32f)
                curveTo(19.66f, 2.13f, 19.91f, 2.18f, 20.13f, 2.2f)
                curveTo(20.68f, 2.29f, 21.24f, 2.33f, 21.79f, 2.4f)
                curveTo(21.88f, 2.42f, 22.01f, 2.43f, 22.04f, 2.54f)
                curveTo(22.08f, 2.74f, 22.06f, 2.95f, 22.06f, 3.16f)
                lineTo(22.06f, 15.65f)
                curveTo(22.06f, 15.86f, 22.07f, 16.07f, 22.05f, 16.28f)
                curveTo(22.04f, 16.36f, 22.03f, 16.44f, 21.97f, 16.5f)
                curveTo(21.85f, 16.56f, 21.71f, 16.58f, 21.58f, 16.61f)
                curveTo(21.03f, 16.75f, 20.47f, 16.85f, 19.92f, 17.0f)
                curveTo(19.64f, 17.08f, 19.36f, 17.05f, 19.08f, 17.05f)
                curveTo(18.94f, 17.05f, 18.79f, 17.06f, 18.64f, 17.04f)
                curveTo(18.56f, 17.03f, 18.5f, 16.95f, 18.49f, 16.87f)
                curveTo(18.46f, 16.66f, 18.48f, 16.45f, 18.47f, 16.24f)
                lineTo(18.47f, 10.09f)
                curveTo(18.47f, 9.89f, 18.48f, 9.68f, 18.45f, 9.48f)
                curveTo(18.42f, 9.25f, 18.19f, 9.08f, 17.97f, 9.11f)
                curveTo(17.85f, 9.11f, 17.75f, 9.19f, 17.66f, 9.27f)
                curveTo(16.71f, 10.21f, 15.74f, 11.13f, 14.79f, 12.06f)
                curveTo(14.68f, 12.16f, 14.56f, 12.28f, 14.4f, 12.26f)
                curveTo(14.2f, 12.24f, 14.0f, 12.16f, 13.81f, 12.12f)
                curveTo(13.02f, 11.91f, 12.19f, 11.88f, 11.38f, 11.96f)
                curveTo(10.99f, 12.02f, 10.59f, 12.07f, 10.21f, 12.2f)
                curveTo(10.09f, 12.24f, 9.96f, 12.28f, 9.84f, 12.27f)
                curveTo(9.71f, 12.26f, 9.61f, 12.17f, 9.52f, 12.08f)
                curveTo(8.58f, 11.15f, 7.62f, 10.24f, 6.68f, 9.31f)
                curveTo(6.59f, 9.22f, 6.49f, 9.13f, 6.36f, 9.11f)
                curveTo(6.2f, 9.09f, 6.04f, 9.16f, 5.93f, 9.28f)
                curveTo(5.84f, 9.39f, 5.83f, 9.55f, 5.83f, 9.69f)
                lineTo(5.83f, 16.67f)
                curveTo(5.83f, 16.76f, 5.83f, 16.85f, 5.79f, 16.94f)
                curveTo(5.74f, 17.04f, 5.62f, 17.04f, 5.53f, 17.05f)
                lineTo(4.8f, 17.05f)
                curveTo(4.52f, 17.05f, 4.26f, 16.97f, 3.99f, 16.91f)
                curveTo(3.5f, 16.8f, 3.01f, 16.68f, 2.52f, 16.57f)
                curveTo(2.42f, 16.54f, 2.27f, 16.51f, 2.25f, 16.37f)
                curveTo(2.22f, 16.13f, 2.24f, 15.89f, 2.24f, 15.65f)
                lineTo(2.24f, 3.21f)
                curveTo(2.24f, 3.01f, 2.23f, 2.82f, 2.25f, 2.63f)
                curveTo(2.26f, 2.56f, 2.28f, 2.48f, 2.34f, 2.44f)
                curveTo(2.41f, 2.41f, 2.49f, 2.4f, 2.56f, 2.39f)
                curveTo(3.18f, 2.34f, 3.79f, 2.26f, 4.41f, 2.2f)
                close()
                moveTo(11.79f, 17.48f)
                curveTo(12.07f, 17.45f, 12.35f, 17.46f, 12.62f, 17.48f)
                curveTo(12.83f, 17.48f, 12.97f, 17.76f, 12.87f, 17.95f)
                curveTo(12.75f, 18.21f, 12.62f, 18.46f, 12.48f, 18.71f)
                curveTo(12.39f, 18.89f, 12.13f, 18.92f, 12.01f, 18.76f)
                curveTo(11.9f, 18.61f, 11.83f, 18.43f, 11.74f, 18.27f)
                curveTo(11.67f, 18.13f, 11.58f, 18.0f, 11.55f, 17.84f)
                curveTo(11.52f, 17.68f, 11.63f, 17.51f, 11.79f, 17.48f)
                close()
                moveTo(2.47f, 18.02f)
                curveTo(2.52f, 18.01f, 2.58f, 18.01f, 2.63f, 18.01f)
                lineTo(7.25f, 18.01f)
                curveTo(7.39f, 18.01f, 7.54f, 18.04f, 7.63f, 18.18f)
                curveTo(7.71f, 18.33f, 7.7f, 18.56f, 7.55f, 18.66f)
                curveTo(7.45f, 18.73f, 7.32f, 18.73f, 7.21f, 18.74f)
                lineTo(2.64f, 18.74f)
                curveTo(2.55f, 18.74f, 2.47f, 18.74f, 2.39f, 18.71f)
                curveTo(2.26f, 18.66f, 2.17f, 18.5f, 2.19f, 18.35f)
                curveTo(2.2f, 18.19f, 2.32f, 18.04f, 2.48f, 18.02f)
                close()
                moveTo(17.0f, 18.02f)
                curveTo(17.21f, 18.0f, 17.42f, 18.02f, 17.62f, 18.01f)
                lineTo(21.73f, 18.01f)
                curveTo(21.82f, 18.01f, 21.91f, 18.01f, 21.99f, 18.05f)
                curveTo(22.11f, 18.11f, 22.18f, 18.25f, 22.18f, 18.39f)
                curveTo(22.18f, 18.55f, 22.06f, 18.71f, 21.9f, 18.72f)
                curveTo(21.7f, 18.74f, 21.51f, 18.73f, 21.31f, 18.73f)
                lineTo(17.52f, 18.73f)
                curveTo(17.33f, 18.73f, 17.13f, 18.75f, 16.94f, 18.71f)
                curveTo(16.8f, 18.68f, 16.68f, 18.54f, 16.69f, 18.38f)
                curveTo(16.67f, 18.19f, 16.82f, 18.03f, 16.99f, 18.01f)
                close()
                moveTo(7.18f, 19.78f)
                curveTo(7.28f, 19.75f, 7.39f, 19.73f, 7.49f, 19.79f)
                curveTo(7.69f, 19.89f, 7.74f, 20.23f, 7.56f, 20.38f)
                curveTo(7.45f, 20.46f, 7.32f, 20.49f, 7.2f, 20.54f)
                curveTo(6.74f, 20.69f, 6.3f, 20.88f, 5.84f, 21.03f)
                curveTo(5.32f, 21.21f, 4.8f, 21.42f, 4.28f, 21.6f)
                curveTo(3.9f, 21.73f, 3.52f, 21.87f, 3.14f, 22.01f)
                curveTo(2.98f, 22.07f, 2.83f, 22.14f, 2.67f, 22.18f)
                curveTo(2.57f, 22.2f, 2.46f, 22.18f, 2.38f, 22.11f)
                curveTo(2.25f, 22.0f, 2.23f, 21.79f, 2.3f, 21.64f)
                curveTo(2.36f, 21.53f, 2.48f, 21.49f, 2.58f, 21.45f)
                curveTo(2.91f, 21.33f, 3.23f, 21.2f, 3.56f, 21.09f)
                curveTo(4.38f, 20.81f, 5.19f, 20.47f, 6.02f, 20.2f)
                curveTo(6.4f, 20.07f, 6.78f, 19.9f, 7.16f, 19.78f)
                close()
                moveTo(16.95f, 19.76f)
                curveTo(17.07f, 19.73f, 17.18f, 19.78f, 17.3f, 19.81f)
                curveTo(17.66f, 19.94f, 18.01f, 20.09f, 18.37f, 20.21f)
                curveTo(19.22f, 20.5f, 20.05f, 20.84f, 20.9f, 21.13f)
                curveTo(21.18f, 21.23f, 21.46f, 21.34f, 21.74f, 21.44f)
                curveTo(21.85f, 21.48f, 21.96f, 21.52f, 22.04f, 21.61f)
                curveTo(22.16f, 21.79f, 22.09f, 22.08f, 21.89f, 22.16f)
                curveTo(21.76f, 22.21f, 21.63f, 22.16f, 21.51f, 22.12f)
                curveTo(21.0f, 21.92f, 20.49f, 21.74f, 19.98f, 21.56f)
                curveTo(19.48f, 21.38f, 18.99f, 21.18f, 18.48f, 21.01f)
                curveTo(18.02f, 20.86f, 17.58f, 20.67f, 17.13f, 20.52f)
                curveTo(17.01f, 20.47f, 16.88f, 20.44f, 16.78f, 20.35f)
                curveTo(16.61f, 20.17f, 16.7f, 19.8f, 16.94f, 19.75f)
                close()
            }
        }
        .build()
        return _meta!!
    }

private var _meta: ImageVector? = null
