package cn.qhplus.emo.photo.ui.edit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf


data class PhotoEditConfig(
    val primaryColor: Color = Color(0xFF4148E2),
    val optionSelectorSize: Dp = 24.dp,

    val paintOptions: PersistentList<PaintOption> = persistentListOf(
        ColorPaintOption(Color.White, 5.dp),
        ColorPaintOption(Color.Black, 5.dp),
        ColorPaintOption(Color.Yellow, 5.dp),
        ColorPaintOption(Color.Red, 5.dp),
        ColorPaintOption(Color.Green, 5.dp),
        ColorPaintOption(Color.Blue, 5.dp),
        ColorPaintOption(Color.Magenta, 5.dp)
    ),
    val textEditMaskColor: Color = Color.Black.copy(alpha = 0.5f),
    val textEditOptions: PersistentList<TextOption> = persistentListOf(
        TextOption(Color.White),
        TextOption(Color.Black),
        TextOption(Color.Yellow),
        TextOption(Color.Red),
        TextOption(Color.Green),
        TextOption(Color.Blue),
        TextOption(Color.Magenta)
    ),
)