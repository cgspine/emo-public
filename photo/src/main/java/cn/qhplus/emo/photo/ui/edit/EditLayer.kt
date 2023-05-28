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

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.nio.ByteBuffer

internal val EditTypeGraffitiPaint = 1
internal val EditTypeText = 3

@Stable
interface EditLayer {
    @Composable
    fun BoxWithConstraintsScope.Content()

    fun serialize(): ByteArray

    fun toMutable(state: EditState): EditLayer

    fun toImmutable(): EditLayer
}

object EditLayerDeserializeFactory {
    fun deserialize(
        value: ByteArray
    ): EditLayer? {
        val buffer = ByteBuffer.wrap(value)
        val type = buffer.get().toInt()
        val version = buffer.short
        if (type == EditTypeGraffitiPaint) {
            if (version > PAINT_VERSION) {
                return null
            }
            val w = buffer.float
            val h = buffer.float
            val size = Size(w, h)
            val color = Color(buffer.long.toULong())
            val strokeWidth = buffer.float
            val pointCount = buffer.int
            val points = (0 until pointCount).asSequence().map {
                Offset(buffer.float, buffer.float)
            }.toMutableList()
            return GraffitiEditLayer(size, color, strokeWidth.dp, points)
        } else if (type == EditTypeText) {
            if (version > TEXT_VERSION) {
                return null
            }
            val w = buffer.float
            val h = buffer.float
            val size = Size(w, h)
            val parentScale = buffer.float
            val parentOffsetX = buffer.float
            val parentOffsetY = buffer.float
            val color = Color(buffer.long.toULong())
            val scale = buffer.float
            val offsetX = buffer.float
            val offsetY = buffer.float
            val rotation = buffer.float
            val reversed = buffer.int == 1
            val textLength = buffer.int
            val text = String(value, buffer.position(), textLength)
            return TextEditLayer(text, reversed, color, size, parentScale, parentOffsetX, parentOffsetY).apply {
                this.scale = scale
                this.offset = Offset(offsetX, offsetY)
                this.rotation = rotation
            }
        }
        return null
    }
}
