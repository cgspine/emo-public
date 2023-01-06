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

package cn.qhplus.emo.kv

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.cn/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class KVTest {

    private val KEY_PREFIX = "key_asdfdfdsafjdsklafj_"
    private val VALUE_SUFFIX = ".emo 是根据功能独立了非常多的子库，没有统一的引入方式，你需要按需引入。" +
        "在每篇文档的开始，都会给出其具体依赖的引入方式。所以去查看左边的目录，看看有没有自己想要的功能吧。"

    @Test
    fun simple_kv_write_read() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emoKV = EmoKV(appContext, "test") { key, e ->
            Log.i("EmoKV", e.message ?: "")
            true
        }
        val v = "test"
        emoKV.put("hehe", v)
        assertEquals(v, emoKV.getString("hehe"))
        emoKV.put("xixi", "hehe")
        assertEquals("hehe", emoKV.getString("xixi"))
        emoKV.close()
    }

    @Test
    fun simple_kv_del() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emoKV = EmoKV(appContext, "test") { key, e ->
            Log.i("EmoKV", e.message ?: "")
            true
        }
        val v = "testttttt"
        emoKV.put("hehe", v)
        assertEquals(v, emoKV.getString("hehe"))
        emoKV.delete("hehe")
        assertEquals(null, emoKV.getString("hehe"))
        emoKV.close()
    }

    @Test
    fun w_kv_write_read() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emoKV = EmoKV(appContext, "test2", indexInitSpace = 4096 * 128, crc = false, compress = false) { key, e ->
            Log.i("EmoKV", e.message ?: "")
            true
        }
        for (i in 0 until 10000) {
            emoKV.put("$KEY_PREFIX$i", "$i$VALUE_SUFFIX")
        }
        for (i in 0 until 10000) {
            val ret = emoKV.getString("$KEY_PREFIX$i")
            assertEquals(ret, "$i$VALUE_SUFFIX")
        }
        emoKV.close()
    }

    @Test
    fun testEmoKvReadSingleThread() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        val kv = EmoKV(appContext, "test2", indexInitSpace = 4096 * 128, crc = false, compress = false)
        for (i in 0 until count) {
            val ret = kv.getString("$KEY_PREFIX$i")
            if (ret != "$i$VALUE_SUFFIX") {
                throw RuntimeException("not matched")
            }
        }
        kv.close()
    }

    @Test
    fun testEmoKvReadMultiThread() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val count = 10000
        runTest {
            val kv = EmoKV(appContext, "test")
            buildList<Job>(10) {
                for (i in 0 until 10) {
                    val eachSize = count / 10
                    launch {
                        for (j in 0 until eachSize) {
                            val n = j * (i + 1)
                            val ret = kv.getString("test_a_b_c_$n")
                            if (ret != "abc$n,bcd$n,eee$n") {
                                throw RuntimeException("not matched")
                            }
                            val nn = n + 3
                            kv.put("test_a_b_c_$nn", "abc$nn,bcd$nn,eee$nn")
                        }
                    }.let {
                        add(it)
                    }
                }
            }.forEach {
                it.join()
            }
            kv.close()
        }
    }
}
