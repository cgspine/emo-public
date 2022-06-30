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

package cn.qhplus.emo.photo.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.unit.IntSize
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BitmapRegions(val width: Int, val height: Int, val list: List<BitmapRegionProvider>)

class BitmapRegionProvider(
    val width: Int,
    val height: Int,
    val loader: BitmapRegionLoader
)

fun interface BitmapRegionLoader {
    suspend fun load(): Bitmap?
}

class EmoAlreadyBitmapRegionLoader(private val bm: Bitmap) : BitmapRegionLoader {
    override suspend fun load(): Bitmap {
        return bm
    }
}

private class CacheBitmapRegionLoader(
    private val origin: BitmapRegionLoader,
    private val caches: BitmapRegionCaches
) : BitmapRegionLoader {

    @Volatile
    private var cache: Bitmap? = null
    private val mutex = Mutex()

    override suspend fun load(): Bitmap? {
        val localCache = cache
        if (localCache != null) {
            return localCache
        }
        return mutex.withLock {
            if (cache != null) {
                return cache
            }
            origin.load().also {
                cache = it
                caches.doWhenLoaded(this)
            }
        }
    }

    suspend fun releaseCache() {
        mutex.withLock {
            cache = null
        }
    }
}

/**
 * fit:
 *  if ture, fit the image to the dst so that both dimensions (width and height) of the image will be equal to or less than the dst
 *  if false, fill the image in the dst such that both dimensions (width and height) of the image will be equal to or larger than the dst
 */
fun loadLongImageThumbnail(
    ins: InputStream,
    preferredSize: IntSize,
    options: BitmapFactory.Options,
    fit: Boolean = false,
): Bitmap? {
    return loadLongImage(ins, preferredSize, options, fit) { regionDecoder ->
        val w = regionDecoder.width
        val h = regionDecoder.height
        val pageHeight = if (preferredSize.width > 0 && preferredSize.height > 0) {
            (w * preferredSize.height / preferredSize.width).coerceAtMost(w * 5).coerceAtMost(h)
        } else {
            (5 * w).coerceAtMost(h)
        }
        regionDecoder.decodeRegion(Rect(0, 0, w, pageHeight), options)
    }
}

/**
 * fit:
 *  if ture, fit the image to the dst so that both dimensions (width and height) of the image will be equal to or less than the dst
 *  if false, fill the image in the dst such that both dimensions (width and height) of the image will be equal to or larger than the dst
 */
fun loadLongImage(
    ins: InputStream,
    preferredSize: IntSize,
    options: BitmapFactory.Options,
    fit: Boolean = false,
    preloadCount: Int = Int.MAX_VALUE,
    cacheTimeoutForLazyLoad: Long = 1000,
    cacheCountForLazyLoad: Int = 5
): BitmapRegions {
    val caches = BitmapRegionCaches(cacheTimeoutForLazyLoad, cacheCountForLazyLoad)
    return loadLongImage(ins, preferredSize, options, fit) { regionDecoder ->
        val w = regionDecoder.width
        val h = regionDecoder.height
        val pageHeight = if (preferredSize.width > 0 && preferredSize.height > 0) {
            (w * preferredSize.height / preferredSize.width).coerceAtMost(w * 5).coerceAtMost(h)
        } else {
            (5 * w).coerceAtMost(h)
        }

        val ret = arrayListOf<BitmapRegionProvider>()
        var top = 0
        var i = 0
        while (top < h) {
            val bottom = (top + pageHeight).coerceAtMost(h)
            if (i < preloadCount) {
                val bm = regionDecoder.decodeRegion(Rect(0, top, w, bottom), options)
                ret.add(BitmapRegionProvider(bm.width, bm.height, EmoAlreadyBitmapRegionLoader(bm)))
            } else {
                val finalTop = top
                val loader = object : BitmapRegionLoader {

                    private val mutex = Mutex()

                    override suspend fun load(): Bitmap? {
                        return mutex.withLock {
                            regionDecoder.decodeRegion(Rect(0, finalTop, w, bottom), options)
                        }
                    }
                }
                ret.add(
                    BitmapRegionProvider(
                        w, bottom - finalTop,
                        if (caches.canCache()) {
                            CacheBitmapRegionLoader(loader, caches)
                        } else {
                            loader
                        }
                    )
                )
            }
            top = bottom
            i++
        }

        BitmapRegions(w, h, ret)
    }
}

private fun <T> loadLongImage(
    ins: InputStream,
    preferredSize: IntSize,
    options: BitmapFactory.Options,
    fit: Boolean = false,
    handler: (BitmapRegionDecoder) -> T
): T {
    // Read the image's dimensions.
    options.inJustDecodeBounds = true
    val bufferedIns = ins.buffered()
    bufferedIns.mark(Int.MAX_VALUE)
    BitmapFactory.decodeStream(bufferedIns, null, options)
    options.inJustDecodeBounds = false
    bufferedIns.reset()

    options.inMutable = false

    if (options.outWidth > 0 && options.outHeight > 0) {
        val dstWidth = if (preferredSize.width <= 0) options.outWidth else preferredSize.width
        val dstHeight = if (preferredSize.height <= 0) options.outHeight else preferredSize.height
        options.inSampleSize = calculateInSampleSize(
            srcWidth = options.outWidth,
            srcHeight = options.outHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            fit = fit
        )
    } else {
        options.inSampleSize = 1
    }

    val regionDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(bufferedIns)
    } else {
        BitmapRegionDecoder.newInstance(bufferedIns, false)
    }
    checkNotNull(regionDecoder) { "BitmapRegionDecoder newInstance failed." }
    return handler(regionDecoder)
}

private fun calculateInSampleSize(
    srcWidth: Int,
    srcHeight: Int,
    dstWidth: Int,
    dstHeight: Int,
    fit: Boolean = false
): Int {
    val widthInSampleSize = Integer.highestOneBit(srcWidth / dstWidth)
    val heightInSampleSize = Integer.highestOneBit(srcHeight / dstHeight)
    return if (fit) {
        max(widthInSampleSize, heightInSampleSize).coerceAtLeast(1)
    } else {
        min(widthInSampleSize, heightInSampleSize).coerceAtLeast(1)
    }
}

private class BitmapRegionCaches(
    val cacheTimeoutForLazyLoad: Long,
    val cacheCountForLazyLoad: Int
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val cacheJobs = object : LruCache<CacheBitmapRegionLoader, Job>(cacheCountForLazyLoad) {
        override fun entryRemoved(evicted: Boolean, key: CacheBitmapRegionLoader?, oldValue: Job?, newValue: Job?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (newValue == null) {
                key?.let {
                    scope.launch {
                        it.releaseCache()
                    }
                }
            } else {
                oldValue?.cancel()
            }
        }
    }

    fun doWhenLoaded(loader: CacheBitmapRegionLoader) {
        val job = scope.launch {
            delay(cacheTimeoutForLazyLoad)
            cacheJobs.remove(loader)
        }
        cacheJobs.put(loader, job)
    }

    fun canCache(): Boolean {
        return cacheTimeoutForLazyLoad > 0 && cacheCountForLazyLoad > 0
    }
}

class BitmapRegionHolderDrawable(val bitmapRegion: BitmapRegions) : Drawable() {

    override fun getIntrinsicHeight(): Int {
        return bitmapRegion.height
    }

    override fun getIntrinsicWidth(): Int {
        return bitmapRegion.width
    }

    override fun draw(canvas: Canvas) {
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }
}
