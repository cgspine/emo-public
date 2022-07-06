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

package cn.qhplus.emo.photo.vm

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cn.qhplus.emo.photo.data.PhotoShot
import cn.qhplus.emo.photo.data.PhotoShotDelivery
import cn.qhplus.emo.photo.data.PhotoShotRecover
import cn.qhplus.emo.photo.data.PhotoViewerData
import cn.qhplus.emo.photo.data.lossPhotoShot

class PhotoViewerViewModel(state: SavedStateHandle) : ViewModel() {

    companion object {
        internal const val PHOTO_CURRENT_INDEX = "emo_photo_current_index"
        internal const val PHOTO_SHOT_DELIVERY_KEY = "emo_photo_shot_delivery"
        internal const val PHOTO_COUNT = "emo_photo_count"
        internal const val PHOTO_META_KEY_PREFIX = "emo_photo_meta_"
        internal const val PHOTO_RECOVER_CLASS_KEY_PREFIX = "emo_photo_recover_cls_"
    }

    private val enterIndex = state.get<Int>(PHOTO_CURRENT_INDEX) ?: 0
    val data: PhotoViewerData?

    private val transitionDeliverKey = state.get<Long>(PHOTO_SHOT_DELIVERY_KEY) ?: -1

    init {
        val transitionDeliverData = PhotoShotDelivery.getAndRemove(transitionDeliverKey)
        data = if (transitionDeliverData != null) {
            transitionDeliverData
        } else {
            val count = state.get<Int>(PHOTO_COUNT) ?: 0
            if (count > 0) {
                val list = arrayListOf<PhotoShot>()
                for (i in 0 until count) {
                    try {
                        val meta = state.get<Bundle>("$PHOTO_META_KEY_PREFIX$i")
                        val clsName =
                            state.get<String>("$PHOTO_RECOVER_CLASS_KEY_PREFIX$i")
                        if (meta == null || clsName.isNullOrBlank()) {
                            list.add(lossPhotoShot)
                        } else {
                            val cls = Class.forName(clsName)
                            val recover = cls.newInstance() as PhotoShotRecover
                            list.add(recover.recover(meta) ?: lossPhotoShot)
                        }
                    } catch (e: Throwable) {
                        list.add(lossPhotoShot)
                    }
                }
                PhotoViewerData(list, enterIndex)
            } else {
                null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        PhotoShotDelivery.remove(transitionDeliverKey)
    }
}
