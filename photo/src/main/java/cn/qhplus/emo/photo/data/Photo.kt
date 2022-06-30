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

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

interface Photo {
    @Composable
    fun Compose(
        contentScale: ContentScale,
        isContainerDimenExactly: Boolean,
        onSuccess: ((PhotoResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )
}

class PhotoResult(val model: Any, val drawable: Drawable)

interface PhotoProvider {
    fun thumbnail(openBlankColor: Boolean): Photo?
    fun photo(): Photo?
    fun ratio(): Float = -1f
    fun isLongImage(): Boolean = false

    fun meta(): Bundle?
    fun recoverCls(): Class<out PhotoShotRecover>?
}

class PhotoShot(
    val photoProvider: PhotoProvider,
    var offsetInWindow: Offset?,
    var size: IntSize?,
    var photo: Drawable?,
) {
    fun photoRect(): Rect? {
        val offset = offsetInWindow
        val size = size?.toSize()
        if (offset == null || size == null || size.width == 0f || size.height == 0f) {
            return null
        }
        return Rect(offset, size)
    }

    fun ratio(): Float {
        var ratio = photoProvider.ratio()
        if (ratio <= 0f) {
            photo?.let {
                if (it.intrinsicWidth > 0 && it.intrinsicHeight > 0) {
                    ratio = it.intrinsicWidth.toFloat() / it.intrinsicHeight
                }
            }
        }
        return ratio
    }
}

interface PhotoShotRecover {
    fun recover(bundle: Bundle): PhotoShot?
}

val lossPhotoProvider = object : PhotoProvider {
    override fun thumbnail(openBlankColor: Boolean): Photo? {
        return null
    }

    override fun photo(): Photo? {
        return null
    }

    override fun meta(): Bundle? {
        return null
    }

    override fun recoverCls(): Class<out PhotoShotRecover>? {
        return null
    }
}

val lossPhotoShot = PhotoShot(lossPhotoProvider, null, null, null)

internal enum class PhotoLoadStatus {
    loading, success, failed
}
