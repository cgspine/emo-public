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
    suspend fun transport(batch: List<T>): Boolean
}

interface StreamReportTransporter<T> {

    suspend fun transport(
        buffer: ByteArray,
        offset: Int,
        len: Int,
        converter: ReportMsgConverter<T>
    )

    suspend fun flush()
}

internal class ListToStreamTransporterAdapter<T>(
    private val delegate: ListReportTransporter<T>
) : StreamReportTransporter<T> {

    private var list = mutableListOf<T>()
    private val mutex = Mutex()

    override suspend fun transport(buffer: ByteArray, offset: Int, len: Int, converter: ReportMsgConverter<T>) {
        mutex.withLock {
            list.add(converter.decode(buffer, offset, len))
        }
    }

    override suspend fun flush() {
        val ret = mutex.withLock {
            val local = list
            list = mutableListOf()
            local
        }
        delegate.transport(ret)
    }
}

fun <T> ListReportTransporter<T>.wrapToStreamTransporter(): StreamReportTransporter<T> {
    return ListToStreamTransporterAdapter(this)
}
