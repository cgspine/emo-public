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

package cn.qhplus.emo.network

import android.os.SystemClock

enum class NetworkType {
    None,
    Cellular,
    Wifi,
    Fake,
    Unknown
}

data class NetworkState(
    val networkType: NetworkType,
    val isValid: Boolean,
    val uuid: String,
    val updateTime: Long
) {
    companion object {

        fun none() = NetworkState(
            NetworkType.None,
            false,
            "",
            SystemClock.elapsedRealtime()
        )
    }

    val isConnected: Boolean = networkType != NetworkType.None

    override fun toString(): String {
        return when (networkType) {
            NetworkType.Wifi -> "wifi"
            NetworkType.Cellular -> "cellular"
            NetworkType.Unknown -> "unknown"
            NetworkType.Fake -> "fake"
            else -> "none"
        }
    }
}
