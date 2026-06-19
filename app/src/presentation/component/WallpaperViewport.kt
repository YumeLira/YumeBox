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

package com.github.yumelira.yumebox.presentation.component

import kotlin.math.max

data class WallpaperViewportLayout(
    val maxShiftX: Float,
    val maxShiftY: Float,
    val biasX: Float,
    val biasY: Float,
)

fun calculateWallpaperViewportLayout(
    containerWidthPx: Float,
    containerHeightPx: Float,
    imageWidthPx: Float?,
    imageHeightPx: Float?,
    zoom: Float = 1f,
    biasX: Float = 0f,
    biasY: Float = 0f,
): WallpaperViewportLayout {
    val safeContainerWidth = containerWidthPx.coerceAtLeast(1f)
    val safeContainerHeight = containerHeightPx.coerceAtLeast(1f)
    val safeZoom = zoom.coerceIn(1f, 5f)
    val safeImageWidth = imageWidthPx?.takeIf { it > 0f && it.isFinite() }
    val safeImageHeight = imageHeightPx?.takeIf { it > 0f && it.isFinite() }

    val renderedWidthPx: Float
    val renderedHeightPx: Float

    if (safeImageWidth != null && safeImageHeight != null) {
        val coverScale =
            max(safeContainerWidth / safeImageWidth, safeContainerHeight / safeImageHeight) *
                safeZoom
        renderedWidthPx = safeImageWidth * coverScale
        renderedHeightPx = safeImageHeight * coverScale
    } else {
        renderedWidthPx = safeContainerWidth * safeZoom
        renderedHeightPx = safeContainerHeight * safeZoom
    }

    val maxShiftX = ((renderedWidthPx - safeContainerWidth) / 2f).coerceAtLeast(0f)
    val maxShiftY = ((renderedHeightPx - safeContainerHeight) / 2f).coerceAtLeast(0f)
    val clampedBiasX = if (maxShiftX > 0.5f) biasX.coerceIn(-1f, 1f) else 0f
    val clampedBiasY = if (maxShiftY > 0.5f) biasY.coerceIn(-1f, 1f) else 0f

    return WallpaperViewportLayout(
        maxShiftX = maxShiftX,
        maxShiftY = maxShiftY,
        biasX = clampedBiasX,
        biasY = clampedBiasY,
    )
}
