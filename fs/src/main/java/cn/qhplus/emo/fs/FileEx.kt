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

package cn.qhplus.emo.fs

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

fun File.md5(): ByteArray {
    return inputStream().digest("MD5")
}

fun File.sha256(): ByteArray {
    return inputStream().digest("SHA-256")
}

fun ByteArray.toHexString(): String {
    return joinToString("") { it.toString(radix = 16).padStart(2, '0') }
}

fun InputStream.digest(algorithm: String): ByteArray {
    val digest = MessageDigest.getInstance(algorithm)
    return use {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = it.read(buffer)
        while (bytes >= 0) {
            digest.update(buffer, 0, bytes)
            bytes = it.read(buffer)
        }
        digest.digest()
    }
}


fun Uri.fileName(context: Context): String? {
    var result: String? = null
    if (scheme == "content") {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if(index >= 0){
                    result = cursor.getString(index)
                }

            }
        }
    }
    return result ?: path?.let {
        val cut = it.lastIndexOf('/')
        if (cut != -1) {
            it.substring(cut + 1);
        } else {
            it
        }
    }
}