package cn.qhplus.emo.ui.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

@Composable
fun ChevronIcon(tint: Color? = null) {
    Image(
        painter = painterResource(id = R.drawable.ic_chevron),
        contentDescription = "",
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}

enum class CheckStatus {
    none, partial, checked
}


@Composable
fun CheckBox(
    size: Dp,
    status: CheckStatus = CheckStatus.none,
    isEnabled: Boolean = true,
    tint: Color?,
    background: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    ) {
        AnimatedVisibility(
            visible = status == CheckStatus.none,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CheckBoxImage(R.drawable.ic_checkbox_normal, isEnabled, tint, background)
        }

        AnimatedVisibility(
            visible = status == CheckStatus.checked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CheckBoxImage(R.drawable.ic_checkbox_checked, isEnabled, tint, background)
        }

        AnimatedVisibility(
            visible = status == CheckStatus.partial,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CheckBoxImage(R.drawable.ic_checkbox_partial, isEnabled, tint, background)
        }
    }
}

@Composable
private fun CheckBoxImage(
    resourceId: Int,
    isEnabled: Boolean = true,
    tint: Color?,
    background: Color = Color.Transparent
){
    Image(
        painter = painterResource(id = resourceId),
        contentScale = ContentScale.Fit,
        contentDescription = "",
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = Modifier
            .fillMaxSize()
            .let {
                if (isEnabled) {
                    it
                } else {
                    it.alpha(0.5f)
                }
            }.let {
                if (background != Color.Transparent) {
                    it.background(background)
                } else {
                    it
                }
            }
    )
}

@Composable
fun MarkIcon(
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    Image(
        painter = painterResource(id = R.drawable.ic_mark),
        contentDescription = "",
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier
    )
}