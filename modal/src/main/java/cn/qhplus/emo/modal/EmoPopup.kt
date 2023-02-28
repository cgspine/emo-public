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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Wrap
import cn.qhplus.emo.ui.core.PressWithAlphaBox
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val DefaultPopupHorEdgeProtectionMargin = 8.dp
val DefaultPopupVerEdgeProtectionMargin = 96.dp

enum class PopupDirection {
    Bottom,
    Top
}

private fun Modifier.combinedPosedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onLongClick: ((Offset) -> Unit)? = null,
    onClick: (Offset) -> Unit
) = composed(
    factory = {
        val onClickState = rememberUpdatedState(onClick)
        val onLongClickState = rememberUpdatedState(onLongClick)
        val hasLongClick = onLongClick != null
        val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
        if (enabled) {
            DisposableEffect(hasLongClick, interactionSource) {
                onDispose {
                    pressedInteraction.value?.let { oldValue ->
                        val interaction = PressInteraction.Cancel(oldValue)
                        interactionSource.tryEmit(interaction)
                        pressedInteraction.value = null
                    }
                }
            }
        }
        val centreOffset = remember { mutableStateOf(Offset.Zero) }

        Modifier.pointerInput(interactionSource, hasLongClick, enabled) {
            centreOffset.value = size.center.toOffset()
            detectTapGestures(
                onLongPress = if (hasLongClick && enabled) {
                    { onLongClickState.value?.invoke(it) }
                } else {
                    null
                },
                onPress = { offset ->
                    if (enabled) {
                        handlePressInteractionWithDelay(
                            offset,
                            interactionSource,
                            pressedInteraction
                        )
                    }
                },
                onTap = {
                    if (enabled) {
                        onClickState.value.invoke(it)
                    }
                }
            )
        }.indication(interactionSource, indication)
    },
    inspectorInfo = debugInspectorInfo {
        name = "combinedPosedClickable"
        properties["enabled"] = enabled
        properties["onClick"] = onClick
        properties["onLongClick"] = onLongClick
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    }
)

suspend fun PressGestureScope.handlePressInteractionWithDelay(
    pressPoint: Offset,
    interactionSource: MutableInteractionSource,
    pressedInteraction: MutableState<PressInteraction.Press?>
) {
    coroutineScope {
        val delayJob = launch {
            delay(200)
            val pressInteraction = PressInteraction.Press(pressPoint)
            interactionSource.emit(pressInteraction)
            pressedInteraction.value = pressInteraction
        }
        val success = tryAwaitRelease()
        if (delayJob.isActive) {
            delayJob.cancelAndJoin()
            // The press released successfully, before the timeout duration - emit the press
            // interaction instantly. No else branch - if the press was cancelled before the
            // timeout, we don't want to emit a press interaction.
            if (success) {
                val pressInteraction = PressInteraction.Press(pressPoint)
                val releaseInteraction = PressInteraction.Release(pressInteraction)
                interactionSource.emit(pressInteraction)
                interactionSource.emit(releaseInteraction)
            }
        } else {
            pressedInteraction.value?.let { pressInteraction ->
                val endInteraction = if (success) {
                    PressInteraction.Release(pressInteraction)
                } else {
                    PressInteraction.Cancel(pressInteraction)
                }
                interactionSource.emit(endInteraction)
            }
        }
        pressedInteraction.value = null
    }
}

@Composable
fun ClickPositionCheckerBox(
    modifier: Modifier,
    interactionSource: MutableInteractionSource = remember {
        MutableInteractionSource()
    },
    indication: Indication? = rememberRipple(),
    onClick: ((Offset) -> Unit),
    onLongClick: ((Offset) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var offset by remember {
        mutableStateOf<Offset?>(null)
    }
    Box(
        modifier = modifier
            .onGloballyPositioned {
                offset = it.positionInWindow()
            }
            .combinedPosedClickable(
                interactionSource,
                indication,
                onLongClick = if (onLongClick != null) { { offset?.plus(it)?.run(onLongClick) } } else null,
                onClick = { offset?.plus(it)?.run(onClick) }
            )
    ) {
        content()
    }
}

val DEFAULT_DIRECTION_CAL: (Int, Offset) -> PopupDirection by lazy {
    { height, offset ->
        if (offset.y > height / 2) {
            PopupDirection.Top
        } else {
            PopupDirection.Bottom
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun View.emoPopup(
    offset: Offset,
    widthCal: (maxWidth: Dp) -> Dp = { it },
    directionCal: (height: Int, Offset) -> PopupDirection = DEFAULT_DIRECTION_CAL,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    horEdge: Dp = DefaultPopupHorEdgeProtectionMargin,
    verEdge: Dp = DefaultPopupVerEdgeProtectionMargin,
    arrowWidth: Dp = 12.dp,
    arrowHeight: Dp = 8.dp,
    radius: Dp = 8.dp,
    background: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    themeProvider: @Composable (@Composable () -> Unit) -> Unit = { inner -> inner() },
    content: @Composable (EmoModal) -> Unit
): EmoModal {
    return emoModal(
        mask = Color.Transparent,
        modalHostProvider = modalHostProvider,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        themeProvider = themeProvider
    ) { modal ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val direction = directionCal(constraints.maxHeight, offset)
            val widthSpace = maxWidth - horEdge * 2
            val width = widthCal(widthSpace).coerceAtMost(widthSpace)
            val widthPx = with(LocalDensity.current) {
                width.toPx()
            }
            val horEdgePx = with(LocalDensity.current) {
                horEdge.toPx()
            }
            val offsetX = (offset.x - widthPx / 2).coerceAtLeast(horEdgePx).coerceAtMost(constraints.maxWidth - horEdgePx - widthPx)
            val arrowWidthPx = with(LocalDensity.current) {
                arrowWidth.toPx()
            }
            val arrowHeightPx = with(LocalDensity.current) {
                arrowHeight.toPx()
            }
            val radiusPx = with(LocalDensity.current) {
                radius.toPx()
            }
            val arrowCenterX = (
                (offset.x - offsetX)
                    .coerceAtLeast(radiusPx + arrowWidthPx)
                    .coerceAtMost(widthPx - radiusPx - arrowWidthPx)
                )
            val backgroundColor = background()
            when (direction) {
                PopupDirection.Top -> {
                    val offsetY = offset.y - constraints.maxHeight
                    val offsetYDp = with(LocalDensity.current) {
                        offsetY.toDp()
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                            .width(width)
                            .heightIn(max = maxHeight + offsetYDp - verEdge)
                            .animateEnterExit(
                                enter = enter,
                                exit = exit
                            )
                            .drawBehind {
                                val path = Path().apply {
                                    moveTo(arrowCenterX, size.height)
                                    lineTo(arrowCenterX - arrowWidthPx / 2, size.height - arrowHeightPx)
                                    lineTo(arrowCenterX + arrowWidthPx / 2, size.height - arrowHeightPx)
                                    close()
                                }
                                drawPath(path, backgroundColor)
                            }
                            .padding(bottom = arrowHeight)
                            .clip(RoundedCornerShape(radius))
                            .background(backgroundColor)
                    ) {
                        content(modal)
                    }
                }
                PopupDirection.Bottom -> {
                    val offsetY = offset.y
                    val offsetYDp = with(LocalDensity.current) {
                        offsetY.toDp()
                    }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                            .width(width)
                            .heightIn(max = maxHeight - offsetYDp - verEdge)
                            .animateEnterExit(
                                enter = enter,
                                exit = exit
                            )
                            .animateEnterExit(
                                enter = enter,
                                exit = exit
                            )
                            .drawBehind {
                                val path = Path().apply {
                                    moveTo(arrowCenterX, 0f)
                                    lineTo(arrowCenterX - arrowWidthPx / 2, arrowHeightPx)
                                    lineTo(arrowCenterX + arrowWidthPx / 2, arrowHeightPx)
                                    close()
                                }
                                drawPath(path, backgroundColor)
                            }
                            .padding(top = arrowHeight)
                            .clip(RoundedCornerShape(radius))
                            .background(backgroundColor)
                    ) {
                        content(modal)
                    }
                }
            }
        }
    }
}

data class QuickAction(val icon: Int, val text: String, val onClick: () -> Unit)

fun View.emoQuickAction(
    offset: Offset,
    actionWidth: Dp,
    actions: List<QuickAction>,
    tintColor: @Composable () -> Color = { Color.White },
    directionCal: (height: Int, Offset) -> PopupDirection = DEFAULT_DIRECTION_CAL,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    horEdge: Dp = DefaultPopupHorEdgeProtectionMargin,
    verEdge: Dp = DefaultPopupVerEdgeProtectionMargin,
    arrowWidth: Dp = 12.dp,
    arrowHeight: Dp = 8.dp,
    radius: Dp = 8.dp,
    background: @Composable () -> Color = { Color.Black },
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    themeProvider: @Composable (@Composable () -> Unit) -> Unit = { inner -> inner() }
): EmoModal {
    return emoPopup(
        offset,
        widthCal = { maxWidth ->
            val expectedWidth = actionWidth * actions.size
            if (maxWidth >= expectedWidth) {
                expectedWidth
            } else {
                val allowCount = (maxWidth.value / actionWidth.value).toInt()
                actionWidth * allowCount
            }
        },
        directionCal,
        modalHostProvider,
        horEdge,
        verEdge,
        arrowWidth,
        arrowHeight,
        radius,
        background,
        enter,
        exit,
        themeProvider
    ) {
        ConstraintLayout {
            val refs = actions.map {
                val ref = createRef()
                QuickActionItem(
                    action = it,
                    tintColor = tintColor(),
                    modifier = Modifier
                        .constrainAs(ref) {}
                        .width(actionWidth)
                )
                ref
            }.toTypedArray()
            createFlow(
                *refs,
                wrapMode = Wrap.Chain
            )
        }
    }
}

@Composable
fun QuickActionItem(action: QuickAction, tintColor: Color, modifier: Modifier) {
    PressWithAlphaBox(modifier = modifier, onClick = {
        action.onClick()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = action.icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tintColor)
            )
            Text(
                text = action.text,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = tintColor
            )
        }
    }
}
