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

import android.view.View
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.Color
import cn.qhplus.emo.modal.EmoModal
import cn.qhplus.emo.modal.emoModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class EmoPermissionModal(
    private val view: View,
    private val scope: CoroutineScope,
    private val tip: EmoPermissionTip
) {

    private var modal: EmoModal? = null
    private var launchJob: Job? = null

    fun launch() {
        dismiss()
        launchJob = scope.launch(Dispatchers.Main) {
            if (view.isAttachedToWindow) {
                modal = view.emoModal(
                    mask = Color.Transparent,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    tip.run {
                        Content()
                    }
                }.show()
            }
        }
    }

    fun dismiss() {
        launchJob?.cancel()
        launchJob = null
        modal?.dismiss()
        modal = null
    }
}
