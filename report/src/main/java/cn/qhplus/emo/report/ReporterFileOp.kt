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

package cn.qhplus.emo.report

import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.closeQuietly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger

private val MAGIC_END = "emo".toByteArray()

//region sink
interface ReporterFileSink<T> {
    @Throws(IOException::class)
    suspend fun write(msg: T, converter: ReportMsgConverter<T>): Boolean
    fun flush()
    fun close()
}

class ReporterMappedByteBufferSink<T>(
    private val mappedByteBuffer: MappedByteBuffer,
    private val accessFile: RandomAccessFile
) : ReporterFileSink<T> {

    override suspend fun write(msg: T, converter: ReportMsgConverter<T>): Boolean {
        val content = converter.encode(msg)
        if (content.isEmpty()) {
            return true
        }
        val totalLength = Integer.BYTES + content.size + MAGIC_END.size
        if (totalLength > mappedByteBuffer.remaining()) {
            return false
        }
        mappedByteBuffer.putInt(content.size)
        mappedByteBuffer.put(content)
        mappedByteBuffer.put(MAGIC_END)
        return true
    }

    override fun flush() {
        mappedByteBuffer.force()
    }

    override fun close() {
        accessFile.closeQuietly()
    }
}

class ReporterStreamSink<T>(
    private val file: File,
    private val fileMaxSize: Long
) : ReporterFileSink<T> {

    private val os = file.outputStream().buffered()

    private val bufferedCount = AtomicInteger(0)

    @Throws(IOException::class)
    override suspend fun write(msg: T, converter: ReportMsgConverter<T>): Boolean = withContext(Dispatchers.IO) {
        val content = converter.encode(msg)
        if (content.isEmpty()) {
            return@withContext true
        }
        val totalLength = Integer.BYTES + content.size + MAGIC_END.size
        if (totalLength + file.length() + bufferedCount.get() > fileMaxSize) {
            return@withContext false
        }
        val buffer = ByteBuffer.allocate(Integer.BYTES)
        buffer.putInt(content.size)
        os.write(buffer.array())
        os.write(content)
        os.write(MAGIC_END)
        os.flush()
        return@withContext true
    }

    override fun flush() {
    }

    override fun close() {
        os.closeQuietly()
    }
}

fun <T> File.createReportSink(bufferLength: Long = 150 * 1024): ReporterFileSink<T> {
    try {
        if (!exists()) {
            createNewFile()
        }
        val randomAccessFile = RandomAccessFile(this, "rw")
        try {
            val mappedByteBuffer = randomAccessFile.channel.map(
                FileChannel.MapMode.READ_WRITE,
                length(),
                bufferLength
            )
            return ReporterMappedByteBufferSink(mappedByteBuffer, randomAccessFile)
        } catch (e: Throwable) {
            randomAccessFile.close()
            throw e
        }
    } catch (e: Throwable) {
        EmoLog.e("createReportSink", "createReportSink failed", e)
        return ReporterStreamSink(this, bufferLength)
    }
}

//endregion

//region source
interface ReporterFileSource<T> {
    suspend fun read(client: ReportClient<T>, transporter: StreamReportTransporter<T>, converter: ReportMsgConverter<T>)
    fun close()
}

class ReporterMappedByteBufferSource<T>(
    private val mappedByteBuffer: MappedByteBuffer,
    private val randomAccessFile: RandomAccessFile
) : ReporterFileSource<T> {

    override suspend fun read(
        client: ReportClient<T>,
        transporter: StreamReportTransporter<T>,
        converter: ReportMsgConverter<T>
    ) {
        val miniLength = Integer.BYTES + MAGIC_END.size
        val end = ByteArray(MAGIC_END.size)
        while (mappedByteBuffer.remaining() >= miniLength) {
            val size = mappedByteBuffer.int
            if (size <= 0) {
                return
            }
            if (mappedByteBuffer.remaining() < size + MAGIC_END.size) {
                return
            }
            val buffer = ByteArray(size)
            mappedByteBuffer.get(buffer, 0, size)
            mappedByteBuffer.get(end)
            if (!end.contentEquals(MAGIC_END)) {
                return
            }
            transporter.transport(client, buffer, converter, ReportStrategy.FileBatch)
        }
    }

    override fun close() {
        randomAccessFile.closeQuietly()
    }
}

class ReporterStreamSource<T>(
    private val ins: InputStream
) : ReporterFileSource<T> {

    override suspend fun read(
        client: ReportClient<T>,
        transporter: StreamReportTransporter<T>,
        converter: ReportMsgConverter<T>
    ) = withContext(Dispatchers.IO) {
        val sizeBuffer = ByteBuffer.allocate(Integer.BYTES)
        val end = ByteArray(MAGIC_END.size)
        while (true) {
            var readCount = ins.read(sizeBuffer.array())
            if (readCount < Integer.BYTES) {
                return@withContext
            }
            sizeBuffer.rewind()
            val size = sizeBuffer.int
            if (size <= 0) {
                return@withContext
            }
            val contentBuffer = ByteArray(size)
            readCount = ins.read(contentBuffer, 0, size)
            if (readCount < size) {
                return@withContext
            }
            ins.read(end)
            if (!end.contentEquals(MAGIC_END)) {
                return@withContext
            }
            transporter.transport(client, contentBuffer, converter, ReportStrategy.FileBatch)
        }
    }

    override fun close() {
        ins.closeQuietly()
    }
}

fun <T> File.createReportSource(): ReporterFileSource<T> {
    try {
        val randomAccessFile = RandomAccessFile(this, "r")
        try {
            val mappedByteBuffer = randomAccessFile.channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                length()
            )
            return ReporterMappedByteBufferSource(mappedByteBuffer, randomAccessFile)
        } catch (e: Throwable) {
            randomAccessFile.close()
            throw e
        }
    } catch (e: Throwable) {
        EmoLog.e("createReportSource", "createReportSource failed", e)
        return ReporterStreamSource(inputStream().buffered())
    }
}
//endregion
