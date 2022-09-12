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

package cn.qhplus.emo.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Test
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class ConcurrencyShareTest {

    @Test
    fun joinPreviousOrRunTest() = runTest {
        val key = "key"
        val testContext = CoroutineScope(SupervisorJob() + TestContext())
        val concurrencyShare = ConcurrencyShare(scope = this)
        launch {
            withContext(testContext.coroutineContext) {
                val a = concurrencyShare.joinPreviousOrRun(key) {
                    delay(300)
                    Assert.assertEquals("test", coroutineContext[TestContext]?.test)
                    "a"
                }
                Assert.assertEquals("a", a)
            }
        }

        launch {
            delay(100)
            val b = concurrencyShare.joinPreviousOrRun(key) {
                delay(1000)
                "b"
            }
            Assert.assertEquals("a", b)
        }
    }

    @Test
    fun cancelPreviousThenRunTest() = runTest {
        val key = "key"
        val concurrencyShare = ConcurrencyShare(scope = this)
        val a = launch {
            val ret = concurrencyShare.cancelPreviousThenRun(key) {
                delay(300)
                "a"
            }
            Assert.assertEquals("No reachable code", ret)
        }

        delay(100)
        val b = concurrencyShare.cancelPreviousThenRun(key) {
            delay(1000)
            "b"
        }
        Assert.assertEquals(true, a.isCancelled)
        Assert.assertEquals("b", b)
    }

    class TestContext : AbstractCoroutineContextElement(TestContext) {

        companion object TestContext : CoroutineContext.Key<ConcurrencyShareTest.TestContext>

        val test = "test"
    }

    /**
     *  Two task A, B. cancellation of A should not affect task B
     */
    @Test
    fun joinPreviousOrRunCancelTest() = runTest {
        val key = "key"
        val concurrencyShare = ConcurrencyShare(scope = this)
        val a = launch() {
            val a = concurrencyShare.joinPreviousOrRun(key) {
                delay(1000)
                "a"
            }
            Assert.assertEquals("No reachable code", a)
        }

        val b = launch {
            delay(100)
            val b = concurrencyShare.joinPreviousOrRun(key) {
                delay(200)
                "b"
            }
            Assert.assertEquals("a", b)
        }

        delay(100)
        a.cancelAndJoin()
        Assert.assertEquals(false, b.isCancelled)
        advanceUntilIdle()
    }

    /**
     *  Only one task A. cancellation of A should cancel inner task.
     */
    @Test
    fun joinPreviousOrRunOnlyOneCancelTest() = runTest{
        val key = "key"
        val concurrencyShare = ConcurrencyShare(scope = this)
        val a = launch {
            val ret = concurrencyShare.joinPreviousOrRun(key) {
                Assert.assertTrue(true)
                delay(1000)
                Assert.assertEquals("No reachable code", key)
                "a"
            }
            Assert.assertEquals("No reachable code", ret)
        }

        delay(300)
        a.cancelAndJoin()
        advanceUntilIdle()
    }
}
