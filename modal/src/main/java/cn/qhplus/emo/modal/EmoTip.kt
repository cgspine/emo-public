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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.qhplus.emo.ui.core.Loading
import kotlinx.coroutines.flow.StateFlow

sealed class TipStatus(open val text: String) {
    data class Loading(override val text: String = "加载中...") : TipStatus(text)
    data class Info(override val text: String) : TipStatus(text)
    data class Done(override val text: String = "加载成功") : TipStatus(text)
    data class Error(override val text: String = "加载失败") : TipStatus(text)
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun EmoTip(flow: StateFlow<TipStatus>) {
    val status by flow.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconSize = 36.dp
            when (status) {
                is TipStatus.Loading -> {
                    Loading(
                        size = iconSize,
                        lineColor = Color.White
                    )
                }
                is TipStatus.Done -> {
                    Image(
                        painter = painterResource(
                            id = R.mipmap.ic_tip_done
                        ),
                        contentDescription = status.text,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit
                    )
                }
                is TipStatus.Error -> {
                    Image(
                        painter = painterResource(
                            id = R.mipmap.ic_tip_error
                        ),
                        contentDescription = status.text,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit
                    )
                }
                is TipStatus.Info -> {
                    Image(
                        painter = painterResource(
                            id = R.mipmap.ic_tip_info
                        ),
                        contentDescription = status.text,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Text(
                text = status.text,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )
        }
    }

}

fun View.emoTip(
    mask: Color = DefaultMaskColor,
    systemCancellable: Boolean = false,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.None,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    enter: EnterTransition = fadeIn(tween(), 0f),
    exit: ExitTransition = fadeOut(tween(), 0f),
    status: StateFlow<TipStatus>
): EmoModal {
    return emoModal(
        mask,
        systemCancellable,
        maskTouchBehavior,
        modalHostProvider = modalHostProvider,
        enter = enter,
        exit = exit
    ) {
        EmoTip(status)
    }
}

fun View.emoStillTip(
    mask: Color = DefaultMaskColor,
    systemCancellable: Boolean = false,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.None,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    status: StateFlow<TipStatus>
): EmoModal {
    return emoStillModal(
        mask,
        systemCancellable,
        maskTouchBehavior,
        modalHostProvider = modalHostProvider
    ) {
        EmoTip(flow = status)
    }
}
