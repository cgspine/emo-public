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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalPermissionsApi::class)
private class EmoMultiplePermissionState(
    view: View,
    scope: CoroutineScope,
    tip: EmoPermissionTip,
    val permissionState: MultiplePermissionsState
) : MultiplePermissionsState {

    private val modal = EmoPermissionModal(view, scope, tip)

    override val allPermissionsGranted: Boolean
        get() = permissionState.allPermissionsGranted
    override val permissions: List<PermissionState>
        get() = permissionState.permissions
    override val revokedPermissions: List<PermissionState>
        get() = permissionState.revokedPermissions
    override val shouldShowRationale: Boolean
        get() = permissionState.shouldShowRationale

    override fun launchMultiplePermissionRequest() {
        permissionState.launchMultiplePermissionRequest()
        modal.launch()
    }

    fun dismiss() {
        modal.dismiss()
    }
}

private class MultiplePermissionCallbackActionHolder(
    var action: ((Map<String, Boolean>) -> Unit)? = null
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberEmoMultiplePermissionsState(
    permissions: List<String>,
    tipContent: String,
    onPermissionsResult: (Map<String, Boolean>) -> Unit = {}
): MultiplePermissionsState {
    val tip = remember(tipContent) {
        SimpleEmoPermissionTip(tipContent)
    }
    return rememberEmoMultiplePermissionsState(permissions, tip, onPermissionsResult)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberEmoMultiplePermissionsState(
    permissions: List<String>,
    tip: EmoPermissionTip,
    onPermissionsResult: (Map<String, Boolean>) -> Unit = {}
): MultiplePermissionsState {
    val callbackActionHolder = remember {
        MultiplePermissionCallbackActionHolder()
    }
    val permissionState = rememberMultiplePermissionsState(permissions) {
        callbackActionHolder.action?.invoke(it)
        onPermissionsResult(it)
    }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val emoPermissionState = remember(permissionState, tip) {
        EmoMultiplePermissionState(view, scope, tip, permissionState)
    }
    callbackActionHolder.action = {
        emoPermissionState.dismiss()
    }
    DisposableEffect(emoPermissionState) {
        object : DisposableEffectResult {
            override fun dispose() {
                emoPermissionState.dismiss()
            }
        }
    }

    SideEffect {
        emoPermissionState.dismiss()
    }
    return emoPermissionState
}
