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

package cn.qhplus.emo.ui.core

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Loading(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    duration: Int = 600,
    lineCount: Int = 12,
    lineColor: Color = Color.LightGray
) {
    val transition = rememberInfiniteTransition()
    val degree = 360f / lineCount
    val rotate = transition.animateValue(
        initialValue = 0,
        targetValue = lineCount - 1,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(tween(duration, 0, LinearEasing))
    )
    Canvas(modifier = modifier.size(size)) {
        rotate(rotate.value * degree, center) {
            for (i in 0 until lineCount) {
                rotate(degree * i, center) {
                    drawLine(
                        lineColor.copy((i + 1) / lineCount.toFloat()),
                        center + Offset(this.size.width / 4f, 0f),
                        center + Offset(this.size.width / 2f, 0f),
                        this.size.width / 16f
                    )
                }
            }
        }
    }
}
