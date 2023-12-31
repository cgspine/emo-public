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

package cn.qhplus.emo.network

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.cn/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testExponentialMovingAverage() {
        val ema = ExponentialMovingAverage(0.2f)
        ema.addMeasurement(1000.0)
        ema.addMeasurement(1300.0)
        ema.addMeasurement(1200.0)

        assertEquals(1080.8836465825684, ema.getAverage(), 1.0)
    }
}
