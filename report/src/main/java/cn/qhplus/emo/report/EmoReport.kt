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
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.network.NetworkConnectivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

private val defaultCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    EmoLog.e("EmoReport", "scope error.", throwable)
}

class EmoReport<T> internal constructor(
    val scope: CoroutineScope,
    context: Context,
    listReportTransporter: ListReportTransporter<T>,
    streamReportTransporter: StreamReportTransporter<T>,
    val converter: ReportMsgConverter<T>,
    val batchInterval: Long,
    val memBatchCount: Int,
    val fileBatchDirName: String,
    val fileBatchFileSize: Long
) {

    private val applicationContext = context.applicationContext

    private val immediatelyReporter by lazy {
        ImmediatelyReporter(scope, listReportTransporter)
    }
    private val memBatchReporter by lazy {
        MemBatchReporter(scope, memBatchCount, batchInterval, listReportTransporter)
    }

    private val fileBatchReporter by lazy {
        FileBatchReporter(
            applicationContext,
            scope,
            batchInterval,
            converter,
            streamReportTransporter,
            fileBatchDirName,
            fileBatchFileSize
        )
    }

    fun report(msg: T, strategy: ReportStrategy = ReportStrategy.FileBatch) {
        if (!NetworkConnectivity.of(applicationContext).getNetworkState().isConnected) {
            fileBatchReporter.report(msg)
            return
        }
        when (strategy) {
            ReportStrategy.Immediately -> immediatelyReporter.report(msg)
            ReportStrategy.MemBach -> memBatchReporter.report(msg)
            ReportStrategy.FileBatch -> fileBatchReporter.report(msg)
        }
    }

    fun destroy() {
        fileBatchReporter.close()
        scope.cancel()
    }
}

fun <T> emoReport(init: EmoReportBuilder<T>.() -> Unit): EmoReport<T> {
    val builder = EmoReportBuilder<T>()
    builder.init()
    val context = builder.context ?: throw RuntimeException("context is required!")
    val listReportTransporter = builder.listReportTransporter ?: throw RuntimeException("listReportTransporter is required!")
    val msgContentConverter = builder.converter ?: throw RuntimeException("converter is required!")
    val scope = builder.scope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO + defaultCoroutineExceptionHandler)

    val streamReportTransporter = builder.streamReportTransporter ?: listReportTransporter.wrapToStreamTransporter(builder.memBatchCount)
    return EmoReport(
        scope,
        context,
        listReportTransporter,
        streamReportTransporter,
        msgContentConverter,
        builder.batchInterval,
        builder.memBatchCount,
        builder.fileBatchDirName,
        builder.fileBatchFileSize
    )
}

fun emoSimpleReport(init: EmoReportBuilder<String>.() -> Unit): EmoReport<String> {
    return emoReport {
        init()
        if (converter == null) {
            converter = ReportStringMsgConverter.instance
        }
    }
}

class EmoReportBuilder<T> internal constructor() {
    var context: Context? = null
    var scope: CoroutineScope? = null
    var listReportTransporter: ListReportTransporter<T>? = null
    var streamReportTransporter: StreamReportTransporter<T>? = null
    var converter: ReportMsgConverter<T>? = null
    var batchInterval: Long = 5 * 60 * 1000
    var memBatchCount: Int = 50
    var fileBatchDirName: String = "emo-report"
    var fileBatchFileSize: Long = 150 * 1024
}
