package cn.qhplus.emo.modal

import android.view.View
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp

val DefaultPopupHorEdgeProtectionMargin = 8.dp
val DefaultPopupVerEdgeProtectionMargin = 96.dp

enum class PopupDirection {
    Bottom,
    Top
}

@Composable
fun ClickBoxWithWindowPos(
    modifier: Modifier,
    onClick: (Offset) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var offset by remember {
        mutableStateOf<Offset?>(null)
    }
    Box(
        modifier = modifier
            .onGloballyPositioned {
                offset = it.positionInWindow()
            }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    offset
                        ?.plus(pos)
                        ?.let {
                            onClick.invoke(it)
                        }
                }
            }
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
        themeProvider = themeProvider,
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
            val arrowCenterX = ((offset.x - offsetX)
                .coerceAtLeast(radiusPx + arrowWidthPx)
                .coerceAtMost(widthPx - radiusPx - arrowWidthPx))
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