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

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TAG = "ReporterFile"

@RunWith(AndroidJUnit4::class)
class ReporterFileTest {

    @Test
    fun test_normal_read_write() = runTest {
        test(
            client = DummyReportClient(this@runTest),
            sinkProvider = { file, size ->
                file.createReportSink(size)
            },
            sourceProvider = { file ->
                file.createReportSource()
            }
        )
        advanceUntilIdle()
    }

    @Test
    fun test_stream_write_read() = runTest {
        test(
            client = DummyReportClient(this@runTest),
            sinkProvider = { file, size ->
                ReporterStreamSink(file, size)
            },
            sourceProvider = { file ->
                ReporterStreamSource(file.inputStream().buffered())
            }
        )
        advanceUntilIdle()
    }

    @Test
    fun test_speed() = runTest {
        val list = mutableListOf<Speed>()
        repeat(100) {
            list += speed(
                client = DummyReportClient(this@runTest),
                sinkProvider = { file, size ->
                    file.createReportSink(size)
                },
                sourceProvider = { file ->
                    file.createReportSource()
                }
            )
        }
        val avgCal = AvgCal(0, 0, 0, 0)
        list.fold(avgCal) { cal, item ->
            cal.read += item.read
            cal.write += item.write
            cal.total += item.total
            cal.count += 1
            cal
        }
        Log.i(TAG, "normal: ${avgCal.cal()}")
        avgCal.reset()
        list.clear()
        repeat(100) {
            list += speed(
                client = DummyReportClient(this@runTest),
                sinkProvider = { file, size ->
                    ReporterStreamSink(file, size)
                },
                sourceProvider = { file ->
                    ReporterStreamSource(file.inputStream().buffered())
                }
            )
        }
        list.fold(avgCal) { cal, item ->
            cal.read += item.read
            cal.write += item.write
            cal.total += item.total
            cal.count += 1
            cal
        }
        Log.i(TAG, "stream: ${avgCal.cal()}")

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        File(appContext.filesDir, "test").run {
            if (exists()) {
                listFiles()?.forEach {
                    it.delete()
                }
            }
        }
    }

    private suspend fun test(
        client: ReportClient<String>,
        sinkProvider: (File, Long) -> ReporterFileSink<String>,
        sourceProvider: (File) -> ReporterFileSource<String>
    ) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val converter = ReportStringMsgConverter.instance
        val file = File(appContext.filesDir, "test-${System.currentTimeMillis()}")
        val sink = sinkProvider(file, 50)
        var ret = sink.write("aaaaaaaaaaa", converter)
        assert(ret) { "first write failed" }
        ret = sink.write("bbbbbbbbbb", converter)
        assert(ret) { "second write failed" }
        ret = sink.write("cccccccccc", converter)
        assert(!ret) { "third write should fail because of buffer length" }
        sink.flush()
        sink.close()
        val source = sourceProvider(file)
        val transporter = object : StreamReportTransporter<String> {
            private val list = mutableListOf<String>()
            override suspend fun transport(
                client: ReportClient<String>,
                buffer: ByteArray,
                offset: Int,
                len: Int,
                converter: ReportMsgConverter<String>,
                usedStrategy: ReportStrategy
            ) {
                list.add(converter.decode(buffer, offset, len))
            }

            override suspend fun flush(client: ReportClient<String>, usedStrategy: ReportStrategy) {
                assert(list.size == 2)
                assert(list[0] == "aaaaaaaaaaa")
                assert(list[1] == "bbbbbbbbbb")
            }
        }
        source.read(client, transporter, converter)
        transporter.flush(client, ReportStrategy.FileBatch)
        source.close()
    }

    private suspend fun speed(
        client: ReportClient<String>,
        sinkProvider: (File, Long) -> ReporterFileSink<String>,
        sourceProvider: (File) -> ReporterFileSource<String>
    ): Speed {
        val start = SystemClock.elapsedRealtime()
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val converter = ReportStringMsgConverter.instance
        val dir = File(appContext.filesDir, "test").apply {
            mkdirs()
        }
        val file = File(dir, "test-${System.currentTimeMillis()}")
        val sink = sinkProvider(file, 150 * 1024)
        var partStart = SystemClock.elapsedRealtime()
        do {
            val ret = sink.write("aaaaaa".repeat(100), converter)
        } while (ret)
        sink.flush()
        sink.close()
        val write = SystemClock.elapsedRealtime() - partStart
        val source = sourceProvider(file)
        val transporter = object : StreamReportTransporter<String> {
            override suspend fun transport(
                client: ReportClient<String>,
                buffer: ByteArray,
                offset: Int,
                len: Int,
                converter: ReportMsgConverter<String>,
                usedStrategy: ReportStrategy
            ) {
            }

            override suspend fun flush(client: ReportClient<String>,usedStrategy: ReportStrategy) {
            }
        }
        partStart = SystemClock.elapsedRealtime()
        source.read(client, transporter, converter)
        transporter.flush(client, ReportStrategy.FileBatch)
        source.close()
        return Speed(
            SystemClock.elapsedRealtime() - partStart,
            write,
            SystemClock.elapsedRealtime() - start
        )
    }
}

class Speed(val read: Long, val write: Long, val total: Long)

class AvgCal(
    var read: Long,
    var write: Long,
    var total: Long,
    var count: Int
) {

    fun reset() {
        read = 0
        write = 0
        total = 0
        count = 0
    }

    fun cal(): String {
        if (count == 0) {
            return "null"
        }
        return "read = ${read / count}, write = ${write / count}, total = ${total / count}"
    }
}
