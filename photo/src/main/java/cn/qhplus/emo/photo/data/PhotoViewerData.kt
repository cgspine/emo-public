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

package cn.qhplus.emo.photo.data

import androidx.annotation.MainThread

class PhotoViewerData(
    val list: List<PhotoShot>,
    val index: Int
)

internal object PhotoShotDelivery {
    private val dataMap = mutableMapOf<Long, PhotoViewerData>()

    @MainThread
    fun put(data: PhotoViewerData): Long {
        val id = System.currentTimeMillis()
        dataMap[id] = data
        // memory leak protection
        val iterator = dataMap.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.key < id - 20 * 1000) {
                iterator.remove()
            }
        }
        return id
    }

    @MainThread
    fun peek(id: Long): PhotoViewerData? {
        return dataMap[id]
    }

    @MainThread
    fun getAndRemove(id: Long): PhotoViewerData? {
        return dataMap.remove(id)
    }

    @MainThread
    fun remove(id: Long) {
        dataMap.remove(id)
    }
}
