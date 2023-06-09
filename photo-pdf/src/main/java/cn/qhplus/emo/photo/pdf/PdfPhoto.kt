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

package cn.qhplus.emo.photo.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import cn.qhplus.emo.core.LogTag
import cn.qhplus.emo.core.closeQuietly
import cn.qhplus.emo.photo.data.Photo
import cn.qhplus.emo.photo.data.PhotoProvider
import cn.qhplus.emo.photo.data.PhotoResult
import cn.qhplus.emo.photo.data.PhotoShot
import cn.qhplus.emo.photo.data.PhotoShotRecover
import cn.qhplus.emo.photo.ui.edit.EditLayer
import cn.qhplus.emo.photo.ui.edit.EditLayerList
import cn.qhplus.emo.ui.core.Loading
import cn.qhplus.emo.ui.core.PressWithAlphaBox
import cn.qhplus.emo.ui.core.emoTopBarHeight
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonTopPadding
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

@Stable
class PdfPage(
    val dataSource: PdfDataSource,
    val page: Int,
    initRatio: Float = 1f,
    private val channel: Channel<PdfBitmapMsg>
) : LogTag {
    val bitmap = mutableStateOf<WeakReference<Bitmap>?>(null)
    val failed = mutableStateOf(false)
    val editLayers = mutableStateOf<PersistentList<EditLayer>>(persistentListOf())
    val ratio = mutableStateOf(initRatio)

    suspend fun load(width: Int) {
        try {
            channel.send(PdfBitmapMsg(width, this))
            val list = dataSource.loadEditLayers(page)
            editLayers.value = list
        } catch (e: Throwable) {
            if (e !is CancellationException) {
                failed.value = true
            }
        }
    }
}

class PdfBitmapMsg(val width: Int, val page: PdfPage)

class PdfDrawable(val source: PdfDataSource, val list: PersistentList<PdfPage>) : Drawable() {
    override fun draw(canvas: Canvas) {
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}

@Stable
interface PdfDataSource {

    val title: State<String>
    fun readInitIndex(context: Context): Int
    fun readInitOffset(context: Context): Int
    suspend fun getFileDescriptor(context: Context): ParcelFileDescriptor?
    suspend fun saveIndexAndOffset(index: Int, offset: Int)

    fun supportEdit(page: Int): Boolean
    suspend fun saveEditLayers(page: Int, layers: PersistentList<EditLayer>)
    suspend fun loadEditLayers(page: Int): PersistentList<EditLayer>
}

data class DefaultUriPdfDataSource(private val uri: Uri) : PdfDataSource {

    override val title: State<String>
        get() = mutableStateOf("")

    override fun readInitIndex(context: Context): Int {
        return 0
    }

    override fun readInitOffset(context: Context): Int {
        return 0
    }

    override suspend fun getFileDescriptor(context: Context): ParcelFileDescriptor? {
        return context.contentResolver.openFileDescriptor(uri, "r")
    }

    override suspend fun saveIndexAndOffset(index: Int, offset: Int) {
        // default do nothing.
    }

    override fun supportEdit(page: Int): Boolean = false

    override suspend fun saveEditLayers(page: Int, layers: PersistentList<EditLayer>) {
    }

    override suspend fun loadEditLayers(page: Int): PersistentList<EditLayer> {
        return persistentListOf()
    }
}

@Composable
fun PdfContent(
    listState: LazyListState,
    pages: PersistentList<PdfPage>,
    firstPagePaddingForTopBar: Boolean = false
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(pages, key = { _, page -> page.page }) { index, page ->
                PdfPage(width = maxWidth, page, if (index == 0) firstPagePaddingForTopBar else false)
            }
        }
    }
}

@Composable
fun PdfBox(
    dataSource: PdfDataSource,
    listState: LazyListState,
    firstPagePaddingForTopBar: Boolean = false,
    onSuccess: ((PdfDrawable) -> Unit)?,
    onError: ((Throwable) -> Unit)?
) {
    val scope = rememberCoroutineScope()
    val pdfPages = remember {
        mutableStateOf(persistentListOf<PdfPage>())
    }
    val context = LocalContext.current.applicationContext
    DisposableEffect(dataSource) {
        val job = scope.launch {
            withContext(Dispatchers.IO) {
                var fd: ParcelFileDescriptor? = null
                try {
                    fd = dataSource.getFileDescriptor(context) ?: throw RuntimeException(
                        "openFileDescriptor failed for dataSource: $dataSource"
                    )
                    PdfRenderer(fd).use { pdfRender ->
                        val caches = LruCache<Int, Bitmap>(5)
                        val channel = Channel<PdfBitmapMsg>()
                        val list = if (pdfRender.pageCount == 0) {
                            persistentListOf()
                        } else {
                            val checkIndex = listState.firstVisibleItemIndex.coerceAtMost(pdfRender.pageCount - 1)
                            val checkPage = pdfRender.openPage(checkIndex)
                            val ratio = checkPage.width * 1f / checkPage.height
                            checkPage.close()
                            (0 until pdfRender.pageCount).asSequence().map {
                                PdfPage(dataSource, it, ratio, channel)
                            }.toPersistentList()
                        }
                        pdfPages.value = list
                        onSuccess?.invoke(PdfDrawable(dataSource, list))
                        for (msg in channel) {
                            try {
                                val cache = caches.get(msg.page.page)
                                if (cache != null) {
                                    msg.page.bitmap.value = WeakReference(cache)
                                } else {
                                    val page = pdfRender.openPage(msg.page.page)
                                    msg.page.ratio.value = page.width * 1f / page.height
                                    val ratio = msg.width * 1f / page.width
                                    val bitmap = Bitmap.createBitmap(msg.width, (page.height * ratio).toInt(), Bitmap.Config.ARGB_8888)
                                    caches.put(msg.page.page, bitmap)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    page.close()
                                    msg.page.bitmap.value = WeakReference(bitmap)
                                }
                            } catch (e: Throwable) {
                                if (e !is CancellationException) {
                                    msg.page.failed.value = true
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    fd?.closeQuietly()
                    if (e !is CancellationException) {
                        onError?.invoke(e)
                    }
                }
            }
        }
        onDispose {
            job.cancel()
        }
    }
    val pages = pdfPages.value
    PdfContent(listState, pages, firstPagePaddingForTopBar)
    PdfRecordIndex(dataSource, listState)
}

@Composable
fun PdfRecordIndex(dataSource: PdfDataSource, listState: LazyListState) {
    val recordInfo = remember {
        derivedStateOf { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
    }
    LaunchedEffect(recordInfo.value) {
        dataSource.saveIndexAndOffset(recordInfo.value.first, recordInfo.value.second)
    }
}

@Composable
fun PdfPage(
    width: Dp,
    page: PdfPage,
    paddingForTopBar: Boolean
) {
    val bitmap = page.bitmap.value?.get()
    val failed = page.failed.value
    val widthPx = with(LocalDensity.current) {
        width.toPx().toInt()
    }
    if (bitmap == null) {
        LaunchedEffect(page.page, widthPx) {
            page.load(widthPx)
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .background(Color.White)
            .width(width)
            .let {
                if (paddingForTopBar) {
                    it
                        .windowInsetsCommonTopPadding()
                        .padding(top = emoTopBarHeight)
                } else {
                    it
                }
            }
            .height(width / page.ratio.value)
    ) {
        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            PdfLayerList(page)
        } else {
            val scope = rememberCoroutineScope()
            if (failed) {
                PressWithAlphaBox(onClick = {
                    scope.launch {
                        page.load(widthPx)
                    }
                }) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "加载失败, 点击重试",
                        fontSize = 15.sp
                    )
                }
            } else {
                Loading(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.PdfLayerList(pdfPage: PdfPage) {
    val list = pdfPage.editLayers.value
    if (list.isNotEmpty()) {
        EditLayerList(list = list)
    }
}

open class PdfThumbPhoto(
    val dataSource: PdfDataSource
) : Photo {
    @Composable
    override fun Compose(
        contentScale: ContentScale,
        isContainerDimenExactly: Boolean,
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val context = LocalContext.current.applicationContext
        val scope = rememberCoroutineScope()
        val bm = remember {
            mutableStateOf<Bitmap?>(null)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            DisposableEffect(dataSource, constraints.maxWidth) {
                val job = scope.launch {
                    withContext(Dispatchers.IO) {
                        var fd: ParcelFileDescriptor? = null
                        try {
                            fd = dataSource.getFileDescriptor(context) ?: throw RuntimeException(
                                "openFileDescriptor failed for dataSource: $dataSource"
                            )
                            PdfRenderer(fd).use { pdfRender ->
                                if (pdfRender.pageCount > 0) {
                                    val firstPage = pdfRender.openPage(0)
                                    val ratio = firstPage.width * 1f / firstPage.height
                                    val height = constraints.maxWidth / ratio
                                    val bitmap = Bitmap.createBitmap(constraints.maxWidth, height.toInt(), Bitmap.Config.ARGB_8888)
                                    firstPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    firstPage.close()
                                    bm.value = bitmap
                                    onSuccess?.invoke(PhotoResult(dataSource, bitmap.toDrawable(context.resources)))
                                }
                            }
                        } catch (e: Throwable) {
                            fd?.closeQuietly()
                            if (e !is CancellationException) {
                                onError?.invoke(e)
                            }
                        }
                    }
                }
                onDispose {
                    job.cancel()
                }
            }

            val bitmap = bm.value
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = "",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

open class PdfPhoto(
    val dataSource: PdfDataSource
) : Photo {

    @Composable
    override fun Compose(
        contentScale: ContentScale,
        isContainerDimenExactly: Boolean,
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val context = LocalContext.current.applicationContext
        val listState = rememberSaveable(saver = LazyListState.Saver) {
            LazyListState(
                dataSource.readInitIndex(context),
                dataSource.readInitOffset(context)
            )
        }
        PdfBox(dataSource, listState, false, {
            onSuccess?.invoke(PhotoResult(dataSource, it))
        }, onError)
    }
}

@Stable
open class PdfPhotoProvider(
    val uri: Uri
) : PhotoProvider {

    companion object {
        const val META_URI_KEY = "meta_uri"
    }

    val dataSource = DefaultUriPdfDataSource(uri)

    override fun id(): Any {
        return uri
    }

    override fun thumbnail(openBlankColor: Boolean): Photo? {
        return null
    }

    override fun photo(): Photo? {
        return PdfPhoto(dataSource)
    }

    override fun ratio(): Float {
        return 0f
    }

    override fun isLongImage(): Boolean {
        return true
    }

    override fun meta(): Bundle? {
        return Bundle().apply {
            putParcelable(META_URI_KEY, uri)
        }
    }

    override fun recoverCls(): Class<out PhotoShotRecover>? {
        return PdfPhotoShotRecover::class.java
    }
}

class PdfPhotoShotRecover : PhotoShotRecover {

    override fun recover(bundle: Bundle): PhotoShot? {
        @Suppress("DEPRECATION")
        val uri = bundle.getParcelable<Uri>(PdfPhotoProvider.META_URI_KEY) ?: return null
        return PhotoShot(
            PdfPhotoProvider(uri),
            null,
            null,
            null
        )
    }
}
