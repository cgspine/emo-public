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

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import cn.qhplus.emo.core.EmoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

const val MediaPhotoBucketAllId = "___all___"
const val MediaPhotoBucketAllName = "最近项目"
const val MediaPhotoBucketSelectedId = "___selected___"

open class MediaModel(
    val id: Long,
    val uri: Uri,
    var width: Int,
    var height: Int,
    val rotation: Int,
    val name: String,
    val modifyTimeSec: Long,
    val bucketId: String,
    val bucketName: String,
    val editable: Boolean
) {
    fun ratio(): Float {
        if (height <= 0 || width <= 0) {
            return -1f
        }
        if (rotation == 90 || rotation == 270) {
            return height.toFloat() / width
        }
        return width.toFloat() / height
    }
}

class MediaPhotoBucket(
    val id: String,
    val name: String,
    val list: List<MediaModel>
)

class MediaPhotoBucketVO(
    val id: String,
    val name: String,
    val list: List<MediaPhotoVO>
)

class MediaPhotoVO(
    val model: MediaModel,
    val photoProvider: PhotoProvider
)

interface MediaPhotoProviderFactory {
    fun factory(model: MediaModel): PhotoProvider
}

interface MediaDataProvider {
    suspend fun provide(
        context: Context,
        supportedMimeTypes: Array<String>
    ): List<MediaPhotoBucket>
}

class EmoDefaultImagesProvider : MediaDataProvider {

    companion object {

        private const val TAG = "EmoDefaultImagesProvider"

        val DEFAULT_SUPPORT_MIMETYPES = arrayOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/heic",
            "image/heif"
        )

        private val COLUMNS = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
    }

    override suspend fun provide(
        context: Context,
        supportedMimeTypes: Array<String>
    ): List<MediaPhotoBucket> {
        return withContext(Dispatchers.IO) {
            val selection = if (supportedMimeTypes.isEmpty()) {
                null
            } else {
                val sb = StringBuilder()
                sb.append(MediaStore.Images.Media.MIME_TYPE)
                sb.append(" IN (")
                supportedMimeTypes.forEachIndexed { index, s ->
                    if (index != 0) {
                        sb.append(",")
                    }
                    sb.append("'")
                    sb.append(s)
                    sb.append("'")
                }
                sb.append(")")
                sb.toString()
            }
            val list = mutableListOf<MediaModel>()
            context.applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                COLUMNS,
                selection,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            val path = cursor.readString(MediaStore.Images.Media.DATA)
                            val id = cursor.readLong(MediaStore.Images.Media._ID)
                            val w = cursor.readInt(MediaStore.Images.Media.WIDTH)
                            val h = cursor.readInt(MediaStore.Images.Media.HEIGHT)
                            val o = cursor.readInt(MediaStore.Images.Media.ORIENTATION)
                            val isRotated = o == 90 || o == 270
                            list.add(
                                MediaModel(
                                    id,
                                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                                    if (isRotated) h else w,
                                    if (isRotated) w else h,
                                    cursor.readInt(MediaStore.Images.Media.ORIENTATION),
                                    cursor.readString(MediaStore.Images.Media.DISPLAY_NAME),
                                    cursor.readLong(MediaStore.Images.Media.DATE_MODIFIED),
                                    cursor.readString(MediaStore.Images.Media.BUCKET_ID),
                                    (cursor.readString(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)).let {
                                        it.ifEmpty { File(path).parent ?: "" }
                                    },
                                    true
                                )
                            )
                        } catch (e: Exception) {
                            EmoLog.e(TAG, "read image data from cursor failed.", e)
                        }
                    } while (cursor.moveToNext())
                }
            }
            val buckets = mutableListOf<MutableMediaPhotoBucket>()
            val defaultPhotoBucket = MutableMediaPhotoBucket(MediaPhotoBucketAllId, MediaPhotoBucketAllName)
            buckets.add(defaultPhotoBucket)
            list.forEach { model ->
                defaultPhotoBucket.list.add(model)
                if (model.name.isNotBlank()) {
                    val bucket = buckets.find {
                        it.id == model.bucketId
                    } ?: MutableMediaPhotoBucket(model.bucketId, model.bucketName).also {
                        buckets.add(it)
                    }
                    bucket.list.add(model)
                }
            }

            buckets.map {
                MediaPhotoBucket(it.id, it.name, it.list)
            }
        }
    }

    private class MutableMediaPhotoBucket(
        val id: String,
        val name: String
    ) {
        val list: MutableList<MediaModel> = mutableListOf()
    }
}

private fun <T> Cursor.getColumnIndexAndDoAction(columnName: String, block: (Int) -> T): T? {
    return try {
        getColumnIndexOrThrow(columnName).let {
            if (it < 0) null else block(it)
        }
    } catch (e: Throwable) {
        EmoLog.e("MediaDataProvider", "getColumnIndex for $columnName failed.", e)
        null
    }
}

fun Cursor.readLong(columnName: String): Long = getColumnIndexAndDoAction(columnName) {
    getLongOrNull(it)
} ?: 0
fun Cursor.readString(columnName: String): String = getColumnIndexAndDoAction(columnName) {
    getStringOrNull(it)
} ?: ""
fun Cursor.readInt(columnName: String): Int = getColumnIndexAndDoAction(columnName) {
    getIntOrNull(it)
} ?: 0

class ImageItem(
    val url: String,
    val thumbnailUrl: String?,
    val thumbnail: Bitmap?
)
