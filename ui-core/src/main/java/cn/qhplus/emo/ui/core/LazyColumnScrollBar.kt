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

package cn.qhplus.emo.ui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.launch
import kotlin.math.floor

@Composable
fun BoxWithConstraintsScope.LazyListScrollBar(
    listState: LazyListState,
    insetTop: Dp,
    insetBottom: Dp,
    insetRight: Dp,
    thumbWidth: Dp,
    thumbHeight: Dp,
    thumbBgColor: Color,
    thumbLineColor: Color,
    thumbShape: Shape = CircleShape
) {
    val needShow by remember {
        derivedStateOf {
            (
                listState.canScrollBackward ||
                    listState.canScrollForward
                ) &&
                listState.layoutInfo.totalItemsCount > 3
        }
    }
    if (needShow) {
        InternalLazyColumnScrollBar(
            listState,
            insetTop, insetBottom, insetRight,
            thumbWidth, thumbHeight, thumbBgColor, thumbLineColor, thumbShape
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.InternalLazyColumnScrollBar(
    listState: LazyListState,
    insetTop: Dp,
    insetBottom: Dp,
    insetRight: Dp,
    thumbWidth: Dp,
    thumbHeight: Dp,
    thumbBgColor: Color,
    thumbLineColor: Color,
    thumbShape: Shape = CircleShape
) {
    val coroutineScope = rememberCoroutineScope()

    val reverseLayout by remember { derivedStateOf { listState.layoutInfo.reverseLayout } }

    val realFirstVisibleItem by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.fastFirstOrNull {
                it.index == listState.firstVisibleItemIndex
            }
        }
    }

    fun LazyListItemInfo.fractionHiddenTop() =
        if (size == 0) 0f else -offset.toFloat() / size.toFloat()

    val normalOffset by remember {
        derivedStateOf {
            listState.layoutInfo.let {
                if (it.totalItemsCount == 0 || it.visibleItemsInfo.isEmpty()) {
                    return@let 0f
                }

                val firstItem = realFirstVisibleItem ?: return@let 0f
                firstItem.run { index.toFloat() + fractionHiddenTop() } / it.totalItemsCount.toFloat()
            }
        }
    }
    var isDragging by remember {
        mutableStateOf(false)
    }
    var offset by remember {
        mutableStateOf(0f)
    }

    val space = with(LocalDensity.current) {
        (maxHeight - insetTop - insetBottom - thumbHeight).toPx()
    }
    if (!isDragging) {
        offset = space * if (reverseLayout) {
            (1 - normalOffset)
        } else {
            normalOffset
        }
    }

    val derivedOffset = remember {
        derivedStateOf { offset }
    }

    val dragState = rememberDraggableState { delta ->
        val displace = if (reverseLayout) -delta else delta
        offset = (offset + displace).coerceIn(0f, space)
        var percent = offset / space
        if (reverseLayout) {
            percent = 1 - percent
        }
        val totalItemsCount = listState.layoutInfo.totalItemsCount.toFloat()
        val exactIndex = (totalItemsCount * percent).toDouble()
        val index: Int = floor(exactIndex).toInt()
        val remainder = (exactIndex - floor(exactIndex)).toFloat()

        coroutineScope.launch {
            listState.scrollToItem(index = index, scrollOffset = 0)
            val itemOffset = realFirstVisibleItem
                ?.size
                ?.let { it.toFloat() * remainder }
                ?.toInt() ?: 0
            listState.scrollToItem(index = index, scrollOffset = itemOffset)
        }
    }

    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .graphicsLayer {
                translationY = derivedOffset.value
            }
            .padding(top = insetTop, bottom = insetBottom)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                startDragImmediately = true,
                onDragStarted = {
                    isDragging = true
                },
                onDragStopped = {
                    isDragging = false
                }
            )
            .padding(start = insetRight, end = insetRight)
            .width(thumbWidth)
            .height(thumbHeight)
            .background(thumbBgColor, thumbShape),
        verticalArrangement = Arrangement.Center
    ) {
        ScrollBarLine(thumbLineColor)
        Spacer(modifier = Modifier.height(4.dp))
        ScrollBarLine(thumbLineColor)
    }
}

@Composable
private fun ScrollBarLine(
    color: Color
) {
    Box(modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth().height(2.dp).background(color, CircleShape))
}
