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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
val DefaultToastHorEdgeProtectionMargin = 20.dp
val DefaultToastVerEdgeProtectionMargin = 96.dp

@Composable
fun EmoToast(
    modal: EmoModal,
    radius: Dp = 8.dp,
    background: Color = Color.DarkGray,
    content: @Composable BoxScope.(EmoModal) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(radius))
            .background(background)
    ) {
        content(modal)
    }
}

fun View.emoToast(
    text: String,
    textColor: Color = Color.White,
    fontSize: TextUnit = 16.sp,
    duration: Long = 1000,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    alignment: Alignment = Alignment.BottomCenter,
    horEdge: Dp = DefaultToastHorEdgeProtectionMargin,
    verEdge: Dp = DefaultToastVerEdgeProtectionMargin,
    radius: Dp = 8.dp,
    background: Color = Color.Black,
    enter: EnterTransition = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit: ExitTransition = slideOutVertically(targetOffsetY = { it }) + fadeOut()
): EmoModal {
    return emoToast(
        duration,
        modalHostProvider,
        alignment,
        horEdge,
        verEdge,
        radius,
        background,
        enter,
        exit
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun View.emoToast(
    duration: Long = 1000,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    alignment: Alignment = Alignment.BottomCenter,
    horEdge: Dp = DefaultToastHorEdgeProtectionMargin,
    verEdge: Dp = DefaultToastVerEdgeProtectionMargin,
    radius: Dp = 8.dp,
    background: Color = Color.Black,
    enter: EnterTransition = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit: ExitTransition = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    content: @Composable BoxScope.(EmoModal) -> Unit
): EmoModal {
    var job: Job? = null
    return emoModal(
        Color.Transparent,
        false,
        MaskTouchBehavior.Penetrate,
        -1,
        modalHostProvider,
        enter = EnterTransition.None,
        exit = ExitTransition.None
    ) { modal ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horEdge, vertical = verEdge),
            contentAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .animateEnterExit(
                        enter = enter,
                        exit = exit
                    )
            ) {
                EmoToast(modal, radius, background, content)
            }
        }
    }.doOnShow {
        job = scope.launch {
            delay(duration)
            job = null
            it.dismiss()
        }
    }.doOnDismiss {
        job?.cancel()
        job = null
    }.show()
}

fun View.emoStillToast(
    text: String,
    textColor: Color = Color.White,
    fontSize: TextUnit = 16.sp,
    duration: Long = 1000,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    alignment: Alignment = Alignment.BottomCenter,
    horEdge: Dp = DefaultToastHorEdgeProtectionMargin,
    verEdge: Dp = DefaultToastVerEdgeProtectionMargin,
    radius: Dp = 8.dp,
    background: Color = Color.Black
): EmoModal {
    return emoStillToast(
        duration,
        modalHostProvider,
        alignment,
        horEdge,
        verEdge,
        radius,
        background
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .align(Alignment.Center)
        )
    }
}

fun View.emoStillToast(
    duration: Long = 1000,
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    alignment: Alignment = Alignment.BottomCenter,
    horEdge: Dp = DefaultToastHorEdgeProtectionMargin,
    verEdge: Dp = DefaultToastVerEdgeProtectionMargin,
    radius: Dp = 8.dp,
    background: Color = Color.Black,
    content: @Composable BoxScope.(EmoModal) -> Unit
): EmoModal {
    var job: Job? = null
    return emoStillModal(
        Color.Transparent,
        false,
        MaskTouchBehavior.Penetrate,
        -1,
        modalHostProvider
    ) { modal ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horEdge, vertical = verEdge),
            contentAlignment = alignment
        ) {
            EmoToast(modal, radius, background, content)
        }
    }.doOnShow {
        job = scope.launch {
            delay(duration)
            job = null
            it.dismiss()
        }
    }.doOnDismiss {
        job?.cancel()
        job = null
    }.show()
}
