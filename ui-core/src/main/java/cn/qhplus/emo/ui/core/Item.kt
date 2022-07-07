package cn.qhplus.emo.ui.core

import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.qhplus.emo.ui.core.modifier.throttleClick

val DefaultItemTitleTextStyle by lazy {
    TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )
}

val DefaultItemDetailTextStyle by lazy {
    TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
}

@Composable
fun Item(
    title: String,
    detail: String = "",
    alpha: Float = 1f,
    background: Color = Color.Transparent,
    indication: Indication = rememberRipple(),
    titleTextStyle: TextStyle = DefaultItemTitleTextStyle,
    titleTextColor: Color = Color.Black,
    detailTextStyle: TextStyle = DefaultItemDetailTextStyle,
    detailTextColor: Color = Color.DarkGray,
    minHeight: Dp = 56.dp,
    paddingHor: Dp = 20.dp,
    paddingVer: Dp = 12.dp,
    gapBetweenTitleAndDetail: Dp = 4.dp,
    accessory: @Composable (RowScope.() -> Unit)? = null,
    drawBehind: (DrawScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = minHeight)
        .alpha(alpha)
        .background(background)
        .drawBehind {
            drawBehind?.invoke(this)
        }
        .throttleClick(
            enabled = onClick != null,
            interactionSource = remember { MutableInteractionSource() },
            indication = indication
        ) {
            onClick?.invoke()
        }
        .padding(horizontal = paddingHor, vertical = paddingVer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleTextColor,
                modifier = Modifier.fillMaxWidth(),
                style = titleTextStyle
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = detailTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = gapBetweenTitleAndDetail),
                    style = detailTextStyle
                )
            }

        }
        accessory?.invoke(this)
    }
}