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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalPermissionsApi::class)
private class EmoPermissionState(
    view: View,
    scope: CoroutineScope,
    tip: EmoPermissionTip,
    val permissionState: PermissionState
) : PermissionState {

    private val modal = EmoPermissionModal(view, scope, tip)

    override val permission: String
        get() = permissionState.permission
    override val status: PermissionStatus
        get() = permissionState.status

    override fun launchPermissionRequest() {
        permissionState.launchPermissionRequest()
        modal.launch()
    }

    fun dismiss() {
        modal.dismiss()
    }
}

private class PermissionCallbackActionHolder(
    var action: ((Boolean) -> Unit)? = null
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberEmoPermissionState(
    permission: String,
    tipContent: String,
    onPermissionResult: (Boolean) -> Unit = {}
): PermissionState {
    val tip = remember(tipContent) {
        SimpleEmoPermissionTip(tipContent)
    }
    return rememberEmoPermissionState(permission, tip, onPermissionResult)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberEmoPermissionState(
    permission: String,
    tip: EmoPermissionTip,
    onPermissionResult: (Boolean) -> Unit = {}
): PermissionState {
    val callbackHolder = remember {
        PermissionCallbackActionHolder()
    }
    val permissionState = rememberPermissionState(permission) {
        callbackHolder.action?.invoke(it)
        onPermissionResult(it)
    }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val emoPermissionState = remember(permissionState, tip) {
        EmoPermissionState(view, scope, tip, permissionState)
    }
    callbackHolder.action = {
        emoPermissionState.dismiss()
    }
    DisposableEffect(emoPermissionState) {
        object : DisposableEffectResult {
            override fun dispose() {
                emoPermissionState.dismiss()
            }
        }
    }

    return emoPermissionState
}
