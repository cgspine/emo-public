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

package cn.qhplus.emo.core

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build

fun Context.currentProcessName(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val processName = Application.getProcessName()
        if (processName != null && processName.isNotBlank()) {
            return processName
        }
    }

    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val infoList = activityManager.runningAppProcesses
    return infoList?.find { it.pid == android.os.Process.myPid() }?.processName ?: packageName
}

fun Context.currentSimpleProcessName(): String {
    val name = currentProcessName()
    return name.split(":").getOrElse(1) { "main" }
}
