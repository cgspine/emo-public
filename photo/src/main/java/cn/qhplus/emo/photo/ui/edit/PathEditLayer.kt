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
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import java.nio.ByteBuffer

internal const val PAINT_VERSION = 1

@Stable
sealed class PathEditLayer(
    internal val points: MutableList<Offset> = mutableListOf()
) : EditLayer {
    internal val path: MutableState<Path> = mutableStateOf(
        Path().apply {
            points.foldIndexed(this) { index, acc, offset ->
                if (index == 0) {
                    acc.moveTo(offset.x, offset.y)
                } else {
                    acc.lineTo(offset.x, offset.y)
                }
                acc
            }
        },
        neverEqualPolicy()
    )

    fun append(offset: Offset) {
        points.add(offset)
        val p = path.value
        if (points.size == 1) {
            p.moveTo(offset.x, offset.y)
        } else {
            p.lineTo(offset.x, offset.y)
        }
        path.value = p
    }

    override fun toMutable(state: EditState): EditLayer {
        return this
    }

    override fun toImmutable(): EditLayer {
        return this
    }
}

@Stable
class GraffitiEditLayer(
    private val size: Size,
    private val color: Color,
    private val strokeWidth: Dp,
    points: MutableList<Offset> = mutableListOf()
) : PathEditLayer(points) {

    @Composable
    override fun BoxWithConstraintsScope.Content() {
        val viewportScale = constraints.maxWidth / size.width
        val p = path.value
        if (!p.isEmpty) {
            Canvas(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = viewportScale
                    scaleY = viewportScale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
                onDraw = {
                    drawPath(
                        p,
                        color = color,
                        style = Stroke(
                            width = strokeWidth.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            )
        }
    }

    override fun serialize(): ByteArray {
        val capacity = Byte.SIZE_BYTES + // type
            Short.SIZE_BYTES + // version
            Float.SIZE_BYTES * 2 + // size
            Long.SIZE_BYTES + // color
            Float.SIZE_BYTES + // stroke width
            Int.SIZE_BYTES + Float.SIZE_BYTES * 2 * points.size
        val byteArray = ByteArray(capacity)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.put(EditTypeGraffitiPaint.toByte())
        byteBuffer.putShort(PAINT_VERSION.toShort())
        byteBuffer.putFloat(size.width)
        byteBuffer.putFloat(size.height)
        byteBuffer.putLong(color.value.toLong())
        byteBuffer.putFloat(strokeWidth.value)
        byteBuffer.putInt(points.size)
        points.forEach {
            byteBuffer.putFloat(it.x)
            byteBuffer.putFloat(it.y)
        }
        return byteArray
    }
}

@Stable
class MosaicEditLayer(
    private val image: ImageBitmap,
    private val strokeWidth: Dp,
    points: MutableList<Offset> = mutableListOf()
) : PathEditLayer(points) {

    private val paint = Paint()

    @Composable
    override fun BoxWithConstraintsScope.Content() {
        val p = path.value
        if (!p.isEmpty) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    drawContext.canvas.withSaveLayer(Rect(Offset.Zero, drawContext.size), paint) {
                        drawPath(
                            p,
                            Color.White,
                            style = Stroke(
                                width = strokeWidth.toPx(),
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
            )
        }
    }

    override fun serialize(): ByteArray {
        return ByteArray(0)
    }
}
