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

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun zip(sourceDirPath: String, outputPath: String) {
    val sourceDir = File(sourceDirPath)
    if (!sourceDir.isDirectory) {
        throw RuntimeException("$sourceDirPath is not dir")
    }
    ZipOutputStream(FileOutputStream(outputPath).buffered()).use {
        it.zipFiles(sourceDir, "")
        it.closeEntry()
        it.flush()
    }
}

private fun ZipOutputStream.zipFiles(sourceDir: File, parentDirPath: String) {
    val files: Array<File> = sourceDir.listFiles() ?: return
    for (f in files) {
        if (f.isDirectory) {
            val entryName: String = parentDirPath + f.name + File.separator
            val entry = ZipEntry(entryName)
            entry.time = f.lastModified()
            entry.size = f.length()
            putNextEntry(entry)

            zipFiles(f, entryName)
            break
        }

        val entry = ZipEntry(parentDirPath + f.name)
        entry.time = f.lastModified()
        entry.size = f.length()
        putNextEntry(entry)
        f.forEachBlock { buffer, bytesRead ->
            write(buffer, 0, bytesRead)
        }
    }
}
