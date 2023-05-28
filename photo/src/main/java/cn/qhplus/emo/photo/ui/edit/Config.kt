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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    )
)
