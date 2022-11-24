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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ListReportTransporter<T> {
    suspend fun transport(
        client: ReportClient<T>,
        batch: List<T>,
        usedStrategy: ReportStrategy
    ): Boolean
}

interface StreamReportTransporter<T> {

    suspend fun transport(
        client: ReportClient<T>,
        buffer: ByteArray,
        offset: Int,
        len: Int,
        converter: ReportMsgConverter<T>,
        usedStrategy: ReportStrategy
    )

    suspend fun flush(client: ReportClient<T>, usedStrategy: ReportStrategy)
}

internal class ListToStreamTransporterAdapter<T>(
    private val delegate: ListReportTransporter<T>,
    private val batchCount: Int = 50
) : StreamReportTransporter<T> {

    private var list = mutableListOf<T>()
    private val mutex = Mutex()

    override suspend fun transport(
        client: ReportClient<T>,
        buffer: ByteArray,
        offset: Int,
        len: Int,
        converter: ReportMsgConverter<T>,
        usedStrategy: ReportStrategy
    ) {
        val batchTransport = mutex.withLock {
            list.add(converter.decode(buffer, offset, len))
            if (list.size >= batchCount) {
                val local = list
                list = mutableListOf()
                local
            } else {
                null
            }
        }

        batchTransport?.let {
            delegate.transport(client, it, usedStrategy)
        }
    }

    override suspend fun flush(client: ReportClient<T>, usedStrategy: ReportStrategy) {
        val ret = mutex.withLock {
            val local = list
            list = mutableListOf()
            local
        }
        delegate.transport(client, ret, usedStrategy)
    }
}

internal class StreamToListTransporterAdapter<T>(
    private val delegate: StreamReportTransporter<T>,
    private val converter: ReportMsgConverter<T>
) : ListReportTransporter<T> {

    override suspend fun transport(
        client: ReportClient<T>,
        batch: List<T>,
        usedStrategy: ReportStrategy
    ): Boolean {
        batch.forEach {
            val buffer = converter.encode(it)
            delegate.transport(client, buffer, 0, buffer.size, converter, usedStrategy)
        }
        delegate.flush(client, usedStrategy)
        return true
    }
}

fun <T> ListReportTransporter<T>.wrapToStreamTransporter(batchCount: Int): StreamReportTransporter<T> {
    return ListToStreamTransporterAdapter(this, batchCount)
}

fun <T> StreamReportTransporter<T>.wrapToListTransporter(
    converter: ReportMsgConverter<T>
): ListReportTransporter<T> {
    return StreamToListTransporterAdapter(this, converter)
}
