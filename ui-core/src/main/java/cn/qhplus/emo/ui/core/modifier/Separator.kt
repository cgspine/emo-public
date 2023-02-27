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

package cn.qhplus.emo.ui.core.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.qhplus.emo.ui.core.ex.drawBottomSeparator
import cn.qhplus.emo.ui.core.ex.drawLeftSeparator
import cn.qhplus.emo.ui.core.ex.drawRightSeparator
import cn.qhplus.emo.ui.core.ex.drawTopSeparator

fun Modifier.topSeparator(
    color: Color,
    insetStart: Dp = 0.dp,
    insetEnd: Dp = 0.dp,
    strokeWidth: Dp = 0.dp,
    pathEffect: PathEffect? = null
) = drawBehind {
    drawTopSeparator(color, insetStart, insetEnd, if(strokeWidth == 0.dp) Stroke.HairlineWidth else strokeWidth.toPx(), pathEffect)
}

fun Modifier.rightSeparator(
    color: Color,
    insetStart: Dp = 0.dp,
    insetEnd: Dp = 0.dp,
    strokeWidth: Dp = 0.dp,
    pathEffect: PathEffect? = null
) = drawBehind {
    drawRightSeparator(color, insetStart, insetEnd, if(strokeWidth == 0.dp) Stroke.HairlineWidth else strokeWidth.toPx(), pathEffect)
}

fun Modifier.bottomSeparator(
    color: Color,
    insetStart: Dp = 0.dp,
    insetEnd: Dp = 0.dp,
    strokeWidth: Dp = 0.dp,
    pathEffect: PathEffect? = null
) = drawBehind {
    drawBottomSeparator(color, insetStart, insetEnd, if(strokeWidth == 0.dp) Stroke.HairlineWidth else strokeWidth.toPx(), pathEffect)
}

fun Modifier.leftSeparator(
    color: Color,
    insetStart: Dp = 0.dp,
    insetEnd: Dp = 0.dp,
    strokeWidth: Dp = 0.dp,
    pathEffect: PathEffect? = null
) = drawBehind {
    drawLeftSeparator(color, insetStart, insetEnd, if(strokeWidth == 0.dp) Stroke.HairlineWidth else strokeWidth.toPx(), pathEffect)
}
