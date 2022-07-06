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

package cn.qhplus.emo.photo.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.qhplus.emo.ui.core.TopBarItem
import kotlinx.coroutines.flow.StateFlow

class PhotoPickerConfig(
    val editable: Boolean = false,
    val primaryColor: Color = Color(0xFF4148E2),
    val commonTextButtonTextColor: Color = Color.White,
    val commonSeparatorColor: Color = Color.White.copy(alpha = 0.3f),
    val commonIconNormalTintColor: Color = Color.White.copy(0.9f),
    val commonIconCheckedTintColor: Color = primaryColor,
    val commonIconCheckedTextColor: Color = Color.White.copy(alpha = 0.6f),

    val commonButtonNormalTextColor: Color = Color.White,
    val commonButtonNormalBgColor: Color = primaryColor,
    val commonButtonDisabledTextColor: Color = Color.White.copy(alpha = 0.3f),
    val commonButtonDisableBgColor: Color = Color.White.copy(alpha = 0.15f),
    val commonButtonPressBgColor: Color = primaryColor.copy(alpha = 0.8f),
    val commonButtonPressedTextColor: Color = commonButtonNormalTextColor,

    val topBarBgColor: Color = Color(0xFF222222),
    val toolBarBgColor: Color = topBarBgColor,

    val topBarBucketFactory: (
        textFlow: StateFlow<String>,
        isFocusFlow: StateFlow<Boolean>,
        onClick: () -> Unit
    ) -> TopBarItem = { textFlow, isFocusFlow, onClick ->
        PhotoPickerBucketTopBarItem(
            bgColor = Color.White.copy(alpha = 0.15f),
            textColor = Color.White,
            iconBgColor = Color.White.copy(alpha = 0.72f),
            iconColor = Color(0xFF333333),
            textFlow = textFlow,
            isFocusFlow = isFocusFlow,
            onClick = onClick
        )
    },
    val topBarSendFactory: (
        canSendSelf: Boolean,
        maxSelectCount: Int,
        selectCountFlow: StateFlow<Int>,
        onClick: () -> Unit
    ) -> TopBarItem = { canSendSelf, maxSelectCount, selectCountFlow, onClick ->
        PhotoSendTopBarItem(
            text = "发送",
            canSendSelf = canSendSelf,
            maxSelectCount = maxSelectCount,
            selectCountFlow = selectCountFlow,
            onClick = onClick
        )
    },

    val screenBgColor: Color = Color(0xFF333333),
    val loadingColor: Color = Color.White,
    val tipTextColor: Color = Color.White,

    val gridPreferredSize: Dp = 80.dp,
    val gridGap: Dp = 2.dp,
    val gridBorderColor: Color = Color.White.copy(alpha = 0.15f),

    val bucketChooserMaskColor: Color = Color.Black.copy(alpha = 0.36f),
    val bucketChooserBgColor: Color = topBarBgColor,
    val bucketChooserIndicationColor: Color = Color.White.copy(alpha = 0.2f),
    val bucketChooserMainTextColor: Color = Color.White,
    val bucketChooserCountTextColor: Color = Color.White.copy(alpha = 0.64f),

    val editPaintOptions: List<EditPaint> = listOf(
        MosaicEditPaint(16),
        MosaicEditPaint(50),
        ColorEditPaint(Color.White),
        ColorEditPaint(Color.Black),
        ColorEditPaint(Color.Red),
        ColorEditPaint(Color.Yellow),
        ColorEditPaint(Color.Green),
        ColorEditPaint(Color.Blue),
        ColorEditPaint(Color.Magenta)
    ),
    val graffitiPaintStrokeWidth: Dp = 5.dp,
    val mosaicPaintStrokeWidth: Dp = 20.dp,

    val textEditMaskColor: Color = Color.Black.copy(0.5f),
    val textEditColorOptions: List<ColorEditPaint> = listOf(
        ColorEditPaint(Color.White),
        ColorEditPaint(Color.Black),
        ColorEditPaint(Color.Red),
        ColorEditPaint(Color.Yellow),
        ColorEditPaint(Color.Green),
        ColorEditPaint(Color.Blue),
        ColorEditPaint(Color.Magenta)
    ),
    val textEditFontSize: TextUnit = 30.sp,
    val textEditLineSpace: TextUnit = 3.sp,
    val textCursorColor: Color = primaryColor,

    val editLayerDeleteAreaNormalBgColor: Color = Color.Black.copy(alpha = 0.3f),
    val editLayerDeleteAreaNormalFocusColor: Color = Color.Red.copy(alpha = 0.6f)
)

val DefaultPhotoPickerConfig by lazy { PhotoPickerConfig() }
val LocalPhotoPickerConfig = staticCompositionLocalOf { DefaultPhotoPickerConfig }

@Composable
fun DefaultPhotoPickerConfigProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPhotoPickerConfig provides DefaultPhotoPickerConfig) {
        content()
    }
}
