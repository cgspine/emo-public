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

package cn.qhplus.emo.kv

import android.content.Context
import androidx.annotation.Keep
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.experimental.and
import kotlin.experimental.or

private const val FLAG_COMPRESSED: Byte = 0x1
private const val FLAG_CRC: Byte = 0x2

@Keep
class EmoKV(
    context: Context,
    name: String,
    private val crc: Boolean = true,
    private val compress: Boolean = true,
    private val compressMiniLen: Int = 500,
    indexInitSpace: Long = 16384, // 16k, for about 600 item when hash factor = 0.75.
    keyInitSpace: Long = 4096, // 4k
    valueInitSpace: Long = 1024 * 1024, // 1m
    hashFactor: Float = 0.75f,
    valueUpdateCountToAutoCompact: Int = 5000,
    private val validateFailedReporter: ((key: ByteArray, e: Throwable) -> Boolean)? = null
) {
    companion object {
        @Volatile
        private var isLibLoaded = false

        @Synchronized
        private fun checkLoadLibrary() {
            if (!isLibLoaded) {
                System.loadLibrary("c++_shared")
                System.loadLibrary("EmoKV")
                isLibLoaded = true
            }
        }
    }

    private var nativePtr: Long

    init {
        checkLoadLibrary()
        val emoDir = File(context.filesDir, "emo")
        emoDir.mkdir()
        val root = File(emoDir, "kv")
        root.mkdir()
        val dir = File(root, name)
        dir.mkdir()
        nativePtr = nInit(
            dir.path,
            indexInitSpace,
            keyInitSpace,
            valueInitSpace,
            hashFactor,
            valueUpdateCountToAutoCompact
        )
        if (nativePtr == 0L) {
            throw RuntimeException("native init failed.")
        }
    }

    @Synchronized
    fun close() {
        if (nativePtr != 0L) {
            nClose(nativePtr)
            nativePtr = 0L
        }
    }

    fun getBool(key: String, default: Boolean = false): Boolean {
        return getInt(key, if (default) 1 else 0) == 1
    }

    fun getChar(key: String, default: Char = '0'): Char {
        return get(key.toByteArray())?.let {
            validResultLength(key, it, Char.SIZE_BYTES)
            ByteBuffer.wrap(it).char
        } ?: default
    }

    fun getShort(key: String, default: Short = 0): Short {
        return get(key.toByteArray())?.let {
            validResultLength(key, it, Short.SIZE_BYTES)
            ByteBuffer.wrap(it).short
        } ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return get(key.toByteArray())?.let {
            validResultLength(key, it, Int.SIZE_BYTES)
            ByteBuffer.wrap(it).int
        } ?: default
    }

    fun getLong(key: String, default: Long = 0): Long {
        return get(key.toByteArray())?.let {
            validResultLength(key, it, Long.SIZE_BYTES)
            ByteBuffer.wrap(it).long
        } ?: default
    }

    fun getFloat(key: String, default: Float = 0f): Float {
        return get(key.toByteArray())?.let {
            validResultLength(key, it, Float.SIZE_BYTES)
            ByteBuffer.wrap(it).float
        } ?: default
    }

    fun getDouble(key: String, default: Double = 0.0): Double {
        return get(key.toByteArray())?.let {
            validResultLength(key, it, Double.SIZE_BYTES)
            ByteBuffer.wrap(it).double
        } ?: default
    }

    fun getString(key: String): String? {
        return get(key.toByteArray())?.let { String(it) }
    }

    fun get(key: ByteArray): ByteArray? {
        validNotClosed()
        val ret = nGet(nativePtr, key) ?: return null
        if (!compress && !crc) {
            return ret
        }
        if (ret.isEmpty()) {
            return ret
        }
        try {
            val flag = ret[ret.size - 1]
            val isCrc = (flag and FLAG_CRC) == FLAG_CRC
            val isCompressed = (flag and FLAG_COMPRESSED) == FLAG_COMPRESSED
            val buffer = ByteArray(512)
            val os = ByteArrayOutputStream(512)
            if (isCompressed) {
                val decompresser = Inflater()
                decompresser.setInput(ret, 0, ret.size - 1 - if (isCrc) Long.SIZE_BYTES else 0)
                while (!decompresser.finished()) {
                    val len = decompresser.inflate(buffer)
                    os.write(buffer, 0, len)
                }
                decompresser.end()
            } else {
                os.write(ret, 0, ret.size - 1 - if (isCrc) Long.SIZE_BYTES else 0)
            }
            val data = os.toByteArray()
            if (isCrc) {
                val crc32 = CRC32()
                crc32.update(data)
                if (crc32.value != ByteBuffer.wrap(ret, ret.size - 1 - Long.SIZE_BYTES, Long.SIZE_BYTES).long) {
                    throw RuntimeException("validate crc failed for key(${String(key)})")
                }
            }
            return data
        } catch (e: Throwable) {
            if (validateFailedReporter?.invoke(key, e) == false) {
                throw e
            }
            return null
        }
    }

    fun put(key: String, value: Boolean): Boolean {
        return put(
            key.toByteArray(),
            ByteBuffer.allocate(Int.SIZE_BYTES).putInt(if (value) 1 else 0).array()
        )
    }

    fun put(key: String, value: Char): Boolean {
        return put(
            key.toByteArray(),
            ByteBuffer.allocate(Short.SIZE_BYTES).putChar(value).array()
        )
    }

    fun put(key: String, value: Short): Boolean {
        return put(
            key.toByteArray(),
            ByteBuffer.allocate(Short.SIZE_BYTES).putShort(value).array()
        )
    }

    fun put(key: String, value: Int): Boolean {
        return put(
            key.toByteArray(),
            ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array()
        )
    }

    fun put(key: String, value: Long): Boolean {
        return put(
            key.toByteArray(),
            ByteBuffer.allocate(Long.SIZE_BYTES).putLong(value).array()
        )
    }

    fun put(key: String, value: Float): Boolean {
        return put(
            key.toByteArray(),
            ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(value).array()
        )
    }

    fun put(key: String, value: Double): Boolean {
        return put(
            key.toByteArray(),
            ByteBuffer.allocate(Double.SIZE_BYTES).putDouble(value).array()
        )
    }

    fun put(key: String, value: String): Boolean {
        return put(key.toByteArray(), value.toByteArray())
    }

    fun put(key: ByteArray, value: ByteArray): Boolean {
        validNotClosed()
        if (key.size > 256) {
            throw RuntimeException("key's len can not be more than 256")
        }
        if (value.size > 65536) {
            throw RuntimeException("value's len can not be more than 65536")
        }

        if (!compress && !crc) {
            return nPut(nativePtr, key, value)
        }
        val buffer = ByteArray(value.size.coerceAtMost(512))
        val os = ByteArrayOutputStream(value.size.coerceAtMost(512))
        try {
            var flag: Byte = 0
            if (compress && value.size > compressMiniLen) {
                val compresser = Deflater()
                compresser.setInput(value)
                compresser.finish()
                while (!compresser.finished()) {
                    val len = compresser.deflate(buffer)
                    os.write(buffer, 0, len)
                }
                compresser.end()
                if (os.size() > value.size) {
                    // it's not worth to compress....
                    os.reset()
                    os.write(value, 0, value.size)
                } else {
                    flag = FLAG_COMPRESSED
                }
            } else {
                os.write(value, 0, value.size)
            }
            if (crc) {
                val crc32 = CRC32()
                crc32.update(value)
                os.write(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(crc32.value).array())
                flag = flag or FLAG_CRC
            }

            os.write(byteArrayOf(flag))
        } catch (e: Throwable) {
            os.reset()
            os.write(value, 0, value.size)
            os.write(byteArrayOf(0))
        }
        val ret = os.toByteArray()
        return nPut(nativePtr, key, ret)
    }

    fun delete(key: String) {
        delete(key.toByteArray())
    }

    fun delete(key: ByteArray) {
        validNotClosed()
        nDelete(nativePtr, key)
    }

    fun compact() {
        validNotClosed()
        nCompact(nativePtr)
    }

    private fun validNotClosed() {
        if (nativePtr == 0L) {
            throw RuntimeException("EmoKv is Closed!!!")
        }
    }

    private external fun nCompact(nativePtr: Long)

    private external fun nInit(
        dir: String,
        indexInitSpace: Long,
        keyInitSpace: Long,
        valueInitSpace: Long,
        hashFactor: Float,
        valueUpdateCountToAutoCompact: Int = 5000
    ): Long

    private external fun nPut(nativePtr: Long, key: ByteArray, value: ByteArray): Boolean
    private external fun nGet(nativePtr: Long, key: ByteArray): ByteArray?
    private external fun nDelete(nativePtr: Long, key: ByteArray)
    private external fun nClose(nativePtr: Long)

    protected fun finalize() {
        close()
    }

    private fun validResultLength(key: String, value: ByteArray, expectLen: Int) {
        if (value.size != expectLen) {
            throw RuntimeException("the value length not matched for $key: expected: $expectLen, actual: ${value.size}")
        }
    }
}
