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

package cn.qhplus.emo.photo.ui.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.qhplus.emo.photo.ui.LayoutInfo
import cn.qhplus.emo.ui.core.modifier.throttleClick

@Stable
sealed class PaintOption {

    abstract fun newPaintLayer(size: Size, scale: Float): PathEditLayer

    @Composable
    abstract fun Selector(size: Dp, selected: Boolean, onClick: (PaintOption) -> Unit)
}

@Stable
class MosaicPaintOption(
    val level: Int,
    val strokeWidth: Dp
) : PaintOption() {

    override fun newPaintLayer(size: Size, scale: Float): PathEditLayer {
        TODO("Not yet implemented")
    }

    @Composable
    override fun Selector(size: Dp, selected: Boolean, onClick: (PaintOption) -> Unit) {
        val ringWidth = with(LocalDensity.current) {
            2.dp.toPx()
        }
        Canvas(
            modifier = Modifier
                .size(size)
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null
                ) {
                    onClick(this@MosaicPaintOption)
                }
        ) {
            drawCircle(
                Color.White,
                radius = this.size.minDimension / 2 - if (selected) 0f else ringWidth
            )
            drawCircle(
                Color.Black,
                radius = this.size.minDimension / 2 - ringWidth * 2
            )
        }
    }
}

@Stable
class ColorPaintOption(val color: Color, val strokeWidth: Dp) : PaintOption() {

    override fun newPaintLayer(size: Size, scale: Float): PathEditLayer {
        return GraffitiEditLayer(size, color, strokeWidth / scale)
    }

    @Composable
    override fun Selector(size: Dp, selected: Boolean, onClick: (PaintOption) -> Unit) {
        ColorOption(size, color, selected) {
            onClick(this)
        }
    }
}

@Stable
class TextOption(val color: Color) {

    fun newTextLayer(
        reversed: Boolean,
        scale: Float,
        translateX: Float,
        translateY: Float,
        layoutInfo: LayoutInfo,
        onEdit: (TextEditLayer) -> Unit,
        onDelete: (TextEditLayer) -> Unit
    ): TextEditLayer {
        val offset = layoutInfo.contentOffset(translateX, translateY, scale)
        val size = layoutInfo.px.let { Size(it.contentWidth, it.contentHeight) }
        val tx = (-offset.x + layoutInfo.px.panArea.width / 2) / scale - layoutInfo.px.contentWidth / 2
        val ty = (-offset.y + layoutInfo.px.panArea.height / 2) / scale - layoutInfo.px.contentHeight / 2
        return TextEditLayer("", reversed, color, size, scale, tx, ty, onEdit, onDelete)
    }

    @Composable
    fun Selector(size: Dp, selected: Boolean, onClick: (TextOption) -> Unit) {
        ColorOption(size, color, selected) {
            onClick(this)
        }
    }
}

@Composable
fun ColorOption(size: Dp, color: Color, selected: Boolean, onClick: () -> Unit) {
    val ringWidth = with(LocalDensity.current) {
        2.dp.toPx()
    }
    Canvas(
        modifier = Modifier
            .size(size)
            .throttleClick(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null
            ) {
                onClick()
            }
    ) {
        drawCircle(
            Color.White,
            radius = this.size.minDimension / 2 - if (selected) 0f else ringWidth
        )
        drawCircle(
            color,
            radius = this.size.minDimension / 2 - ringWidth * 2
        )
    }
}
