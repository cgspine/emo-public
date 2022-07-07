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

import android.os.SystemClock
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView

val DefaultMaskColor = Color.Black.copy(alpha = 0.5f)

enum class MaskTouchBehavior {
    Dismiss, Penetrate, None
}

private class ModalHolder(var current: EmoModal? = null)

class EmoModalAction(
    val text: String,
    val color: Color,
    val enabled: Boolean = true,
    val onClick: (EmoModal) -> Unit
)

private class ShowingModals {
    val modals = mutableMapOf<Long, EmoModal>()
}

@Composable
fun EmoModal(
    isVisible: Boolean,
    mask: Color = DefaultMaskColor,
    enter: EnterTransition = fadeIn(tween(), 0f),
    exit: ExitTransition = fadeOut(tween(), 0f),
    systemCancellable: Boolean = true,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.Dismiss,
    doOnShow: EmoModal.Action? = null,
    doOnDismiss: EmoModal.Action? = null,
    uniqueId: Long = SystemClock.elapsedRealtimeNanos(),
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    content: @Composable AnimatedVisibilityScope.(EmoModal) -> Unit
) {
    val modalHolder = remember {
        ModalHolder(null)
    }
    if (isVisible) {
        if (modalHolder.current == null) {
            val modal = LocalView.current.emoModal(
                mask,
                systemCancellable,
                maskTouchBehavior,
                uniqueId,
                modalHostProvider,
                enter,
                exit,
                content
            )
            doOnShow?.let { modal.doOnShow(it) }
            doOnDismiss?.let { modal.doOnDismiss(it) }
            modalHolder.current = modal
        }
    } else {
        modalHolder.current?.dismiss()
    }
    DisposableEffect("") {
        object : DisposableEffectResult {
            override fun dispose() {
                modalHolder.current?.dismiss()
            }
        }
    }
}

interface EmoModal {
    fun show(): EmoModal
    fun dismiss()
    fun isShowing(): Boolean

    fun doOnShow(listener: Action): EmoModal
    fun doOnDismiss(listener: Action): EmoModal
    fun removeOnShowAction(listener: Action): EmoModal
    fun removeOnDismissAction(listener: Action): EmoModal

    fun interface Action {
        fun invoke(modal: EmoModal)
    }
}

fun interface ModalHostProvider {
    fun provide(view: View): Pair<FrameLayout, OnBackPressedDispatcher>
}

class ActivityHostModalProvider : ModalHostProvider {
    override fun provide(view: View): Pair<FrameLayout, OnBackPressedDispatcher> {
        val contentLayout = view.rootView
            .findViewById<FrameLayout>(Window.ID_ANDROID_CONTENT) ?: throw RuntimeException("View is not attached to Activity")
        val activity = contentLayout.context as? ComponentActivity ?: throw RuntimeException("view's rootView's context is not ComponentActivity")
        return contentLayout to activity.onBackPressedDispatcher
    }
}

val DefaultModalHostProvider = ActivityHostModalProvider()

fun View.emoModal(
    mask: Color = DefaultMaskColor,
    systemCancellable: Boolean = true,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.Dismiss,
    uniqueId: Long = SystemClock.elapsedRealtimeNanos(),
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    enter: EnterTransition = fadeIn(tween(), 0f),
    exit: ExitTransition = fadeOut(tween(), 0f),
    content: @Composable AnimatedVisibilityScope.(EmoModal) -> Unit
): EmoModal {
    if (!isAttachedToWindow) {
        throw RuntimeException("View is not attached to window")
    }
    val modalHost = modalHostProvider.provide(this)
    val modal = AnimateModalImpl(
        modalHost.first,
        modalHost.second,
        mask,
        systemCancellable,
        maskTouchBehavior,
        enter,
        exit,
        content
    )
    val hostView = modalHost.first
    handleModelUnique(hostView, modal, uniqueId)
    return modal
}

fun View.emoStillModal(
    mask: Color = DefaultMaskColor,
    systemCancellable: Boolean = true,
    maskTouchBehavior: MaskTouchBehavior = MaskTouchBehavior.Dismiss,
    uniqueId: Long = SystemClock.elapsedRealtimeNanos(),
    modalHostProvider: ModalHostProvider = DefaultModalHostProvider,
    content: @Composable (EmoModal) -> Unit
): EmoModal {
    if (!isAttachedToWindow) {
        throw RuntimeException("View is not attached to window")
    }
    val modalHost = modalHostProvider.provide(this)
    val modal = StillModalImpl(modalHost.first, modalHost.second, mask, systemCancellable, maskTouchBehavior, content)
    val hostView = modalHost.first
    handleModelUnique(hostView, modal, uniqueId)
    return modal
}

private fun handleModelUnique(hostView: FrameLayout, modal: EmoModal, uniqueId: Long) {
    val showingModals = (hostView.getTag(R.id.emo_modals) as? ShowingModals) ?: ShowingModals().also {
        hostView.setTag(R.id.emo_modals, it)
    }

    modal.doOnShow {
        showingModals.modals.put(uniqueId, it)?.dismiss()
    }

    modal.doOnDismiss {
        if (showingModals.modals[uniqueId] == it) {
            showingModals.modals.remove(uniqueId)
        }
    }
}
