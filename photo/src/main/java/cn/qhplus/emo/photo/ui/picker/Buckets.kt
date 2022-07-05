package cn.qhplus.emo.photo.ui.picker

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import cn.qhplus.emo.photo.data.MediaPhotoBucketVO
import cn.qhplus.emo.ui.core.MarkIcon
import cn.qhplus.emo.ui.core.modifier.bottomSeparator
import cn.qhplus.emo.ui.core.modifier.throttleClick
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding

@Composable
fun ConstraintLayoutScope.PhotoPickerBucketChooser(
    focus: Boolean,
    data: List<MediaPhotoBucketVO>,
    currentId: String,
    onBucketClick: (MediaPhotoBucketVO) -> Unit,
    onDismiss: () -> Unit
) {
    val (mask, content) = createRefs()
    AnimatedVisibility(
        visible = focus,
        modifier = Modifier.constrainAs(mask) {
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        },
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalPhotoPickerConfig.current.bucketChooserMaskColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
        )
    }
    AnimatedVisibility(
        visible = focus,
        modifier = Modifier.constrainAs(content) {
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        },
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        BoxWithConstraints(modifier = Modifier.windowInsetsCommonNavPadding()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.8f)
                    .wrapContentHeight()
                    .background(LocalPhotoPickerConfig.current.bucketChooserBgColor),
            ) {
                items(data, key = { it.id }) {
                    PhotoPickerBucketItem(it, it.id == currentId, onBucketClick)
                }
            }
        }

    }
}

@Composable
fun PhotoPickerBucketItem(
    data: MediaPhotoBucketVO,
    isCurrent: Boolean,
    onBucketClick: (MediaPhotoBucketVO) -> Unit
) {
    val h = 60.dp
    val textBeginMargin = 16.dp
    val config = LocalPhotoPickerConfig.current
    ConstraintLayout(modifier = Modifier
        .fillMaxWidth()
        .height(h)
        .bottomSeparator(color = config.commonSeparatorColor, insetStart = h + textBeginMargin)
        .throttleClick(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(color = config.bucketChooserIndicationColor)
        ) {
            onBucketClick(data)
        }
    ) {
        val (pic, title, num, mark) = createRefs()
        val chainHor = createHorizontalChain(title, num, chainStyle = ChainStyle.Packed(0f))
        constrain(chainHor) {
            start.linkTo(pic.end, margin = textBeginMargin)
            end.linkTo(mark.start, margin = 16.dp)
        }
        Box(modifier = Modifier
            .size(h)
            .constrainAs(pic) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }) {
            val thumbnail = remember(data) {
                data.list.firstOrNull()?.photoProvider?.thumbnail(true)
            }
            thumbnail?.Compose(
                contentScale = ContentScale.Crop,
                isContainerDimenExactly = true,
                onSuccess = null,
                onError = null
            )
        }
        Text(
            text = data.name,
            fontSize = 17.sp,
            color = config.bucketChooserMainTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(title) {
                width = Dimension.preferredWrapContent
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
        )
        Text(
            text = "(${data.list.size})",
            fontSize = 17.sp,
            color = config.bucketChooserCountTextColor,
            modifier = Modifier.constrainAs(num) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
        )
        MarkIcon(
            modifier = Modifier.constrainAs(mark) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end, 16.dp)
                visibility = if (isCurrent) Visibility.Visible else Visibility.Gone
            },
            tint = config.commonIconCheckedTintColor
        )
    }
}