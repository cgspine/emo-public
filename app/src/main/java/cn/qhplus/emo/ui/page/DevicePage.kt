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

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import cn.qhplus.emo.MainActivity
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.device.getBatteryCapacity
import cn.qhplus.emo.device.getDataStorageSize
import cn.qhplus.emo.device.getExtraStorageSize
import cn.qhplus.emo.device.getTotalMemory
import cn.qhplus.emo.device.isHonor
import cn.qhplus.emo.device.isHuawei
import cn.qhplus.emo.device.isMeizu
import cn.qhplus.emo.device.isOppo
import cn.qhplus.emo.device.isVivo
import cn.qhplus.emo.device.isXiaomi
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.ui.CommonItem

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_DEVICE,
    alternativeHosts = [MainActivity::class]
)
@Composable
fun DevicePage() {
    val view = LocalView.current
    OnlyBackListPage(
        title = "Device"
    ) {
        item {
            CommonItem("是小米吗？ ${isXiaomi()}")
        }

        item {
            CommonItem("是华为吗？ ${isHuawei()}")
        }

        item {
            CommonItem("是荣耀吗？ ${isHonor()}")
        }

        item {
            CommonItem("是魅族吗？ ${isMeizu()}")
        }

        item {
            CommonItem("是Oppo吗？ ${isOppo()}")
        }

        item {
            CommonItem("是Vivo吗？ ${isVivo()}")
        }

        item {
            CommonItem("是Vivo吗？ ${isVivo()}")
        }

        item {
            CommonItem("总内存: ${getTotalMemory(LocalContext.current.applicationContext)}")
        }

        item {
            CommonItem("Data 存储容量: ${getDataStorageSize()}")
        }

        item {
            CommonItem("外部存储容量: ${getExtraStorageSize()}")
        }

        item {
            CommonItem("电池总存储: ${getBatteryCapacity(LocalContext.current.applicationContext)}")
        }
    }
}
