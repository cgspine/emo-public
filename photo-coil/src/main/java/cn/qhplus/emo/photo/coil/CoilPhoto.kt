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

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toBitmap
import cn.qhplus.emo.photo.data.BitmapRegionHolderDrawable
import cn.qhplus.emo.photo.data.BitmapRegionProvider
import cn.qhplus.emo.photo.data.Photo
import cn.qhplus.emo.photo.data.PhotoProvider
import cn.qhplus.emo.photo.data.PhotoResult
import cn.qhplus.emo.photo.data.PhotoShot
import cn.qhplus.emo.photo.data.PhotoShotRecover
import cn.qhplus.emo.photo.ui.BitmapRegionItem
import cn.qhplus.emo.photo.ui.ThumbBlankBox
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class CoilThumbPhoto(
    val uri: Uri,
    val isLongImage: Boolean,
    val openBlankColor: Boolean
) : Photo {
    @Composable
    override fun Compose(
        contentScale: ContentScale,
        isContainerDimenExactly: Boolean,
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        if (isLongImage) {
            LongImage(onSuccess, onError, openBlankColor)
        } else {
            val context = LocalContext.current
            val model = remember(context, uri, onSuccess, onError) {
                ImageRequest.Builder(context)
                    .data(uri)
                    .setParameter("isThumb", true)
                    .crossfade(true)
                    .decoderFactory(CoilImageDecoderFactory.defaultInstance)
                    .listener(onError = { _, result ->
                        onError?.invoke(result.throwable)
                    }) { _, result ->
                        onSuccess?.invoke(PhotoResult(uri, result.drawable))
                    }.build()
            }
            SubcomposeAsyncImage(
                model = model,
                contentDescription = "",
                contentScale = if (isContainerDimenExactly) contentScale else ContentScale.Inside,
                alignment = Alignment.Center,
                modifier = Modifier.let {
                    if (isContainerDimenExactly) {
                        it.fillMaxSize()
                    } else {
                        it
                    }
                }
            ) {
                val state = painter.state
                if (state == AsyncImagePainter.State.Empty ||
                    state is AsyncImagePainter.State.Loading
                ) {
                    if (isContainerDimenExactly && openBlankColor) {
                        ThumbBlankBox()
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }

    @Composable
    fun LongImage(
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?,
        openBlankColor: Boolean
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val request = ImageRequest.Builder(LocalContext.current)
                .setParameter("isThumb", true)
                .setParameter("isLongImage", true)
                .crossfade(true)
                .decoderFactory(CoilImageDecoderFactory.defaultInstance)
                .data(uri)
                .scale(Scale.FILL)
                .size(constraints.maxWidth, constraints.maxHeight)
                .build()
            LongImageContent(request, onSuccess, onError, openBlankColor)
        }
    }

    @Composable
    fun LongImageContent(
        request: ImageRequest,
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?,
        openBlankColor: Boolean
    ) {
        val imageLoader = LocalContext.current.imageLoader
        var bitmap by remember("") {
            mutableStateOf<Bitmap?>(null)
        }
        LaunchedEffect("") {
            withContext(Dispatchers.IO) {
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    bitmap = result.drawable.toBitmap()
                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke(PhotoResult(uri, result.drawable))
                    }
                } else if (result is ErrorResult) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke(result.throwable)
                    }
                }
            }
        }
        val bm = bitmap
        if (bm != null) {
            Image(
                painter = BitmapPainter(bm.asImageBitmap()),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        } else if (openBlankColor) {
            ThumbBlankBox()
        }
    }
}

class CoilPhoto(
    val uri: Uri,
    val isLongImage: Boolean
) : Photo {

    @Composable
    override fun Compose(
        contentScale: ContentScale,
        isContainerDimenExactly: Boolean,
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        if (isLongImage) {
            LongImage(onSuccess, onError)
        } else {
            val context = LocalContext.current
            val model = remember(context, uri, onSuccess, onError) {
                ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .decoderFactory(CoilImageDecoderFactory.defaultInstance)
                    .listener(onError = { _, result ->
                        onError?.invoke(result.throwable)
                    }) { _, result ->
                        onSuccess?.invoke(PhotoResult(uri, result.drawable))
                    }.build()
            }
            AsyncImage(
                model = model,
                contentDescription = "",
                contentScale = contentScale,
                alignment = Alignment.Center,
                modifier = Modifier.let {
                    if (isContainerDimenExactly) {
                        it.fillMaxSize()
                    } else {
                        it
                    }
                }
            )
        }
    }

    @Composable
    fun LongImage(
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            var images by remember {
                mutableStateOf(emptyList<BitmapRegionProvider>())
            }
            val context = LocalContext.current
            LaunchedEffect(key1 = constraints.maxWidth, key2 = constraints.maxHeight) {
                val result = withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .size(constraints.maxWidth, constraints.maxHeight)
                        .scale(Scale.FILL)
                        .setParameter("isLongImage", true)
                        .decoderFactory(CoilImageDecoderFactory.defaultInstance)
                        .build()
                    context.imageLoader.execute(request)
                }
                if (result is SuccessResult) {
                    (result.drawable as? BitmapRegionHolderDrawable)?.bitmapRegion?.let {
                        images = it.list
                    }
                    onSuccess?.invoke(PhotoResult(uri, result.drawable))
                } else if (result is ErrorResult) {
                    onError?.invoke(result.throwable)
                }
            }
            if (images.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(images) { image ->
                        BoxWithConstraints() {
                            val width = constraints.maxWidth
                            val height = width * image.height / image.width
                            val heightDp = with(LocalDensity.current) {
                                height.toDp()
                            }
                            BitmapRegionItem(image, maxWidth, heightDp)
                        }
                    }
                }
            }
        }
    }
}

open class CoilPhotoProvider(
    val uri: Uri,
    val thumbUri: Uri = uri,
    val ratio: Float
) : PhotoProvider {

    companion object {
        const val META_URI_KEY = "meta_uri"
        const val META_THUMB_URI_KEY = "meta_thumb_uri"
        const val META_RATIO_KEY = "meta_ratio"
    }

    override fun thumbnail(openBlankColor: Boolean): Photo? {
        return CoilThumbPhoto(thumbUri, isLongImage(), openBlankColor)
    }

    override fun photo(): Photo? {
        return CoilPhoto(uri, isLongImage())
    }

    override fun ratio(): Float {
        return ratio
    }

    override fun isLongImage(): Boolean {
        return ratio > 0 && ratio < 0.2f
    }

    override fun meta(): Bundle? {
        return Bundle().apply {
            putParcelable(META_URI_KEY, uri)
            if (thumbUri != uri) {
                putParcelable(META_THUMB_URI_KEY, thumbUri)
            }
            putFloat(META_RATIO_KEY, ratio)
        }
    }

    override fun recoverCls(): Class<out PhotoShotRecover>? {
        return CoilPhotoShotRecover::class.java
    }
}

class CoilPhotoShotRecover : PhotoShotRecover {
    @Suppress("DEPRECATION")
    override fun recover(bundle: Bundle): PhotoShot? {
        val uri = bundle.getParcelable<Uri>(CoilPhotoProvider.META_URI_KEY) ?: return null
        val thumbUri = bundle.getParcelable(CoilPhotoProvider.META_THUMB_URI_KEY) ?: uri
        val ratio = bundle.getFloat(CoilPhotoProvider.META_RATIO_KEY)
        return PhotoShot(
            CoilPhotoProvider(uri, thumbUri, ratio),
            null,
            null,
            null
        )
    }
}
