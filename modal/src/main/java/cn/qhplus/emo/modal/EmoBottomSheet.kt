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

package cn.qhplus.emo.modal

import android.view.View
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

@Composable
fun EmoBottomSheetList(
    modal: EmoModal,
    state: LazyListState = rememberLazyListState(),
    children: LazyListScope.(EmoModal) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxWidth()
    ) {
        children(modal)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityScope.EmoBottomSheet(
    modal: EmoModal,
    draggable: Boolean,
    widthLimit: (maxWidth: Dp) -> Dp,
    heightLimit: (maxHeight: Dp) -> Dp,
    radius: Dp = 2.dp,
    background: Color = Color.White,
    mask: Color = DefaultMaskColor,
    modifier: Modifier,
    content: @Composable (EmoModal) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val wl = widthLimit(maxWidth)
        val wh = heightLimit(maxHeight)

        var contentModifier = if (wl < maxWidth) {
            Modifier.width(wl)
        } else {
            Modifier.fillMaxWidth()
        }

        contentModifier = contentModifier
            .heightIn(max = wh.coerceAtMost(maxHeight))

        if (radius > 0.dp) {
            contentModifier =
                contentModifier.clip(RoundedCornerShape(topStart = radius, topEnd = radius))
        }
        contentModifier = contentModifier
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
            }

        if (draggable) {
            NestScrollWrapper(modal, modifier, mask) {
                Box(modifier = contentModifier) {
                    content(modal)
                }
            }
        } else {
            if (mask != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateEnterExit(
                            enter = fadeIn(tween()),
                            exit = fadeOut(tween())
                        )
                        .background(mask)
                )
            }
            Box(modifier = modifier.then(contentModifier)) {
                content(modal)
            }
        }
    }
}

private class MutableHeight(var height: Float)

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityScope.NestScrollWrapper(
    modal: EmoModal,
    modifier: Modifier,
    mask: Color,
    content: @Composable () -> Unit
) {
    val yOffsetState = remember {
        mutableStateOf(0f)
    }

    val mutableContentHeight = remember {
        MutableHeight(0f)
    }
    val contentHeight = mutableContentHeight.height

    val percent = if (contentHeight <= 0f) 1f else {
        ((contentHeight - yOffsetState.value) / contentHeight)
            .coerceAtMost(1f)
            .coerceAtLeast(0f)
    }

    val nestedScrollConnection = remember(modal, yOffsetState) {
        BottomSheetNestedScrollConnection(modal, yOffsetState, mutableContentHeight)
    }

    val yOffset = yOffsetState.value

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (mask != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(percent)
                    .animateEnterExit(
                        enter = fadeIn(tween()),
                        exit = fadeOut(tween())
                    )
                    .background(mask)
            )
            Box(
                modifier = modifier
                    .graphicsLayer { translationY = yOffset }
                    .nestedScroll(nestedScrollConnection)
                    .onGloballyPositioned {
                        mutableContentHeight.height = it.size.height.toFloat()
                    }
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun View.emoBottomSheet(
    mask: Color = DefaultMaskColor,
    systemCancellable: Boolean = true,
    draggable: Boolean = true,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.Dismiss,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    enter: EnterTransition = slideInVertically(tween()) { it },
    exit: ExitTransition = slideOutVertically(tween()) { it },
    widthLimit: (maxWidth: Dp) -> Dp = { it.coerceAtMost(420.dp) },
    heightLimit: (maxHeight: Dp) -> Dp = { if (it < 640.dp) it - 40.dp else it * 0.85f },
    radius: Dp = 12.dp,
    background: @Composable () -> Color = { MaterialTheme.colorScheme.background },
    themeProvider: @Composable (@Composable () -> Unit) -> Unit = { inner -> inner() },
    content: @Composable (EmoModal) -> Unit
): EmoModal {
    return emoModal(
        Color.Transparent,
        systemCancellable,
        maskTouchBehavior,
        modalHostProvider = modalHostProvider,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        themeProvider = themeProvider
    ) { modal ->
        EmoBottomSheet(
            modal,
            draggable,
            widthLimit,
            heightLimit,
            radius,
            background(),
            mask,
            Modifier.animateEnterExit(
                enter = enter,
                exit = exit
            ),
            content
        )
    }
}

private class BottomSheetNestedScrollConnection(
    val modal: EmoModal,
    val yOffsetStateFlow: MutableState<Float>,
    val contentHeight: MutableHeight
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.Fling) {
            return Offset.Zero
        }
        val currentOffset = yOffsetStateFlow.value
        if (available.y < 0 && currentOffset > 0) {
            val consume = available.y.coerceAtLeast(-currentOffset)
            yOffsetStateFlow.value = currentOffset + consume
            return Offset(0f, consume)
        }
        return super.onPreScroll(available, source)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source == NestedScrollSource.Fling) {
            return Offset.Zero
        }
        if (available.y > 0) {
            yOffsetStateFlow.value = yOffsetStateFlow.value + available.y
            return Offset(0f, available.y)
        }
        return super.onPostScroll(consumed, available, source)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (yOffsetStateFlow.value > 0) {
            if (available.y > 0 || (available.y == 0f && yOffsetStateFlow.value > contentHeight.height / 2)) {
                modal.dismiss()
            } else {
                val animated = Animatable(yOffsetStateFlow.value, Float.VectorConverter)
                animated.asState()
                animated.animateTo(0f, tween()) {
                    yOffsetStateFlow.value = value
                }
            }
            return available
        }
        return Velocity.Zero
    }
}
