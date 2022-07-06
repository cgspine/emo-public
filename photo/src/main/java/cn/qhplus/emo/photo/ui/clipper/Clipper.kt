/*
 * Copyright 2022 emo Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.qhplus.emo.photo.ui.clipper

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cn.qhplus.emo.photo.data.PhotoLoadStatus
import cn.qhplus.emo.photo.data.PhotoProvider
import cn.qhplus.emo.photo.ui.GesturePhoto
import cn.qhplus.emo.ui.core.Loading

private class ClipperPhotoInfo(
    var scale: Float = 1f,
    var rect: Rect? = null,
    var drawable: Drawable? = null,
    var clipArea: Rect
)

val DefaultClipFocusAreaSquareCenter = Rect.Zero

@Composable
fun PhotoClipper(
    photoProvider: PhotoProvider,
    maskColor: Color = Color.Black.copy(0.64f),
    clipFocusArea: Rect = DefaultClipFocusAreaSquareCenter,
    drawClipFocusArea: DrawScope.(Rect) -> Unit = { area ->
        drawCircle(
            Color.Black,
            radius = area.size.minDimension / 2,
            center = area.center,
            blendMode = BlendMode.DstOut
        )
    },
    bitmapClipper: (origin: Bitmap, clipArea: Rect, scale: Float) -> Bitmap? = { origin, clipArea, scale ->
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        Bitmap.createBitmap(
            origin,
            clipArea.left.toInt(),
            clipArea.top.toInt(),
            clipArea.width.toInt(),
            clipArea.height.toInt(),
            matrix,
            false
        )
    },
    operateContent: @Composable BoxWithConstraintsScope.(doClip: () -> Bitmap?) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val focusArea = if (clipFocusArea == DefaultClipFocusAreaSquareCenter) {
            val size = (constraints.maxWidth.coerceAtMost(constraints.maxHeight)).toFloat()
            val left = (constraints.maxWidth - size) / 2
            val top = (constraints.maxHeight - size) / 2
            Rect(left, top, left + size, top + size)
        } else {
            clipFocusArea
        }

        val photoInfo = remember(photoProvider) {
            ClipperPhotoInfo(clipArea = focusArea)
        }.apply {
            clipArea = focusArea
        }

        val doClip = remember(photoInfo) {
            val func: () -> Bitmap? = lambda@{
                val origin = photoInfo.drawable?.toBitmap() ?: return@lambda null
                val rect = photoInfo.rect ?: return@lambda null
                val scale = rect.width / origin.width
                val clipRect = photoInfo.clipArea.translate(Offset(-rect.left, -rect.top))
                val imageArea = Rect(
                    clipRect.left / scale,
                    clipRect.top / scale,
                    clipRect.right / scale,
                    clipRect.bottom / scale
                )
                bitmapClipper(origin, imageArea, scale)
            }
            func
        }

        GesturePhoto(
            containerWidth = maxWidth,
            containerHeight = maxHeight,
            imageRatio = photoProvider.ratio(),
            isLongImage = photoProvider.isLongImage(),
            shouldTransitionExit = false,
            panEdgeProtection = focusArea,
            onBeginPullExit = { false },
            onTapExit = {}
        ) { _, scale, rect, onImageRatioEnsured ->
            photoInfo.scale = scale
            photoInfo.rect = rect
            PhotoClipperContent(photoProvider) {
                photoInfo.drawable = it
                if (it.intrinsicWidth > 0 && it.intrinsicHeight > 0) {
                    onImageRatioEnsured(it.intrinsicWidth.toFloat() / it.intrinsicHeight)
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawContext.canvas.withSaveLayer(Rect(Offset.Zero, drawContext.size), Paint()) {
                drawRect(maskColor)
                drawClipFocusArea(focusArea)
            }
        }
        operateContent(doClip)
    }
}

@Composable
fun BoxScope.PhotoClipperContent(
    photoProvider: PhotoProvider,
    onSuccess: (Drawable) -> Unit
) {
    var loadStatus by remember {
        mutableStateOf(PhotoLoadStatus.Loading)
    }
    val photo = remember(photoProvider) {
        photoProvider.photo()
    }
    photo?.Compose(
        contentScale = ContentScale.Fit,
        isContainerDimenExactly = true,
        onSuccess = {
            loadStatus = PhotoLoadStatus.Success
            onSuccess.invoke(it.drawable)
        },
        onError = {
            loadStatus = PhotoLoadStatus.Failed
        }
    )

    if (loadStatus == PhotoLoadStatus.Loading) {
        Loading(
            modifier = Modifier.align(Alignment.Center),
            size = 48.dp
        )
    }
}
