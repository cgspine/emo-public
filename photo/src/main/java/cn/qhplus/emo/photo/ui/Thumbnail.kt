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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cn.qhplus.emo.photo.activity.PhotoViewerActivity
import cn.qhplus.emo.photo.data.Photo
import cn.qhplus.emo.photo.data.PhotoProvider
import cn.qhplus.emo.photo.data.PhotoResult
import cn.qhplus.emo.photo.data.PhotoShot
import cn.qhplus.emo.photo.ui.viewer.LocalPhotoViewerConfig
import cn.qhplus.emo.photo.util.getWindowSize

const val SINGLE_HIGH_IMAGE_MINI_SCREEN_HEIGHT_RATIO = -1F

class PhotoThumbnailConfig(
    val singleSquireImageWidthRatio: Float = 0.5f,
    val singleWideImageMaxWidthRatio: Float = 0.667f,
    val singleHighImageDefaultWidthRatio: Float = 0.5f,
    val singleHighImageMiniHeightRatio: Float = SINGLE_HIGH_IMAGE_MINI_SCREEN_HEIGHT_RATIO,
    val singleLongImageWidthRatio: Float = 0.5f,
    val averageIfTwoImage: Boolean = true,
    val horGap: Dp = 5.dp,
    val verGap: Dp = 5.dp,
    val alphaWhenPressed: Float = 1f
)

val emoDefaultPhotoThumbnailConfig = PhotoThumbnailConfig()

@Composable
private fun PhotoThumbnailItem(
    thumb: Photo?,
    width: Dp,
    height: Dp,
    alphaWhenPressed: Float,
    isContainerDimenExactly: Boolean,
    onLayout: (offset: Offset, size: IntSize) -> Unit,
    onPhotoLoaded: (PhotoResult) -> Unit,
    click: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .let {

                if (click != null) {
                    it
                        .clickable(interactionSource, null) {
                            click.invoke()
                        }
                        .alpha(if (isPressed.value) alphaWhenPressed else 1f)
                } else {
                    it
                }
            }
            .onGloballyPositioned {
                onLayout(it.positionInWindow(), it.size)
            }
    ) {
        thumb?.Compose(
            contentScale = if (isContainerDimenExactly) ContentScale.Crop else ContentScale.Fit,
            isContainerDimenExactly = isContainerDimenExactly,
            onSuccess = {
                onPhotoLoaded(it)
            },
            onError = null
        )
    }
}

@Composable
fun PhotoThumbnailWithViewer(
    targetActivity: Class<out PhotoViewerActivity> = PhotoViewerActivity::class.java,
    images: List<PhotoProvider>,
    config: PhotoThumbnailConfig = remember { emoDefaultPhotoThumbnailConfig }
) {
    val context = LocalContext.current
    PhotoThumbnail(images, config) { list, index ->
        val intent = PhotoViewerActivity.intentOf(context, targetActivity, list, index)
        context.startActivity(intent)
        context.findActivity()?.overridePendingTransition(0, 0)
    }
}

internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun PhotoThumbnail(
    images: List<PhotoProvider>,
    config: PhotoThumbnailConfig = remember { emoDefaultPhotoThumbnailConfig },
    onClick: ((images: List<PhotoShot>, index: Int) -> Unit)? = null
) {
    if (images.size < 0) {
        return
    }
    val renderInfo = remember(images) {
        Array(images.size) {
            PhotoShot(images[it], null, null, null)
        }
    }
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (images.size == 1) {
            val image = images[0]
            val thumb = remember(image) {
                image.thumbnail(true)
            }
            if (thumb != null) {
                val ratio = image.ratio()
                when {
                    ratio <= 0 -> {
                        PhotoThumbnailItem(
                            thumb,
                            Dp.Unspecified,
                            Dp.Unspecified,
                            config.alphaWhenPressed,
                            isContainerDimenExactly = false,
                            onLayout = { offset, size ->
                                renderInfo[0].offsetInWindow = offset
                                renderInfo[0].size = size
                            },
                            onPhotoLoaded = {
                                renderInfo[0].photo = it.drawable
                            },
                            click = if (onClick != null) {
                                {
                                    onClick.invoke(renderInfo.toList(), 0)
                                }
                            } else null
                        )
                    }
                    ratio == 1f -> {
                        val wh = maxWidth * config.singleSquireImageWidthRatio
                        PhotoThumbnailItem(
                            thumb,
                            wh,
                            wh,
                            config.alphaWhenPressed,
                            isContainerDimenExactly = true,
                            onLayout = { offset, size ->
                                renderInfo[0].offsetInWindow = offset
                                renderInfo[0].size = size
                            },
                            onPhotoLoaded = {
                                renderInfo[0].photo = it.drawable
                            },
                            click = if (onClick != null) {
                                {
                                    onClick.invoke(renderInfo.toList(), 0)
                                }
                            } else null
                        )
                    }
                    ratio > 1f -> {
                        val width = maxWidth * config.singleWideImageMaxWidthRatio
                        val height = width / ratio
                        PhotoThumbnailItem(
                            thumb,
                            width,
                            height,
                            config.alphaWhenPressed,
                            isContainerDimenExactly = true,
                            onLayout = { offset, size ->
                                renderInfo[0].offsetInWindow = offset
                                renderInfo[0].size = size
                            },
                            onPhotoLoaded = {
                                renderInfo[0].photo = it.drawable
                            },
                            click = if (onClick != null) {
                                {
                                    onClick.invoke(renderInfo.toList(), 0)
                                }
                            } else null
                        )
                    }
                    image.isLongImage() -> {
                        val width = maxWidth * config.singleLongImageWidthRatio
                        val useScreenHeightRatio = config.singleHighImageMiniHeightRatio == SINGLE_HIGH_IMAGE_MINI_SCREEN_HEIGHT_RATIO
                        val heightRatio = if (useScreenHeightRatio) {
                            val windowSize = context.getWindowSize()
                            windowSize.width * 1f / windowSize.height
                        } else {
                            config.singleHighImageMiniHeightRatio
                        }
                        val height = width / heightRatio
                        PhotoThumbnailItem(
                            thumb,
                            width,
                            height,
                            config.alphaWhenPressed,
                            isContainerDimenExactly = true,
                            onLayout = { offset, size ->
                                renderInfo[0].offsetInWindow = offset
                                renderInfo[0].size = size
                            },
                            onPhotoLoaded = {
                                renderInfo[0].photo = it.drawable
                            },
                            click = if (onClick != null) {
                                {
                                    onClick.invoke(renderInfo.toList(), 0)
                                }
                            } else null
                        )
                    }
                    else -> {
                        var width = maxWidth * config.singleHighImageDefaultWidthRatio
                        var height = width / ratio
                        val heightMiniRatio = if (config.singleHighImageMiniHeightRatio == SINGLE_HIGH_IMAGE_MINI_SCREEN_HEIGHT_RATIO) {
                            val windowSize = context.getWindowSize()
                            windowSize.width * 1f / windowSize.height
                        } else {
                            config.singleHighImageMiniHeightRatio
                        }
                        if (ratio < heightMiniRatio) {
                            height = width * heightMiniRatio
                            width = height * ratio
                        }
                        PhotoThumbnailItem(
                            thumb,
                            width,
                            height,
                            config.alphaWhenPressed,
                            isContainerDimenExactly = true,
                            onLayout = { offset, size ->
                                renderInfo[0].offsetInWindow = offset
                                renderInfo[0].size = size
                            },
                            onPhotoLoaded = {
                                renderInfo[0].photo = it.drawable
                            },
                            click = if (onClick != null) {
                                {
                                    onClick.invoke(renderInfo.toList(), 0)
                                }
                            } else null
                        )
                    }
                }
            }
        } else if (images.size == 2 && config.averageIfTwoImage) {
            RowImages(images, renderInfo, config, maxWidth, 2, 0, onClick)
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                for (
                    i in 0 until (images.size / 3 + if (images.size % 3 > 0) 1 else 0).coerceAtMost(
                        3
                    )
                ) {
                    if (i > 0) {
                        Spacer(modifier = Modifier.height(config.verGap))
                    }
                    RowImages(
                        images,
                        renderInfo,
                        config,
                        this@BoxWithConstraints.maxWidth,
                        3,
                        i * 3,
                        onClick
                    )
                }
            }
        }
    }
}

@Composable
fun RowImages(
    images: List<PhotoProvider>,
    renderInfo: Array<PhotoShot>,
    config: PhotoThumbnailConfig,
    containerWidth: Dp,
    rowCount: Int,
    startIndex: Int,
    onClick: ((images: List<PhotoShot>, index: Int) -> Unit)?
) {
    val wh = (containerWidth - config.horGap * (rowCount - 1)) / rowCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(wh)
    ) {
        for (i in startIndex until (startIndex + rowCount).coerceAtMost(images.size)) {
            if (i != startIndex) {
                Spacer(modifier = Modifier.width(config.horGap))
            }
            val image = images[i]
            PhotoThumbnailItem(
                remember(image) {
                    image.thumbnail(true)
                },
                wh,
                wh,
                config.alphaWhenPressed,
                isContainerDimenExactly = true,
                onLayout = { offset, size ->
                    renderInfo[i].offsetInWindow = offset
                    renderInfo[i].size = size
                },
                onPhotoLoaded = {
                    renderInfo[i].photo = it.drawable
                },
                click = if (onClick != null) {
                    {
                        onClick.invoke(renderInfo.toList(), i)
                    }
                } else null
            )
        }
    }
}

@Composable
fun ThumbBlankBox() {
    val blankColor = LocalPhotoViewerConfig.current.blankColor
    if (blankColor != Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(blankColor)
        )
    }
}
