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
