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

package com.github.yumelira.yumebox.screen.settings

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.sketch.resize.Scale
import com.github.panpf.sketch.util.screenSize
import com.github.yumelira.yumebox.presentation.component.calculateWallpaperViewportLayout
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AcgWallpaperCropScreen(
    navigator: DestinationsNavigator,
    wallpaperUri: String,
    initialZoom: Float = 1f,
    initialBiasX: Float = 0f,
    initialBiasY: Float = 0f,
) {
    val viewModel = koinViewModel<AppSettingsViewModel>()
    val context = LocalContext.current
    var biasX by
        remember(wallpaperUri, initialBiasX) { mutableFloatStateOf(initialBiasX.coerceIn(-1f, 1f)) }
    var biasY by
        remember(wallpaperUri, initialBiasY) { mutableFloatStateOf(initialBiasY.coerceIn(-1f, 1f)) }
    val painter =
        rememberAsyncImagePainter(
            request =
                remember(wallpaperUri, context) {
                    ImageRequest(context, wallpaperUri) {
                        crossfade(true)
                        precision(Precision.LESS_PIXELS)
                        scale(Scale.CENTER_CROP)
                        size(context.screenSize())
                    }
                }
        )
    val density = LocalDensity.current
    val imageBounds by
        produceState<Pair<Int, Int>?>(initialValue = null, wallpaperUri) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            context.contentResolver.openInputStream(Uri.parse(wallpaperUri))?.use {
                                input ->
                                val options =
                                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeStream(input, null, options)
                                if (options.outWidth > 0 && options.outHeight > 0) {
                                    options.outWidth to options.outHeight
                                } else {
                                    null
                                }
                            }
                        }
                        .getOrNull()
                }
        }

    Scaffold { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val containerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val painterIntrinsic = painter.intrinsicSize
            val imageWidthPx =
                painterIntrinsic.width.takeIf { it > 0f && it.isFinite() }
                    ?: imageBounds?.first?.toFloat()
            val imageHeightPx =
                painterIntrinsic.height.takeIf { it > 0f && it.isFinite() }
                    ?: imageBounds?.second?.toFloat()
            val viewportLayout =
                calculateWallpaperViewportLayout(
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    imageWidthPx = imageWidthPx,
                    imageHeightPx = imageHeightPx,
                    zoom = initialZoom,
                    biasX = biasX,
                    biasY = biasY,
                )

            Box(
                modifier =
                    Modifier.fillMaxSize().pointerInput(
                        viewportLayout.maxShiftX,
                        viewportLayout.maxShiftY,
                    ) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (viewportLayout.maxShiftX > 0.5f) {
                                val currentTranslationX =
                                    biasX.coerceIn(-1f, 1f) * viewportLayout.maxShiftX
                                val nextTranslationX =
                                    (currentTranslationX - dragAmount.x).coerceIn(
                                        -viewportLayout.maxShiftX,
                                        viewportLayout.maxShiftX,
                                    )
                                biasX =
                                    (nextTranslationX / viewportLayout.maxShiftX).coerceIn(-1f, 1f)
                            } else {
                                biasX = 0f
                            }
                            if (viewportLayout.maxShiftY > 0.5f) {
                                val currentTranslationY =
                                    biasY.coerceIn(-1f, 1f) * viewportLayout.maxShiftY
                                val nextTranslationY =
                                    (currentTranslationY - dragAmount.y).coerceIn(
                                        -viewportLayout.maxShiftY,
                                        viewportLayout.maxShiftY,
                                    )
                                biasY =
                                    (nextTranslationY / viewportLayout.maxShiftY).coerceIn(-1f, 1f)
                            } else {
                                biasY = 0f
                            }
                        }
                    }
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(viewportLayout.biasX, viewportLayout.biasY),
                    modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                )
            }

            val bottomInsetPx =
                with(density) {
                    max(
                            WindowInsets.navigationBars.getBottom(this),
                            WindowInsets.systemGestures.getBottom(this),
                        )
                        .toFloat()
                }
            Button(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(
                            start = UiDp.dp16,
                            end = UiDp.dp16,
                            bottom = with(density) { bottomInsetPx.toDp() } + UiDp.dp12,
                        )
                        .fillMaxWidth()
                        .height(UiDp.dp52)
                        .clip(RoundedCornerShape(UiDp.dp12)),
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = {
                    viewModel.onAcgWallpaperCropChange(
                        zoom = 1f,
                        biasX = viewportLayout.biasX,
                        biasY = viewportLayout.biasY,
                    )
                    viewModel.applyAcgWallpaper(sourceUri = wallpaperUri) {
                        navigator.popBackStack()
                    }
                },
            ) {
                Text(
                    text = MLang.AppSettings.Button.Apply,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
