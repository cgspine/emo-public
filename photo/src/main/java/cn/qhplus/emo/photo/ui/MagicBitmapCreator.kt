package cn.qhplus.emo.photo.ui

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

suspend fun View.createMagicBitmap(
    width: Int,
    height: Int,
    content: @Composable () -> Unit
): Bitmap? {
    if(width <= 0 || height <= 0){
        return null
    }
    val contentLayout = rootView.findViewById<FrameLayout>(Window.ID_ANDROID_CONTENT) ?: return null
    return suspendCancellableCoroutine { continuation ->
        contentLayout.addView(ComposeView(context).apply {
            setContent(content)
            OneShotPreDrawListener.add(this){
                post {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    draw(canvas)
                    contentLayout.removeView(this)
                    continuation.resume(bitmap)
                }
            }
        }, FrameLayout.LayoutParams(width, height).apply {
            leftMargin = 100000
        })
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
    if(drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0){
        return null
    }
    val mini = drawable.intrinsicWidth.coerceAtLeast(drawable.intrinsicHeight)
    var w = drawable.intrinsicWidth
    var h = drawable.intrinsicHeight
    if(mini < shortSideMin){
        val scale = shortSideMin * 1f / mini
        w = (w * scale).toInt()
        h = (h * scale).toInt()
    }
    val source = drawable.toBitmap().let {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.config == Bitmap.Config.HARDWARE){
            it.copy(Bitmap.Config.ARGB_8888, false)
        } else it
    }
    val bitmap = createMagicBitmap(w, h){
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = BitmapPainter(source.asImageBitmap()),
                contentDescription = "",
                contentScale = ContentScale.Fit
            )
            EditLayerList(editLayers)
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