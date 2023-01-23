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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cn.qhplus.emo.ui.core.helper.OnePx
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonTopPadding

fun interface TopBarItem {
    @Composable
    fun Compose(topBarHeight: Dp)
}

interface TopBarTitleLayout {
    @Composable
    fun Compose(titleGetter: () -> CharSequence, subTitleGetter: () -> CharSequence, alignTitleCenter: Boolean)
}

class DefaultTopBarTitleLayout(
    val titleFontWeight: FontWeight = FontWeight.Bold,
    val titleFontFamily: FontFamily? = null,
    val titleFontSize: TextUnit = 16.sp,
    val titleOnlyFontSize: TextUnit = 17.sp,
    val subTitleFontWeight: FontWeight = FontWeight.Normal,
    val subTitleFontFamily: FontFamily? = null,
    val subTitleFontSize: TextUnit = 11.sp

) : TopBarTitleLayout {
    @Composable
    override fun Compose(
        titleGetter: () -> CharSequence,
        subTitleGetter: () -> CharSequence,
        alignTitleCenter: Boolean
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (alignTitleCenter) {
                Alignment.CenterHorizontally
            } else {
                Alignment.Start
            }
        ) {
            val title = titleGetter()
            val subTitle = subTitleGetter()
            Text(
                title.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = titleFontWeight,
                fontFamily = titleFontFamily,
                fontSize = if (subTitle.isNotEmpty()) titleFontSize else titleOnlyFontSize,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (subTitle.isNotEmpty()) {
                Text(
                    subTitle.toString(),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontWeight = subTitleFontWeight,
                    fontFamily = subTitleFontFamily,
                    fontSize = subTitleFontSize,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }
}

open class TopBarBackIconItem(
    tint: Color = Color.White,
    pressAlpha: Float = 0.5f,
    disableAlpha: Float = 0.5f,
    enable: Boolean = true,
    onClick: () -> Unit
) : TopBarIconItem(
    R.drawable.ic_topbar_back,
    "返回",
    tint,
    pressAlpha,
    disableAlpha,
    enable,
    onClick
)

open class TopBarIconItem(
    @DrawableRes val icon: Int,
    val contentDescription: String = "",
    val tint: Color = Color.White,
    val pressAlpha: Float = 0.5f,
    val disableAlpha: Float = 0.5f,
    val enable: Boolean = true,
    val onClick: () -> Unit
) : TopBarItem {

    @Composable
    override fun Compose(topBarHeight: Dp) {
        PressWithAlphaBox(
            modifier = Modifier.size(topBarHeight),
            enable = enable,
            pressAlpha = pressAlpha,
            disableAlpha = disableAlpha,
            onClick = onClick
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(icon),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(tint),
                contentScale = ContentScale.Inside
            )
        }
    }
}

open class TopBarTextItem(
    val text: String,
    val paddingHor: Dp = 12.dp,
    val fontSize: TextUnit = 14.sp,
    val fontWeight: FontWeight = FontWeight.Medium,
    val color: Color = Color.White,
    val pressAlpha: Float = 0.5f,
    val disableAlpha: Float = 0.5f,
    val enable: Boolean = true,
    val onClick: () -> Unit
) : TopBarItem {

    @Composable
    override fun Compose(topBarHeight: Dp) {
        PressWithAlphaBox(
            modifier = Modifier
                .height(topBarHeight)
                .padding(horizontal = paddingHor),
            enable = enable,
            pressAlpha = pressAlpha,
            disableAlpha = disableAlpha,
            onClick = onClick
        ) {
            Text(
                text = text,
                modifier = Modifier.align(Alignment.Center),
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        }
    }
}

@Composable
fun TopBarWithLazyListScrollState(
    scrollState: LazyListState,
    title: () -> CharSequence = { "" },
    subTitle: () -> CharSequence = { "" },
    alignTitleCenter: Boolean = true,
    height: Dp = emoTopBarHeight,
    zIndex: Float = emoTopBarZIndex,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    changeWithBackground: Boolean = false,
    scrollAlphaChangeMaxOffset: Dp = emoScrollAlphaChangeMaxOffset,
    shadowElevation: Dp = 16.dp,
    shadowAlpha: Float = 0.6f,
    separatorHeight: Dp = OnePx(),
    separatorColor: Color = MaterialTheme.colorScheme.outline,
    paddingStart: Dp = 4.dp,
    paddingEnd: Dp = 4.dp,
    titleBoxPaddingHor: Dp = 8.dp,
    leftItems: List<TopBarItem> = emptyList(),
    rightItems: List<TopBarItem> = emptyList(),
    titleLayout: TopBarTitleLayout = remember { DefaultTopBarTitleLayout() }
) {
    val percentGetter: Density.() -> Float = {
        if (scrollState.firstVisibleItemIndex > 0 ||
            scrollState.firstVisibleItemScrollOffset.toDp() > scrollAlphaChangeMaxOffset
        ) {
            1f
        } else scrollState.firstVisibleItemScrollOffset.toDp() / scrollAlphaChangeMaxOffset
    }
    TopBarWithPercent(
        percentGetter, title, subTitle, alignTitleCenter, height,
        zIndex, backgroundColor, changeWithBackground,
        shadowElevation, shadowAlpha, separatorHeight, separatorColor,
        paddingStart, paddingEnd, titleBoxPaddingHor,
        leftItems, rightItems, titleLayout
    )
}

@Composable
fun TopBarWithLazyGridScrollState(
    scrollState: LazyGridState,
    title: () -> CharSequence = { "" },
    subTitle: () -> CharSequence = { "" },
    alignTitleCenter: Boolean = true,
    height: Dp = emoTopBarHeight,
    zIndex: Float = emoTopBarZIndex,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    changeWithBackground: Boolean = false,
    scrollAlphaChangeMaxOffset: Dp = emoScrollAlphaChangeMaxOffset,
    shadowElevation: Dp = 16.dp,
    shadowAlpha: Float = 0.6f,
    separatorHeight: Dp = OnePx(),
    separatorColor: Color = MaterialTheme.colorScheme.outline,
    paddingStart: Dp = 4.dp,
    paddingEnd: Dp = 4.dp,
    titleBoxPaddingHor: Dp = 8.dp,
    leftItems: List<TopBarItem> = emptyList(),
    rightItems: List<TopBarItem> = emptyList(),
    titleLayout: TopBarTitleLayout = remember { DefaultTopBarTitleLayout() }
) {
    val percentGetter: Density.() -> Float = {
        if (scrollState.firstVisibleItemIndex > 0 ||
            scrollState.firstVisibleItemScrollOffset.toDp() > scrollAlphaChangeMaxOffset
        ) {
            1f
        } else scrollState.firstVisibleItemScrollOffset.toDp() / scrollAlphaChangeMaxOffset
    }
    TopBarWithPercent(
        percentGetter, title, subTitle, alignTitleCenter, height,
        zIndex, backgroundColor, changeWithBackground,
        shadowElevation, shadowAlpha, separatorHeight, separatorColor,
        paddingStart, paddingEnd, titleBoxPaddingHor,
        leftItems, rightItems, titleLayout
    )
}

@Composable
fun TopBarWithScrollState(
    scrollState: ScrollState,
    title: () -> CharSequence = { "" },
    subTitle: () -> CharSequence = { "" },
    alignTitleCenter: Boolean = true,
    height: Dp = emoTopBarHeight,
    zIndex: Float = emoTopBarZIndex,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    changeWithBackground: Boolean = false,
    scrollAlphaChangeMaxOffset: Dp = emoScrollAlphaChangeMaxOffset,
    shadowElevation: Dp = 16.dp,
    shadowAlpha: Float = 0.6f,
    separatorHeight: Dp = OnePx(),
    separatorColor: Color = MaterialTheme.colorScheme.outline,
    paddingStart: Dp = 4.dp,
    paddingEnd: Dp = 4.dp,
    titleBoxPaddingHor: Dp = 8.dp,
    leftItems: List<TopBarItem> = emptyList(),
    rightItems: List<TopBarItem> = emptyList(),
    titleLayout: TopBarTitleLayout = remember { DefaultTopBarTitleLayout() }
){
    val percentGetter: Density.() -> Float = {
        if (scrollState.value.toDp() >= scrollAlphaChangeMaxOffset ) {
            1f
        } else scrollState.value.toDp() / scrollAlphaChangeMaxOffset
    }
    TopBarWithPercent(
        percentGetter, title, subTitle, alignTitleCenter, height,
        zIndex, backgroundColor, changeWithBackground,
        shadowElevation, shadowAlpha, separatorHeight, separatorColor,
        paddingStart, paddingEnd, titleBoxPaddingHor,
        leftItems, rightItems, titleLayout
    )
}

@Composable
fun TopBarWithPercent(
    percentGetter: Density.() -> Float,
    title: () -> CharSequence,
    subTitle: () -> CharSequence = { "" },
    alignTitleCenter: Boolean = true,
    height: Dp = emoTopBarHeight,
    zIndex: Float = emoTopBarZIndex,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    changeWithBackground: Boolean = false,
    shadowElevation: Dp = 16.dp,
    shadowAlpha: Float = 0.6f,
    separatorHeight: Dp = OnePx(),
    separatorColor: Color = MaterialTheme.colorScheme.outline,
    paddingStart: Dp = 4.dp,
    paddingEnd: Dp = 4.dp,
    titleBoxPaddingHor: Dp = 8.dp,
    leftItems: List<TopBarItem> = emptyList(),
    rightItems: List<TopBarItem> = emptyList(),
    titleLayout: TopBarTitleLayout = remember { DefaultTopBarTitleLayout() }
) {
    val density = LocalDensity.current
    val percent by remember {
        derivedStateOf {
            with(density) {
                percentGetter()
            }
        }
    }
    TopBar(
        title, subTitle,
        alignTitleCenter, height, zIndex,
        if (changeWithBackground) {
            backgroundColor.copy(backgroundColor.alpha * percent)
        } else backgroundColor,
        shadowElevation, { shadowAlpha * percent },
        separatorHeight, { separatorColor.copy(separatorColor.alpha * percent) },
        paddingStart, paddingEnd,
        titleBoxPaddingHor, leftItems, rightItems, titleLayout
    )
}


@Composable
fun TopBar(
    title: () -> CharSequence,
    subTitle: () -> CharSequence = { "" },
    alignTitleCenter: Boolean = true,
    height: Dp = emoTopBarHeight,
    zIndex: Float = emoTopBarZIndex,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    shadowElevation: Dp = 16.dp,
    shadowAlpha: () -> Float = { 0.4f },
    separatorHeight: Dp = OnePx(),
    separatorColor: () -> Color = { Color.Transparent },
    paddingStart: Dp = 4.dp,
    paddingEnd: Dp = 4.dp,
    titleBoxPaddingHor: Dp = 8.dp,
    leftItems: List<TopBarItem> = emptyList(),
    rightItems: List<TopBarItem> = emptyList(),
    titleLayout: TopBarTitleLayout = remember { DefaultTopBarTitleLayout() }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .zIndex(zIndex)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = shadowAlpha()
                    this.shadowElevation = shadowElevation.toPx()
                    this.shape = RectangleShape
                    this.clip = shadowElevation > 0.dp
                }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .windowInsetsCommonTopPadding()
                .height(height)
        ) {
            TopBarContent(
                title,
                subTitle,
                alignTitleCenter,
                height,
                paddingStart,
                paddingEnd,
                titleBoxPaddingHor,
                leftItems,
                rightItems,
                titleLayout
            )
            TopBarSeparator(separatorHeight, separatorColor)
        }
    }
}

@Composable
fun BoxScope.TopBarSeparator(height: Dp, colorGetter: () -> Color){
    val color = colorGetter()
    if (height > 0.dp && color != Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomStart)
                .background(color)
        )
    }
}

@Composable
fun TopBarContent(
    title: () -> CharSequence,
    subTitle: () -> CharSequence,
    alignTitleCenter: Boolean,
    height: Dp = emoTopBarHeight,
    paddingStart: Dp = 4.dp,
    paddingEnd: Dp = 4.dp,
    titleBoxPaddingHor: Dp = 8.dp,
    leftItems: List<TopBarItem> = emptyList(),
    rightItems: List<TopBarItem> = emptyList(),
    titleLayout: TopBarTitleLayout = remember { DefaultTopBarTitleLayout() }
) {
    val measurePolicy = remember(alignTitleCenter) {
        MeasurePolicy { measurables, constraints ->
            var centerMeasurable: Measurable? = null
            var leftPlaceable: Placeable? = null
            var rightPlaceable: Placeable? = null
            var centerPlaceable: Placeable? = null
            val usedConstraints = constraints.copy(minWidth = 0)
            measurables
                .forEach {
                    when ((it.parentData as? TopBarAreaParentData)?.area ?: TopBarArea.Left) {
                        TopBarArea.Left -> {
                            leftPlaceable = it.measure(usedConstraints)
                        }
                        TopBarArea.Right -> {
                            rightPlaceable = it.measure(usedConstraints)
                        }
                        TopBarArea.Center -> {
                            centerMeasurable = it
                        }
                    }
                }
            val leftItemsWidth = leftPlaceable?.measuredWidth ?: 0
            val rightItemsWidth = rightPlaceable?.measuredWidth ?: 0
            val itemsWidthMax = maxOf(leftItemsWidth, rightItemsWidth)
            val titleContainerWidth = if (alignTitleCenter) {
                constraints.maxWidth - itemsWidthMax * 2
            } else {
                constraints.maxWidth - leftItemsWidth - rightItemsWidth
            }
            if (titleContainerWidth > 0) {
                centerPlaceable = centerMeasurable?.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = titleContainerWidth
                    )
                )
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                leftPlaceable?.place(0, 0, 0f)
                rightPlaceable?.let {
                    it.place(constraints.maxWidth - it.measuredWidth, 0, 1f)
                }
                centerPlaceable?.let {
                    if (alignTitleCenter) {
                        it.place(itemsWidthMax, 0, 2f)
                    } else {
                        it.place(leftItemsWidth, 0, 2f)
                    }
                }
            }
        }
    }
    Layout(
        content = {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .topBarArea(TopBarArea.Left),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach {
                    it.Compose(height)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .topBarArea(TopBarArea.Center)
                    .padding(horizontal = titleBoxPaddingHor),
                contentAlignment = Alignment.CenterStart
            ) {
                titleLayout.Compose(title, subTitle, alignTitleCenter)
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .topBarArea(TopBarArea.Right),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rightItems.forEach {
                    it.Compose(height)
                }
            }
        },
        measurePolicy = measurePolicy,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(start = paddingStart, end = paddingEnd)
    )
}

internal enum class TopBarArea { Left, Center, Right }

internal data class TopBarAreaParentData(
    var area: TopBarArea = TopBarArea.Left
)

internal fun Modifier.topBarArea(area: TopBarArea) = this.then(
    TopBarAreaModifier(
        area = area,
        inspectorInfo = debugInspectorInfo {
            name = "area"
            value = area.name
        }
    )
)

internal class TopBarAreaModifier(
    val area: TopBarArea,
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?): TopBarAreaParentData {
        return ((parentData as? TopBarAreaParentData) ?: TopBarAreaParentData()).also {
            it.area = area
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? TopBarAreaParentData ?: return false
        return area == otherModifier.area
    }

    override fun hashCode(): Int {
        return area.hashCode()
    }

    override fun toString(): String =
        "TopBarAreaModifier(area=$area)"
}
