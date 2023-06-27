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

package cn.qhplus.emo.photo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.OneShotPreDrawListener
import cn.qhplus.emo.photo.ui.edit.EditLayer
import cn.qhplus.emo.photo.ui.edit.EditLayerList
import cn.qhplus.emo.photo.util.PhotoHelper
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

const val MAX_BITMAP_SIZE = 1024 * 1024 * 20

suspend fun View.createMagicBitmap(
    width: Int,
    height: Int,
    content: @Composable (fullDrawnReporter: () -> Unit) -> Unit
): Bitmap? {
    if (width <= 0) {
        return null
    }
    val contentLayout = rootView.findViewById<FrameLayout>(Window.ID_ANDROID_CONTENT) ?: return null
    return suspendCancellableCoroutine { continuation ->
        contentLayout.addView(
            MagicBitmapContainer(context, width, height).apply {
                composeView.setContent {
                    content {
                        OneShotPreDrawListener.add(this) {
                            post {
                                val bitmap = Bitmap.createBitmap(composeView.width, composeView.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                draw(canvas)
                                contentLayout.removeView(this)
                                continuation.resume(bitmap)
                            }
                        }
                        invalidate()
                    }
                }
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                leftMargin = 100000
            }
        )
    }
}

@SuppressLint("ViewConstructor")
private class MagicBitmapContainer(
    context: Context,
    val w: Int,
    val h: Int
) : FrameLayout(context) {

    val composeView = ComposeView(context)

    init {
        addView(
            composeView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                if (h > 0) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
            )
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val ws = if (w > 0) w else MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            if (ws > 0) {
                MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
            } else {
                widthMeasureSpec
            },
            if (h > 0) {
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
            } else {
                MeasureSpec.makeMeasureSpec(MAX_BITMAP_SIZE / ws, MeasureSpec.AT_MOST)
            }
        )
    }
}

suspend fun View.saveEditBitmapToStore(
    drawable: Drawable,
    editLayers: PersistentList<EditLayer>,
    nameWithoutSuffix: String,
    shortSideMin: Int = 0,
    dirName: String = Environment.DIRECTORY_PICTURES,
    compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    compressQuality: Int = 100
): Uri? {
    if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        return null
    }
    val mini = drawable.intrinsicWidth.coerceAtLeast(drawable.intrinsicHeight)
    var w = drawable.intrinsicWidth
    var h = drawable.intrinsicHeight
    if (mini < shortSideMin) {
        val scale = shortSideMin * 1f / mini
        w = (w * scale).toInt()
        h = (h * scale).toInt()
    }
    val source = drawable.toBitmap().let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.config == Bitmap.Config.HARDWARE) {
            it.copy(Bitmap.Config.ARGB_8888, false)
        } else it
    }
    val bitmap = createMagicBitmap(w, h) { fullDrawnReporter ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = BitmapPainter(source.asImageBitmap()),
                contentDescription = "",
                contentScale = ContentScale.Fit
            )
            EditLayerList(editLayers)
        }
        LaunchedEffect(Unit) {
            fullDrawnReporter()
        }
    } ?: return null
    return withContext(Dispatchers.IO) {
        PhotoHelper.saveToStore(
            context.applicationContext,
            bitmap,
            nameWithoutSuffix,
            dirName,
            compressFormat,
            compressQuality
        )
    }
}
