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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.screen.profiles

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.coroutines.resume

@Composable
internal fun StableQrScanner(
    onScanned: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { createBarcodeScannerOrNull() }

    val currentOnScanned by rememberUpdatedState(onScanned)
    val hasScanned = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    DisposableEffect(barcodeScanner) {
        onDispose {
            barcodeScanner?.close()
        }
    }

    DisposableEffect(cameraExecutor) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(), factory = { context ->
            val previewView = PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }

            val previewUseCase = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysisUseCase = barcodeScanner?.let { scanner ->
                ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processStableQrImage(scanner, imageProxy) { text ->
                                if (hasScanned.compareAndSet(false, true)) {
                                    currentOnScanned(text)
                                }
                            }
                        }
                    }
            }

            coroutineScope.launch {
                try {
                    val cameraProvider = context.getStableCameraProvider()
                    cameraProvider.unbindAll()
                    if (imageAnalysisUseCase != null) {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            previewUseCase,
                            imageAnalysisUseCase,
                        )
                    } else {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            previewUseCase,
                        )
                    }
                } catch (error: Exception) {
                    Timber.w(error, "Failed to bind camera lifecycle")
                }
            }

            previewView
        }, onRelease = { previewView ->
            try {
                val context = previewView.context
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
        })
}

@SuppressLint("UnsafeOptInUsageError")
internal fun processStableQrImage(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onScanned: (String) -> Unit,
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees,
        )

        barcodeScanner.process(inputImage).addOnSuccessListener { barcodeList ->
            barcodeList.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue?.let { text ->
                onScanned(text)
            }
        }.addOnCompleteListener {
            imageProxy.image?.close()
            imageProxy.close()
        }
    } ?: imageProxy.close()
}

internal suspend fun Context.getStableCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener(
                {
                    if (continuation.isActive) {
                        continuation.resume(future.get())
                    }
                },
                ContextCompat.getMainExecutor(this),
            )
        }
    }

internal suspend fun readQrFromImage(context: Context, uri: Uri): String? =
    suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val scanner = createBarcodeScannerOrNull()
                ?: run {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                    return@suspendCancellableCoroutine
                }
            continuation.invokeOnCancellation {
                scanner.close()
            }
            scanner.process(inputImage).addOnSuccessListener { barcodes ->
                val barcode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                if (continuation.isActive) {
                    continuation.resume(barcode?.rawValue)
                }
            }.addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }.addOnCompleteListener {
                scanner.close()
            }
        } catch (_: Exception) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

private fun createBarcodeScannerOrNull(): BarcodeScanner? =
    runCatching {
        BarcodeScanning.getClient()
    }.getOrNull()
