package cn.qhplus.emo.photo.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.qhplus.emo.photo.ui.picker.LocalPhotoPickerConfig
import cn.qhplus.emo.ui.core.PressWithAlphaBox

@Composable
internal fun EditImageButton(
    modifier: Modifier = Modifier,
    config: PhotoEditConfig,
    res: Int,
    enabled: Boolean = true,
    checked: Boolean = false,
    onClick: () -> Unit
) {
    PressWithAlphaBox(
        modifier = modifier,
        enable = enabled,
        onClick = {
            onClick()
        }
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = "",
            colorFilter = ColorFilter.tint(if(checked) config.primaryColor else Color.White),
            contentScale = ContentScale.Inside
        )
    }
}

@Composable
internal fun EditSureButton(
    modifier: Modifier = Modifier,
    config: PhotoEditConfig,
    enabled: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val bgColor = when {
        !enabled -> config.primaryColor.copy(0.5f)
        isPressed.value -> config.primaryColor.copy(0.7f)
        else -> config.primaryColor
    }
    val textColor = Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                onClick()
            }
            .padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            color = textColor
        )
    }
}