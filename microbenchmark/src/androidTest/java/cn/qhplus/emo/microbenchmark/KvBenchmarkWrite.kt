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


import androidx.core.content.ContextCompat.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.cn/tools/testing).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class KvBenchmarkWrite {

    // I have no idea microbenchmark can not run.......

    @Test
    fun measureEmoKvWrite() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        KvRW.emoKvWrite(appContext, count)
        Mem.logCurrent(appContext, "measureEmoKvWrite")
    }

    @Test
    fun measureMmkvWrite() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        KvRW.mmkvWrite(appContext, count)
        Mem.logCurrent(appContext, "measureMmkvWrite")
    }

    @Test
    fun measureLevelDbWrite() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        KvRW.levelDbWrite(appContext, count)
        Mem.logCurrent(appContext, "measureLevelDbWrite")
    }
}
