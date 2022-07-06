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

package cn.qhplus.emo.photo.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

sealed class EditPaint {
    @Composable
    abstract fun Compose(size: Dp, selected: Boolean, onClick: () -> Unit)
}

class MosaicEditPaint(
    val scaleLevel: Int
) : EditPaint() {

    @Composable
    override fun Compose(size: Dp, selected: Boolean, onClick: () -> Unit) {
        val ringWidth = with(LocalDensity.current) {
            2.dp.toPx()
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(size)
                .clickable(
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
                Color.Black,
                radius = this.size.minDimension / 2 - ringWidth * 2
            )
        }
    }
}

class ColorEditPaint(val color: Color) : EditPaint() {
    @Composable
    override fun Compose(size: Dp, selected: Boolean, onClick: () -> Unit) {
        val ringWidth = with(LocalDensity.current) {
            2.dp.toPx()
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(size)
                .clickable(
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
}

sealed class PaintEditLayer(val path: Path) {
    abstract fun DrawScope.draw()
    abstract fun drawToBitmap()
}

class GraffitiEditLayer(
    path: Path,
    val color: Color,
    val strokeWidth: Float
) : PaintEditLayer(path) {

    override fun DrawScope.draw() {
        drawPath(
            path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    override fun drawToBitmap() {
    }
}

class MosaicEditLayer(
    path: Path,
    val image: ImageBitmap,
    val strokeWidth: Float
) : PaintEditLayer(path) {

    private val paint = Paint()

    override fun DrawScope.draw() {
        if (!path.isEmpty) {
            drawContext.canvas.withSaveLayer(Rect(Offset.Zero, drawContext.size), paint) {
                drawPath(
                    path,
                    Color.White,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawImage(
                    image,
                    dstSize = IntSize(
                        drawContext.size.width.toInt(),
                        drawContext.size.height.toInt()
                    ),
                    blendMode = BlendMode.SrcIn,
                    filterQuality = FilterQuality.None
                )
            }
        }
    }

    override fun drawToBitmap() {
    }
}
