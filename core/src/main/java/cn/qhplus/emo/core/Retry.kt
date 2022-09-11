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

import kotlinx.coroutines.delay
import java.util.concurrent.CancellationException

fun <T> retry(times: Int, block: () -> T): T {
    var throwable: Throwable? = null
    for (i in 0 until times) {
        try {
            return block()
        } catch (e: Throwable) {
            throwable = e
            if (e is CancellationException) {
                break
            }
        }
    }
    throw throwable ?: RuntimeException("failed after retry $times times")
}

suspend fun <T> retry2(times: Int, failedDelay: Long = 0, block: suspend () -> T): T {
    var throwable: Throwable? = null
    for (i in 0 until times) {
        try {
            return block()
        } catch (e: Throwable) {
            throwable = e
            if (e is CancellationException) {
                break
            }
        }
        if (failedDelay > 0) {
            delay(failedDelay)
        }
    }
    throw throwable ?: RuntimeException("failed after retry $times times")
}
