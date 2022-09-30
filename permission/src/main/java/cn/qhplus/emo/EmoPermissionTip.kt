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

package cn.qhplus.emo

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

interface EmoPermissionTip {
    @Composable
    fun AnimatedVisibilityScope.Content()
}

class SimpleEmoPermissionTip(val text: String) : EmoPermissionTip {

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun AnimatedVisibilityScope.Content() {
        val isDarkTheme = isSystemInDarkTheme()
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .animateEnterExit(
                    slideInVertically(tween(), initialOffsetY = { -it }),
                    slideOutVertically(tween(), targetOffsetY = { -it })
                )
                .shadow(32.dp, RoundedCornerShape(12.dp), true)
                .background(if (isDarkTheme) Color.DarkGray else Color.White)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                color = if (isDarkTheme) Color.White else Color.Black
            )
        }
    }
}
