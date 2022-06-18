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

package cn.qhplus.emo.ui.core.ex

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun DrawScope.drawTopSeparator(color: Color, insetStart: Dp = 0.dp, insetEnd: Dp = 0.dp) {
    drawLine(
        color = color,
        start = Offset(insetStart.toPx(), 0f),
        end = Offset(size.width - insetEnd.toPx(), 0f),
        cap = StrokeCap.Square
    )
}

fun DrawScope.drawBottomSeparator(color: Color, insetStart: Dp = 0.dp, insetEnd: Dp = 0.dp) {
    drawLine(
        color = color,
        start = Offset(insetStart.toPx(), size.height),
        end = Offset(size.width - insetEnd.toPx(), size.height),
        cap = StrokeCap.Square
    )
}

fun DrawScope.drawLeftSeparator(color: Color, insetStart: Dp = 0.dp, insetEnd: Dp = 0.dp) {
    drawLine(
        color = color,
        start = Offset(0f, insetStart.toPx()),
        end = Offset(0f, size.height - insetEnd.toPx()),
        cap = StrokeCap.Square
    )
}

fun DrawScope.drawRightSeparator(color: Color, insetStart: Dp = 0.dp, insetEnd: Dp = 0.dp) {
    drawLine(
        color = color,
        start = Offset(size.width, insetStart.toPx()),
        end = Offset(size.width, size.height - insetEnd.toPx()),
        cap = StrokeCap.Square
    )
}
