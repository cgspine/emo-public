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

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.cn/tools/testing).
 */

private const val TAG = "EmoReport"

@RunWith(AndroidJUnit4::class)
class EmoReportTest {
    @Test
    fun test_immediate_report() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emoReport = emoSimpleReport {
            scope = this@runTest
            context = appContext
            listReportTransporter = TestListReportTrans()
            streamReportTransporter = TestStreamReportTrans()
        }

        Log.i(TAG, "===== test_immediate_report =====")
        emoReport.report("immediate", ReportStrategy.Immediately)
        advanceUntilIdle()
    }

    @Test
    fun test_mem_batch_report() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emoReport = emoSimpleReport {
            scope = this@runTest
            context = appContext
            listReportTransporter = TestListReportTrans()
            streamReportTransporter = TestStreamReportTrans()
            batchInterval = 5 * 1000
            memBatchCount = 3
        }

        Log.i(TAG, "====== test_mem_batch_report =====")
        launch {
            for (i in 0 until 4) {
                delay(50)
                emoReport.report("a$i", ReportStrategy.MemBach)
            }
        }
        launch {
            for (i in 0 until 4) {
                delay(100)
                emoReport.report("b$i", ReportStrategy.MemBach)
            }
        }
        advanceUntilIdle()
    }

    @Test
    fun test_file_batch_report() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emoReport = emoSimpleReport {
            scope = this@runTest
            context = appContext
            listReportTransporter = TestListReportTrans()
            streamReportTransporter = TestStreamReportTrans()
            batchInterval = 0 // test delay not work, so give up!
            fileBatchFileSize = 50
        }

        Log.i(TAG, "====== test_mem_batch_report =====")
        launch {
            for (i in 0 until 3) {
                delay(50)
                emoReport.report("aaaaaaaaa$i", ReportStrategy.FileBatch)
            }
        }
        launch {
            for (i in 0 until 3) {
                delay(100)
                emoReport.report("bbbbbbbbbb$i", ReportStrategy.FileBatch)
            }
        }
        advanceUntilIdle()
    }

    @Test
    fun test_file_batch_report_with_list_transporter() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emoReport = emoSimpleReport {
            scope = this@runTest
            context = appContext
            listReportTransporter = TestListReportTrans()
//            streamReportTransporter = TestStreamReportTrans()
            batchInterval = 0 // test delay not work, so give up!
            fileBatchFileSize = 50
        }

        Log.i(TAG, "====== test_file_batch_report_with_list_transporter =====")
        launch {
            for (i in 0 until 3) {
                delay(50)
                emoReport.report("aaaaaaaaa$i", ReportStrategy.FileBatch)
            }
        }
        launch {
            for (i in 0 until 3) {
                delay(100)
                emoReport.report("bbbbbbbbbb$i", ReportStrategy.FileBatch)
            }
        }
        advanceUntilIdle()
    }
}

class TestListReportTrans : ListReportTransporter<String> {

    override suspend fun transport(batch: List<String>): Boolean {
        Log.i(TAG, "listTransport: count = ${batch.count()}, " + batch.joinToString(","))
        return true
    }
}

class TestStreamReportTrans : StreamReportTransporter<String> {
    override suspend fun transport(buffer: ByteArray, offset: Int, len: Int, converter: ReportMsgConverter<String>) {
        Log.i(TAG, "streamTransport:" + converter.decode(buffer, offset, len))
    }

    override suspend fun flush() {
        Log.i(TAG, "streamTransport:flush")
    }
}
