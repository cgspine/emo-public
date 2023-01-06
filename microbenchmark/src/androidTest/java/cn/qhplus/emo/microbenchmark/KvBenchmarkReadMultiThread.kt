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
package cn.qhplus.emo.microbenchmark


import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.cn/tools/testing).
 */
@LargeTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class KvBenchmarkReadMultiThread {

    @Test
    fun measureEmoKvReadMultiThread() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        runTest {
            KvRW.emoKvReadMultiThread(this, appContext, count)
        }
        Mem.logCurrent(appContext, "measureEmoKvReadMultiThread")
    }

    @Test
    fun measureMmkvReadMultiThread() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        runTest {
            KvRW.mmkvReadMultiThread(this, appContext, count)
        }
        Mem.logCurrent(appContext, "measureMmkvReadMultiThread")
    }

    @Test
    fun measureLevelDbReadMultiThread() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        runTest {
            KvRW.levelDbReadMultiThread(this, appContext, count)
        }
        Mem.logCurrent(appContext, "measureLevelDbReadMultiThread")
    }
}
