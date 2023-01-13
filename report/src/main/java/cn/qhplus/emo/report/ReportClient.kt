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
import cn.qhplus.emo.core.coroutineLogExceptionHandler
import cn.qhplus.emo.network.NetworkConnectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

interface ReportClient<T> {
    val scope: CoroutineScope
    fun report(msg: T, strategy: ReportStrategy = ReportStrategy.FileBatch) {
        batchReport(listOf(msg), strategy)
    }

    fun batchReport(list: List<T>, strategy: ReportStrategy = ReportStrategy.FileBatch)
    fun flush()
    fun destroy()
}

// For Test
class DummyReportClient<T>(override val scope: CoroutineScope) : ReportClient<T> {

    override fun report(msg: T, strategy: ReportStrategy) {
    }

    override fun batchReport(list: List<T>, strategy: ReportStrategy) {
    }

    override fun flush() {
    }

    override fun destroy() {
    }
}

class EmoReportClient<T> internal constructor(
    override val scope: CoroutineScope,
    context: Context,
    listReportTransporter: ListReportTransporter<T>,
    streamReportTransporter: StreamReportTransporter<T>,
    val converter: ReportMsgConverter<T>,
    val batchInterval: Long,
    val memBatchCount: Int,
    val fileBatchDirName: String,
    val fileBatchFileSize: Long
) : ReportClient<T> {

    private val applicationContext = context.applicationContext

    private val immediatelyReporter by lazy {
        ImmediatelyReporter(this, listReportTransporter)
    }
    private val memBatchReporter by lazy {
        MemBatchReporter(this, memBatchCount, batchInterval, listReportTransporter)
    }

    private val fileBatchReporter by lazy {
        FileBatchReporter(
            applicationContext,
            this,
            batchInterval,
            converter,
            streamReportTransporter,
            fileBatchDirName,
            fileBatchFileSize
        )
    }

    override fun report(msg: T, strategy: ReportStrategy) {
        if (!NetworkConnectivity.of(applicationContext).getNetworkState().isConnected) {
            fileBatchReporter.report(msg)
            return
        }
        when (strategy) {
            ReportStrategy.Immediately -> immediatelyReporter.report(msg)
            ReportStrategy.MemBach -> memBatchReporter.report(msg)
            ReportStrategy.FileBatch -> fileBatchReporter.report(msg)
            ReportStrategy.WriteBackBecauseOfFailed -> {
                fileBatchReporter.blockTransport(30 * 1000)
            }
        }
    }

    override fun batchReport(list: List<T>, strategy: ReportStrategy) {
        if (!NetworkConnectivity.of(applicationContext).getNetworkState().isConnected) {
            fileBatchReporter.batchReport(list)
            return
        }
        when (strategy) {
            ReportStrategy.Immediately -> immediatelyReporter.batchReport(list)
            ReportStrategy.MemBach -> memBatchReporter.batchReport(list)
            ReportStrategy.FileBatch -> fileBatchReporter.batchReport(list)
            ReportStrategy.WriteBackBecauseOfFailed -> {
                fileBatchReporter.blockTransport(30 * 1000)
            }
        }
    }

    override fun flush() {
        memBatchReporter.flush()
        fileBatchReporter.flush()
    }

    override fun destroy() {
        fileBatchReporter.close()
        scope.cancel()
    }
}

fun <T> newReportClient(
    context: Context,
    listReportTransporter: ListReportTransporter<T>,
    converter: ReportMsgConverter<T>,
    scope: CoroutineScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.IO +
            coroutineLogExceptionHandler("ReportClient")
    ),
    streamReportTransporter: StreamReportTransporter<T>? = null,
    batchInterval: Long = 5 * 60 * 1000,
    memBatchCount: Int = 50,
    fileBatchDirName: String = "report",
    fileBatchFileSize: Long = 150 * 1024
): ReportClient<T> {
    return EmoReportClient(
        scope,
        context,
        listReportTransporter,
        streamReportTransporter ?: listReportTransporter.wrapToStreamTransporter(memBatchCount),
        converter,
        batchInterval,
        memBatchCount,
        fileBatchDirName,
        fileBatchFileSize
    )
}

fun simpleReportClient(
    context: Context,
    listReportTransporter: ListReportTransporter<String>,
    converter: ReportMsgConverter<String> = ReportStringMsgConverter,
    scope: CoroutineScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.IO +
            coroutineLogExceptionHandler("ReportClient")
    ),
    streamReportTransporter: StreamReportTransporter<String>? = null,
    batchInterval: Long = 5 * 60 * 1000,
    memBatchCount: Int = 50,
    fileBatchDirName: String = "report",
    fileBatchFileSize: Long = 150 * 1024
): ReportClient<String> {
    return newReportClient(
        context,
        listReportTransporter,
        converter,
        scope,
        streamReportTransporter,
        batchInterval,
        memBatchCount,
        fileBatchDirName,
        fileBatchFileSize
    )
}
