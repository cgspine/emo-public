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

import androidx.compose.animation.core.animateRect
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class GestureContentState(
    val ratio: Float,
    val isLongContent: Boolean,
    val maxScale: Float = 4f,
    val transitionDurationMs: Int = 360,
    val panAreaFactory: (Float, Float) -> Rect = { containerWidth, containerHeight ->
        Rect(0f, 0f, containerWidth, containerHeight)
    }
) {
    internal var contentRatio by mutableStateOf(ratio)
    var targetScale by mutableStateOf(1f)
        private set
    var targetTranslateX by mutableStateOf(0f)
        private set
    var targetTranslateY by mutableStateOf(0f)
        private set

    var layoutInfo by mutableStateOf<LayoutInfo?>(null)

    internal val nestedScrollConnection = GestureNestScrollConnection()

    fun reset() {
        targetScale = 1f
        targetTranslateX = 0f
        targetTranslateY = 0f
    }

    fun setScale(layoutInfo: LayoutInfoPx, scaleParam: Float, center: Offset) {
        var scale = scaleParam
        if (targetScale * scaleParam > maxScale) {
            scale = maxScale / targetScale
        }
        if (scale == 1f) {
            return
        }
        val x = center.x + ((targetTranslateX - center.x) * scale)
        val y = center.y + ((targetTranslateY - center.y) * scale)
        targetScale *= scale
        setTranslateX(layoutInfo, x)
        setTranslateY(layoutInfo, y)
    }

    fun setTranslateX(layoutInfo: LayoutInfoPx, x: Float): Boolean {
        if (x == targetTranslateX) {
            return false
        }
        val fixed = fixTranslate(
            layoutInfo.containerWidth,
            layoutInfo.contentWidth,
            x,
            layoutInfo.panArea.left,
            layoutInfo.panArea.right
        )
        if (fixed == targetTranslateX) {
            return false
        }
        targetTranslateX = fixed
        return true
    }

    fun setTranslateY(layoutInfo: LayoutInfoPx, y: Float): Boolean {
        if (y == targetTranslateY) {
            return false
        }
        val fixed = fixTranslate(
            layoutInfo.containerHeight,
            layoutInfo.contentHeight,
            y,
            layoutInfo.panArea.top,
            layoutInfo.panArea.bottom
        )
        if (fixed == targetTranslateY) {
            return false
        }
        targetTranslateY = fixed
        return true
    }

    private fun fixTranslate(containerSize: Float, contentSize: Float, value: Float, min: Float, max: Float): Float {
        val containerScale = containerSize * targetScale
        val contentScale = contentSize * targetScale
        val gapSize = (containerScale - contentScale) / 2
        val viewportSize = max - min
        var usedValue = value
        if (contentScale <= viewportSize) {
            usedValue = (viewportSize - contentScale) / 2 - gapSize
        } else if (value + gapSize > min) {
            usedValue = min - gapSize
        } else if (value + containerScale - gapSize < max) {
            usedValue = max + gapSize - containerScale
        }
        return usedValue
    }
}

data class LayoutInfoPx(
    val containerWidth: Float,
    val containerHeight: Float,
    val contentWidth: Float,
    val contentHeight: Float,
    val panArea: Rect
)

@Stable
data class LayoutInfo(
    val containerWidth: Dp,
    val containerHeight: Dp,
    val contentWidth: Dp,
    val contentHeight: Dp,
    val px: LayoutInfoPx
) {
    fun cropScale(): Float {
        return (containerWidth / contentWidth).coerceAtLeast((containerHeight / contentHeight))
    }

    fun contentOffset(
        containerTranslateX: Float,
        containerTranslateY: Float,
        scale: Float
    ): Offset {
        val x = contentSideOffset(px.containerWidth, px.contentWidth, scale, containerTranslateX, px.panArea.left, px.panArea.right)
        val y = contentSideOffset(px.containerHeight, px.contentHeight, scale, containerTranslateY, px.panArea.top, px.panArea.bottom)
        return Offset(x, y)
    }

    private fun contentSideOffset(
        containerSize: Float,
        contentSize: Float,
        scale: Float,
        translate: Float,
        min: Float,
        max: Float
    ): Float {
        val viewport = max - min
        val contentScale = contentSize * scale
        val containerScale = containerSize * scale
        if (contentScale < viewport) {
            return (viewport - contentScale) / 2
        }
        val gap = (containerScale - contentScale) / 2
        return translate + gap - min
    }
}

@Composable
fun GestureContent(
    modifier: Modifier = Modifier,
    state: GestureContentState,
    onTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    canTransformStart: ((Offset) -> Boolean) = { true },
    content: @Composable (onImageRatioEnsured: (Float) -> Unit) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val contentRatio = state.contentRatio
        val (contentWidth, contentHeight) = calculateContentSize(maxWidth, maxHeight, contentRatio, state.isLongContent)
        val px = with(LocalDensity.current) {
            val cw = maxWidth.toPx()
            val ch = maxHeight.toPx()
            LayoutInfoPx(
                cw,
                ch,
                contentWidth.toPx(),
                contentHeight.toPx(),
                state.panAreaFactory(cw, ch)
            )
        }
        val layoutInfo = LayoutInfo(maxWidth, maxHeight, contentWidth, contentHeight, px)
        state.layoutInfo = layoutInfo
        GestureContentInner(state, layoutInfo, onTap, onLongPress, canTransformStart, content)
    }
}

@Composable
private fun BoxWithConstraintsScope.GestureContentInner(
    state: GestureContentState,
    layoutInfo: LayoutInfo,
    onTap: ((Offset) -> Unit)?,
    onLongPress: ((Offset) -> Unit)?,
    canTransformStart: ((Offset) -> Boolean),
    content: @Composable (onImageRatioEnsured: (Float) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val flingBehavior = ScrollableDefaults.flingBehavior()
    val transition = updateTransition(targetState = true, "transition")
    val rect = transition.animateRect(
        transitionSpec = { tween(durationMillis = state.transitionDurationMs) },
        label = "gestureContentTransition"
    ) {
        if (it) {
            Rect(
                state.targetTranslateX,
                state.targetTranslateY,
                state.targetTranslateX + 100 * state.targetScale,
                state.targetTranslateY + 100 * state.targetScale
            )
        } else {
            Rect(0f, 0f, 100f, 100f)
        }
    }
    Box(
        modifier = Modifier
            .width(layoutInfo.containerWidth)
            .height(layoutInfo.containerHeight)
            .nestedScroll(state.nestedScrollConnection)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = rect.value.left
                translationY = rect.value.top
                val scale = rect.value.width / 100
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(state, scope, layoutInfo, onTap, onLongPress, canTransformStart) {
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onTap = {
                                onTap?.invoke(it)
                            },
                            onLongPress = {
                                onLongPress?.invoke(it)
                            },
                            onDoubleTap = {
                                if (state.targetScale == 1f) {
                                    var scale = 2f
                                    val cropScale = layoutInfo.cropScale()
                                    if (cropScale > 1.25 && cropScale < scale) {
                                        scale = cropScale
                                    }
                                    state.setScale(layoutInfo.px, scale, it)
                                } else {
                                    state.reset()
                                }
                            }
                        )
                    }

                    launch {
                        transformTouch(state, scope, layoutInfo.px, canTransformStart, flingBehavior)
                    }
                }
            }
    ) {
        content {
            state.contentRatio = it
        }
    }
}

internal suspend fun PointerInputScope.transformTouch(
    state: GestureContentState,
    composeScope: CoroutineScope,
    layoutInfo: LayoutInfoPx,
    canTransformStart: (Offset) -> Boolean,
    flingBehavior: FlingBehavior
) {
    val velocityTracker = VelocityTracker()
    var flingXJob: Job? = null
    var flingYJob: Job? = null

    val scrollXScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float {
            return if (state.setTranslateX(layoutInfo, state.targetTranslateX + pixels)) {
                pixels
            } else 0f
        }
    }

    val scrollYScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float {
            return if (state.setTranslateY(layoutInfo, state.targetTranslateY + pixels)) {
                pixels
            } else 0f
        }
    }
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        val touchSlop = viewConfiguration.touchSlop
        var isZooming = false
        var isPanning = false
        val down = awaitFirstDown(requireUnconsumed = false)
        flingXJob?.cancel()
        flingYJob?.cancel()
        velocityTracker.resetTracking()
        val enabled = canTransformStart(down.position)
        if (enabled) {
            state.nestedScrollConnection.canConsumeEvent = false
            state.nestedScrollConnection.isIntercepted = false
            velocityTracker.addPointerInputChange(down)
            do {
                val event = awaitPointerEvent()
                event.changes
                    .fastFirstOrNull { it.id == down.id }
                    ?.let {
                        velocityTracker.addPointerInputChange(it)
                    }
                if (isZooming) {
                    state.nestedScrollConnection.isIntercepted = true
                }
                val needHandle = state.nestedScrollConnection.canConsumeEvent || event.changes.none { it.isConsumed }
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
                            isZooming = true
                        } else if (panMotion > touchSlop) {
                            isPanning = true
                        }
                    }

                    if (isZooming) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        if (zoomChange != 1f) {
                            state.setScale(layoutInfo, zoomChange, centroid)
                        }
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    } else if (isPanning) {
                        val xConsumed = panChange.x != 0f && state.setTranslateX(layoutInfo, state.targetTranslateX + panChange.x)
                        val yConsumed = panChange.y != 0f && state.setTranslateY(layoutInfo, state.targetTranslateY + panChange.y)
                        if (xConsumed || yConsumed) {
                            event.changes.forEach {
                                if (it.positionChanged()) {
                                    it.consume()
                                }
                            }
                        }
                    }
                }
            } while (event.changes.any { it.pressed })

            if (isZooming) {
                if (state.targetScale < 1f) {
                    state.reset()
                }
            }

            if (isPanning) {
                val v = velocityTracker.calculateVelocity()
                if (v.x != 0f) {
                    flingXJob = composeScope.launch {
                        with(scrollXScope) {
                            with(flingBehavior) {
                                performFling(v.x)
                            }
                        }
                    }
                }
                if (v.y != 0f) {
                    flingYJob = composeScope.launch {
                        with(scrollYScope) {
                            with(flingBehavior) {
                                performFling(v.y)
                            }
                        }
                    }
                }
            }
        }
    }
}
