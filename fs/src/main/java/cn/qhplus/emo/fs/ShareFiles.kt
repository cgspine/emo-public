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
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

object ShareFiles {

    fun getAppShareDir(context: Context): File {
        val file = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(context.getExternalFilesDir(""), "emo_public")
        } else {
            File(context.filesDir, "emo_public")
        }
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    fun getShareFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context.applicationContext,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
