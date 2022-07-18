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

package cn.qhplus.emo.photo.ui.viewer

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cn.qhplus.emo.photo.activity.MutableDrawableCache
import cn.qhplus.emo.photo.data.PhotoLoadStatus
import cn.qhplus.emo.photo.data.PhotoResult
import cn.qhplus.emo.photo.data.PhotoShot
import cn.qhplus.emo.photo.ui.GesturePhoto
import cn.qhplus.emo.ui.core.Loading
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerScope
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.StateFlow

class PhotoPageCtrl(
    val transitionTargetFlow: StateFlow<Boolean>,
    val onTapExit: (page: Int, afterTransition: Boolean) -> Unit,
    val onLongClick: (page: Int, drawable: Drawable) -> Unit,
    val loading: @Composable BoxScope.() -> Unit = {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Loading(size = 48.dp)
        }
    },
    val loadingFailed: (@Composable BoxScope.() -> Unit)? = null,
    val pullExitMiniTranslateY: Dp = 72.dp,
    val shouldTransition: Boolean = true,
    val allowPullExit: Boolean = true
)

class PhotoViewerArg(
    val list: List<PhotoShot>,
    val index: Int,
    val photoPageCtrl: PhotoPageCtrl
)

class PhotoPageArg(
    val page: Int,
    val item: PhotoShot,
    val shouldTransitionEnter: Boolean,
    val ctrl: PhotoPageCtrl
)

class PhotoPageContentArg(
    val transition: Transition<Boolean>,
    val photoShot: PhotoShot,
    val onPhotoLoaded: (PhotoResult) -> Unit,
    val photoPageCtrl: PhotoPageCtrl
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PhotoViewerScaffold(
    viewerArg: PhotoViewerArg,
    ConfigProvider: @Composable (
        @Composable () -> Unit
    ) -> Unit = {
        DefaultPhotoViewerConfigProvider(it)
    },
    PhotoViewer: @Composable (
        PhotoViewerArg,
        @Composable PagerScope.(PhotoPageArg) -> Unit
    ) -> Unit = { arg, photoPage ->
        DefaultPhotoViewer(arg, photoPage)
    },
    PhotoPage: @Composable PagerScope.(
        PhotoPageArg,
        @Composable (PhotoPageContentArg) -> Unit
    ) -> Unit = { arg, photoPageContent ->
        DefaultPhotoPage(arg, photoPageContent)
    },
    PhotoPageContent: @Composable (PhotoPageContentArg) -> Unit = { arg ->
        DefaultPhotoPageContent(arg)
    }
) {
    ConfigProvider {
        PhotoViewer(viewerArg) { arg ->
            PhotoPage(arg) {
                PhotoPageContent(it)
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun DefaultPhotoViewer(
    arg: PhotoViewerArg,
    PhotoPage: @Composable PagerScope.(PhotoPageArg) -> Unit
) {
    val pagerState = rememberPagerState(arg.index)
    HorizontalPager(
        count = arg.list.size,
        state = pagerState
    ) { page ->
        PhotoPage(PhotoPageArg(page, arg.list[page], page == arg.index, arg.photoPageCtrl))
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerScope.DefaultPhotoPage(
    pageArg: PhotoPageArg,
    PhotoPageContent: @Composable (PhotoPageContentArg) -> Unit,
    GestureBox: @Composable BoxWithConstraintsScope.(
        PhotoShot,
        @Composable BoxWithConstraintsScope.(onPhotoLoaded: ((PhotoResult) -> Unit)?) -> Unit
    ) -> Unit = { _, content ->
        DefaultPhotoGestureBox(content)
    }
) {
    val initRect = pageArg.item.photoRect()
    val transitionTarget = if (currentPage == pageArg.page) {
        pageArg.ctrl.transitionTargetFlow.collectAsState().value
    } else true
    val drawableCache = remember {
        MutableDrawableCache()
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        GestureBox(pageArg.item) {
            GesturePhoto(
                containerWidth = maxWidth,
                containerHeight = maxHeight,
                imageRatio = pageArg.item.ratio(),
                isLongImage = pageArg.item.photoProvider.isLongImage(),
                initRect = initRect,
                shouldTransitionEnter = pageArg.shouldTransitionEnter && pageArg.ctrl.shouldTransition,
                shouldTransitionExit = pageArg.ctrl.shouldTransition,
                transitionTarget = transitionTarget,
                pullExitMiniTranslateY = pageArg.ctrl.pullExitMiniTranslateY,
                onBeginPullExit = {
                    pageArg.ctrl.allowPullExit
                },
                onLongPress = {
                    drawableCache.drawable?.let {
                        pageArg.ctrl.onLongClick(pageArg.page, it)
                    }
                },
                onTapExit = {
                    pageArg.ctrl.onTapExit(pageArg.page, it)
                }
            ) { transition, _, _, onImageRatioEnsured ->

                val onPhotoLoad: (PhotoResult) -> Unit = remember(drawableCache, onImageRatioEnsured) {
                    {
                        drawableCache.drawable = it.drawable
                        if (it.drawable.intrinsicWidth > 0 && it.drawable.intrinsicHeight > 0) {
                            onImageRatioEnsured(it.drawable.intrinsicWidth.toFloat() / it.drawable.intrinsicHeight)
                        }
                    }
                }

                val contentArg = remember(transition, pageArg.item, onPhotoLoad) {
                    PhotoPageContentArg(transition, pageArg.item, onPhotoLoad, pageArg.ctrl)
                }

                PhotoPageContent(contentArg)
            }
        }
    }
}

@Composable
fun BoxWithConstraintsScope.DefaultPhotoGestureBox(
    content: @Composable BoxWithConstraintsScope.(onPhotoLoaded: ((PhotoResult) -> Unit)?) -> Unit
) {
    content(null)
}

@Composable
fun DefaultPhotoPageContent(
    arg: PhotoPageContentArg
) {
    val thumb = remember(arg) { arg.photoShot.photoProvider.thumbnail(false) }

    var loadStatus by remember {
        mutableStateOf(PhotoLoadStatus.Loading)
    }

    val onSuccess: (PhotoResult) -> Unit = remember(arg) {
        {
            arg.onPhotoLoaded(it)
            loadStatus = PhotoLoadStatus.Success
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PhotoItem(
            arg.photoShot,
            onSuccess = onSuccess,
            onError = {
                loadStatus = PhotoLoadStatus.Failed
            }
        )

        if (loadStatus != PhotoLoadStatus.Success ||
            !arg.transition.currentState ||
            !arg.transition.targetState
        ) {
            val transitionPhoto = arg.photoShot.photo
            val contentScale = when {
                arg.photoShot.photoProvider.isLongImage() -> {
                    ContentScale.FillWidth
                }
                arg.photoShot.ratio() > 0f &&
                    arg.photoShot.offsetInWindow != null &&
                    arg.photoShot.size != null -> {
                    ContentScale.Crop
                }
                else -> ContentScale.Fit
            }
            if (transitionPhoto != null) {
                Image(
                    painter = BitmapPainter(transitionPhoto.toBitmap().asImageBitmap()),
                    contentDescription = "",
                    alignment = if (arg.photoShot.photoProvider.isLongImage()) {
                        Alignment.TopCenter
                    } else Alignment.Center,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                thumb?.Compose(
                    contentScale = contentScale,
                    isContainerDimenExactly = true,
                    onSuccess = null,
                    onError = null
                )
            }
        }

        if (loadStatus == PhotoLoadStatus.Loading) {
            arg.photoPageCtrl.loading.let {
                it()
            }
        } else if (loadStatus == PhotoLoadStatus.Failed) {
            arg.photoPageCtrl.loadingFailed?.let {
                it()
            }
        }
    }
}

@Composable
private fun PhotoItem(
    photoShot: PhotoShot,
    onSuccess: ((PhotoResult) -> Unit)?,
    onError: ((Throwable) -> Unit)? = null
) {
    val photo = remember(photoShot) {
        photoShot.photoProvider.photo()
    }
    photo?.Compose(
        contentScale = ContentScale.Fit,
        isContainerDimenExactly = true,
        onSuccess = onSuccess,
        onError = onError
    )
}
