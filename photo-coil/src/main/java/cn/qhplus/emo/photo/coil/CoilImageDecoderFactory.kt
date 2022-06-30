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

package cn.qhplus.emo.photo.coil

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.unit.IntSize
import cn.qhplus.emo.photo.data.BitmapRegionHolderDrawable
import cn.qhplus.emo.photo.data.loadLongImage
import cn.qhplus.emo.photo.data.loadLongImageThumbnail
import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Scale
import coil.size.pxOrElse
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class CoilImageDecoderFactory(maxParallelism: Int = 4) : Decoder.Factory {

    companion object {
        val defaultInstance by lazy {
            CoilImageDecoderFactory()
        }
    }

    private val parallelismLock = Semaphore(maxParallelism)

    override fun create(
        result: SourceResult,
        options: Options,
        imageLoader: ImageLoader
    ): Decoder? {
        return if ((options.parameters.entry("isLongImage")?.value as? Boolean) == true) {
            CoilLongImageDecoder(result.source, options, parallelismLock)
        } else {
            BitmapFactoryDecoder(result.source, options, parallelismLock)
        }
    }
}

class CoilLongImageDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE)
) : Decoder {

    private val isThumb = options.parameters.entry("isThumb")?.value == true

    override suspend fun decode(): DecodeResult = parallelismLock.withPermit {
        runInterruptible { decode(BitmapFactory.Options()) }
    }

    private fun decode(bmOptions: BitmapFactory.Options): DecodeResult {
        val ins = source.source().inputStream()
        val (width, height) = options.size
        val dstWidth = width.pxOrElse { -1 }
        val dstHeight = height.pxOrElse { -1 }
        if (isThumb) {
            val bm = loadLongImageThumbnail(
                ins,
                IntSize(dstWidth, dstHeight),
                bmOptions,
                options.scale == Scale.FIT
            )
            return DecodeResult(
                drawable = BitmapDrawable(options.context.resources, bm),
                isSampled = bmOptions.inSampleSize > 1
            )
        } else {
            val bitmapRegion = loadLongImage(
                ins,
                IntSize(dstWidth, dstHeight),
                bmOptions,
                options.scale == Scale.FIT,
                preloadCount = 2
            )
            return DecodeResult(
                drawable = BitmapRegionHolderDrawable(bitmapRegion),
                isSampled = bmOptions.inSampleSize > 1 || bmOptions.inScaled
            )
        }
    }
}
