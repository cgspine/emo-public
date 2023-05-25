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

package cn.qhplus.emo.photo.pdf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import cn.qhplus.emo.photo.ui.edit.PhotoEditConfig

data class PdfConfig(
    val statusBarDarkContent: Boolean = true,
    val scrollBarBgColor: Color = Color.Black.copy(alpha=0.2f),
    val scrollBarLineColor: Color = Color.White.copy(alpha=0.4f),
    val bgColor: Color = Color.LightGray,
    val tipColor: Color = Color.DarkGray,
    val barBgColor: Color = Color.White,
    val barContentColor: Color = Color.Black,
    val barDividerColor: Color = Color.Black.copy(alpha=0.05f),
    val editConfig: PhotoEditConfig = PhotoEditConfig()
)

val DefaultPdfConfig by lazy { PdfConfig() }
val LocalPdfConfig = staticCompositionLocalOf { DefaultPdfConfig }

interface PdfConfigProvider {
    @Composable
    fun Provide(content: @Composable () -> Unit)
}

class DefaultPdfConfigProvider : PdfConfigProvider {

    @Composable
    override fun Provide(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalPdfConfig provides DefaultPdfConfig) {
            content()
        }
    }
}
