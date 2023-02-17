package cn.qhplus.emo.fs

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

fun InputStream.digest(algorithm: String): ByteArray{
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