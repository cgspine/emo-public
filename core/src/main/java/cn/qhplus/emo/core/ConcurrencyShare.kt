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

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class ConcurrencyShare(
    scopeErrorHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        EmoLog.e("ConcurrencyShare", "scope error.", throwable)
    }
) {
    companion object {
        val globalInstance by lazy {
            ConcurrencyShare()
        }
    }

    private val caches = ConcurrentHashMap<String, Deferred<*>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + scopeErrorHandler)

    // Notice:
    // this job will run in a inner scope, so cancel parent job will not cancel this job.
    // because it may be reused by other coroutines.
    suspend fun <T> joinPreviousOrRun(key: String, block: suspend CoroutineScope.() -> T): T {
        val activeTask = caches[key] as? Deferred<T>
        activeTask?.let {
            return it.await()
        }

        val keepContext = coroutineContext
        val newTask = scope.async(start = CoroutineStart.LAZY) {
            withContext(keepContext) {
                block()
            }
        }
        newTask.invokeOnCompletion {
            caches.remove(key, newTask)
        }

        val otherTask = caches.putIfAbsent(key, newTask) as? Deferred<T>
        try {
            return if (otherTask != null) {
                newTask.cancel()
                otherTask.await()
            } else {
                newTask.await()
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                newTask.onAwait
            }
            throw e
        }
    }

    suspend fun <T> cancelPreviousThenRun(key: String, block: suspend CoroutineScope.() -> T): T = supervisorScope {
        val newTask = async(start = CoroutineStart.LAZY, block = block)
        newTask.invokeOnCompletion {
            caches.remove(key, newTask)
        }
        val oldTask = caches.put(key, newTask) as? Deferred<T>
        oldTask?.cancelAndJoin()
        newTask.await()
    }

    fun destroy() {
        scope.cancel()
    }
}
