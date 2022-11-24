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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

class ConcurrencyShare(
    private val successResultKeepTime: Long = 5 * 1000,
    private val timeoutByCancellation: Long = 300,
    private val scope: CoroutineScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + coroutineLogExceptionHandler("ConcurrencyShare")
    )
) {
    companion object {
        val globalInstance by lazy {
            ConcurrencyShare()
        }
    }

    private val caches = ConcurrentHashMap<String, Item<*>>()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> joinPreviousOrRun(key: String, block: suspend CoroutineScope.() -> T): T {
        while (true) {
            val activeTask = caches[key] ?: break
            if (activeTask.task.isCancelled) {
                yield()
                continue
            }

            if (activeTask.task.isCompleted) {
                return activeTask.task.getCompleted() as T
            }

            val counter = activeTask.counter.get()
            if (counter < 0) {
                yield()
                continue
            }
            if (activeTask.counter.compareAndSet(counter, counter + 1)) {
                return awaitItem(activeTask) as T
            } else {
                yield()
            }
        }

        val contextInfo = coroutineContext.minusKey(Job)
        val newTask = scope.async(start = CoroutineStart.LAZY) {
            withContext(contextInfo) {
                block()
            }
        }
        val item = Item(newTask)
        invokeWhenCompletion(key, item)

        while (true) {
            val otherTask = caches.putIfAbsent(key, item)
            if (otherTask != null) {
                if (otherTask.task.isCancelled) {
                    yield()
                    continue
                }

                if (otherTask.task.isCompleted) {
                    return otherTask.task.getCompleted() as T
                }

                val counter = otherTask.counter.get()
                if (counter < 0) {
                    yield()
                } else if (otherTask.counter.compareAndSet(counter, counter + 1)) {
                    newTask.cancel()
                    return awaitItem(otherTask) as T
                } else {
                    yield()
                }
            } else {
                return awaitItem(item)
            }
        }
    }

    suspend fun <T> cancelPreviousThenRun(key: String, block: suspend CoroutineScope.() -> T): T {
        val contextInfo = coroutineContext.minusKey(Job)
        val newTask = scope.async(start = CoroutineStart.LAZY) {
            withContext(contextInfo) {
                block()
            }
        }
        val item = Item(newTask)
        invokeWhenCompletion(key, item)
        val oldTask = caches.put(key, item)
        oldTask?.task?.cancelAndJoin()
        return awaitItem(item)
    }

    private suspend fun <T> awaitItem(item: Item<T>): T {
        try {
            return item.task.await()
        } finally {
            val count = item.counter.decrementAndGet()
            if (count == 0 && item.task.isActive) {
                scope.launch {
                    delay(timeoutByCancellation)
                    if (item.counter.compareAndSet(0, -1) && item.task.isActive) {
                        item.task.cancel()
                    }
                }
            }
        }
    }

    private fun invokeWhenCompletion(key: String, item: Item<*>) {
        item.task.invokeOnCompletion {
            if (it != null) {
                caches.remove(key, item)
            } else {
                scope.launch {
                    delay(successResultKeepTime)
                    caches.remove(key, item)
                }
            }
        }
    }

    private class Item<T>(
        val task: Deferred<T>,
        val counter: AtomicInteger = AtomicInteger(1)
    )
}
