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