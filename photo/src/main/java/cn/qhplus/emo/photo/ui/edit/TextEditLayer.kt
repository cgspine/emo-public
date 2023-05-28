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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cn.qhplus.emo.photo.R
import cn.qhplus.emo.ui.core.modifier.throttleNoIndicationClick
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

internal val TEXT_VERSION = 1

internal val TextEditStyle by lazy {
    TextStyle.Default.copy(
        fontSize = 18.sp,
        lineHeight = 1.5.em,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun TextLayout(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    color: Color,
    bgColor: Color = Color.Transparent,
    focus: () -> Boolean,
    onDelete: () -> Unit
) {
    val focusPointSize = with(LocalDensity.current) {
        6.dp.toPx()
    }
    val focusLineWidth = with(LocalDensity.current) {
        2.dp.toPx()
    }
    val isFocused = focus()
    Box(modifier = modifier) {
        Text(
            text = text,
            color = color,
            style = style,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(28.dp)
                .let {
                    if (isFocused) {
                        it.background(bgColor)
                    } else {
                        it.background(bgColor, shape = RoundedCornerShape(6.dp))
                    }
                }
                .drawWithContent {
                    drawContent()

                    if (isFocused) {
                        drawRect(
                            Color.White,
                            style = Stroke(focusLineWidth)
                        )
                        val focusSize = Size(focusPointSize, focusPointSize)
                        drawRect(
                            Color.White,
                            topLeft = Offset.Zero,
                            size = focusSize
                        )
                        drawRect(
                            Color.White,
                            topLeft = Offset(size.width - focusPointSize, 0f),
                            size = focusSize
                        )
                        drawRect(
                            Color.White,
                            topLeft = Offset(0f, size.height - focusPointSize),
                            size = focusSize
                        )
                        drawRect(
                            Color.White,
                            topLeft = Offset(size.width - focusPointSize, size.height - focusPointSize),
                            size = focusSize
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (isFocused) {
            Image(
                painter = painterResource(R.drawable.ic_edit_del),
                contentDescription = "",
                colorFilter = ColorFilter.tint(Color.White),
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .throttleNoIndicationClick {
                        onDelete()
                    }
            )
        }
    }
}

@Stable
class TextEditLayer(
    initText: String,
    initReversed: Boolean,
    initColor: Color,
    val size: Size,
    val parentScale: Float,
    val parentOffsetX: Float,
    val parentOffsetY: Float,
    val onEdit: ((TextEditLayer) -> Unit)? = null,
    val onDelete: ((TextEditLayer) -> Unit)? = null
) : EditLayer {
    internal var color by mutableStateOf(initColor)
    internal var text by mutableStateOf(TextFieldValue(initText, TextRange(initText.length)))
    internal var offset by mutableStateOf(Offset.Zero)
    internal var scale by mutableStateOf(1f)
    internal var rotation by mutableStateOf(0f)
    internal var isFocus by mutableStateOf(false)
    internal var reversed by mutableStateOf(initReversed)

    override fun toImmutable(): EditLayer {
        if (onEdit == null || onDelete == null) {
            return this
        }
        return TextEditLayer(text.text, reversed, color, size, parentScale, parentOffsetX, parentOffsetY).also {
            it.offset = offset
            it.scale = scale
            it.rotation = rotation
        }
    }

    override fun toMutable(state: EditState): EditLayer {
        return TextEditLayer(
            text.text,
            reversed,
            color,
            size,
            parentScale,
            parentOffsetX,
            parentOffsetY,
            { state.scene.value = EditSceneText(it) },
            { state.textEditLayers.remove(it) }
        ).also {
            it.offset = offset
            it.scale = scale
            it.rotation = rotation
        }
    }

    @Composable
    override fun BoxWithConstraintsScope.Content() {
        val viewportScale = constraints.maxWidth / size.width
        if (isFocus) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
                    .throttleNoIndicationClick { isFocus = false }
            )
        }
        TextLayout(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale * viewportScale / parentScale
                    scaleY = scale * viewportScale / parentScale
                    translationX = parentOffsetX * viewportScale + offset.x * viewportScale / parentScale
                    translationY = parentOffsetY * viewportScale + offset.y * viewportScale / parentScale
                    rotationZ = rotation
                }
                .let {
                    if (onEdit == null || onDelete == null) {
                        it
                    } else {
                        it.pointerInput(this@TextEditLayer, isFocus) {
                            coroutineScope {
                                launch {
                                    detectTapGestures(
                                        onTap = {
                                            if (isFocus) {
                                                onEdit.invoke(this@TextEditLayer)
                                            } else {
                                                isFocus = true
                                            }
                                        }
                                    )
                                }

                                if (isFocus) {
                                    launch {
                                        detectTransformGestures { _, pan, zoom, rotate ->
                                            offset += pan
                                            scale *= zoom
                                            rotation += rotate
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            text = text.text,
            style = TextEditStyle,
            color = if (reversed) {
                if (color == Color.White) Color.Black else Color.White
            } else color,
            bgColor = if (reversed) color else Color.Transparent,
            focus = { isFocus },
            onDelete = {
                onDelete?.invoke(this@TextEditLayer)
            }
        )
    }

    override fun serialize(): ByteArray {
        val content = text.text.toByteArray()
        val capacity = Byte.SIZE_BYTES + // type
            Short.SIZE_BYTES + // version
            Float.SIZE_BYTES * 5 + // size, parentScale, parentOffsetX, parentOffsetY
            Long.SIZE_BYTES + // color
            Float.SIZE_BYTES * 4 + // scale, offset, rotation
            Int.SIZE_BYTES + // reversed
            Int.SIZE_BYTES + content.size
        val byteArray = ByteArray(capacity)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.put(EditTypeText.toByte())
        byteBuffer.putShort(TEXT_VERSION.toShort())
        byteBuffer.putFloat(size.width)
        byteBuffer.putFloat(size.height)
        byteBuffer.putFloat(parentScale)
        byteBuffer.putFloat(parentOffsetX)
        byteBuffer.putFloat(parentOffsetY)
        byteBuffer.putLong(color.value.toLong())
        byteBuffer.putFloat(scale)
        byteBuffer.putFloat(offset.x)
        byteBuffer.putFloat(offset.y)
        byteBuffer.putFloat(rotation)
        byteBuffer.putInt(if (reversed) 1 else 0)
        byteBuffer.putInt(content.size)
        byteBuffer.put(content)
        return byteArray
    }
}
