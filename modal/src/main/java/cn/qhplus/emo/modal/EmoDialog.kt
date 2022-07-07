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

package cn.qhplus.emo.modal

import android.view.View
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.qhplus.emo.ui.core.Item
import cn.qhplus.emo.ui.core.R
import cn.qhplus.emo.ui.core.modifier.throttleClick

val DefaultDialogPaddingHor = 20.dp
val DefaultDialogHorEdgeProtectionMargin = 20.dp
val DefaultDialogVerEdgeProtectionMargin = 20.dp

val DefaultDialogListItemTextStyle by lazy {
    TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun EmoDialog(
    modal: EmoModal,
    horEdge: Dp = DefaultDialogHorEdgeProtectionMargin,
    verEdge: Dp = DefaultDialogVerEdgeProtectionMargin,
    widthLimit: Dp = 360.dp,
    radius: Dp = 2.dp,
    background: Color = Color.White,
    content: @Composable (EmoModal) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horEdge, vertical = verEdge),
        contentAlignment = Alignment.Center
    ) {
        var modifier = if (widthLimit < maxWidth) {
            Modifier.width(widthLimit)
        } else {
            Modifier.fillMaxWidth()
        }
        if (radius > 0.dp) {
            modifier = modifier.clip(RoundedCornerShape(radius))
        }
        modifier = modifier
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
        Box(modifier = modifier) {
            content(modal)
        }
    }
}

@Composable
fun EmoDialogActions(
    modal: EmoModal,
    actions: List<EmoModalAction>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        actions.forEach {
            EmoDialogAction(
                text = it.text,
                enabled = it.enabled,
                color = it.color
            ) {
                it.onClick(modal)
            }
        }
    }
}

@Composable
fun EmoDialogMsg(
    modal: EmoModal,
    title: String,
    content: String,
    actions: List<EmoModalAction>
) {
    Column {
        EmoDialogTitle(title)
        EmoDialogMsgContent(content)
        EmoDialogActions(modal, actions)
    }
}

@Composable
fun EmoDialogList(
    modal: EmoModal,
    maxHeight: Dp = Dp.Unspecified,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    children: LazyListScope.(EmoModal) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(0.dp, maxHeight),
        contentPadding = contentPadding
    ) {
        children(modal)
    }
}

@Composable
fun EmoDialogMarkList(
    modal: EmoModal,
    list: List<String>,
    markIndex: Int,
    state: LazyListState = rememberLazyListState(markIndex),
    maxHeight: Dp = Dp.Unspecified,
    itemIndication: Indication = rememberRipple(),
    itemTextStyle: TextStyle = DefaultDialogListItemTextStyle,
    itemTextColor: Color = Color.Black,
    itemMarkTintColor: Color = Color.Black,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    onItemClick: (modal: EmoModal, index: Int) -> Unit
) {
    EmoDialogList(modal, maxHeight, state, contentPadding) {
        itemsIndexed(list) { index, item ->
            Item(
                title = item,
                indication = itemIndication,
                titleTextStyle = itemTextStyle,
                titleTextColor = itemTextColor,
                accessory = {
                    if (markIndex == index) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_mark),
                            contentDescription = "",
                            colorFilter = ColorFilter.tint(itemMarkTintColor)
                        )
                    }
                }
            ) {
                onItemClick(modal, index)
            }
        }
    }
}

@Composable
fun EmoDialogMutiCheckList(
    modal: EmoModal,
    list: List<String>,
    checked: Set<Int>,
    disabled: Set<Int> = emptySet(),
    disableAlpha: Float = 0.5f,
    state: LazyListState = rememberLazyListState(0),
    maxHeight: Dp = Dp.Unspecified,
    itemIndication: Indication = rememberRipple(),
    itemTextStyle: TextStyle = DefaultDialogListItemTextStyle,
    itemTextColor: Color = Color.Black,
    itemCheckNormalTint: Color = Color.DarkGray,
    itemCheckCheckedTint: Color = Color.Black,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    onItemClick: (modal: EmoModal, index: Int) -> Unit
) {
    EmoDialogList(modal, maxHeight, state, contentPadding) {
        itemsIndexed(list) { index, item ->
            val isDisabled = disabled.contains(index)
            val onClick: (() -> Unit)? = if (isDisabled) null else {
                {
                    onItemClick(modal, index)
                }
            }
            Item(
                title = item,
                indication = itemIndication,
                titleTextStyle = itemTextStyle,
                titleTextColor = itemTextColor,
                alpha = if (isDisabled) disableAlpha else 1f,
                accessory = {
                    if (checked.contains(index)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_checkbox_checked),
                            contentDescription = "",
                            colorFilter = ColorFilter.tint(itemCheckCheckedTint)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_checkbox_normal),
                            contentDescription = "",
                            colorFilter = ColorFilter.tint(itemCheckNormalTint)
                        )
                    }
                },
                onClick = onClick
            )
        }
    }
}

@Composable
fun EmoDialogTitle(
    text: String,
    fontSize: TextUnit = 16.sp,
    textAlign: TextAlign? = null,
    color: Color = Color.Black,
    fontWeight: FontWeight? = FontWeight.Bold,
    fontFamily: FontFamily? = null,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = 20.sp
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 24.dp,
                start = DefaultDialogPaddingHor,
                end = DefaultDialogPaddingHor
            ),
        textAlign = textAlign,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        maxLines = maxLines,
        lineHeight = lineHeight
    )
}

@Composable
fun EmoDialogMsgContent(
    text: String,
    fontSize: TextUnit = 14.sp,
    textAlign: TextAlign? = null,
    color: Color = Color.Black,
    fontWeight: FontWeight? = FontWeight.Normal,
    fontFamily: FontFamily? = null,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = 16.sp
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = DefaultDialogPaddingHor,
                end = DefaultDialogPaddingHor,
                top = 16.dp,
                bottom = 24.dp
            ),
        textAlign = textAlign,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        maxLines = maxLines,
        lineHeight = lineHeight
    )
}

@Composable
fun EmoDialogAction(
    text: String,
    color: Color,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = FontWeight.Bold,
    fontFamily: FontFamily? = null,
    paddingVer: Dp = 9.dp,
    paddingHor: Dp = 14.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = paddingHor, vertical = paddingVer)
            .alpha(if (isPressed.value) 0.5f else 1f)
            .throttleClick(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick.invoke()
            },
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily
    )
}

fun View.emoDialog(
    mask: Color = DefaultMaskColor,
    systemCancellable: Boolean = true,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.Dismiss,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    enter: EnterTransition = fadeIn(tween(), 0f),
    exit: ExitTransition = fadeOut(tween(), 0f),
    horEdge: Dp = DefaultDialogHorEdgeProtectionMargin,
    verEdge: Dp = DefaultDialogVerEdgeProtectionMargin,
    widthLimit: Dp = 360.dp,
    radius: Dp = 12.dp,
    background: Color = Color.White,
    content: @Composable (EmoModal) -> Unit
): EmoModal {
    return emoModal(
        mask,
        systemCancellable,
        maskTouchBehavior,
        modalHostProvider = modalHostProvider,
        enter = enter,
        exit = exit
    ) { modal ->
        EmoDialog(modal, horEdge, verEdge, widthLimit, radius, background, content)
    }
}

fun View.emoStillDialog(
    mask: Color = DefaultMaskColor,
    systemCancellable: Boolean = true,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.Dismiss,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    horEdge: Dp = DefaultDialogHorEdgeProtectionMargin,
    verEdge: Dp = DefaultDialogVerEdgeProtectionMargin,
    widthLimit: Dp = 360.dp,
    radius: Dp = 12.dp,
    background: Color = Color.White,
    content: @Composable (EmoModal) -> Unit
): EmoModal {
    return emoStillModal(mask, systemCancellable, maskTouchBehavior, modalHostProvider = modalHostProvider) { modal ->
        EmoDialog(modal, horEdge, verEdge, widthLimit, radius, background, content)
    }
}
