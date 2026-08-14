/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.app.WallpaperManager
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.net.toUri
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.sketch.resize.Scale
import com.github.panpf.sketch.util.Size
import com.github.yumeyucca.yumebox.R
import com.github.yumeyucca.yumebox.presentation.component.calculateWallpaperViewportLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal val LocalUseSystemWallpaper = compositionLocalOf { false }
internal val LocalWallpaperRefreshKey = compositionLocalOf { 0 }

@Composable
internal fun MoeWallpaperBackground(
    wallpaperUri: String,
    wallpaperZoom: Float = 1f,
    wallpaperBiasX: Float = 0f,
    wallpaperBiasY: Float = 0f,
    qualityMode: MoeWallpaperQualityMode = MoeWallpaperQualityMode.Foreground,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val bundledWallpaper = newResourceUri(R.drawable.wallpaper)
    val useSystemWallpaper = LocalUseSystemWallpaper.current
    val wallpaperRefreshKey = LocalWallpaperRefreshKey.current
    val model by
    produceState(bundledWallpaper, wallpaperUri, useSystemWallpaper, wallpaperRefreshKey) {
        value =
            withContext(Dispatchers.IO) {
                val sourceUri =
                    if (useSystemWallpaper) {
                        copySystemWallpaper(context)
                    } else {
                        wallpaperUri
                    }
                resolveWallpaperModel(context, sourceUri)
            }
    }
    val imageBounds by
    produceState<Pair<Int, Int>?>(null, model) {
        value = if (model == bundledWallpaper) null else readImageBounds(context, model)
    }

    BoxWithConstraints(modifier = modifier) {
        val width = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val height = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val painter = rememberWallpaperPainter(context, model, width, height, qualityMode)
        val intrinsic = painter.intrinsicSize
        val layout =
            calculateWallpaperViewportLayout(
                containerWidthPx = width,
                containerHeightPx = height,
                imageWidthPx =
                    intrinsic.width.takeIf { it > 0f && it.isFinite() }
                        ?: imageBounds?.first?.toFloat(),
                imageHeightPx =
                    intrinsic.height.takeIf { it > 0f && it.isFinite() }
                        ?: imageBounds?.second?.toFloat(),
                zoom = wallpaperZoom.coerceIn(1f, 5f),
                biasX = wallpaperBiasX,
                biasY = wallpaperBiasY,
            )
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(layout.biasX, layout.biasY),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun rememberWallpaperPainter(
    context: Context,
    model: String,
    width: Float,
    height: Float,
    quality: MoeWallpaperQualityMode,
) =
    rememberAsyncImagePainter(
        request =
            ImageRequest(context, model) {
                scale(Scale.CENTER_CROP)
                memoryCachePolicy(CachePolicy.DISABLED)
                downloadCachePolicy(CachePolicy.DISABLED)
                resultCachePolicy(CachePolicy.DISABLED)
                if (quality == MoeWallpaperQualityMode.BackgroundBlur) {
                    size(
                        kotlin.math.ceil(width * 1.2f).toInt(),
                        kotlin.math.ceil(height * 1.2f).toInt(),
                    )
                    precision(Precision.LESS_PIXELS)
                } else {
                    size(Size.Origin)
                    precision(Precision.EXACTLY)
                }
            }
    )

private suspend fun readImageBounds(context: Context, model: String): Pair<Int, Int>? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(model.toUri())?.use { input ->
                BitmapFactory.Options()
                    .apply { inJustDecodeBounds = true }
                    .also { options ->
                        BitmapFactory.decodeStream(input, null, options)
                    }
                    .takeIf { it.outWidth > 0 && it.outHeight > 0 }
                    ?.let { it.outWidth to it.outHeight }
            }
        }
            .getOrNull()
    }

private fun resolveWallpaperModel(context: Context, uri: String): String {
    val bundledWallpaper = newResourceUri(R.drawable.wallpaper)
    if (uri.isBlank()) return bundledWallpaper
    if (uri.startsWith("file://")) {
        val path = uri.removePrefix("file://")
        return if (File(path).exists()) uri else bundledWallpaper
    }
    val readable = runCatching {
        context.contentResolver.openInputStream(uri.toUri())?.use { true } ?: false
    }
        .getOrDefault(false)
    return if (readable) uri else bundledWallpaper
}

@SuppressLint("NewApi")
private fun copySystemWallpaper(context: Context): String {
    if (!SystemWallpaperAccess.isGranted(context)) return ""
    val target = File(context.cacheDir, "system-wallpaper.jpg")
    return runCatching {
        WallpaperManager.getInstance(context).getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use {
            ParcelFileDescriptor.AutoCloseInputStream(it).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } ?: return ""
        "file://${target.absolutePath}"
    }.getOrDefault("")
}
