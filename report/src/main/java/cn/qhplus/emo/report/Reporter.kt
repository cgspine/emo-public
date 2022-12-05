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

import android.content.Context
import android.os.Process
import android.os.SystemClock
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.LogTag
import cn.qhplus.emo.core.currentSimpleProcessName
import cn.qhplus.emo.network.NetworkConnectivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

interface Reporter<T> {
    fun report(msg: T) {
        batchReport(listOf(msg))
    }
    fun batchReport(list: List<T>)
}

interface BatchReporter<T> : Reporter<T> {
    fun flush()
}

enum class ReportStrategy {
    Immediately,
    MemBach,
    FileBatch,
    WriteBackBecauseOfFailed
}

class ImmediatelyReporter<T>(
    private val client: ReportClient<T>,
    private val transporter: ListReportTransporter<T>
) : Reporter<T> {

    override fun batchReport(list: List<T>) {
        client.scope.launch {
            transporter.transport(client, list, ReportStrategy.Immediately)
        }
    }
}

abstract class IntervalBatchReporter<T>(
    private val client: ReportClient<T>,
    private val batchInterval: Long
) : BatchReporter<T>, Closeable {

    private val channel = Channel<Unit>(1, BufferOverflow.DROP_LATEST)

    init {
        client.scope.launch {
            var intervalJob: Job? = createIntervalJob()
            for (i in channel) {
                intervalJob?.cancel()
                doFlush()
                intervalJob = createIntervalJob()
            }
        }
    }

    private fun createIntervalJob(): Job? {
        return if (batchInterval <= 0) {
            null
        } else client.scope.launch {
            delay(batchInterval)
            channel.send(Unit)
        }
    }

    final override fun flush() {
        client.scope.launch {
            channel.send(Unit)
        }
    }

    abstract suspend fun doFlush()

    override fun close() {
        channel.close()
    }
}

class MemBatchReporter<T>(
    private val client: ReportClient<T>,
    private val batchCount: Int,
    batchInterval: Long,
    private val transporter: ListReportTransporter<T>
) : IntervalBatchReporter<T>(client, batchInterval) {

    @Volatile
    private var reportList = mutableListOf<T>()
    private val mutex = Mutex()

    override fun report(msg: T) {
        client.scope.launch {
            val shouldFlush = mutex.withLock {
                reportList.add(msg)
                reportList.size >= batchCount
            }
            if (shouldFlush) {
                flush()
            }
        }
    }

    override fun batchReport(list: List<T>) {
        client.scope.launch {
            list.forEach {
                val shouldFlush = mutex.withLock {
                    reportList.add(it)
                    reportList.size >= batchCount
                }
                if (shouldFlush) {
                    flush()
                }
            }
        }
    }

    override suspend fun doFlush() {
        val toFlush = mutex.withLock {
            val local = reportList
            reportList = mutableListOf()
            local
        }
        if (toFlush.isNotEmpty()) {
            transporter.transport(client, toFlush, ReportStrategy.MemBach)
        }
    }
}

class FileBatchReporter<T>(
    context: Context,
    private val client: ReportClient<T>,
    batchInterval: Long,
    private val converter: ReportMsgConverter<T>,
    private val transporter: StreamReportTransporter<T>,
    dirName: String = "emo-report",
    private val fileSize: Long = 150 * 1024
) : IntervalBatchReporter<T>(client, batchInterval), LogTag {

    private val applicationContext = context.applicationContext
    private val rootDir = File(applicationContext.filesDir, dirName).apply {
        mkdirs()
    }

    private val dir = File(rootDir, applicationContext.currentSimpleProcessName()).apply {
        mkdirs()
    }

    private val mutex = Mutex()
    private var currentFile = newFile()
    private var currentWriter = currentFile.createReportSink<T>(fileSize)
    private val transportChannel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)
    private val blockTransportUntil = AtomicLong(0)

    init {
        client.scope.launch(Dispatchers.IO) {

            val deleteFailedFileList = mutableListOf<File>()

            for (i in transportChannel) {
                if (blockTransportUntil.get() > SystemClock.elapsedRealtime()) {
                    continue
                }
                if (deleteFailedFileList.isNotEmpty()) {
                    for (j in deleteFailedFileList.size - 1 downTo 0) {
                        if (deleteFailedFileList[j].delete()) {
                            deleteFailedFileList.removeAt(j)
                        }
                    }
                }
                if (!NetworkConnectivity.of(applicationContext).getNetworkState().isConnected) {
                    continue
                }
                kotlin.runCatching {
                    val newestTime = currentFile.name.split("-")[2].toLong()
                    val list = dir.listFiles(
                        FileFilter { file ->
                            if (file.length() <= 0) {
                                return@FileFilter false
                            }
                            val parts = file.name.split("-")
                            if (parts.size != 3) {
                                return@FileFilter false
                            }

                            if (deleteFailedFileList.find { it.name == file.name } != null) {
                                return@FileFilter false
                            }

                            val process = parts[1].toInt()
                            if (Process.myPid() != process) {
                                true
                            } else {
                                val time = parts[2].toLong()
                                time < newestTime
                            }
                        }
                    )
                    if (list != null && list.isNotEmpty()) {
                        for (file in list) {
                            if (!NetworkConnectivity.of(applicationContext).getNetworkState().isConnected) {
                                break
                            }
                            val source = file.createReportSource<T>()
                            val success = kotlin.runCatching {
                                source.read(client, transporter, converter)
                                true
                            }.getOrDefault(false)
                            source.close()
                            if (success) {
                                if (!file.delete()) {
                                    // handle if MappedByteBuffer not recycled.
                                    deleteFailedFileList.add(file)
                                } else {
                                    EmoLog.w(TAG, "delete file(${file.name}) failed.")
                                }
                            }
                        }
                        transporter.flush(client, ReportStrategy.FileBatch)
                    }
                }
            }
        }
        client.scope.launch {
            transportChannel.send(Unit)
        }
    }

    fun blockTransport(time: Long) {
        blockTransportUntil.set(SystemClock.elapsedRealtime() + time)
    }

    private fun newFile(): File {
        return File(dir, "report-${Process.myPid()}-${System.currentTimeMillis()}")
    }

    override fun batchReport(list: List<T>) {
        client.scope.launch {
            list.forEach {
                doReport(it)
            }
        }
    }

    override fun report(msg: T) {
        client.scope.launch {
            doReport(msg)
        }
    }

    private suspend fun doReport(msg: T) {
        try {
            var ret = mutex.withLock {
                currentWriter.write(msg, converter)
            }
            if (!ret) {
                doFlush()
                ret = mutex.withLock {
                    currentWriter.write(msg, converter)
                }
                if (!ret) {
                    EmoLog.w(TAG, "msg is too big, failed to write to file failed: $msg")
                }
            }
        } catch (e: IOException) {
            doFlush()
            // try again
            mutex.withLock {
                currentWriter.write(msg, converter)
            }
        }
    }

    override suspend fun doFlush() {
        mutex.withLock {
            currentWriter.flush()
            currentWriter.close()
            currentFile = newFile()
            currentWriter = currentFile.createReportSink(fileSize)
            currentFile
        }
        // send to another channel to avoid file being filled.
        transportChannel.send(Unit)
    }

    override fun close() {
        transportChannel.close()
        client.scope.launch {
            mutex.withLock {
                currentWriter.flush()
                currentWriter.close()
            }
        }
    }
}
