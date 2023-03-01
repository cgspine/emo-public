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

package cn.qhplus.emo.ui.page

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import cn.qhplus.emo.EmoScheme
import cn.qhplus.emo.MainActivity
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.modal.emoToast
import cn.qhplus.emo.rememberEmoMultiplePermissionsState
import cn.qhplus.emo.rememberEmoPermissionState
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.ui.CommonItem
import cn.qhplus.emo.ui.core.TopBar
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.collections.immutable.persistentListOf

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_PERMISSION,
    alternativeHosts = [MainActivity::class]
)
@Composable
fun PermissionPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val topBarIconColor = MaterialTheme.colorScheme.onPrimary
        TopBar(
            title = { "Permission" },
            leftItems = remember(topBarIconColor) {
                persistentListOf(
                    TopBarBackIconItem(tint = topBarIconColor) {
                        EmoScheme.pop()
                    }
                )
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
        ) {
            PermissionDemo()
            Spacer(modifier = Modifier.height(10.dp))
            MultiplePermissionDemo()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionDemo() {
    val permissionState = rememberEmoPermissionState(
        permission = Manifest.permission.CAMERA,
        tipContent = "为了录入羞羞的事情申请个相机权限"
    )
    val view = LocalView.current
    val status = permissionState.status
    CommonItem("申请单个权限") {
        when (status) {
            PermissionStatus.Granted -> {
                view.emoToast("权限已经获取啦")
            }
            is PermissionStatus.Denied -> {
                permissionState.launchPermissionRequest()
            }
        }
    }
    val text = when (status) {
        PermissionStatus.Granted -> {
            "已获取权限"
        }
        is PermissionStatus.Denied -> {
            if (status.shouldShowRationale) {
                "你不应该拒绝这个权限"
            } else {
                "点击获取权限"
            }
        }
    }
    Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        text = text,
        color = Color.DarkGray
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MultiplePermissionDemo() {
    val permissionState = rememberEmoMultiplePermissionsState(
        permissions = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
        tipContent = "一次性录入多个权限"
    )
    val view = LocalView.current
    CommonItem("申请多个权限， ${permissionState.allPermissionsGranted}") {
        if (permissionState.allPermissionsGranted) {
            view.emoToast("权限已经获取啦")
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }
    val text = if (permissionState.allPermissionsGranted) {
        "已获取权限"
    } else if (permissionState.shouldShowRationale) {
        "你不应该拒绝这些权限：${permissionState.revokedPermissions.joinToString(";"){it.permission}}"
    } else {
        "点击获取权限}"
    }
    Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        text = text,
        color = Color.DarkGray
    )
}
