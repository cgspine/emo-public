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

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRect
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun GesturePhoto(
    containerWidth: Dp,
    containerHeight: Dp,
    imageRatio: Float,
    isLongImage: Boolean,
    initRect: Rect? = null,
    shouldTransitionEnter: Boolean = false,
    shouldTransitionExit: Boolean = true,
    transitionTarget: Boolean = true,
    transitionDurationMs: Int = 360,
    pullExitMiniTranslateY: Dp = 72.dp,
    panEdgeProtection: Rect = Rect(
        0f,
        0f,
        with(LocalDensity.current) { containerWidth.toPx() },
        with(LocalDensity.current) { containerHeight.toPx() }
    ),
    maxScale: Float = 4f,
    onPress: suspend PressGestureScope.(Offset) -> Unit = { },
    onBeginPullExit: () -> Boolean,
    onLongPress: (() -> Unit)? = null,
    onTapExit: (afterTransition: Boolean) -> Unit,
    content: @Composable (
        transition: Transition<Boolean>,
        scale: Float,
        rect: Rect,
        onImageRatioEnsured: (Float) -> Unit
    ) -> Unit
) {

    val (imageWidth, imageHeight) = calculateImageSize(containerWidth, containerHeight, imageRatio, isLongImage)

    var calculatedImageRatio by remember {
        mutableStateOf(imageRatio)
    }

    val density = LocalDensity.current
    val imagePaddingFix by remember(
        density,
        panEdgeProtection,
        isLongImage,
        containerWidth,
        containerHeight,
        calculatedImageRatio,
        imageRatio
    ) {
        val (expectWidth, expectHeight) = calculateImageSize(
            containerWidth,
            containerHeight,
            calculatedImageRatio,
            isLongImage
        )
        val widthPadding = with(density) {
            (imageWidth - expectWidth).toPx() / 2
        }
        val heightPadding = with(density) {
            (imageHeight - expectHeight).toPx() / 2
        }

        mutableStateOf(widthPadding to heightPadding)
    }

    val usedImageRatioUpdater = remember {
        val func: (Float) -> Unit = { value ->
            if (value > 0) {
                calculatedImageRatio = value
            }
        }
        func
    }

    var backgroundTargetAlpha by remember {
        mutableStateOf(1f)
    }

    val targetNormalTranslateX = with(LocalDensity.current) {
        ((containerWidth - imageWidth) / 2f).toPx()
    }

    val targetNormalTranslateY = with(LocalDensity.current) {
        ((containerHeight - imageHeight) / 2f).toPx()
    }

    var targetScale by remember(containerWidth, containerHeight) { mutableStateOf(1f) }
    var targetTranslateX by remember(containerWidth, containerHeight) { mutableStateOf(targetNormalTranslateX) }
    var targetTranslateY by remember(containerWidth, containerHeight) { mutableStateOf(targetNormalTranslateY) }

    val containerWidthPx = with(LocalDensity.current) { containerWidth.toPx() }
    val containerHeightPx = with(LocalDensity.current) { containerHeight.toPx() }
    val imageWidthPx = with(LocalDensity.current) { imageWidth.toPx() }
    val imageHeightPx = with(LocalDensity.current) { imageHeight.toPx() }
    var isGestureHandling by remember(containerWidth, containerHeight) {
        mutableStateOf(false)
    }

    var transitionTargetState by remember(containerWidth, containerHeight, transitionTarget) {
        mutableStateOf(
            transitionTarget
        )
    }
    val transitionState = remember(containerWidth, containerHeight) {
        MutableTransitionState(!shouldTransitionEnter)
    }

    val scaleHandler: (Offset, Float, Boolean) -> Unit = remember(containerWidth, containerHeight, maxScale, imageRatio) {
        lambda@{ center, scaleParam, edgeProtection ->
            var scale = scaleParam
            if (targetScale * scaleParam > maxScale) {
                scale = maxScale / targetScale
            }
            if (scale == 1f) {
                return@lambda
            }
            var targetLeft = center.x + ((targetTranslateX - center.x) * scale)
            var targetTop = center.y + ((targetTranslateY - center.y) * scale)
            val targetWidth = imageWidthPx * targetScale * scale
            val targetHeight = imageHeightPx * targetScale * scale

            if (edgeProtection) {
                when {
                    containerWidthPx > targetWidth -> {
                        targetLeft = (containerWidthPx - targetWidth) / 2
                    }
                    targetLeft > 0 -> {
                        targetLeft = 0f
                    }
                    targetLeft + targetWidth < containerWidthPx -> {
                        targetLeft = containerWidthPx - targetWidth
                    }
                }

                when {
                    containerHeightPx > targetHeight -> {
                        targetTop = (containerHeightPx - targetHeight) / 2
                    }
                    targetTop > 0 -> {
                        targetTop = 0f
                    }
                    targetTop + targetHeight < containerHeightPx -> {
                        targetTop = containerHeightPx - targetHeight
                    }
                }
            }
            targetTranslateX = targetLeft
            targetTranslateY = targetTop
            targetScale *= scale
        }
    }

    val reset: () -> Unit = remember(containerWidth, containerHeight, imageRatio) {
        {
            backgroundTargetAlpha = 1f
            targetScale = 1f
            targetTranslateX = targetNormalTranslateX
            targetTranslateY = targetNormalTranslateY
        }
    }

    transitionState.targetState = transitionTargetState
    val transition = updateTransition(transitionState = transitionState, label = "PhotoPager")

    val nestedScrollConnection = remember {
        GestureNestScrollConnection()
    }

    Box(
        modifier = Modifier
            .width(containerWidth)
            .height(containerHeight)
    ) {
        PhotoBackgroundWithTransition(backgroundTargetAlpha, transition, transitionDurationMs) {
            PhotoBackground(alpha = it)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .pointerInput(
                    containerWidth,
                    containerHeight,
                    maxScale,
                    shouldTransitionExit,
                    onTapExit,
                    onBeginPullExit,
                    imagePaddingFix
                ) {
                    coroutineScope {
                        launch {
                            detectTapGestures(
                                onTap = {
                                    if (shouldTransitionExit) {
                                        transitionTargetState = false
                                    } else {
                                        onTapExit(false)
                                    }
                                },
                                onLongPress = {
                                    onLongPress?.invoke()
                                },
                                onDoubleTap = {
                                    if (targetScale == 1f) {
                                        var scale = 2f
                                        val alignScale = (containerWidth / imageWidth).coerceAtLeast((containerHeight / imageHeight))
                                        if (alignScale > 1.25 && alignScale < scale) {
                                            scale = alignScale
                                        }
                                        scaleHandler.invoke(it, scale, true)
                                    } else {
                                        reset()
                                    }
                                },
                                onPress = onPress
                            )
                        }

                        launch {
                            forEachGesture {
                                awaitPointerEventScope {
                                    var zoom = 1f
                                    var pan = Offset.Zero
                                    val touchSlop = viewConfiguration.touchSlop
                                    var isZooming = false
                                    var isPanning = false
                                    var isExitPanning = false
                                    isGestureHandling = false
                                    awaitFirstDown(requireUnconsumed = false)
                                    nestedScrollConnection.canConsumeEvent = false
                                    nestedScrollConnection.isIntercepted = false
                                    do {
                                        val event = awaitPointerEvent()
                                        if (isZooming || isExitPanning) {
                                            nestedScrollConnection.isIntercepted = true
                                        }
                                        val needHandle = nestedScrollConnection.canConsumeEvent || event.changes.none { it.isConsumed }
                                        if (needHandle) {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()

                                            if (!isZooming && !isPanning) {
                                                zoom *= zoomChange
                                                pan += panChange

                                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                                val zoomMotion = abs(1 - zoom) * centroidSize
                                                val panMotion = pan.getDistance()

                                                if (zoomMotion > touchSlop) {
                                                    isGestureHandling = true
                                                    isZooming = true
                                                } else if (panMotion > touchSlop) {
                                                    isPanning = true
                                                    isGestureHandling = true
                                                }
                                            }

                                            if (isZooming) {
                                                val centroid = event.calculateCentroid(useCurrent = false)
                                                if (zoomChange != 1f) {
                                                    scaleHandler(centroid, zoomChange, true)
                                                }
                                                event.changes.forEach {
                                                    if (it.positionChanged()) {
                                                        it.consume()
                                                    }
                                                }
                                            } else if (isPanning) {
                                                if (!isExitPanning) {
                                                    var xConsumed = false
                                                    var yConsumed = false
                                                    if (panChange != Offset.Zero) {
                                                        if (panChange.x > 0) {
                                                            val fixEdgeLeft = panEdgeProtection.left - imagePaddingFix.first * targetScale
                                                            if (targetTranslateX < fixEdgeLeft) {
                                                                targetTranslateX = (targetTranslateX + panChange.x).coerceAtMost(fixEdgeLeft)
                                                                xConsumed = true
                                                            }
                                                        }
                                                        if (panChange.x < 0) {
                                                            val w = imageWidthPx * targetScale
                                                            val fixEdgeRight = panEdgeProtection.right + imagePaddingFix.first * targetScale
                                                            if (targetTranslateX + w > fixEdgeRight) {
                                                                targetTranslateX = (targetTranslateX + panChange.x).coerceAtLeast(
                                                                    fixEdgeRight - w
                                                                )
                                                                xConsumed = true
                                                            }
                                                        }

                                                        if (panChange.y > 0) {
                                                            val fixEdgeTop = panEdgeProtection.top - imagePaddingFix.second * targetScale
                                                            if (targetTranslateY < fixEdgeTop) {
                                                                targetTranslateY = (targetTranslateY + panChange.y).coerceAtMost(fixEdgeTop)
                                                                yConsumed = true
                                                            } else if (!xConsumed &&
                                                                panChange.y > panChange.x.absoluteValue
                                                            ) {
                                                                isExitPanning = targetScale == 1f && onBeginPullExit()
                                                            }
                                                        }

                                                        if (panChange.y < 0) {
                                                            val h = imageHeightPx * targetScale
                                                            val fixEgeBottom = (
                                                                panEdgeProtection.bottom +
                                                                    imagePaddingFix.second * targetScale
                                                                )
                                                            if (targetTranslateY + h > fixEgeBottom) {
                                                                targetTranslateY = (targetTranslateY + panChange.y).coerceAtLeast(
                                                                    fixEgeBottom - h
                                                                )
                                                                yConsumed = true
                                                            }
                                                        }
                                                    }

                                                    if (xConsumed || yConsumed) {
                                                        event.changes.forEach {
                                                            if (it.positionChanged()) {
                                                                it.consume()
                                                            }
                                                        }
                                                    }
                                                }

                                                if (isExitPanning) {
                                                    val center = event.calculateCentroid(useCurrent = true)
                                                    val scaleChange = 1 - panChange.y / containerHeightPx / 2
                                                    val finalScale = (targetScale * scaleChange)
                                                        .coerceAtLeast(0.5f)
                                                        .coerceAtMost(1f)
                                                    backgroundTargetAlpha = finalScale
                                                    targetTranslateX += panChange.x
                                                    targetTranslateY += panChange.y
                                                    scaleHandler(center, finalScale / targetScale, false)
                                                    event.changes.forEach {
                                                        if (it.positionChanged()) {
                                                            it.consumeAllChanges()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    isGestureHandling = false
                                    if (isZooming) {
                                        if (targetScale < 1f) {
                                            reset()
                                        }
                                    }

                                    if (isExitPanning) {
                                        if (targetTranslateY - targetNormalTranslateY < pullExitMiniTranslateY.toPx()) {
                                            reset()
                                        } else {
                                            transitionTargetState = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {

            if (initRect == null || initRect == Rect.Zero || imageRatio <= 0f) {
                PhotoContentWithAlphaTransition(
                    transition = transition,
                    transitionDurationMs = transitionDurationMs,
                    isGestureHandling = isGestureHandling,
                    scale = targetScale,
                    translateX = targetTranslateX,
                    translateY = targetTranslateY
                ) { alpha, scale, translateX, translateY ->
                    PhotoTransformContent(
                        alpha,
                        imageWidthPx,
                        imageHeightPx,
                        scale,
                        scale,
                        translateX,
                        translateY
                    ) {
                        val imageLeft = translateX + imagePaddingFix.first * it
                        val imageTop = translateY + imagePaddingFix.second * it
                        content(
                            transition,
                            it,
                            Rect(
                                imageLeft,
                                imageTop,
                                imageLeft + imageWidthPx * it,
                                imageTop + imageHeightPx * it
                            ),
                            usedImageRatioUpdater
                        )
                    }
                }
            } else {
                PhotoContentWithRectTransition(
                    imageWidth = imageWidthPx,
                    imageHeight = imageHeightPx,
                    initRect = initRect,
                    scale = targetScale,
                    translateX = targetTranslateX,
                    translateY = targetTranslateY,
                    transition = transition,
                    transitionDurationMs = transitionDurationMs
                ) { scaleX, scaleY, translateX, translateY ->
                    PhotoTransformContent(
                        1f,
                        imageWidthPx,
                        imageHeightPx,
                        scaleX,
                        scaleY,
                        translateX,
                        translateY
                    ) {
                        val imageLeft = translateX + imagePaddingFix.first * it
                        val imageTop = translateY + imagePaddingFix.second * it
                        content(
                            transition,
                            it,
                            Rect(
                                imageLeft,
                                imageTop,
                                imageLeft + imageWidthPx * it,
                                imageTop + imageHeightPx * it
                            ),
                            usedImageRatioUpdater
                        )
                    }
                }
            }
        }
    }

    if (!transitionState.currentState && !transitionState.targetState) {
        onTapExit(true)
    }
}

@Composable
fun PhotoBackgroundWithTransition(
    backgroundTargetAlpha: Float,
    transition: Transition<Boolean>,
    transitionDurationMs: Int,
    content: @Composable (alpha: Float) -> Unit
) {
    val alpha = transition.animateFloat(
        transitionSpec = { tween(durationMillis = transitionDurationMs) },
        label = "PhotoBackgroundWithTransition"
    ) {
        if (it) backgroundTargetAlpha else 0f
    }
    content(alpha.value)
}

@Composable
fun PhotoContentWithAlphaTransition(
    transition: Transition<Boolean>,
    transitionDurationMs: Int,
    isGestureHandling: Boolean,
    scale: Float,
    translateX: Float,
    translateY: Float,
    content: @Composable (alpha: Float, scale: Float, translateX: Float, translateY: Float) -> Unit
) {
    val alphaState = transition.animateFloat(
        transitionSpec = { tween(durationMillis = transitionDurationMs) },
        label = "PhotoContentWithAlphaTransition"
    ) {
        if (it) 1f else 0f
    }
    val duration = if (isGestureHandling) 0 else transitionDurationMs
    val scaleState = animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = duration)
    )
    val translateXState = animateFloatAsState(
        targetValue = translateX,
        animationSpec = tween(durationMillis = duration)
    )
    val translateYState = animateFloatAsState(
        targetValue = translateY,
        animationSpec = tween(durationMillis = duration)
    )
    content(alphaState.value, scaleState.value, translateXState.value, translateYState.value)
}

@Composable
fun PhotoContentWithRectTransition(
    imageWidth: Float,
    imageHeight: Float,
    initRect: Rect,
    scale: Float,
    translateX: Float,
    translateY: Float,
    transition: Transition<Boolean>,
    transitionDurationMs: Int,
    content: @Composable (scaleX: Float, scaleY: Float, translateX: Float, translateY: Float) -> Unit
) {
    val rect = transition.animateRect(
        transitionSpec = { tween(durationMillis = transitionDurationMs) },
        label = "PhotoContentWithRectTransition"
    ) {
        if (it) {
            Rect(
                translateX,
                translateY,
                translateX + imageWidth * scale,
                translateY + imageHeight * scale
            )
        } else initRect
    }
    content(
        (rect.value.width / imageWidth).coerceAtLeast(0f),
        (rect.value.height / imageHeight).coerceAtLeast(0f),
        rect.value.left,
        rect.value.top
    )
}

@Composable
fun PhotoBackground(
    alpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(Color.Black)
    )
}

@Composable
fun PhotoTransformContent(
    alpha: Float,
    width: Float,
    height: Float,
    scaleX: Float,
    scaleY: Float,
    translateX: Float,
    translateY: Float,
    content: @Composable (scale: Float) -> Unit
) {
    val widthDp = with(LocalDensity.current) { width.toDp() }
    val heightDp = with(LocalDensity.current) { height.toDp() }
    val scale = scaleX.coerceAtLeast(scaleY)
    val clipSize = remember(scaleX, scaleY, width, height) {
        if (scale == 0f) {
            Size(0f, 0f)
        } else {
            val expectedW = width * scaleX / scale
            val expectedH = height * scaleY / scale
            val clipW = (width - expectedW) / 2
            val clipH = (height - expectedH) / 2
            Size(clipW, clipH)
        }
    }
    Box(
        modifier = Modifier
            .width(widthDp)
            .height(heightDp)
            .graphicsLayer {
                this.transformOrigin = TransformOrigin(0f, 0f)
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.clip = true
                this.shape = object : Shape {
                    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
                        Outline.Rectangle(
                            Rect(
                                clipSize.width,
                                clipSize.height,
                                size.width - clipSize.width,
                                size.height - clipSize.height
                            )
                        )

                    override fun toString(): String = "PhotoTransformShape"
                }
                this.translationX = translateX - clipSize.width * scale
                this.translationY = translateY - clipSize.height * scale
            }
    ) {
        content(scale)
    }
}

internal class GestureNestScrollConnection : NestedScrollConnection {

    var isIntercepted: Boolean = false
    var canConsumeEvent: Boolean = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (isIntercepted) {
            return available
        }
        return super.onPreScroll(available, source)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 0) {
            canConsumeEvent = true
        }
        return available
    }
}

private fun calculateImageSize(
    containerWidth: Dp,
    containerHeight: Dp,
    imageRatio: Float,
    isLongImage: Boolean
): Pair<Dp, Dp> {
    val layoutRatio = containerWidth / containerHeight
    return when {
        isLongImage || imageRatio <= 0f -> containerWidth to containerHeight
        imageRatio >= layoutRatio -> containerWidth to (containerWidth / imageRatio)
        else -> (containerHeight * imageRatio) to containerHeight
    }
}
